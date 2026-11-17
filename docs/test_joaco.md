# Testeo funcional del MVP — Informe de Joaquín

**Fecha:** 17/11/2026  
**Versión probada:** `main` en el commit `63534b3` (código sin cambios locales).  
**Datos de partida:** base reinstalada con `00` → `01` → `02` → `03` → `04_datos_testeo.sql` (8 categorías, 5 depósitos, 24 productos, 40 habilitaciones, 57 movimientos).  
**Objetivo:** comprobar el alcance imprescindible de `plan_mvp.md` (sección 3) contra las reglas de `modelo_relacional_mvp.md` (secciones 5.3 y 5.4) y dejar registradas las inconsistencias encontradas. Este documento no propone cambios ya aplicados: el código no se modificó.

---

## 1. Entorno

| Elemento | Valor |
|---|---|
| Sistema | Debian 13, Linux 6.12 |
| Java | OpenJDK 21.0.13 (el proyecto compila con `release 17`) |
| Servidor | MySQL 8.4.13, `REPEATABLE-READ` |
| `sql_mode` | `ONLY_FULL_GROUP_BY, STRICT_TRANS_TABLES, NO_ZERO_IN_DATE, NO_ZERO_DATE, ERROR_FOR_DIVISION_BY_ZERO, NO_ENGINE_SUBSTITUTION` |
| Cuenta de la aplicación | `controlstock_app` (la de `db.properties`) |

La sección 9 de `modelo_relacional_mvp.md` registra las verificaciones sobre MySQL 8.0.46. Este testeo se hizo sobre 8.4.13 y el comportamiento fue el mismo.

## 2. Método

1. Compilación con `mvn compile` y `mvn test-compile`.
2. Ejecución de `controlstock.Main` con `target/classes` y `target/dependency/*` en el classpath, enviando las respuestas de cada pantalla por entrada estándar. Cada sesión es un proceso nuevo, así que cada una también comprueba que lo hecho en la anterior persiste.
3. Contraste de cada operación que escribe con consultas de solo lectura sobre la base, usando la misma cuenta `controlstock_app`.
4. Orden de las pruebas: primero los rechazos, comprobando después que la base no cambió (57 movimientos, 40 filas de STOCK, saldos intactos). Después las operaciones que escriben.
5. Ejecución de `controlstock.PruebaConcurrencia`, tres veces.

Los ids que se citan son los que asigna `04_datos_testeo.sql`: depósitos Central = 1, Norte = 2, Sucursal Centro = 3, Temporal = 4, Anexo Sur = 5; productos ordenados por código, de ALM-001 = 1 a TEM-001 = 24.

## 3. Resultados

En la columna **Resultado**, *OK* significa que la aplicación hizo lo que piden el plan y el modelo relacional. Los casos marcados con un código **H-xx** tienen su detalle en la sección 4.

### 3.1 Acceso

| Caso | Entrada | Obtenido | Resultado |
|---|---|---|---|
| Acceso válido | `admin` / `admin123` | «¡Bienvenido, admin (administrador)!» | OK |
| Contraseña incorrecta | `admin` / `malaclave` | «Usuario o contraseña incorrectos.» | OK |
| Usuario inexistente | `noexiste` | Mismo mensaje, no revela si el usuario existe | OK |
| Usuario y contraseña vacíos | enter / enter | Mismo mensaje | OK |
| Usuario en mayúsculas | `ADMIN` / `admin123` | Ingresa como `admin` (la intercalación ignora mayúsculas) | OK |
| Usuario con espacios en los extremos | `  admin  ` | Ingresa (se recortan los espacios) | OK |
| Opción de menú inválida | `9`, `abc` | «Opción no válida. Ingresá un número entre 0 y 4.» | OK |
| Cerrar sesión y volver a ingresar | opción 4 | Vuelve a pedir usuario | OK |
| Salir | `salir` como usuario, u opción 0 | «¡Hasta luego!» | OK |
| Fin de la entrada (Ctrl+D) | cierre de la entrada estándar | Excepción sin capturar | **H-07** |

### 3.2 Consultas

