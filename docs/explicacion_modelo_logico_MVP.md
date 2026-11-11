# Modelo lógico del MVP (v0.1): cómo se deriva del DER

Estado: borrador — revisión del documento: 0.2 — modelo lógico: v0.1 — última actualización: 04/11/2026 — base: DER del MVP v0.1 (`der_mvp.drawio`), Diseño de base de datos v0.3 (DD)


## 1. Qué es

El modelo lógico pasa el DER (conceptual, en notación de Chen) a **tablas**. Se expresa en dos partes:

- **Esquema por intensión:** cada tabla con su nombre y su lista de atributos. La **clave primaria va subrayada** (en este documento, en negrita).
- **Camino referencial:** las **claves foráneas**, con una flecha por cada atributo que referencia, que llega a la clave primaria referenciada. Una clave foránea compuesta se dibuja con una flecha por cada uno de sus atributos.

## 2. Esquema por intensión

CATEGORIA(**idCategoria**, nombreCategoria, estadoCategoria)

PRODUCTO(**idProducto**, codigoProducto, idCategoria, nombreProducto, descripcion, precioReferenciaCompra, precioReferenciaVenta, estadoProducto)

DEPOSITO(**idDeposito**, nombreDeposito, direccion, estadoDeposito)

STOCK(**idProducto**, **idDeposito**, cantidad, stockMinimo)

MOVIMIENTO_STOCK(**idMovimiento**, idProducto, idDeposito, idUsuario, fechaHoraMovimiento, tipoMovimiento, cantidad, motivo, idMovimientoCorregido)

USUARIO(**idUsuario**, nombreUsuario, hashContrasena, rol, estadoUsuario)

AUDITORIA(**idAuditoria**, idUsuario, fechaHoraAuditoria, accion, entidadAfectada, registroAfectado, descripcionCambio)

## 3. Camino referencial

```
PRODUCTO.idCategoria                    → CATEGORIA.idCategoria
STOCK.idProducto                        → PRODUCTO.idProducto
STOCK.idDeposito                        → DEPOSITO.idDeposito
MOVIMIENTO_STOCK.idProducto             → STOCK.idProducto
MOVIMIENTO_STOCK.idDeposito             → STOCK.idDeposito
MOVIMIENTO_STOCK.idUsuario              → USUARIO.idUsuario
MOVIMIENTO_STOCK.idMovimientoCorregido  → MOVIMIENTO_STOCK.idMovimiento
AUDITORIA.idUsuario                     → USUARIO.idUsuario
```

Hay 7 tablas y **7 claves foráneas, dibujadas con 8 flechas**. Las líneas `MOVIMIENTO_STOCK.idProducto` y `MOVIMIENTO_STOCK.idDeposito` no son dos claves foráneas independientes: son los dos atributos de **una única clave foránea compuesta**:

```
MOVIMIENTO_STOCK.(idProducto, idDeposito) → STOCK.(idProducto, idDeposito)
```

Las tablas están ordenadas siguiendo las dependencias: primero los maestros y, después, las tablas que los referencian (STOCK, MOVIMIENTO_STOCK y AUDITORIA). USUARIO queda entre las dos tablas que lo referencian para que sus flechas sean cortas.

## 4. Cómo se pasó del DER a las tablas

### 4.1 Entidades → tablas

Cada entidad del DER genera una tabla con el mismo nombre y los atributos de su composición en el DD:

| Entidad del DER | Tipo | Tabla | Clave primaria |
|---|---|---|---|
| CATEGORIA | fuerte | CATEGORIA | idCategoria |
| PRODUCTO | fuerte | PRODUCTO | idProducto |
| DEPOSITO | fuerte | DEPOSITO | idDeposito |
| STOCK | débil | STOCK | idProducto + idDeposito (compuesta) |
| MOVIMIENTO_STOCK | fuerte | MOVIMIENTO_STOCK | idMovimiento |
| USUARIO | fuerte | USUARIO | idUsuario |
| AUDITORIA | fuerte | AUDITORIA | idAuditoria |

En las **entidades fuertes**, el identificador subrayado en el DER pasa a ser la clave primaria.

**STOCK es una entidad débil** y no tiene identificador propio: en el DER se identifica a través de sus dos relaciones identificadoras (DE_PRODUCTO y EN_DEPOSITO). Por la regla de la entidad débil, su clave primaria se forma con las claves de las entidades dueñas: `(idProducto, idDeposito)`. Esos dos atributos son, a la vez, parte de la clave primaria y claves foráneas hacia PRODUCTO y DEPOSITO. Como no hay discriminante, la clave compuesta garantiza que exista **a lo sumo un STOCK por cada par producto–depósito** (restricción 4 del DER), sin necesidad de un `UNIQUE` aparte.

### 4.2 Relaciones → claves foráneas

Todas las relaciones del DER del MVP son **1:N** (o identificadoras). Por eso **ninguna genera una tabla propia**: la clave primaria del lado 1 se agrega como clave foránea en la tabla del lado N.

