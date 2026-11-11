# Modelo relacional del MVP (v0.4)

Estado: cerrado — versión 0.4 — última actualización: 04/11/2026 — base: Modelo lógico del MVP v0.1, DER del MVP v0.1, Diseño de base de datos v0.3 (DD)


Implementación: `sql/00_reiniciar.sql`, `sql/01_schema.sql` y `sql/03_usuario_app.sql` (ver la sección 8).

El modelo lógico definió **qué tablas hay y cómo se relacionan** (esquema por intensión y camino referencial). El modelo relacional define, sobre esas mismas tablas, **qué datos son válidos**: el dominio de cada atributo, las claves y las restricciones. De acá se pasa directo a MySQL: dominio → tipo de dato; NO NULO → `NOT NULL`; PK → `PRIMARY KEY`; clave alternativa → `UNIQUE`; FK → `FOREIGN KEY`; reglas de una sola fila → `CHECK`; reglas entre filas → capa de servicio (o triggers).

## 1. Dominios

| Dominio | Definición |
|---|---|
| IdsCategoria | Entero+ (mayor que cero), asignado por la base (autoincremental) |
| IdsProducto | Entero+, asignado por la base |
| IdsDeposito | Entero+, asignado por la base |
| IdsUsuario | Entero+, asignado por la base |
| IdsMovimiento | Entero+, asignado por la base |
| IdsAuditoria | Entero+, asignado por la base |
| NombresCategoria | Cadena(50): letras (con tildes y ñ), dígitos, espacios y signos habituales |
| NombresDeposito | Cadena(50): letras, dígitos, espacios y signos habituales |
| CodigosProducto | Cadena(30): letras, dígitos y signos habituales (por ejemplo, guion o punto) |
| NombresProducto | Cadena(100): letras, dígitos, espacios y signos habituales |
| Descripciones | Cadena(255): texto libre |
| Direcciones | Cadena(150): letras, dígitos, espacios y signos habituales |
| NombresUsuario | Cadena(30): letras, dígitos y signos habituales |
| HashesContrasena | Cadena(255): hash con sal de la contraseña, codificado como texto (BCrypt ocupa 60 caracteres; el tamaño deja margen para PBKDF2) |
| Moneda | Decimal mayor o igual que cero, hasta 12 dígitos con 2 decimales, en la moneda única de la empresa |
| Unidades | Entero mayor o igual que cero: unidades enteras de un producto |
| FechasHora | Fecha y hora (AAAA-MM-DD hh:mm:ss), tomadas del servidor de base de datos |
| Motivos | Cadena(255): texto libre |
| DescripcionesCambio | Cadena(500): texto libre |
| RegistrosAfectados | Cadena(30): identificador interno del registro afectado, escrito como texto |
| Estados | Enumerado: activo, inactivo |
| Roles | Enumerado: administrador, encargado_deposito, operador |
| TiposMovimiento | Enumerado: ajuste_positivo, ajuste_negativo |
| Acciones | Enumerado: alta, modificacion, baja_logica |
| EntidadesAuditadas | Enumerado: categoria, producto, deposito, stock, usuario |

Reglas que respetamos de la materia base de datos I:

- Cada atributo se define sobre **un único dominio**.
- Los atributos que representan el mismo concepto **comparten dominio**: los cuatro `estado…` usan Estados; los dos precios de referencia usan Moneda; `STOCK.cantidad`, `STOCK.stockMinimo` y `MOVIMIENTO_STOCK.cantidad` usan Unidades. Solo se comparan atributos del mismo dominio (por ejemplo, la cantidad de un ajuste negativo con la cantidad del STOCK, para saber si alcanza).
- Cada **clave foránea usa el mismo dominio que la clave primaria que referencia**: `PRODUCTO.idCategoria` es IdsCategoria; `MOVIMIENTO_STOCK.idMovimientoCorregido` es IdsMovimiento. Por eso cada tabla tiene su propio dominio de identificadores: un `idProducto` y un `idDeposito` son enteros, pero no tiene sentido compararlos.

Criterios de los enumerados: los valores van en minúscula, **sin tildes y con guion bajo en lugar de espacios** (`ajuste_positivo`, `baja_logica`), para que coincidan uno a uno con los `enum` de Java (`AJUSTE_POSITIVO`, `BAJA_LOGICA`). Se conservan todos los valores del DD de los atributos que el MVP conserva (por ejemplo, los tres roles, aunque solo se use el administrador), salvo en dos dominios que dependen de tablas fuera del recorte:

- **TiposMovimiento** no incluye *entrada* ni *salida*: solo las generan compras, ventas y transferencias (restricción 1 del DER).
- **EntidadesAuditadas** no incluye *proveedor* ni *cliente*, porque esas tablas no existen en el MVP.

Al completar el modelo se agregan esos valores con un `ALTER TABLE`.

## 2. Esquema de las relaciones

La clave primaria va en **negrita**.

CATEGORIA(**idCategoria**: IdsCategoria, nombreCategoria: NombresCategoria, estadoCategoria: Estados)

DEPOSITO(**idDeposito**: IdsDeposito, nombreDeposito: NombresDeposito, direccion: Direcciones, estadoDeposito: Estados)

USUARIO(**idUsuario**: IdsUsuario, nombreUsuario: NombresUsuario, hashContrasena: HashesContrasena, rol: Roles, estadoUsuario: Estados)

PRODUCTO(**idProducto**: IdsProducto, codigoProducto: CodigosProducto, idCategoria: IdsCategoria, nombreProducto: NombresProducto, descripcion: Descripciones, precioReferenciaCompra: Moneda, precioReferenciaVenta: Moneda, estadoProducto: Estados)

