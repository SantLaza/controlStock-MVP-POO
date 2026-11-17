# Plan de implementación — Módulo Stock (Integrante B: Santiago)

**Fecha:** 11/11/2026  
**Dependencia:** base de código integrada (docs/base_codigo.md).  
**Objetivo:** habilitación de productos en depósitos, definición de stock mínimo, ajustes positivos y negativos.

---

## 1. Archivos a crear

| Capa | Archivo | Responsabilidad |
|---|---|---|
| dao/jdbc | `StockJdbcDao.java` | Implementar `StockDao` con JDBC |
| dao/jdbc | `MovimientoStockJdbcDao.java` | Implementar `MovimientoStockDao` con JDBC |
| servicio | `StockServicio.java` | Reglas de negocio de habilitación, mínimos y ajustes |

## 2. Archivos a modificar

| Archivo | Cambio |
|---|---|
| `ui/MenuStock.java` | Reemplazar el cascarón vacío por el menú completo |
| `Main.java` | Crear los DAOs, servicios y pasarlos a `MenuStock` |

---

## 3. Orden de implementación

### Etapa 1 — StockJdbcDao

Implementar cada método de la interfaz `StockDao`:

| Método | SQL | Notas |
|---|---|---|
| `insertar` | `INSERT INTO STOCK (idProducto, idDeposito, cantidad, stockMinimo) VALUES (?, ?, ?, ?)` | No tiene AUTO_INCREMENT (PK compuesta) |
| `actualizar` | `UPDATE STOCK SET cantidad = ?, stockMinimo = ? WHERE idProducto = ? AND idDeposito = ?` | |
| `buscarPorIds` | `SELECT ... WHERE idProducto = ? AND idDeposito = ?` | Devuelve `null` si no existe |
| `buscarPorIdsConBloqueo` | `SELECT ... WHERE idProducto = ? AND idDeposito = ? FOR UPDATE` | Bloquea la fila para concurrencia |
| `listarPorProducto` | `SELECT ... WHERE idProducto = ? ORDER BY idDeposito` | |
| `listarPorDeposito` | `SELECT ... WHERE idDeposito = ? ORDER BY idProducto` | |
| `listarStockBajo` | `SELECT ... WHERE cantidad < stockMinimo AND stockMinimo > 0` | Solo los que tienen mínimo definido |
| `existeConCantidadPositiva` | `SELECT 1 FROM STOCK WHERE idProducto = ? AND cantidad > 0 LIMIT 1` | Usado por Maestros para baja de producto |
| `existeConCantidadPositivaPorDeposito` | `SELECT 1 FROM STOCK WHERE idDeposito = ? AND cantidad > 0 LIMIT 1` | Usado por Maestros para baja de depósito |

### Etapa 2 — MovimientoStockJdbcDao

| Método | SQL | Notas |
|---|---|---|
| `insertar` | `INSERT INTO MOVIMIENTO_STOCK (idProducto, idDeposito, idUsuario, tipoMovimiento, cantidad, motivo, idMovimientoCorregido) VALUES (?, ?, ?, ?, ?, ?, ?)` | Usar `RETURN_GENERATED_KEYS` |
| `buscarPorId` | `SELECT ... WHERE idMovimiento = ?` | |
| `listarPorProductoDeposito` | `SELECT ... WHERE idProducto = ? AND idDeposito = ? ORDER BY fechaHoraMovimiento DESC` | |
| `listarTodos` | `SELECT ... ORDER BY fechaHoraMovimiento DESC` | |

### Etapa 3 — StockServicio

Este es el servicio más complejo del MVP. Controla transacciones y concurrencia.

**3.1 Habilitar producto en depósito**

```
habilitar(idProducto, idDeposito)
```