| Caso | Entrada | Obtenido | Resultado |
|---|---|---|---|
| Stock por depósito, Central | 1 | 19 filas; todos los saldos coinciden con los movimientos. LIB-001 y TEM-001 (inactivos) no aparecen | OK |
| Stock por depósito, Norte | 2 | 7 filas; LIM-001 con faltante 7 y marca BAJO | OK |
| Stock por depósito, Sucursal Centro | 3 | 5 filas; ALM-004 y PER-003 con marca BAJO | OK |
| Stock por depósito, Temporal | 4 | 4 filas con saldo 0 | OK |
| Depósito inactivo | 5 (Anexo Sur) | «El depósito 'Anexo Sur' está inactivo.» | OK |
| Depósito inexistente | 99, -1 | «No existe el depósito con id …» | OK |
| Stock total de ALM-003 | 3 | 54 + 36 + 14 = 104 | OK |
| Stock total de ALM-001 | 1 | 8 + 25 = 33 | OK |
| Producto habilitado en un depósito inactivo | 5 (ALM-005) | Solo muestra Central; total 50 (no suma Anexo Sur) | OK |
| Producto sin habilitar | 6 (ALM-006) | «El producto no está habilitado en ningún depósito activo. Stock total: 0.» | OK |
| Producto inactivo | 14 (LIB-001) | «El producto 'Cuaderno tapa dura 48 hojas' está inactivo.» | OK |
| Producto inexistente | 99 | «No existe el producto con id 99.» | OK |
| Nombre largo | 8 (BAZ-002, 97 caracteres) | Completo en el encabezado; recortado con «...» en las tablas | OK |
| Stock bajo | — | Exactamente las 5 filas esperadas; quedan afuera ALM-002 Central (15 de 15) y BEB-003 Sucursal Centro (mínimo 0) | OK |
| Códigos de más de 12 caracteres | códigos `CONC-…` de `PruebaConcurrencia` | La columna Código se desplaza | **H-05** |

### 3.3 Stock: rechazos

Después de estas pruebas la base seguía con 57 movimientos y 40 filas de STOCK, y los saldos de ALM-001 Central (8) y ALM-002 Norte (12) no habían cambiado.

| Caso | Entrada (producto, depósito, …) | Obtenido | Resultado |
|---|---|---|---|
| Habilitación duplicada | 1, 1 | «El producto ya está habilitado en ese depósito.» | OK |
| Habilitar un producto inactivo | 14, 1 | «El producto 'Cuaderno tapa dura 48 hojas' está inactivo.» | OK |
| Habilitar en un depósito inactivo | 6, 5 | «El depósito 'Anexo Sur' está inactivo.» | OK |
| Habilitar con un producto o depósito inexistente | 99, 1 / 6, 99 / 0, 0 | «No existe el producto/depósito con id …» | OK |
| Mínimo negativo | 1, 1, -1 | «El stock mínimo no puede ser negativo.» | OK |
| Mínimo sobre un par no habilitado | 6, 2, 5 | «El producto no está habilitado en ese depósito.» | OK |
| Mínimo con un producto inexistente | 99, 1, 5 | «El producto no está habilitado en ese depósito.» | **H-04** |
| Ajuste con cantidad 0 o negativa | 1, 1, 0 / -5 | «La cantidad del ajuste debe ser mayor a 0.» | OK |
| Cantidad no numérica o fuera de rango de `int` | `abc`, `99999999999` | «Ingresá un número entero.» y vuelve a pedir | OK |
| Motivo vacío o solo espacios | `"   "` | «El motivo es obligatorio.» | OK |
| Motivo de 256 caracteres | — | «El motivo no puede superar los 255 caracteres.» | OK |
| Ajuste negativo mayor al saldo | 1, 1, 9 (saldo 8) | «Stock insuficiente. Saldo actual: 8» | OK |
| Ajuste sobre un par no habilitado | 6, 2 | «El producto no está habilitado en ese depósito.» | OK |
| Ajuste en un depósito inactivo | 5, 5 | «El depósito 'Anexo Sur' está inactivo.» | OK |
| Ajuste de un producto inactivo | 14, 1 | «El producto … está inactivo.» | OK |
| Desborde del saldo | 2, 2, 2147483647 | «No se puede aumentar 2147483647: el saldo superaría el máximo permitido.» | OK |

### 3.4 Stock: operaciones que escriben (pasos 3 a 6 de la demo)