STOCK(**idProducto**: IdsProducto, **idDeposito**: IdsDeposito, cantidad: Unidades, stockMinimo: Unidades)

MOVIMIENTO_STOCK(**idMovimiento**: IdsMovimiento, idProducto: IdsProducto, idDeposito: IdsDeposito, idUsuario: IdsUsuario, fechaHoraMovimiento: FechasHora, tipoMovimiento: TiposMovimiento, cantidad: Unidades, motivo: Motivos, idMovimientoCorregido: IdsMovimiento)

AUDITORIA(**idAuditoria**: IdsAuditoria, idUsuario: IdsUsuario, fechaHoraAuditoria: FechasHora, accion: Acciones, entidadAfectada: EntidadesAuditadas, registroAfectado: RegistrosAfectados, descripcionCambio: DescripcionesCambio)

## 3. Claves

Todas las claves primarias del MVP salvo la de STOCK son **abstractas y autoincrementales**: la base inventa un número que no significa nada en el negocio y que nunca cambia. Así las claves foráneas apuntan a un entero estable aunque cambie un nombre o un código (DBD-01 del documento 03).

**Superclave y clave candidata (ejemplo).** En STOCK, `(idProducto, idDeposito, cantidad)` es una **superclave**: no se repite en dos filas. Pero no es **clave**, porque no es irreducible: si se le quita `cantidad`, `(idProducto, idDeposito)` sigue sin repetirse. Esa sí es irreducible:

| idProducto | idDeposito | cantidad |
|---|---|---|
| 1 | 1 | 10 |
| 1 | 2 | 4 |
| 2 | 1 | 0 |

- Si se quita `idDeposito`, `idProducto = 1` se repite (un producto puede estar habilitado en varios depósitos).
- Si se quita `idProducto`, `idDeposito = 1` se repite (un depósito puede tener varios productos).

Por eso `(idProducto, idDeposito)` es clave candidata, y como es la única, es la primaria.

### CATEGORIA
- Clave primaria: idCategoria (simple, abstracta, autoincremental).
- Claves candidatas: idCategoria, nombreCategoria.
- Claves alternativas: nombreCategoria (el DD exige que sea único).
- Claves foráneas: no posee.

### DEPOSITO
- Clave primaria: idDeposito (simple, abstracta, autoincremental).
- Claves candidatas: idDeposito, nombreDeposito.
- Claves alternativas: nombreDeposito (único según el DD). La dirección no es candidata porque es opcional y puede repetirse.
- Claves foráneas: no posee.

### USUARIO
- Clave primaria: idUsuario (simple, abstracta, autoincremental).
- Claves candidatas: idUsuario, nombreUsuario.
- Claves alternativas: nombreUsuario (único según el DD; es con lo que el usuario inicia sesión).
- Claves foráneas: no posee.

### PRODUCTO
- Clave primaria: idProducto (simple, abstracta, autoincremental). Se eligió en lugar de codigoProducto porque el código puede corregirse si se cargó mal (DBD-01).
- Claves candidatas: idProducto, codigoProducto.
- Claves alternativas: codigoProducto (clave **concreta**: es el código con que la empresa identifica al producto). El nombre no es candidato porque puede repetirse.
- Claves foráneas: idCategoria → CATEGORIA.idCategoria (simple, no admite nulo).

### STOCK
- Clave primaria: (idProducto, idDeposito) (compuesta). STOCK es entidad débil sin discriminante: su clave se forma con las de sus dueñas.
- Claves candidatas: (idProducto, idDeposito).
- Claves alternativas: no posee (cantidad y stockMinimo se repiten libremente).
- Claves foráneas: idProducto → PRODUCTO.idProducto e idDeposito → DEPOSITO.idDeposito (simples, cada una parte de la clave primaria, no admiten nulo).

### MOVIMIENTO_STOCK
- Clave primaria: idMovimiento (simple, abstracta, autoincremental).
- Claves candidatas: idMovimiento. Ninguna otra combinación es única: el mismo usuario puede hacer dos ajustes iguales del mismo producto en el mismo depósito en el mismo segundo.
- Claves alternativas: no posee.
- Claves foráneas:
  - (idProducto, idDeposito) → STOCK.(idProducto, idDeposito) (**compuesta**, no admite nulo).
  - idUsuario → USUARIO.idUsuario (simple, no admite nulo).
  - idMovimientoCorregido → MOVIMIENTO_STOCK.idMovimiento (simple, **autorreferencia**, admite nulo: es nula en un ajuste común).

### AUDITORIA
- Clave primaria: idAuditoria (simple, abstracta, autoincremental).
- Claves candidatas: idAuditoria.
- Claves alternativas: no posee.
- Claves foráneas: idUsuario → USUARIO.idUsuario (simple, no admite nulo). `registroAfectado` **no** es clave foránea: guarda un identificador que puede ser de cualquier tabla.

### Resumen de claves foráneas

| Relación | Clave foránea | Referencia a | Tipo | ¿Admite nulo? |
|---|---|---|---|---|
| PRODUCTO | idCategoria | CATEGORIA.idCategoria | Simple | No |
| STOCK | idProducto | PRODUCTO.idProducto | Simple, parte de la PK | No |
| STOCK | idDeposito | DEPOSITO.idDeposito | Simple, parte de la PK | No |
| MOVIMIENTO_STOCK | (idProducto, idDeposito) | STOCK.(idProducto, idDeposito) | Compuesta | No |
| MOVIMIENTO_STOCK | idUsuario | USUARIO.idUsuario | Simple | No |
| MOVIMIENTO_STOCK | idMovimientoCorregido | MOVIMIENTO_STOCK.idMovimiento | Simple, autorreferencia | Sí |
| AUDITORIA | idUsuario | USUARIO.idUsuario | Simple | No |