| Paso | Detalle |
|---|---|
| 1 | Abrir conexión, `setAutoCommit(false)` |
| 2 | `ProductoDao.buscarPorIdConBloqueo` → verificar que existe y `estaActivo()` |
| 3 | `DepositoDao.buscarPorIdConBloqueo` → verificar que existe y `estaActivo()` |
| 4 | Verificar que no exista ya un Stock para ese par (duplicado) |
| 5 | `StockDao.insertar` con cantidad 0 y stockMinimo 0 |
| 6 | Commit |

Orden de bloqueos: PRODUCTO → DEPOSITO (respeta el orden del plan).

**3.2 Definir stock mínimo**

```
definirMinimo(idProducto, idDeposito, nuevoMinimo)
```

| Paso | Detalle |
|---|---|
| 1 | Abrir conexión, `setAutoCommit(false)` |
| 2 | `StockDao.buscarPorIdsConBloqueo` → verificar que existe la habilitación |
| 3 | Validar que `nuevoMinimo >= 0` |
| 4 | `StockDao.actualizar` con el nuevo mínimo |
| 5 | Commit |

**3.3 Ajuste positivo**

```
ajustarPositivo(idProducto, idDeposito, cantidad, motivo, idUsuario)
```

| Paso | Detalle |
|---|---|
| 1 | Abrir conexión, `setAutoCommit(false)` |
| 2 | Validar: cantidad > 0, motivo no vacío después de trim |
| 3 | `StockDao.buscarPorIdsConBloqueo` → verificar que existe (producto habilitado en depósito) |
| 4 | Sumar la cantidad al saldo: `stock.aumentar(cantidad)` |
| 5 | `StockDao.actualizar` |
| 6 | Crear `MovimientoStock` con tipo `AJUSTE_POSITIVO` |
| 7 | `MovimientoStockDao.insertar` |
| 8 | Commit |

Los pasos 5 y 7 están en la misma transacción: si falla uno, se revierte todo.

**3.4 Ajuste negativo**

```
ajustarNegativo(idProducto, idDeposito, cantidad, motivo, idUsuario)
```

Igual que el positivo, con estas diferencias:

| Paso | Detalle |
|---|---|
| 4 | **Verificar que el saldo alcanza:** `stock.getCantidad() >= cantidad`. Si no alcanza → `ReglaNegocioException("Stock insuficiente. Saldo actual: " + stock.getCantidad())` |
| 4b | Restar: `stock.disminuir(cantidad)` |
| 6 | Tipo `AJUSTE_NEGATIVO` |

**3.5 Manejo de deadlocks**

En `ajustarPositivo` y `ajustarNegativo`, envolver la transacción con reintento:

```java
int intentos = 0;
while (true) {
    try (Connection conn = ConexionDB.obtenerConexion()) {
        conn.setAutoCommit(false);
        try {
            // operación completa
            conn.commit();
            return;
        } catch (SQLException e) {
            conn.rollback();
            if (ReglaNegocioException.esDeadlock(e) && intentos < 1) {
                intentos++;
                continue; // reintentar una vez
            }
            throw ReglaNegocioException.desdeSQL(e);
        }
    }
}
```

**3.6 Aviso de stock bajo**

Requisito de la presentación: después de una operación, el programa tiene que avisar si el par producto–depósito quedó con stock bajo (`cantidad < stockMinimo`).

| Decisión | Detalle |
|---|---|
| Qué devuelve el servicio | `definirMinimo`, `ajustarPositivo` y `ajustarNegativo` devuelven el `Stock` actualizado (después del commit), en vez de `void` |
| Quién decide si avisar | El menú: llama a `stock.esBajo()` y, si da `true`, imprime el aviso con `stock.faltante()` |
| Por qué no lo imprime el servicio | El servicio no escribe en consola; solo la capa `ui` lo hace |
| Base de datos | Sin cambios: se usa el objeto que el servicio ya tiene en memoria, sin otra consulta |

Cuándo puede aparecer el aviso:

| Operación | Ejemplo |
|---|---|
| Ajuste negativo | Saldo 10, mínimo 5, ajuste −7 → saldo 3, faltan 2 |
| Definir mínimo | Saldo 3, se sube el mínimo de 0 a 5 → faltan 2 (el saldo no cambió, pero ahora está bajo) |
| Ajuste positivo | Saldo 3, mínimo 10, ajuste +2 → saldo 5, todavía faltan 5 |

Texto del aviso:

```
Ajuste registrado. Saldo actual: 3.
⚠ Stock bajo: el mínimo es 5, faltan 2 unidades.
```

### Etapa 4 — MenuStock

**Estructura del menú:**

```
--- Stock ---
1. Habilitar producto en depósito
2. Definir stock mínimo
3. Ajuste positivo
4. Ajuste negativo
0. Volver
```

Para cada opción, el menú debe:
1. Mostrar listas de referencia (productos activos, depósitos activos) para que el usuario sepa qué ids usar.
2. Pedir los datos por consola.
3. Llamar al servicio.
4. Mostrar resultado o error.
5. En las opciones 2, 3 y 4: mostrar el aviso de stock bajo si corresponde (ver 3.6). Conviene un método privado `mostrarAvisoStockBajo(Stock)` que usen las tres opciones.

**Constructor de MenuStock:**

Cambiar de:
```java
public MenuStock(Scanner scanner)
```
A:
```java
public MenuStock(Scanner scanner, StockServicio stockServicio,
                 SesionServicio sesionServicio)
```

Necesita `SesionServicio` para obtener el `idUsuario` del usuario logueado al registrar movimientos.

**Para mostrar listas de referencia** (productos y depósitos activos), el menú puede recibir también los DAOs de consulta o los servicios de maestros. Alternativas:
- Recibir `ProductoServicio` y `DepositoServicio` (si Joaquín ya los tiene).
- Recibir `ProductoDao` y `DepositoDao` directamente y hacer consultas de solo lectura.

Recomendación: recibir los servicios de Joaquín cuando estén listos. Mientras tanto, pedir los ids directamente sin mostrar lista.

### Etapa 5 — Integrar en Main.java

```java
StockDao stockDao = new StockJdbcDao();
MovimientoStockDao movimientoStockDao = new MovimientoStockJdbcDao();

StockServicio stockServicio = new StockServicio(
    stockDao, movimientoStockDao, productoDao, depositoDao);

MenuStock menuStock = new MenuStock(scanner, stockServicio, sesionServicio);
```

---

## 4. Validaciones críticas a no olvidar

- [ ] Producto y depósito activos para habilitar (`buscarPorIdConBloqueo` en ambos).
- [ ] Habilitación única por par producto-depósito (duplicado → 1062 de la base).
- [ ] Cantidad de ajuste: entero > 0.
- [ ] Motivo: no nulo y no vacío después de trim.
- [ ] Stock nunca negativo: verificar saldo antes del ajuste negativo (con bloqueo FOR UPDATE).
- [ ] Saldo y movimiento en la misma transacción: se guardan juntos o se revierten juntos.
- [ ] Orden de bloqueos: PRODUCTO → DEPOSITO → STOCK (nunca al revés).
- [ ] Deadlock (error 1213): rollback + un reintento.
- [ ] Consultas parametrizadas siempre.
- [ ] Aviso de stock bajo después de definir mínimo y de cada ajuste (se muestra en la presentación).

## 5. Concurrencia — Casos a probar

Del plan_mvp.md sección 8:

| Caso | Resultado esperado |
|---|---|
| Saldo 3, dos ajustes simultáneos de −2 | Uno pasa (saldo 1), el otro se rechaza. Un solo movimiento nuevo. |
| Dos depósitos activos, dos bajas simultáneas | Una pasa, la otra se rechaza. Queda un depósito activo. |
| Ajuste y baja del mismo producto al mismo tiempo | Nunca dejar un producto inactivo con existencias. |

Estos se verifican gracias a `FOR UPDATE`: la segunda sesión espera a que la primera termine y después ve el estado actualizado.