| Caso | Entrada | Obtenido | Resultado |
|---|---|---|---|
| Habilitar ALM-006 en Central | 6, 1 | «Producto habilitado en el depósito (saldo 0, mínimo 0).» | OK |
| Definir mínimo 5 | 6, 1, 5 | Mínimo actualizado y aviso de stock bajo, faltan 5 | OK |
| Ajuste +10 con motivo `"  Ingreso de prueba  "` | 6, 1, 10 | Saldo 10. En la base, el motivo quedó sin los espacios de los extremos y `idUsuario` = 1 | OK |
| Ajuste −7 | 6, 1, 7 | Saldo 3, aviso de stock bajo, faltan 2 | OK |
| Ajuste −4 con saldo 3 | 6, 1, 4 | Rechazo; saldo 3; no se registró movimiento (los ids quedaron consecutivos: 64, 65, 66) | OK |
| Ajuste a cero | 1, 1, 8 | Saldo 0; aparece en stock bajo con faltante 20 | OK |
| Consulta posterior | Stock total de ALM-006 y stock bajo | Total 3; stock bajo suma ALM-006 (3 de 5) y ALM-001 Central (0 de 20) | OK |
| Mínimo sobre un producto inactivo | 14, 1, 5 | Se acepta | **H-03** |
| Mínimo en un depósito inactivo | 5, 5, 3 | Se acepta | **H-03** |

### 3.5 Maestros: rechazos

| Caso | Entrada | Obtenido | Resultado |
|---|---|---|---|
| Categoría duplicada | `almacen`, `ALMACÉN`, `  Bebidas  ` | «Ya existe un registro con ese valor único.» | OK, mensaje genérico: **H-06** |
| Categoría con el nombre vacío | enter | «Este campo es obligatorio.» y vuelve a pedir | OK |
| Categoría de 51 caracteres | — | «El nombre no puede superar los 50 caracteres.» | OK |
| Modificar una categoría inexistente | 99 | «No se encontró una categoría con ese ID.» | OK |
| Renombrar Mascotas a `bebidas` | 7 | Rechazo por duplicado | OK |
| Baja de Almacén | 1 | «No se puede dar de baja: tiene productos activos asignados.» | OK |
| Baja de Temporada | 8 | «La categoría 'Temporada' ya está inactiva.» | OK |
| Baja de una categoría inexistente | 99 | «No existe la categoría con id 99.» | OK |
| Producto en una categoría inactiva | categoría 8 | «La categoría 'Temporada' está inactiva.» | OK |
| Producto en una categoría inexistente | categoría 99 | «No existe la categoría con id 99.» | OK |
| Código duplicado | `alm-001` | Rechazo por duplicado | OK |
| Código de 31 o nombre de 101 caracteres | — | Rechazo por largo | OK |
| Precio negativo o con coma | `-5`, `1,5`, `abc` | La interfaz vuelve a pedir el valor | OK |
| Precio mayor que `DECIMAL(12,2)` | `99999999999` | «Error de base de datos: Data truncation: Out of range value for column 'precioReferenciaCompra' at row 1» | **H-06** |
| Cambiar el código a uno existente | ALM-001 → `ALM-002` | Rechazo por duplicado | OK |
| Baja de BAZ-003 | 9 | «No se puede dar de baja: tiene stock con cantidad mayor a 0.» | OK |
| Baja de LIB-001 | 14 | «El producto … ya está inactivo.» | OK |
| Depósito duplicado | `deposito norte` | Rechazo por duplicado | OK |
| Depósito de 51 caracteres | — | Rechazo por largo | OK |
| Baja de Central | 1 | «No se puede dar de baja: tiene stock con cantidad mayor a 0.» | OK |
| Baja de Anexo Sur | 5 | «El depósito 'Anexo Sur' ya está inactivo.» | OK |

### 3.6 Maestros: operaciones que escriben