## 4. Especificación de dominios

**Dominios utilizados**

- **Entero+:** número entero mayor que cero. En las claves abstractas lo asigna la base (autoincremental).
- **Entero ≥ 0:** número entero mayor o igual que cero (unidades de producto).
- **Cadena(n):** texto de hasta *n* caracteres.
- **Moneda:** decimal mayor o igual que cero, hasta 12 dígitos con 2 decimales, en la moneda única de la empresa.
- **Fecha y hora:** AAAA-MM-DD hh:mm:ss, tomada del servidor de base de datos.
- **Enumerado:** solo admite los valores listados, escritos exactamente así.
- **(NO NULO, no vacío):** el atributo es obligatorio y además no puede ser una cadena vacía ni formada solo por espacios. NO NULO no alcanza: `''` o `'   '` no son nulos, pero no tienen información.
- **Opcional (admite nulo):** si el dato no se conoce, se guarda NULO; si se informa, no puede ser vacío.

**Tabla: CATEGORIA**

| Atributo | Descripción | Dominio |
|---|---|---|
| idCategoria | Identificador interno de la categoría | Entero+ (PK) |
| nombreCategoria | Nombre de la categoría | Cadena(50) letras, dígitos, espacios y signos habituales (NO NULO, no vacío) (Clave alternativa) |
| estadoCategoria | Si la categoría puede asignarse a productos | Enumerado: activo, inactivo (NO NULO). Al crearla: activo |

**Tabla: DEPOSITO**

| Atributo | Descripción | Dominio |
|---|---|---|
| idDeposito | Identificador interno del depósito | Entero+ (PK) |
| nombreDeposito | Nombre del depósito | Cadena(50) letras, dígitos, espacios y signos habituales (NO NULO, no vacío) (Clave alternativa) |
| direccion | Dirección física del depósito | Cadena(150) letras, dígitos, espacios y signos habituales. Opcional (admite nulo) |
| estadoDeposito | Si el depósito puede usarse en habilitaciones y ajustes | Enumerado: activo, inactivo (NO NULO). Al crearlo: activo |

**Tabla: USUARIO**

| Atributo | Descripción | Dominio |
|---|---|---|
| idUsuario | Identificador interno del usuario | Entero+ (PK) |
| nombreUsuario | Nombre con el que el usuario inicia sesión | Cadena(30) letras, dígitos y signos habituales (NO NULO, no vacío) (Clave alternativa) |
| hashContrasena | Hash con sal de la contraseña; nunca la contraseña en texto plano | Cadena(255) (NO NULO, no vacío) |
| rol | Rol del usuario; en el MVP solo existe el administrador | Enumerado: administrador, encargado_deposito, operador (NO NULO) |
| estadoUsuario | Si el usuario puede acceder al sistema | Enumerado: activo, inactivo (NO NULO). Al crearlo: activo |

**Tabla: PRODUCTO**

| Atributo | Descripción | Dominio |
|---|---|---|
| idProducto | Identificador interno del producto; no cambia | Entero+ (PK) |
| codigoProducto | Código con que la empresa identifica al producto; puede corregirse | Cadena(30) letras, dígitos y signos habituales (NO NULO, no vacío) (Clave alternativa) |
| idCategoria | Categoría a la que pertenece el producto | Entero+ (NO NULO) (Referencia a la tabla CATEGORIA) |
| nombreProducto | Nombre del producto | Cadena(100) letras, dígitos, espacios y signos habituales (NO NULO, no vacío) |
| descripcion | Descripción del producto | Cadena(255) texto libre. Opcional (admite nulo) |
| precioReferenciaCompra | Precio de referencia de compra | Moneda (NO NULO) |
| precioReferenciaVenta | Precio de referencia de venta | Moneda (NO NULO) |
| estadoProducto | Si el producto puede usarse en habilitaciones y ajustes | Enumerado: activo, inactivo (NO NULO). Al crearlo: activo |

**Tabla: STOCK**

| Atributo | Descripción | Dominio |
|---|---|---|
| idProducto | Producto habilitado | Entero+ (PK, junto con idDeposito) (Referencia a la tabla PRODUCTO) |
| idDeposito | Depósito en el que está habilitado | Entero+ (PK, junto con idProducto) (Referencia a la tabla DEPOSITO) |
| cantidad | Unidades del producto en ese depósito | Entero ≥ 0 (NO NULO). Al habilitar: 0 |
| stockMinimo | Cantidad por debajo de la cual hay stock bajo | Entero ≥ 0 (NO NULO). Si no se indica: 0 |

**Tabla: MOVIMIENTO_STOCK**

| Atributo | Descripción | Dominio |
|---|---|---|
| idMovimiento | Identificador interno del movimiento | Entero+ (PK) |
| idProducto | Producto del STOCK afectado | Entero+ (NO NULO) (Referencia a la tabla STOCK, junto con idDeposito) |
| idDeposito | Depósito del STOCK afectado | Entero+ (NO NULO) (Referencia a la tabla STOCK, junto con idProducto) |
| idUsuario | Usuario que registró el ajuste o la corrección | Entero+ (NO NULO) (Referencia a la tabla USUARIO) |
| fechaHoraMovimiento | Momento en que se registró | Fecha y hora (NO NULO). La asigna el servidor |
| tipoMovimiento | Sentido del ajuste | Enumerado: ajuste_positivo, ajuste_negativo (NO NULO) |
| cantidad | Unidades que suma o resta; el signo lo da tipoMovimiento | Entero ≥ 0 (NO NULO); además, mayor que cero (regla de negocio 2) |
| motivo | Por qué se hizo el ajuste o la corrección | Cadena(255) texto libre (NO NULO, no vacío) |
| idMovimientoCorregido | Movimiento original que este compensatorio corrige | Entero+ (Referencia a la tabla MOVIMIENTO_STOCK). NULO en un ajuste común; NO NULO en un compensatorio |

