# Plan de corrección — Hallazgos del testeo en Maestros (Integrante A: Joaquín)

**Fecha:** 17/11/2026  
**Dependencia:** informe de testeo `docs/test_joaco.md` (hallazgos H-01, H-02, H-06 y H-08).  
**Objetivo:** corregir los tres errores de lógica del módulo Maestros que pueden aparecer en la demo, sin cambiar el esquema, los DAO ni los archivos del módulo Stock.

---

## 1. Alcance

| Corrección | Hallazgo | Archivo | Tipo |
|---|---|---|---|
| Modificar un producto de una categoría inactiva | H-01 | `servicio/ProductoServicio.java` | Contradice la regla 9 de `modelo_relacional_mvp.md` §5.3 |
| Precios que la base no guarda tal cual | H-08 y H-06 (precio fuera de rango) | `servicio/ProductoServicio.java` | Validación faltante |
| Cancelar un alta | H-02 | `ui/MenuMaestros.java` | Usabilidad que crea datos no deseados |

Quedan fuera de este plan:

| Hallazgo | Motivo |
|---|---|
| H-03 y H-04 | Son de `StockServicio.definirMinimo`, archivo de Santiago. Se le pasan por separado |
| H-05, H-07, H-09 y H-10 | De presentación o de usabilidad. No afectan la lógica ni la demo |
| H-06 (duplicados) | El mensaje genérico es correcto, solo poco específico |

No cambian el esquema, los scripts SQL, las interfaces DAO ni `Main.java`.

---

## 2. H-01 — Modificar un producto de una categoría inactiva

### 2.1 Problema

`ProductoServicio.modificar` (líneas 74-82) bloquea y valida siempre la categoría recibida, aunque sea la misma que ya tiene el producto. La regla 9 de §5.3 dice: «Al dar de alta un producto o cambiarle la categoría, la categoría debe estar activa. Que una categoría pase después a inactiva no impide operar con sus productos». Hoy, después de dar de baja una categoría, ninguno de sus productos se puede editar.

### 2.2 Diseño

Validar la categoría solo cuando cambia. Para saber si cambia, primero hay que leer el producto. Pero si se leyera con bloqueo, se tomaría PRODUCTO antes que CATEGORIA, y eso rompe el orden de §5.4 (CATEGORIA → PRODUCTO → DEPOSITO → STOCK). Por eso se lee dos veces:

| Paso | Lectura | Bloqueo | Para qué |
|---|---|---|---|
| 1 | `productoDao.buscarPorId` | Ninguno | Saber si existe y cuál es su categoría actual |
| 2 | `verificarCategoriaActiva` (solo si la categoría cambia) | CATEGORIA `FOR UPDATE` | Regla 9: la categoría nueva debe estar activa |
| 3 | `productoDao.buscarPorIdConBloqueo` | PRODUCTO `FOR UPDATE` | Leer el último valor confirmado y retenerlo hasta el `COMMIT` |
| 4 | Comprobación de carrera | — | Ver 2.3 |
| 5 | `productoDao.actualizar` y `commit` | — | — |

El orden de bloqueo se mantiene: si hay bloqueo de categoría, se toma antes que el del producto.

### 2.3 Carrera entre el paso 1 y el paso 3

La lectura del paso 1 no bloquea, así que entre los pasos 1 y 3 otra sesión puede cambiarle la categoría al producto. Ejemplo:

1. La sesión A lee el producto en la categoría *Almacén* y el usuario la deja igual. No se valida la categoría.
2. La sesión B pasa el producto a *Bebidas* y confirma.
3. La sesión A bloquea el producto, lo guarda con *Almacén* y lo devuelve a esa categoría sin haber comprobado que siga activa.

Para evitarlo, en el paso 4: si no se validó la categoría y la categoría del producto bloqueado es distinta de la recibida, se rechaza con «Otra sesión cambió la categoría del producto. Volvé a intentarlo.». Si la categoría sí se validó en el paso 2, no hace falta comprobar nada: la nueva categoría está bloqueada y activa.

### 2.4 Código propuesto

`modificar` reutiliza el método privado `verificarCategoriaActiva`, que ya existe y hoy solo usa `crear`. Así desaparece el bloque duplicado de las líneas 74-82.