| Caso | Entrada | Obtenido | Resultado |
|---|---|---|---|
| Alta de categoría | `Juguetería` | Creada, activa | OK |
| Modificar categoría | Mascotas → `Mascotas y Accesorios` | Modificada | OK |
| Alta de producto con precio 0 y sin descripción | MAS-001 | Creado; en la base, descripción `NULL` y precios 0.00 | OK |
| Descripción solo con espacios | MAS-002, `"   "` | Se guarda `NULL` | OK |
| Precio con tres decimales o en notación científica | `1.555`, `1e3` | Se guarda 1.56 y 1000.00, sin aviso | **H-08** |
| Modificar el precio de TEM-001 sin cambiar la categoría | 24 | «La categoría 'Temporada' está inactiva.» | **H-01** |
| Modificar el precio de LIB-001 (producto inactivo, categoría activa) | 14 | Modificado | OK |
| Baja de BEB-004 (saldo 0 en todos sus depósitos) | 13 | Dado de baja; desaparece de las consultas y no se puede habilitar | OK |
| Baja de Librería (solo productos inactivos) | 6 | Dada de baja | OK |
| Modificar LIB-001 después de la baja de Librería | 14 | «La categoría 'Librería' está inactiva.» | **H-01** |
| Baja de Depósito Temporal (saldos en 0) | 4 | Dado de baja; consultas y ajustes lo rechazan por inactivo; el total de BEB-001 pasa a 96 + 48 = 144 | OK |
| Alta de depósito sin dirección, modificación y baja | `Depósito Prueba` | Las tres operaciones se completan | OK |
| Borrar la dirección al modificar | enter | Conserva la dirección anterior | **H-09** |
| Alta de depósito con el nombre `0` | `0` | Se crea el depósito «0» | **H-02** |

### 3.7 Concurrencia (`PruebaConcurrencia`)

Tres ejecuciones; las 26 comprobaciones pasaron en todas:

| Caso (`plan_mvp.md`, sección 8) | Resultado |
|---|---|
| Saldo 3 y dos ajustes simultáneos de −2 | Uno pasa y el otro se rechaza; saldo final 1 y un solo movimiento |
| Dos depósitos activos y dos bajas simultáneas | Una pasa y la otra se rechaza por ser el último activo; queda 1 activo |
| Ajuste +5 y baja del mismo producto, en los dos órdenes | Exactamente una pasa; nunca queda un producto inactivo con existencias |

Esto confirma la corrección del orden de bloqueo de `DepositoServicio.darDeBaja` (commit `49a83e0`) con el código actual. Las observaciones de `plan_modulo_stock.md`, sección 5.2, se pueden marcar como resueltas.

## 4. Hallazgos

Prioridad **media**: contradice una regla documentada. Prioridad **baja**: es de usabilidad o de presentación, y no afecta la integridad de los datos. El responsable sigue el reparto de `plan_mvp.md`, sección 6.2.

| Id | Prioridad | Resumen | Archivo | Responsable | Estado |
|---|---|---|---|---|---|
| H-01 | Media | No se puede modificar un producto cuya categoría está inactiva, aunque no se cambie la categoría | `servicio/ProductoServicio.java` | Joaquín | Corregido (17/11) |
| H-02 | Baja | Las altas no se pueden cancelar; escribir `0` crea un registro con ese nombre | `ui/MenuMaestros.java` | Joaquín | Corregido (17/11) |
| H-03 | Baja | Definir el mínimo no exige producto ni depósito activos | `servicio/StockServicio.java` | Santiago | Pendiente |
| H-04 | Baja | Definir el mínimo con un producto inexistente informa «no está habilitado» | `servicio/StockServicio.java` | Santiago | Pendiente |
| H-05 | Baja | La columna Código no tolera códigos de más de 12 caracteres | `ui/MenuConsultas.java` | Santiago | Pendiente |
| H-06 | Baja | Mensajes genéricos o técnicos ante errores de la base | `servicio/ReglaNegocioException.java` | Compartido | Corregido en parte (17/11): precio fuera de rango |
| H-07 | Baja | La aplicación termina con una excepción si se cierra la entrada | Menús (`ui/`) | Compartido | Pendiente |
| H-08 | Baja | Los precios con más de dos decimales se redondean sin aviso | `ui/MenuMaestros.java`, `servicio/ProductoServicio.java` | Joaquín | Corregido (17/11) |
| H-09 | Baja | Al modificar, no se puede borrar la dirección ni la descripción | `ui/MenuMaestros.java` | Joaquín | Pendiente |
| H-10 | Baja | Los menús de Stock piden todos los datos antes de validar y no muestran listas | `ui/MenuStock.java`, `ui/MenuMaestros.java` | Santiago / Joaquín | Pendiente |