**Tabla: AUDITORIA**

| Atributo | Descripción | Dominio |
|---|---|---|
| idAuditoria | Identificador interno del registro de auditoría | Entero+ (PK) |
| idUsuario | Usuario que hizo la acción | Entero+ (NO NULO) (Referencia a la tabla USUARIO) |
| fechaHoraAuditoria | Momento de la acción | Fecha y hora (NO NULO). La asigna el servidor |
| accion | Qué se hizo | Enumerado: alta, modificacion, baja_logica (NO NULO) |
| entidadAfectada | Sobre qué tabla se hizo | Enumerado: categoria, producto, deposito, stock, usuario (NO NULO) |
| registroAfectado | Identificador del registro afectado | Cadena(30) dígitos; para stock, "idProducto-idDeposito" (por ejemplo, "5-2") (NO NULO, no vacío) |
| descripcionCambio | Detalle del cambio (por ejemplo, valor anterior y nuevo) | Cadena(500) texto libre (NO NULO, no vacío) |

## 5. Restricciones

### 5.1 Inherentes al modelo

- No hay dos tuplas iguales en una relación (lo garantiza la clave primaria).
- No importa el orden de las tuplas ni el de los atributos.
- Los valores son atómicos: no hay atributos compuestos ni multivaluados. Por eso, por ejemplo, el stock de un producto en cada depósito es una tupla de STOCK y no una lista dentro de PRODUCTO.
- Cada atributo toma valores de un único dominio.

### 5.2 Basadas en el esquema

- **De dominio:** cada atributo solo admite valores de su dominio (tipo, tamaño, enumerado y, con `CHECK`, rangos como Moneda ≥ 0 o Unidades ≥ 0). El rechazo de tipos, tamaños y enumerados **depende del modo estricto** de MySQL: sin él, un valor fuera del `ENUM` se guarda como cadena vacía y solo genera una advertencia. Por eso el modo estricto es un requisito de la conexión (sección 8.2). Los `CHECK` se cumplen con cualquier modo.
- **De obligatoriedad:** todos los atributos son NO NULO salvo `DEPOSITO.direccion`, `PRODUCTO.descripcion` y `MOVIMIENTO_STOCK.idMovimientoCorregido`.
- **De unicidad (claves alternativas):** `nombreCategoria`, `nombreDeposito`, `nombreUsuario` y `codigoProducto`. La comparación no distingue mayúsculas ni tildes: «Bebidas», «bebidas» y «Bébidas» cuentan como el mismo nombre (intercalación `utf8mb4_0900_ai_ci`). En cambio, los espacios al final sí cuentan, así que el servicio quita los espacios de los extremos antes de guardar.
- **De clave primaria e integridad de entidad:** el valor de la clave primaria no puede repetirse en dos tuplas, y **ningún atributo que la forma puede ser nulo**. En una clave compuesta, lo que no se repite es la **combinación completa**: en STOCK, el mismo `idProducto` aparece en varias filas (una por depósito) y el mismo `idDeposito` también, pero el par `(idProducto, idDeposito)` aparece una sola vez. Así se asegura **a lo sumo un registro por par producto–depósito** (restricción 4 del DER).
- **De integridad referencial:** toda clave foránea no nula debe coincidir con una clave primaria existente en la relación referenciada. En la clave foránea compuesta de MOVIMIENTO_STOCK la coincidencia se exige para la **combinación completa**: no alcanza con que el producto y el depósito existan por separado, el par tiene que estar habilitado en STOCK. Ninguna clave foránea propaga borrados ni cambios (`RESTRICT`): como el sistema usa baja lógica, no se borra físicamente ningún registro que tenga otros que dependan de él.

### 5.3 Basadas en la aplicación (reglas de negocio)

**Dentro de una misma tupla** (se implementan con `CHECK`):

1. Los textos obligatorios no pueden quedar vacíos después de quitar espacios; los opcionales son nulos o tienen contenido.
2. La cantidad de un movimiento es mayor que cero; el sentido lo da `tipoMovimiento` (restricción del DD).
3. En el MVP todo movimiento es un ajuste, así que el `motivo` es obligatorio (restricción 1 del DER).
4. Un movimiento compensatorio no puede corregirse a sí mismo (`idMovimientoCorregido ≠ idMovimiento`). Es de una sola tupla, pero **va en el servicio**: MySQL no permite usar una columna `AUTO_INCREMENT` en un `CHECK`.

**Entre varias tuplas** (se validan en el servicio, dentro de la transacción):