### 5.1 Cómo se prueban

Programa `src/test/java/controlstock/PruebaConcurrencia.java` (con `main`, no es un test JUnit: necesita la base levantada y no tiene que correr en `mvn package`). Usa los servicios reales con la cuenta de `db.properties`.

Para que las dos operaciones se crucen de verdad, una tercera conexión (el «tapón») toma antes con `FOR UPDATE` la fila que las dos bloquean primero (el PRODUCTO en los casos 1 y 3; los dos DEPOSITO en el caso 2). Se lanzan los dos hilos, se comprueba que **los dos quedaron esperando** y recién entonces se suelta el tapón.

| Caso | Datos que usa |
|---|---|
| 1 | Producto nuevo en un depósito nuevo, saldo 3; dos ajustes de −2 |
| 3 | 4 rondas, cada una con un producto nuevo con saldo 0; ajuste +5 y baja a la vez, alternando cuál se lanza primero |
| 2 | Dos depósitos nuevos. Para que haya exactamente dos activos, los demás se pasan a inactivo con un `UPDATE` directo y se restauran al final (el programa imprime sus ids por si se corta) |

Compilar y ejecutar desde la raíz, en Git Bash sobre Windows (sin Maven; `R` es el repositorio local de Maven; en Linux/Mac el separador del classpath es `:` en vez de `;`):

```bash
R=~/.m2/repository
CP="$R/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar;$R/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"
javac -encoding UTF-8 --release 17 -cp "$CP" -d target/prueba $(find src/main/java src/test/java -name "*.java")
java -cp "target/prueba;$CP" controlstock.PruebaConcurrencia
```

Los datos que crea llevan el prefijo `CONC-` y quedan en la base (la cuenta de la aplicación no puede borrar). Se limpian reinstalando con `00` → `03`, antes de la demo.

### 5.2 Resultados (16/11/2026, MySQL 8.0.46, REPEATABLE READ)

Dos ejecuciones completas, todas las comprobaciones OK:

| Caso | Resultado |
|---|---|
| 1. Saldo 3, dos ajustes de −2 | Uno pasa; el otro se rechaza con «Stock insuficiente. Saldo actual: 1» (leyó el saldo ya confirmado). Saldo final 1, un solo movimiento nuevo |
| 2. Dos depósitos activos, dos bajas | Una pasa; la otra se rechaza con «debe quedar al menos un depósito activo». Queda 1 activo |
| 3. Ajuste +5 y baja del producto | Se dieron los dos órdenes. Si gana la baja, el ajuste se rechaza por producto inactivo (saldo 0). Si gana el ajuste, la baja se rechaza por stock mayor a 0 (activo, saldo 5). Nunca quedó un producto inactivo con existencias |

Observaciones sobre el módulo Maestros (funcionan, pero difieren de `modelo_relacional_mvp.md` §5.4):

- `DepositoServicio.darDeBaja` bloquea primero su propio depósito y después todos los activos (`contarActivos` con `FOR UPDATE`). Con dos bajas simultáneas eso puede producir un deadlock (1213), que se resuelve con el reintento (el resultado de la prueba es correcto, pero no se midió si el reintento llegó a usarse). La §5.4 indica bloquear primero **todos** los activos, lo que evita el deadlock.
- `ProductoServicio.darDeBaja` consulta el STOCK con un `SELECT` común (la §5.4 pide `FOR SHARE`). Hoy da bien porque la foto de la transacción se toma después de esperar el bloqueo del producto, pero si antes se agregara otra lectura común, la baja vería un saldo viejo.

## 6. Dependencias con el módulo Maestros