Las correcciones del 17/11 siguen `docs/plan_correcciones_maestros.md`, que tiene el diseño y la verificación. Las secciones 3 y 4 describen el comportamiento del commit `63534b3`, previo a esas correcciones.

### H-01 — Modificar un producto de una categoría inactiva

- **Regla:** `modelo_relacional_mvp.md`, sección 5.3, regla 9: «Al dar de alta un producto o cambiarle la categoría, la categoría debe estar activa. Que una categoría pase después a inactiva no impide operar con sus productos».
- **Comportamiento:** `ProductoServicio.modificar` (línea 74) bloquea y valida la categoría recibida siempre, aunque sea la misma que ya tenía el producto. Por eso se rechaza cualquier cambio de precio, nombre o descripción de un producto cuya categoría está inactiva.
- **Reproducción:** Maestros → Productos → Modificar → 24 (TEM-001), cambiar solo el precio de compra. Respuesta: «La categoría 'Temporada' está inactiva.» Pasa lo mismo con LIB-001 después de dar de baja Librería.
- **Impacto:** después de dar de baja una categoría, sus productos quedan sin posibilidad de edición. No se puede ni corregir un precio.
- **Corrección sugerida:** leer primero el producto y validar la categoría solo si `nuevaCategoria` es distinta de la actual. El orden de bloqueo CATEGORIA → PRODUCTO de la sección 5.4 se puede respetar leyendo el producto sin bloqueo, decidiendo si cambia la categoría y después tomando los bloqueos en orden.

### H-02 — Las altas no se pueden cancelar

- **Comportamiento:** en las altas de categoría, producto y depósito no hay forma de volver sin crear un registro. Si se escribe `0` pensando que es «Volver», se crea un registro con ese nombre. Antes de la reinstalación ya había en la base un depósito «0», probablemente creado así.
- **Corrección sugerida:** indicar en el primer campo que con enter vacío o con `0` se cancela, como ya hacen los campos opcionales con enter.

### H-03 — Mínimo sobre producto o depósito inactivo

- **Comportamiento:** `StockServicio.definirMinimo` (línea 93) solo verifica que el par esté habilitado. Se aceptó un mínimo de 5 para LIB-001, que está inactivo, y de 3 en Anexo Sur, que está inactivo.
- **Contraste con la documentación:** no contradice la regla 8, que solo nombra habilitar, ajustar y compensar, ni la tabla de la sección 5.4. Pero el valor no tiene efecto visible, porque las consultas excluyen los inactivos.
- **Decisión pendiente:** dejarlo como está y documentarlo, o rechazarlo para ser coherentes con la habilitación y los ajustes.

### H-04 — Mensaje de mínimo con producto inexistente

- **Comportamiento:** con el producto 99 la respuesta es «El producto no está habilitado en ese depósito.», mientras que la habilitación y los ajustes responden «No existe el producto con id 99.». El resultado es correcto (se rechaza), pero el mensaje no es coherente con el resto.

### H-05 — Ancho de la columna Código

- **Comportamiento:** `MenuConsultas` usa `%-12s` para el código (líneas 95, 100, 150 y 156), pero `codigoProducto` admite hasta 30 caracteres. Con los códigos `CONC-…`, de 15 caracteres, el resto de la fila se desplaza. Los nombres sí se recortan (`recortar`), y los códigos no.
- **Corrección sugerida:** aplicar el mismo recorte al código o ensanchar la columna.

### H-06 — Mensajes ante errores de la base

- **Duplicados:** el error 1062 se traduce siempre como «Ya existe un registro con ese valor único.», sin decir qué campo (nombre de categoría, de depósito o código de producto). El servicio sabe en cada caso qué campo es único y podría dar un mensaje específico.
- **Valores fuera de rango:** un precio mayor que 9.999.999.999,99 llega a la base y vuelve con el texto técnico del driver, en inglés (`default` de `desdeSQL`, línea 27). Convendría validar el máximo en `ProductoServicio.validarPrecio`, igual que ya se validan los largos.

### H-07 — Fin de la entrada