5. La `cantidad` de un STOCK solo cambia mediante movimientos y siempre es el resultado de aplicarlos: los ajustes positivos suman y los negativos restan. El saldo y el movimiento se guardan en la misma transacción, con los bloqueos de la sección 5.4.
6. Ningún movimiento puede dejar la cantidad del STOCK por debajo de cero (restricción 2 del DER). El servicio lo valida antes de registrar; el `CHECK (cantidad >= 0)` de STOCK es la última barrera.
7. Habilitar un producto en un depósito crea su STOCK con cantidad 0 y el mínimo indicado, o 0 si no se indica.
8. Solo se habilita, se ajusta y se compensa con producto y depósito activos (restricción 5 del DER, SR-19).
9. Al dar de alta un producto o cambiarle la categoría, la categoría debe estar activa. Que una categoría pase después a inactiva no impide operar con sus productos.
10. Un producto solo se da de baja si su stock es cero en todos los depósitos; un depósito, solo si todos sus STOCK están en cero; y siempre queda al menos un depósito activo. Estas comprobaciones también se protegen con los bloqueos de la sección 5.4.
11. Los movimientos y los registros de auditoría no se modifican ni se eliminan. La base lo respalda con los permisos de la cuenta de la aplicación, que en esas dos tablas solo puede leer e insertar (sección 8.3).
12. Generan auditoría las altas, modificaciones y bajas de categorías, productos y depósitos, las habilitaciones y los cambios de stock mínimo; el cambio y su auditoría se guardan en la misma transacción. Los movimientos no generan auditoría (restricción 6 del DER).
13. En la auditoría, `registroAfectado` guarda el identificador interno (`idProducto`, no `codigoProducto`); para stock, `"idProducto-idDeposito"`.

**Atributos derivados (no se almacenan):** se calculan en las consultas.

- **Stock total** de un producto: `SUM(cantidad)` de sus STOCK.
- **Stock bajo:** `cantidad < stockMinimo`.
- **Faltante:** unidades que faltan para llegar al mínimo, `GREATEST(stockMinimo − cantidad, 0)`. Vale 0 cuando no hay stock bajo (con 15 unidades y mínimo 8 da 0, no −7), así que sirve en cualquier consulta y no solo en una ya filtrada por stock bajo. En la demo: cantidad 3 y mínimo 5 dan faltante 2.

### 5.4 Control de concurrencia

Que las reglas 5, 6, 8 y 10 se validen dentro de una transacción no alcanza. En InnoDB, un `SELECT` común lee una **foto** de los datos y no bloquea nada: dos sesiones pueden validar a la vez sobre la misma foto y las dos seguir adelante. Las **lecturas bloqueantes** sí leen el último valor confirmado y retienen la fila hasta el `COMMIT` o el `ROLLBACK`:

- `SELECT … FOR SHARE` (bloqueo compartido): otras sesiones también pueden leer con `FOR SHARE`, pero nadie puede modificar la fila hasta que se libere.
- `SELECT … FOR UPDATE` (bloqueo exclusivo): nadie más puede bloquear ni modificar la fila hasta que se libere.

Sin estos bloqueos se producen, por ejemplo, estas carreras:

- **Último depósito activo:** hay dos depósitos activos y dos sesiones dan de baja uno distinto cada una. Las dos ven «dos activos» y las dos confirman: quedan cero.
- **Ajuste contra baja de producto:** una sesión comprueba que el producto está activo y suma 5 unidades. Al mismo tiempo, otra ve el stock en 0 y da de baja el producto. Resultado: un producto inactivo con 5 unidades.
- **Habilitación contra baja:** el mismo caso que el anterior, con una habilitación en lugar de un ajuste.

**Regla:** todos los casos de uso que comprueban un estado o un saldo y después escriben lo hacen con lecturas bloqueantes, **siempre en el mismo orden: CATEGORIA → PRODUCTO → DEPOSITO → STOCK**, y recién después insertan en MOVIMIENTO_STOCK o AUDITORIA. Un orden único evita que dos sesiones queden esperándose mutuamente.

| Caso de uso | Bloqueos, en este orden |
|---|---|
| Alta o cambio de categoría de un producto | La CATEGORIA con `FOR SHARE` (comprobar que está activa) |
| Habilitar producto en depósito | El PRODUCTO y el DEPOSITO con `FOR SHARE` (activos); después el `INSERT` en STOCK (la clave primaria rechaza la duplicada) |
| Ajustar stock y compensar movimiento | El PRODUCTO y el DEPOSITO con `FOR SHARE` (activos); el STOCK con `FOR UPDATE` (saldo); después el `UPDATE` del saldo y el `INSERT` del movimiento |
| Definir stock mínimo | El `UPDATE` de STOCK ya bloquea la fila |
| Baja de producto | El PRODUCTO con `FOR UPDATE`; después la suma de sus STOCK con `FOR SHARE` |
| Baja de depósito | **Todos** los DEPOSITO activos con `FOR UPDATE` (`WHERE estadoDeposito = 'activo'`); después la suma de sus STOCK con `FOR SHARE` |
| Baja de categoría | El `UPDATE` de CATEGORIA ya bloquea la fila |

Con esto:

- **Ajuste contra baja de producto:** si el ajuste llega primero, la baja espera su `COMMIT`, después lee el stock actualizado y rechaza. Si la baja llega primero, el ajuste espera, después lee el producto inactivo y rechaza.
- **Último depósito activo:** la segunda baja espera a la primera y, cuando lee, ve un solo depósito activo y rechaza.

Errores que el servicio tiene que manejar haciendo `ROLLBACK`:

- **1213 (deadlock):** con el orden único es improbable. Si ocurre, MySQL deshace una de las transacciones y el servicio puede reintentarla una vez.
- **1205 (tiempo de espera agotado):** se informa que la operación no se pudo completar y se pide reintentar.

Estas carreras se suman a las pruebas de concurrencia del plan del MVP (sección 8 del plan). Las dos primeras se reprodujeron y se comprobó la solución (sección 9 de este documento); la tercera usa el mismo bloqueo del producto que la segunda.

## 6. Normalización

El esquema está en **forma normal de Boyce-Codd** (FNBC), que incluye a las anteriores:

- **1FN:** todos los valores son atómicos y no hay grupos repetitivos. El stock de un producto en cada depósito es una tupla de STOCK y no una lista dentro de PRODUCTO.
- **2FN:** solo puede haber dependencias parciales con una clave compuesta, y la única es la de STOCK. Se cumple `(idProducto, idDeposito) → cantidad, stockMinimo`, y ninguno de los dos atributos depende de una sola parte de la clave: el mismo producto tiene cantidades distintas en cada depósito, y el mismo depósito, cantidades distintas para cada producto.
- **3FN:** ningún atributo no clave depende de otro atributo no clave. Por ejemplo, PRODUCTO guarda `idCategoria` y no `nombreCategoria`. Si lo guardara, habría una dependencia transitiva `idProducto → idCategoria → nombreCategoria`, y cambiar el nombre de una categoría obligaría a actualizar todos sus productos. Por lo mismo, MOVIMIENTO_STOCK no guarda el nombre del producto, del depósito ni del usuario.
- **FNBC:** para toda dependencia funcional no trivial X → Y, X es una **superclave**. En este esquema, las dependencias no triviales parten de una clave candidata (`idProducto` o `codigoProducto` en PRODUCTO; `idCategoria` o `nombreCategoria` en CATEGORIA, y lo mismo en DEPOSITO y USUARIO) o de una superclave que la contiene, como `(idProducto, codigoProducto)`. Ningún atributo que no forme parte de una clave determina a otro.

**Redundancia controlada:** `STOCK.cantidad` podría calcularse sumando los movimientos de ese par (empieza en 0 y solo cambia por movimientos). No viola ninguna forma normal, porque es una dependencia entre tablas y no dentro de STOCK, pero es una redundancia **elegida a propósito**. Consultar el saldo no obliga a sumar todo el historial, y la fila de STOCK es la que se bloquea para validar el saldo. La regla 5 mantiene las dos cosas coherentes: el saldo y el movimiento se guardan juntos o no se guarda ninguno.

## 7. Diagrama referencial

Se reutiliza el del modelo lógico (`docs/modelo_logico_mvp.drawio`). Las flechas van de cada clave foránea a la clave primaria que referencia:

```
PRODUCTO.idCategoria                                → CATEGORIA.idCategoria
STOCK.idProducto                                    → PRODUCTO.idProducto
STOCK.idDeposito                                    → DEPOSITO.idDeposito
MOVIMIENTO_STOCK.(idProducto, idDeposito)           → STOCK.(idProducto, idDeposito)
MOVIMIENTO_STOCK.idUsuario                          → USUARIO.idUsuario
MOVIMIENTO_STOCK.idMovimientoCorregido              → MOVIMIENTO_STOCK.idMovimiento
AUDITORIA.idUsuario                                 → USUARIO.idUsuario
```

**Caminos referenciales:**

- MOVIMIENTO_STOCK → STOCK → PRODUCTO → CATEGORIA
- MOVIMIENTO_STOCK → STOCK → DEPOSITO
- MOVIMIENTO_STOCK → USUARIO
- AUDITORIA → USUARIO

**Ciclo referencial:** hay uno solo, la **autorreferencia** de MOVIMIENTO_STOCK (relación CORRIGE), equivalente al `EMPLEADO.jefe → EMPLEADO.codEmp` del apunte. Como la clave foránea admite nulo, el primer movimiento se carga sin problema (no corrige a nadie), y un compensatorio solo puede apuntar a un movimiento que ya existe.

El camino referencial también fija el **orden de creación** de las tablas: CATEGORIA, DEPOSITO, USUARIO, PRODUCTO, STOCK, MOVIMIENTO_STOCK y AUDITORIA.

## 8. Paso a MySQL

### 8.1 Tipos y decisiones de implementación

| Dominio | Tipo en MySQL | Restricción agregada |
|---|---|---|
| Ids… (Entero+) | `INT UNSIGNED`; en la PK, `AUTO_INCREMENT` | — |
| Cadena(n) | `VARCHAR(n)` | `CHECK (CHAR_LENGTH(TRIM(col)) > 0)` si es no vacío |
| Moneda | `DECIMAL(12,2)` | `CHECK (col >= 0)` |
| Unidades | `INT` | `CHECK (col >= 0)`; en el movimiento, `> 0` |
| FechasHora | `DATETIME DEFAULT CURRENT_TIMESTAMP` | — |
| Enumerados | `ENUM('v1', 'v2', …)` | — |

Decisiones de implementación:

- **Base de datos:** `controlstock_mvp`, con `utf8mb4` (admite tildes, ñ y cualquier carácter) y motor InnoDB (el único de MySQL con claves foráneas y transacciones).
- **Cantidades con `INT` y `CHECK`, no con `UNSIGNED`.** Los dos rechazan un negativo, pero con el `CHECK` el error nombra la regla (`chk_stock_cantidad`), y el servicio puede traducirlo a un mensaje claro.
- **Valores por defecto:** los estados arrancan en `activo`, `cantidad` y `stockMinimo` en 0, y las fechas las pone el servidor (RNF-26). La aplicación no manda la fecha.
- **Nombres de restricciones:** `ak_` (clave alternativa), `fk_` (clave foránea) y `chk_` (CHECK), seguidos de la tabla y el atributo. Aparecen en los mensajes de error y facilitan identificar qué regla se violó.
- **`DATETIME` y no `TIMESTAMP`:** `TIMESTAMP` convierte la hora según la zona horaria de la conexión y llega solo hasta 2038.

### 8.2 Modo estricto