| Relación del DER | Cardinalidad | Resultado en el modelo lógico |
|---|---|---|
| PERTENECE | PRODUCTO (1,1) — CATEGORIA (0,N) | `idCategoria` en PRODUCTO → CATEGORIA |
| DE_PRODUCTO (identificadora) | STOCK (1,1) — PRODUCTO (0,N) | `idProducto` en STOCK, parte de su clave primaria → PRODUCTO |
| EN_DEPOSITO (identificadora) | STOCK (1,1) — DEPOSITO (0,N) | `idDeposito` en STOCK, parte de su clave primaria → DEPOSITO |
| AFECTA | MOVIMIENTO_STOCK (1,1) — STOCK (0,N) | `(idProducto, idDeposito)` en MOVIMIENTO_STOCK → STOCK (clave foránea compuesta) |
| ES_RESPONSABLE | MOVIMIENTO_STOCK (1,1) — USUARIO (0,N) | `idUsuario` en MOVIMIENTO_STOCK → USUARIO |
| CORRIGE (recursiva) | original (0,N) — compensatorio (0,1) | `idMovimientoCorregido` en MOVIMIENTO_STOCK → MOVIMIENTO_STOCK |
| AUDITA | AUDITORIA (1,1) — USUARIO (0,N) | `idUsuario` en AUDITORIA → USUARIO |

Como no hay relaciones N:M, ni atributos multivaluados, de las 7 relaciones no sale ninguna tabla nueva. Por eso el modelo lógico tiene las mismas 7 tablas que entidades tiene el DER.

### 4.3 Decisiones 

- **MOVIMIENTO_STOCK referencia a STOCK, no a PRODUCTO y DEPOSITO por separado.** En el DER, el movimiento se relaciona con STOCK (AFECTA). Como la clave de STOCK es compuesta, la clave foránea también lo es: `(idProducto, idDeposito)`, y en el diagrama se dibuja **una flecha por atributo**. Así la base garantiza que solo se registren movimientos de un producto habilitado en ese depósito: si la combinación no existe en STOCK, la integridad referencial rechaza el movimiento.
- **CORRIGE es una autorreferencia.**. El compensatorio es el lado N; el original, el lado 1. Por eso la clave foránea va en el compensatorio y apunta al original. El nombre del atributo indica el rol: `idMovimientoCorregido`. En un ajuste común queda nulo.
- **La clave foránea conserva el nombre de la clave que referencia** (`idCategoria`, `idProducto`, `idDeposito`, `idUsuario`). La única excepción es `idMovimientoCorregido`, porque la tabla ya tiene un `idMovimiento` propio y se necesita otro nombre para el rol.
- **`registroAfectado` de AUDITORIA no es clave foránea.** Guarda como texto el identificador del registro afectado, que puede ser de cualquier entidad (DD AUDITORIA). Por eso no tiene flecha.

## 5. Trazabilidad con el DD

Todos los atributos salen de la composición del DD (documento 03), con el mismo nombre en camelCase. Hay dos diferencias respecto de esa composición, ambas por el recorte del MVP:

- **MOVIMIENTO_STOCK no lleva `idCompra`, `idVenta` ni `idTransferencia`.** En el DD son las claves foráneas de las relaciones GENERA_COMPRA, GENERA_VENTA y GENERA_TRANSFERENCIA, que quedaron fuera del DER del MVP junto con esas tablas. Sin la tabla referenciada no puede existir la clave foránea. El resto de los atributos de cada entidad se conserva, aunque el MVP no use alguno (precios de referencia, rol), como se decidió en el DER.
- **No se almacenan atributos derivados:** el stock total (suma de la cantidad de los STOCK de un producto), la situación de stock bajo (cantidad menor que stockMinimo) y el faltante se calculan en las consultas.

Para completar el modelo después, se agregan las tablas que faltan y, en MOVIMIENTO_STOCK, las claves foráneas `idCompra`, `idVenta` e `idTransferencia`. La regla de origen abarca los **cuatro** campos: `idCompra`, `idVenta`, `idTransferencia` e `idMovimientoCorregido`; a lo sumo uno puede tener valor, y un ajuste común los tiene todos nulos. En ese momento `motivo` pasa a depender del origen: sigue siendo obligatorio en ajustes y compensatorios, y queda nulo en los movimientos generados por operaciones.

## 6. Qué no muestra el modelo lógico (queda para el relacional)

El formato de la cátedra muestra tablas, claves primarias y claves foráneas. Lo siguiente se define en el **modelo relacional** (especificación de dominios: tabla por relación con Atributo | Descripción | Dominio) y en el script SQL:

- **Dominios y tipos:** enteros, cadenas con su tamaño, `DECIMAL` con dos decimales para los precios, fecha y hora para `fechaHora…`, y enumerados para `estado…`, `rol`, `tipoMovimiento`, `accion` y `entidadAfectada`.
- **Opcionalidad (nulos):** `descripcion`, `direccion` e `idMovimientoCorregido` admiten nulo; `idMovimientoCorregido` queda nulo cuando el ajuste no corrige otro movimiento. El resto es NO NULO.
- **`motivo` es NO NULO en el MVP** y no puede quedar vacío después de quitar espacios. En el DD figura como opcional (`[motivo]`) porque en el producto completo los movimientos generados por operaciones no lo registran; en el MVP todos los movimientos son ajustes y siempre lo requieren (restricción 1 del DER).
- **Claves alternativas (`UNIQUE`):** `codigoProducto`, `nombreCategoria`, `nombreDeposito` y `nombreUsuario`.
- **Restricciones `CHECK` y reglas de negocio:** cantidades y mínimos válidos, motivo no vacío, tipo de movimiento limitado a ajustes en el MVP, stock nunca negativo, etc. Están en la sección 5 de la explicación del DER.

## 7. También ya quedo hecho:

1. **Modelo relacional:** especificación de dominios de las 7 tablas, a partir de este esquema y de las restricciones del DER.
2. **`sql/01_schema.sql`:** crear las tablas en el orden del camino referencial (primero las referenciadas): CATEGORIA, DEPOSITO, USUARIO, PRODUCTO, STOCK, MOVIMIENTO_STOCK y AUDITORIA.