| Lo que necesito de Joaquín | Para qué | Mientras tanto |
|---|---|---|
| `ProductoJdbcDao.buscarPorIdConBloqueo()` | Verificar producto activo al habilitar/ajustar | Implementar el método yo mismo (es un SELECT simple) o esperar |
| `DepositoJdbcDao.buscarPorIdConBloqueo()` | Verificar depósito activo al habilitar/ajustar | Idem |
| `ProductoServicio.listarActivos()` | Mostrar lista de productos en el menú | Pedir id directamente sin mostrar lista |
| `DepositoServicio.listarActivos()` | Mostrar lista de depósitos en el menú | Idem |

Los primeros dos son más urgentes. Si Joaquín los tiene antes, perfecto. Si no, Santiago puede implementar `ProductoJdbcDao` y `DepositoJdbcDao` con solo esos dos métodos (el resto lo completa Joaquín).

## 7. Estimación

| Etapa | Tiempo estimado |
|---|---|
| Etapa 1 — StockJdbcDao | 2-3 horas |
| Etapa 2 — MovimientoStockJdbcDao | 1-2 horas |
| Etapa 3 — StockServicio (habilitación + mínimos + ajustes + deadlock) | 4-5 horas |
| Etapa 4 — MenuStock | 2-3 horas |
| Etapa 5 — Integración en Main | 30 min |
| **Total** | **10-14 horas** |

## 8. Consultas (MenuConsultas)

El plan del MVP (§6.2) dejaba las consultas imprescindibles para «quien libere capacidad primero». Las tomó el módulo Stock (16/11/2026).

| Archivo | Cambio |
|---|---|
| `servicio/LineaStock.java` | Nuevo. Una fila de consulta: `Stock` + `Producto` + `Deposito` (el `Stock` solo trae ids; el menú necesita nombres y códigos) |
| `servicio/ConsultaServicio.java` | Nuevo. Solo lectura, una conexión por consulta. Reutiliza `listarPorDeposito`, `listarPorProducto` y `listarStockBajo` de `StockDao`, y `buscarPorId` de los DAOs de Maestros (con un `Map` para no repetir búsquedas) |
| `ui/MenuConsultas.java` | Menú completo (antes era un cascarón vacío) |
| `Main.java` | Crea `ConsultaServicio` y se lo pasa a `MenuConsultas` junto con `ProductoServicio` y `DepositoServicio` |

No se agregó SQL ni se tocaron los DAOs.

| Opción | Qué muestra |
|---|---|
| 1. Stock por depósito | Lista los depósitos activos, pide uno y muestra cada producto habilitado: saldo, mínimo, faltante y la marca `BAJO` |
| 2. Stock total de un producto | Lista los productos activos, pide uno y muestra el desglose por depósito y el **stock total** (suma en `long`) |
| 3. Productos con stock bajo | Todos los pares con `cantidad < stockMinimo`: producto, depósito, saldo, mínimo y faltante |

Decisiones:

- **Solo activos.** «Inclusión de inactivos en consultas» es del bloque «si alcanza» (plan_mvp.md §3). El servicio filtra por `estaActivo()` del producto y del depósito. Sin ese filtro, un producto dado de baja con mínimo mayor a 0 (saldo 0) aparecería para siempre en el listado de stock bajo. Pedir un id inactivo o inexistente da un error claro.
- **Atributos derivados** (modelo_relacional_mvp.md §5.3): el total se suma en el servicio; el stock bajo y el faltante los calcula `Stock` (`esBajo()`, `faltante()`), igual que el aviso de §3.6.

Verificado el 16/11/2026 contra MySQL local, con la aplicación real y la entrada por consola redirigida:

| Prueba | Resultado |
|---|---|
| Producto en dos depósitos (saldo 3 con mínimo 5, saldo 4 con mínimo 0) | Opción 1: saldo 3, faltante 2, `BAJO`. Opción 2: desglose 3 y 4, total 7 |
| Producto dado de baja con mínimo 5 y saldo 0 | No aparece en stock bajo; la opción 2 lo rechaza por inactivo |
| Depósito inexistente, id no numérico | Error «No existe el depósito…» y se vuelve a pedir el número |