Las restricciones de dominio de tipo, tamaño y `ENUM` solo se hacen cumplir con **modo estricto** (`STRICT_TRANS_TABLES` o `STRICT_ALL_TABLES` en `sql_mode`). Sin él, MySQL guarda un valor ajustado o vacío y solo emite una advertencia: por ejemplo, `estadoCategoria = 'baja'` queda guardado como `''`. MySQL 8.0 trae `STRICT_TRANS_TABLES` por defecto, pero un servidor configurado a mano podría no tenerlo. Por eso:

- **Instalación:** el README lo pide como requisito y explica cómo comprobarlo con `SELECT @@GLOBAL.sql_mode;`.
- **Aplicación:** la clase de conexión (`util`) consulta `SELECT @@SESSION.sql_mode` al conectarse. Si no contiene `STRICT_TRANS_TABLES` ni `STRICT_ALL_TABLES`, la aplicación no arranca y muestra un mensaje que explica el motivo.

### 8.3 Cuenta de la aplicación

La aplicación no se conecta como `root`, sino con la cuenta `controlstock_app`, que solo tiene los permisos que necesita (`sql/03_usuario_app.sql`). Así la base respalda dos reglas aunque el código tenga un error:

| Tablas | Permisos | Qué respalda |
|---|---|---|
| CATEGORIA, PRODUCTO, DEPOSITO, STOCK | `SELECT`, `INSERT`, `UPDATE` | Altas, modificaciones y bajas lógicas (la baja es un `UPDATE` del estado) |
| USUARIO | `SELECT` | En el MVP solo se lee para iniciar sesión |
| MOVIMIENTO_STOCK, AUDITORIA | `SELECT`, `INSERT` | Historial inmutable (regla 11): no se puede modificar ni borrar |

Ninguna tabla tiene `DELETE`, porque todo es baja lógica. Las lecturas bloqueantes piden permisos distintos (desde MySQL 8.0.22; comprobado en 8.0.46):

- `SELECT … FOR SHARE` necesita solo `SELECT`.
- `SELECT … FOR UPDATE` necesita `SELECT` y, además, `UPDATE`, `DELETE` o `LOCK TABLES`.

Las tablas que la sección 5.4 bloquea con `FOR UPDATE` (PRODUCTO, DEPOSITO y STOCK) tienen `UPDATE`. MOVIMIENTO_STOCK y AUDITORIA no lo tienen, pero tampoco se bloquean: solo se insertan.

**Permisos exactos:** `03_usuario_app.sql` empieza con `REVOKE ALL PRIVILEGES, GRANT OPTION` y después da los permisos de la tabla. Así, si la cuenta ya existía con permisos de más (por ejemplo, `ALL` sobre la base), se los quita. Un `CREATE USER IF NOT EXISTS` con `GRANT` no alcanza, porque los `GRANT` suman permisos y no sacan los anteriores. Después de ejecutarlo se comprueba con `SHOW GRANTS FOR 'controlstock_app'@'localhost';`.

**Sin roles:** el `REVOKE ALL PRIVILEGES` quita los permisos directos, pero **no los roles** asignados a la cuenta (`GRANT rol TO cuenta`). La cuenta es exclusiva del MVP y tiene que estar sin roles. `SHOW GRANTS` muestra cada rol en una línea `GRANT 'rol'@'host' TO …`; si aparece alguna, se quita con `REVOKE 'rol'@'host' FROM 'controlstock_app'@'localhost';`. También se comprueba que `SELECT @@mandatory_roles;` dé vacío, porque los roles obligatorios se aplican a todas las cuentas.

**Contraseña:** el script está versionado, así que no lleva la contraseña real. Crea la cuenta con una contraseña provisoria **vencida** (`PASSWORD EXPIRE`), que no sirve para conectarse. Cada integrante le pone su propia contraseña con un `ALTER USER` que no se guarda en el repositorio, y la escribe en su configuración local, que tampoco se versiona. Si la cuenta ya existía, el script no le cambia la contraseña.

### 8.4 Scripts y orden de ejecución

| Script | Qué hace | Cuándo |
|---|---|---|
| `00_reiniciar.sql` | Borra la base `controlstock_mvp` con todos sus datos | Solo para reinstalar desde cero o reiniciar la demo |
| `01_schema.sql` | Crea la base y las 7 tablas. Si la base ya existe, se detiene en el `CREATE DATABASE` (ver la nota) | Instalación |
| `02_datos_prueba.sql` | Administrador precargado, depósito inicial y datos de la demo | Instalación (pendiente) |
| `03_usuario_app.sql` | Crea la cuenta `controlstock_app` si no existe y le deja exactamente sus permisos. Se puede ejecutar más de una vez | Instalación, una vez por servidor |

`00`, `01` y `03` se ejecutan con una cuenta administradora (por ejemplo, `root`). Reiniciar la base no borra la cuenta de la aplicación: sus permisos quedan guardados y vuelven a valer cuando `01_schema.sql` crea las tablas de nuevo.

**Detención ante el primer error.** Que `01_schema.sql` «no toque nada» si la base existe depende del cliente: se cumple solo si el script se detiene en el primer error. Así funciona Workbench por defecto, y también el cliente `mysql` sin `--force`. Si se activa la opción de continuar después de errores, el script sigue de largo y **crea las tablas que falten** dentro de una base a medio instalar. Por eso los scripts se ejecutan con la detención activada, y ante un fallo a mitad de camino se reinicia con `00_reiniciar.sql`.

## 9. Verificación

Los scripts se ejecutaron sobre MySQL 8.0.46. `01_schema.sql` crea las 7 tablas con 7 claves primarias, 7 claves foráneas, 4 claves alternativas y 16 restricciones `CHECK`. Se probaron casos válidos (alta de maestros, habilitación, ajuste, compensatorio y auditoría) y estos rechazos:

| Caso | Error de MySQL |
|---|---|
| Nombre de categoría repetido (también «bébidas» o «BEBIDAS» frente a «Bebidas») | 1062, `ak_categoria_nombre` |
| Nombre formado solo por espacios | 3819, `chk_categoria_nombre` |
| Estado, rol o tipo de movimiento fuera del enumerado (por ejemplo, `entrada`) | 1265 (Data truncated) |
| Código de producto repetido | 1062, `ak_producto_codigo` |
| Producto con categoría inexistente | 1452, `fk_producto_categoria` |
| Precio negativo / descripción vacía | 3819, `chk_producto_precio_compra` / `chk_producto_descripcion` |
| Habilitación duplicada | 1062, `STOCK.PRIMARY` |
| Stock mínimo negativo / saldo que quedaría negativo | 3819, `chk_stock_minimo` / `chk_stock_cantidad` |
| Movimiento sobre un par producto–depósito no habilitado | 1452, `fk_movimiento_stock` |
| Cantidad 0 / motivo vacío / motivo nulo | 3819 `chk_movimiento_cantidad` / 3819 `chk_movimiento_motivo` / 1048 |
| Compensatorio de un movimiento inexistente | 1452, `fk_movimiento_corregido` |
| Borrar un producto habilitado / un movimiento ya corregido | 1451 (RESTRICT) |
| Auditoría con usuario inexistente | 1452, `fk_auditoria_usuario` |

Los rechazos de enumerados (1265) se verificaron con el modo estricto por defecto. Sin modo estricto, el mismo `INSERT` se acepta con el estado vacío, y eso confirmó el requisito de la sección 8.2.

**Scripts y cuenta de la aplicación:**

| Caso | Resultado |
|---|---|
| `00` → `01` → `03` sobre un servidor vacío | Correcto |
| `01_schema.sql` con la base ya creada, con detención ante errores | Error 1007; la base queda intacta |
| `01_schema.sql` sobre una base a la que le falta una tabla, **continuando** después de errores (`--force`) | Errores 1007 y 1050 en lo existente, pero **crea la tabla que faltaba** (por eso se exige la detención) |
| `03_usuario_app.sql` ejecutado dos veces | Correcto |
| `03_usuario_app.sql` sobre una cuenta que ya tenía `ALL PRIVILEGES` en la base y `SELECT` global | Quedan solo `USAGE` y los siete permisos por tabla; la contraseña que tenía se conserva |
| `03_usuario_app.sql` sobre una cuenta con un rol que tiene `ALL` en la base | Los permisos directos quedan bien, pero el rol sigue y la cuenta **puede borrar**. `SHOW GRANTS` lo muestra; después de `REVOKE` del rol, `DELETE` da error 1142 |
| Cuenta con solo `SELECT`: `FOR SHARE` / `FOR UPDATE` | Correcto / error 1142 |
| Conectarse con la contraseña provisoria | Rechazado (contraseña vencida) |
| Con la cuenta de la aplicación: ajuste completo con `FOR SHARE` y `FOR UPDATE`, baja lógica | Correcto |
| Con la cuenta de la aplicación: `DELETE` en una categoría o un movimiento; `UPDATE` en un movimiento o un usuario | Error 1142 (permiso denegado) |
| Con la cuenta de la aplicación: `DROP DATABASE` | Error 1044 |
| Permisos después de `00_reiniciar.sql` + `01_schema.sql` | Siguen vigentes |

**Concurrencia** (dos sesiones simultáneas, ver la sección 5.4):

| Carrera | Con lectura común | Con lecturas bloqueantes |
|---|---|---|
| Dos bajas de depósito con dos activos | Las dos confirman: **0 activos** | La segunda ve 1 activo y rechaza: queda 1 |
| Ajuste +5 y, a la vez, baja del producto | Producto **inactivo con 5 unidades** | La baja espera, ve 5 y rechaza |
| Baja del producto y, a la vez, ajuste +5 | — | El ajuste espera, ve el producto inactivo y rechaza |

## 10. Próximos pasos

1. **`sql/02_datos_prueba.sql`:** el administrador precargado (requiere definir el algoritmo de hash) y el depósito inicial, más los datos de la demo de aceptación.
2. **Capa DAO:** mapear los códigos de error (1062, 1452, 3819, 1142, 1213, 1205) a errores de negocio con mensaje claro.
3. **Clase de conexión:** comprobar el modo estricto al conectarse (sección 8.2).
4. **Pruebas:** sumar las carreras de la sección 5.4 a las pruebas de concurrencia del plan.

## 11. Cambios

| Versión | Cambios |
|---|---|
| 0.1 | Primera versión. |
| 0.2 | Se corrige la definición de clave primaria compuesta (5.2). Se agrega el control de concurrencia (5.4), la sección de normalización (6), el modo estricto (8.2) y la cuenta de la aplicación con permisos mínimos (8.3). El faltante pasa a `GREATEST(stockMinimo − cantidad, 0)`. El borrado de la base sale de `01_schema.sql` y pasa a `00_reiniciar.sql` (8.4). |
| 0.3 | Se corrige la definición de FNBC: el determinante de toda dependencia no trivial es superclave (6). Se precisan los permisos de las lecturas bloqueantes: `FOR SHARE` pide solo `SELECT` (8.3). `03_usuario_app.sql` quita los permisos anteriores antes de dar los suyos (8.3). Se documenta que los scripts se ejecutan con detención ante el primer error (8.4). |
| 0.4 | Se aclara que la cuenta de la aplicación tiene que estar sin roles, porque el `REVOKE` no los quita, y cómo comprobarlo y corregirlo (8.3). |