- **Comportamiento:** si la entrada estándar se cierra (Ctrl+D en la terminal), `Scanner.nextLine` lanza `NoSuchElementException` y la aplicación termina mostrando la traza. Le pasa a cualquier menú. No hay pérdida de datos, porque cada operación ya confirmó o deshizo su transacción.
- **Corrección sugerida:** capturar la excepción en `Main` o en `MenuPrincipal` y cerrar con un mensaje.

### H-08 — Precios con más de dos decimales

- **Comportamiento:** `leerBigDecimal` acepta cualquier escala y notación científica. `1.555` se guarda como 1.56 (MySQL redondea al tipo `DECIMAL(12,2)` y no lo rechaza, ni siquiera en modo estricto), y el usuario no se entera.
- **Corrección sugerida:** rechazar valores con más de dos decimales en `validarPrecio`, o informar el valor que se va a guardar.

### H-09 — Borrar campos opcionales al modificar

- **Comportamiento:** en la modificación de productos y depósitos, el enter vacío conserva el valor actual (`MenuMaestros`, líneas 250 y 374). No hay forma de dejar la descripción o la dirección en `NULL` después de haberlas cargado.
- **Corrección sugerida:** aceptar una marca explícita, por ejemplo `-`, para borrar el valor.

### H-10 — Orden de las preguntas en Stock y en el alta de producto

- **Comportamiento:** `MenuStock` pide producto, depósito, cantidad y motivo, y recién después el servicio valida. Si el producto no existe, el usuario se entera al final. Además, a diferencia de `MenuConsultas`, no muestra la lista de productos y depósitos antes de pedir los ids (lo aclara el propio comentario de la clase). Lo mismo pasa en el alta de producto: la categoría inactiva se informa después de cargar todos los campos.
- **Impacto:** es solo de usabilidad, pero afecta la fluidez de la demo.

## 5. Observaciones que no son defectos

- **Huecos en los ids.** InnoDB no reutiliza los valores de `AUTO_INCREMENT`. Los `INSERT … SELECT` del script `04` reservan ids de más, y cada alta rechazada por duplicado también consume uno. Por eso, después del testeo, la categoría nueva quedó con el id 12, los productos nuevos con 33 y 34, y el primer movimiento nuevo con el 64. Es esperable, pero en la demo conviene leer los ids en los listados y no suponerlos.
- **Bloqueos más fuertes que los documentados.** La sección 5.4 pide `FOR SHARE` sobre PRODUCTO y DEPOSITO al habilitar y ajustar, y sobre CATEGORIA en el alta de producto. El código usa `FOR UPDATE`. Es más restrictivo, así que no rompe la consistencia. La tabla de la sección 5.4 podría aclararlo.
- **Modificar productos inactivos.** Se permite modificar un producto inactivo si su categoría está activa (LIB-001 antes de la baja de Librería). La documentación no lo prohíbe.

## 6. No probado

| Verificación (`plan_mvp.md`, sección 8) | Motivo |
|---|---|
| Arranque rechazado sin modo estricto | Requiere cambiar `sql_mode` global con una cuenta administradora. El chequeo existe en `ConexionDB.verificarModoEstricto` |
| Reversión ante un fallo entre la actualización del saldo y el registro del movimiento | No se puede provocar desde la interfaz: las validaciones previas impiden que el `INSERT` del movimiento falle. Necesita una prueba dedicada con un DAO que falle a propósito |
| Acceso de un usuario inactivo | No hay usuarios inactivos en los datos y la aplicación no administra usuarios |
| Baja rechazada por ser el último depósito activo, desde la interfaz | Requiere dejar en cero el stock de todos los depósitos. La regla está cubierta por el caso 2 de `PruebaConcurrencia` |
| Auditoría, compensaciones e historial | Fuera del alcance implementado |
| Instalación desde cero en la segunda computadora | Corresponde a Santiago |

## 7. Estado de la base después del testeo

El testeo dejó cambios en `controlstock_mvp`: ajustes sobre ALM-006 y ALM-001, bajas (BEB-004, Librería, Depósito Temporal, Depósito Prueba, depósito «0»), altas (Juguetería, MAS-001, MAS-002) y los datos `CONC-…` de `PruebaConcurrencia`. **Antes de la demo hay que reinstalar** con `00` → `01` → `02` → `03` → `04`, y comprobar que el resumen final del `04` devuelve 8 / 5 / 24 / 40 / 57.