```java
Producto leido = productoDao.buscarPorId(conn, id);
if (leido == null) {
    throw new ReglaNegocioException("No existe el producto con id " + id + ".");
}
boolean cambiaCategoria = nuevaCategoria != leido.getIdCategoria();
if (cambiaCategoria) {
    verificarCategoriaActiva(conn, nuevaCategoria);        // CATEGORIA FOR UPDATE
}

Producto producto = productoDao.buscarPorIdConBloqueo(conn, id);   // PRODUCTO FOR UPDATE
if (!cambiaCategoria && producto.getIdCategoria() != nuevaCategoria) {
    throw new ReglaNegocioException(
            "Otra sesión cambió la categoría del producto. Volvé a intentarlo.");
}
// asignación de campos, actualizar y commit: sin cambios
```

Después del bloqueo no hace falta comprobar si el producto es `null`: la cuenta `controlstock_app` no tiene `DELETE`, así que un producto que existía en el paso 1 sigue existiendo.

**Cambio de comportamiento:** si no existen ni el producto ni la categoría, ahora se informa primero el producto. Antes se informaba la categoría.

---

## 3. Precios que la base no guarda tal cual (H-08 y H-06)

### 3.1 Problema

`ProductoServicio.validarPrecio` solo rechaza los negativos. Los precios se guardan en `DECIMAL(12,2)`:

- **Más de dos decimales:** `1.555` se guarda como 1.56 sin avisar. MySQL redondea y no da error, ni siquiera en modo estricto. El dato guardado no es el que cargó el usuario.
- **Fuera de rango:** `99999999999` llega a la base y vuelve con el mensaje del driver, en inglés: «Data truncation: Out of range value…».

### 3.2 Diseño

Las dos validaciones se agregan en `validarPrecio`, que ya usan `crear` y `modificar`. Las reglas quedan en el servicio y no se repiten en el menú.

| Validación | Implementación | Mensaje |
|---|---|---|
| Hasta dos decimales | `precio.stripTrailingZeros().scale() > 2` → rechazo | «El precio de referencia de {tipo} no puede tener más de dos decimales.» |
| Máximo de `DECIMAL(12,2)` | `precio.compareTo(PRECIO_MAXIMO) > 0` → rechazo | «El precio de referencia de {tipo} no puede superar 9999999999.99.» |

- Nueva constante: `private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("9999999999.99");`. Son 12 dígitos, 10 enteros y 2 decimales, igual que la columna. Sigue el mismo criterio que `LARGO_MAXIMO_CODIGO` y las demás constantes de largo.
- Se usa `stripTrailingZeros()` para no rechazar valores exactos escritos con ceros de más: `1.500` vale 1.50 y se guarda sin pérdida.
- La notación científica (`1e3`) se sigue aceptando: se guarda como 1000.00, sin pérdida de datos.

**Limitación aceptada:** el rechazo llega al final del alta, después de cargar todos los campos, igual que el resto de las validaciones del servicio (H-10). Repetir la validación en `leerBigDecimal` duplicaría la regla en dos capas.

---

## 4. H-02 — Las altas no se pueden cancelar

### 4.1 Problema

En las altas de categoría, producto y depósito no hay forma de volver sin crear un registro. En los menús, `0` significa «Volver». Si alguien lo escribe en el primer campo con esa intención, se crea un registro llamado «0». En la base anterior al testeo ya había un depósito así.

### 4.2 Diseño

El primer campo de cada alta acepta enter vacío o `0` para cancelar. Los demás campos no cambian.

| Alta | Primer campo | Indicación nueva |
|---|---|---|
| Categoría | Nombre | `Nombre (enter o 0 para cancelar): ` |
| Producto | Código (después de listar las categorías activas) | `Código (enter o 0 para cancelar): ` |
| Depósito | Nombre | `Nombre (enter o 0 para cancelar): ` |

Nuevo método privado en `MenuMaestros`, en la sección «Lectura de datos»:

```java
/** Primer campo de un alta: devuelve null si el usuario cancela con enter vacío o 0. */
private String leerPrimerCampoDeAlta(String campo) {
    System.out.print(campo + " (enter o 0 para cancelar): ");
    String linea = scanner.nextLine().trim();
    return (linea.isEmpty() || linea.equals("0")) ? null : linea;
}
```

Cada alta lo usa así:

```java
String nombre = leerPrimerCampoDeAlta("Nombre");
if (nombre == null) {
    System.out.println("Alta cancelada.\n");
    return;
}
```

**Por qué en la UI y no en el servicio:** cancelar es una decisión de navegación del menú, no una regla de negocio. El servicio sigue rechazando nombres vacíos.

**Consecuencia:** ya no se puede crear una categoría, un producto o un depósito llamado exactamente `0`. Para este dominio no es una pérdida.

---

## 5. Verificación

Requisito: la base con los datos de `04_datos_testeo.sql`, como en el testeo. Los casos repiten los de `test_joaco.md` que fallaron y agregan los límites de cada validación.

| Caso | Entrada | Resultado esperado |
|---|---|---|
| Compilación | `mvn compile test-compile` | Sin errores |
| H-01: modificar el precio de TEM-001 (categoría Temporada, inactiva) | 24, cambiar solo el precio | Modificado |
| H-01: dar de baja Librería y modificar LIB-001 | 6, después 14 | Modificado |
| H-01: pasar un producto a una categoría inactiva | categoría 8 | «La categoría 'Temporada' está inactiva.» |
| H-01: pasar un producto a una categoría inexistente | categoría 99 | «No existe la categoría con id 99.» |
| H-01: cambiar a una categoría activa | ALM-001 → categoría 2 | Modificado |
| H-01: modificar un producto inexistente desde el servicio | id 99 | «No existe el producto con id 99.» |
| Precio con tres decimales | `1.555` | Rechazo por decimales; no se crea el producto |
| Precio con ceros de más | `1.500` | Se guarda 1.50 |
| Precio en el máximo | `9999999999.99` | Se guarda |
| Precio fuera de rango | `99999999999` | Rechazo con el mensaje nuevo, en castellano |
| H-02: cancelar las tres altas con `0` | — | «Alta cancelada.»; no hay registros nuevos |
| H-02: cancelar las tres altas con enter | — | Igual que el anterior |
| Regresión de concurrencia | `PruebaConcurrencia` | Las 26 comprobaciones OK |

La carrera de 2.3 no se puede provocar desde la interfaz: requiere dos sesiones detenidas entre los pasos 1 y 3. Queda cubierta por el diseño, igual que los casos de §5.4 antes de que se escribiera `PruebaConcurrencia`.

Después de verificar hay que **reinstalar la base** con `00` → `01` → `02` → `03` (→ `04`), porque las pruebas escriben datos.

## 6. Documentación a actualizar al terminar

| Archivo | Cambio |
|---|---|
| `docs/plan_modulo_maestros.md` §7 | Fila de progreso con las tres correcciones |
| `docs/test_joaco.md` §4 | Marcar H-01, H-02 y H-08 como corregidos y H-06 como corregido en parte (falta el mensaje de duplicados) |

## 7. Estimación

| Corrección | Tiempo estimado |
|---|---|
| H-01 | 30 minutos |
| Precios | 15 minutos |
| H-02 | 20 minutos |
| Verificación y documentación | 45 minutos |
| **Total** | **Menos de 2 horas** |

## 8. Resultado (17/11/2026)

Implementado y verificado sobre la base del testeo (MySQL 8.4.13):

| Caso | Resultado |
|---|---|
| Compilación | Sin errores |
| Modificar el precio de TEM-001 (Temporada, inactiva) y de LIB-001 (Librería, inactiva) | Modificados |
| Pasar MAS-001 a la categoría 8 (inactiva) y a la 99 (inexistente) | Rechazados con los mensajes esperados |
| Pasar ALM-001 a la categoría 2 y devolverlo a la 1 | Modificado las dos veces |
| Precio `1.555` | «El precio de referencia de compra no puede tener más de 2 decimales.» |
| Precios `1.500` y `9999999999.99` | Guardados como 1.50 y 9999999999.99 |
| Precio `99999999999` | «El precio de referencia de compra no puede superar 9999999999.99.» |
| Cancelar las tres altas con `0` y con enter | Seis veces «Alta cancelada.»; la cantidad de categorías, productos y depósitos no cambió |
| `PruebaConcurrencia` | Todas las comprobaciones OK |

El caso «modificar un producto inexistente desde el servicio» no se puede llegar a probar desde la interfaz, porque el menú comprueba el id contra el listado antes de llamar al servicio. El servicio mantiene el rechazo.
