# DER del MVP (v0.1): en qué se basa y cómo se armó

Estado: cerrado — versión 0.1 — última actualización: 29/10/2026 — base: Análisis del dominio v1.5, Requisitos v1.8, Diseño de base de datos v0.3

Ámbito: entrega de Programación Orientada a Objetos (18/11/2026). No forma parte de la documentación del producto ni reemplaza a la versión V1 definida en Requisitos.

Este documento explica el DER del MVP de controlStock: de dónde sale, con qué criterio se eligió el subconjunto, qué contiene y qué reglas lo acompañan. Sirve de base para el modelo lógico, los scripts SQL y las clases Java, sin tener que reconstruir las decisiones.

## 1. Contexto

- **Proyecto:** controlStock, un sistema de control de stock en Java + Maven + MySQL (JDBC), desarrollado por Santiago y Joaquin. Empezó como proyecto universitario y la idea es venderlo después.
- **Por qué existe un MVP:** para aprobar Programación Orientada a Objetos hay que entregar, el **18/11/2026**, un producto mínimo que funcione, conectado a la base de datos y hecho con lo que se vio en Java. No hay consigna formal; el profesor pide que funcione y que use la base de datos. No hace falta una tabla de conceptos de POO.
- **Por qué es un subconjunto:** no hay tiempo para implementar todo el sistema antes del 18/11. Se toma la parte del modelo que permite demostrar un recorrido completo, y el resto queda en el modelo completo del proyecto.

## 2. Relación con la documentación del proyecto

El MVP es una entrega académica para la materia Programación Orientada a Objetos, no una versión del producto. Su alcance se toma de la V1, pero las versiones V1, V2 y V3 se definen en el documento 02 (sección 2.2), y el DER completo con sus vistas (`docs/diagramas/der_chen.drawio`) es el modelo vigente del sistema. Este documento y `der_mvp.drawio` no modifican ninguna decisión de los documentos 01 a 03.

En particular, la implementación de la base de datos completa desde V1 (02, sección 1.3) y la carga inicial de «Consumidor final» (RNF-28) se aplican al producto, no al MVP: para la entrega de POO solo se crean las siete tablas de este subconjunto. Si el código del MVP se reutiliza como punto de partida de V1, antes se completa el esquema con las entidades que faltan.

## 3. En qué se basa

El DER del MVP **no es un modelo nuevo**: es un **recorte del DER conceptual completo** del proyecto (`docs/diagramas/der_chen.drawio`, 5 páginas: integrado + vistas 1, 2a, 2b y 3). Ese DER completo se armó a partir de:

- **01 – Análisis del dominio (v1.5):** supuestos S-1 a S-28 (por ejemplo, S-12: solo se modifica o anula una operación pendiente; S-14: un producto no se repite en una misma operación).
- **02 – Requisitos (v1.8):** requisitos funcionales (RF), no funcionales (RNF) y supuestos de requisitos (SR).
- **03 – Diseño de base de datos (v0.3):** Diccionario de Datos; de ahí salen los nombres de entidades y atributos (composición en camelCase).

Y el recorte se definió a partir del **plan del MVP** (`docs/plan_mvp.md`), según el alcance por prioridad:

| Prioridad | Funcionalidades |
|---|---|
| Imprescindible | Inicio y cierre de sesión con un administrador precargado; alta, modificación, listado y baja lógica de categorías, productos y depósitos; habilitación de productos en depósitos y definición de mínimos; ajustes positivos y negativos en transacción; consulta por depósito, stock total y stock bajo |
| Importante | Movimientos compensatorios; auditoría automática; historial de movimientos con filtros |
| Si alcanza | Reactivación; inactivos en consultas; navegación al movimiento corregido; búsqueda avanzada |

Quedan fuera del MVP: compras, ventas, transferencias, clientes, proveedores, administración de usuarios, permisos por rol, reportes y exportaciones.

**Criterio del recorte:** se incluye una entidad o relación del DER completo si alguna funcionalidad del MVP (imprescindible o importante) la necesita para guardar o consultar datos. Todo lo que se incluye se copia **igual** que en el DER completo: mismos nombres, mismos atributos, mismas claves y mismas cardinalidades. Así, el modelo del MVP nunca contradice al del proyecto. Conservar nombres y claves facilita completarlo después, aunque eso va a requerir ajustar restricciones, referencias y servicios (ver la sección 6).

## 4. Qué contiene

Archivo: `der_mvp.drawio` (página «DER MVP (v0.1)») y su exportación `der_mvp.png`. Van en la carpeta `docs/` del repositorio de la entrega de POO, separado del repositorio del producto, para no mezclarlos con el DER completo.

Notación de Chen: rectángulo = entidad; rectángulo doble = entidad débil; rombo = relación; rombo doble = relación identificadora; óvalo = atributo; subrayado = identificador; (mín,máx) junto a cada entidad = en cuántas instancias de la relación participa cada instancia de esa entidad; línea doble = participación total.

### 4.1 Entidades (7)

| Entidad | Atributos (identificador primero) | Para qué la necesita el MVP |
|---|---|---|
| CATEGORIA | idCategoria, nombreCategoria, estadoCategoria | ABM de categorías; todo producto tiene una |
| PRODUCTO | idProducto, codigoProducto, nombreProducto, descripcion, precioReferenciaCompra, precioReferenciaVenta, estadoProducto | ABM de productos |
| DEPOSITO | idDeposito, nombreDeposito, direccion, estadoDeposito | ABM de depósitos |
| STOCK (débil) | cantidad, stockMinimo | Habilitación, mínimos, saldos y stock bajo |
| MOVIMIENTO_STOCK | idMovimiento, fechaHoraMovimiento, tipoMovimiento, cantidad, motivo | Ajustes, compensatorios e historial |
| USUARIO | idUsuario, nombreUsuario, hashContrasena, rol, estadoUsuario | Inicio de sesión y responsable de cada acción |
| AUDITORIA | idAuditoria, fechaHoraAuditoria, accion, entidadAfectada, registroAfectado, descripcionCambio | Auditoría automática de cambios sobre maestros |

Las entidades conservan **todos** sus atributos del DD, aunque el MVP no use alguno (por ejemplo, los precios de referencia o el rol). Sacarlos obligaría a modificar las tablas si después se completa el modelo.

### 4.2 Relaciones (7)

| Relación | Participantes y cardinalidad | Significado |
|---|---|---|
| PERTENECE | PRODUCTO (1,1) total — CATEGORIA (0,N) | Todo producto tiene exactamente una categoría; una categoría puede tener ninguno o muchos productos |
| DE_PRODUCTO (identificadora) | STOCK (1,1) total — PRODUCTO (0,N) | Cada STOCK es de un producto; un producto puede estar habilitado en ninguno o muchos depósitos |
| EN_DEPOSITO (identificadora) | STOCK (1,1) total — DEPOSITO (0,N) | Cada STOCK está en un depósito; un depósito puede tener ninguno o muchos productos habilitados |
| AFECTA | MOVIMIENTO_STOCK (1,1) total — STOCK (0,N) | Cada movimiento cambia la cantidad de un único par producto–depósito |
| ES_RESPONSABLE | MOVIMIENTO_STOCK (1,1) total — USUARIO (0,N) | Todo movimiento tiene un usuario responsable |
| CORRIGE (recursiva) | MOVIMIENTO_STOCK rol *original* (0,N) — rol *compensatorio* (0,1) | Un movimiento compensatorio corrige a un original; un original puede tener uno o más compensatorios |
| AUDITA | AUDITORIA (1,1) total — USUARIO (0,N) | Cada registro de auditoría tiene el usuario que hizo la acción |

### 4.3 Decisiones de modelado que conviene entender

- **STOCK es entidad débil**, identificada en conjunto por PRODUCTO y DEPOSITO, sin discriminador: existe **a lo sumo un STOCK por cada par producto–depósito**. «Habilitar un producto en un depósito» es crear esa instancia, con cantidad inicial 0. Se modeló como entidad débil y no como relación N:M con atributos porque MOVIMIENTO_STOCK necesita relacionarse con STOCK (AFECTA), y en Chen una relación no puede participar en otra relación.
- **Los movimientos no se editan ni se borran.** Un error se corrige con uno o más movimientos compensatorios nuevos, cada uno vinculado al original mediante CORRIGE. El original queda intacto. Por eso CORRIGE es recursiva, (0,1) del lado compensatorio y (0,N) del lado original (DD MOVIMIENTO_STOCK y RF-21).
- **El compensatorio no tiene que ser del tipo opuesto.** Se registra como ajuste positivo o negativo según el sentido de la corrección (S-19): si se cargó un ajuste positivo de 10 y eran 12, se corrige con un ajuste positivo de 2.
- **Corrección entre depósitos (decisión del MVP).** S-19 no dice si un compensatorio puede afectar a un STOCK distinto del original. En el MVP se permite: un ajuste cargado en el depósito equivocado se corrige con un compensatorio negativo en ese depósito y uno positivo en el correcto, los dos vinculados al original. Cada compensatorio es una acción independiente y se guarda en su propia transacción junto con su saldo (RNF-01); si el segundo no se puede registrar, el primero sigue siendo válido y el segundo se registra después.
- **El rol del usuario es un atributo**, no una entidad (roles fijos). En el MVP solo existe el administrador precargado y no hay permisos por rol.
- **MOVIMIENTO_STOCK y AUDITORIA son cosas distintas.** Los movimientos registran cambios físicos de stock y son su propio historial. La auditoría registra cambios sobre los datos maestros. Por eso un movimiento no genera un registro de auditoría (no se duplica la información).

## 5. Restricciones que el diagrama no expresa

Están en el recuadro de notas del diagrama. En la implementación van como `CHECK`, `UNIQUE` o `NOT NULL` en MySQL (≥ 8.0.16, que hace cumplir los `CHECK`) o en la capa de servicio de Java:

1. En el MVP todos los movimientos son **ajustes**: `tipoMovimiento` admite solo *ajuste positivo* y *ajuste negativo* (también los compensatorios). Entrada y salida están en el modelo completo, ligadas a las operaciones. El **motivo es obligatorio** y no puede quedar vacío (después de quitar espacios).
2. Ningún movimiento puede dejar la cantidad de su STOCK **por debajo de cero**.
3. Un movimiento compensatorio queda relacionado con el original que corrige y **no puede corregirse a sí mismo**. Esta regla se valida en el servicio: MySQL no permite que un `CHECK` use una columna `AUTO_INCREMENT` como `idMovimiento`.
4. Existe **a lo sumo un STOCK** por combinación producto–depósito.
5. Solo se habilita, se ajusta y se compensa stock con producto y depósito **activos** (SR-19).
6. Los movimientos no generan registros de auditoría. Sí generan auditoría las altas, modificaciones y bajas de maestros, las habilitaciones y los cambios de stock mínimo.

Los dominios de las cantidades salen del DD: la cantidad de un movimiento es un entero mayor que cero (el signo lo da `tipoMovimiento`), y la cantidad y el stock mínimo de STOCK son enteros mayores o iguales que cero. Se refuerzan con `CHECK` (RNF-04).

La unicidad de `codigoProducto`, `nombreCategoria`, `nombreDeposito` y `nombreUsuario` (DD) va como `UNIQUE` en la base, además de validarse en el servicio para mostrar un mensaje claro.

Reglas de negocio del plan que se validan en el servicio (no son del DER, pero dependen de él):

- Categoría activa al asignarla a un producto.
- Baja (lógica) de producto o depósito solo si su stock es cero; tiene que quedar al menos un depósito activo.
- Una categoría inactiva no impide ajustar el stock de sus productos activos.
- Para compensar un movimiento de un producto o depósito inactivo, primero hay que reactivarlo (SR-19). La reactivación está en «si alcanza»: si no entra en el alcance final (corte del 08/11), esos movimientos no se van a poder corregir en el MVP.
- El saldo de STOCK y el movimiento se guardan en la **misma transacción** (el servicio la controla y le pasa la misma conexión a todos los DAO). Para evitar problemas de concurrencia se bloquea la fila de STOCK con `SELECT ... FOR UPDATE`.

## 6. Qué queda fuera del MVP

No están en el DER del MVP, y siguen en el DER completo: PROVEEDOR, CLIENTE, COMPRA, VENTA, TRANSFERENCIA, DETALLE_COMPRA, DETALLE_VENTA, DETALLE_TRANSFERENCIA, SUMINISTRAR, SE_COMPRA_A, RECIBE, SE_VENDE_A, SALE_DE, ORIGEN, DESTINO, las 12 relaciones de responsables (REGISTRA_, MODIFICA_, CONFIRMA_, ANULA_ por cada operación) y GENERA_COMPRA, GENERA_VENTA y GENERA_TRANSFERENCIA.

Como el MVP usa los mismos nombres y claves, para completar el modelo se agregan esas entidades y relaciones. No alcanza con agregar: también cambian restricciones de MOVIMIENTO_STOCK y hay que adaptar los servicios. Las dos restricciones que cambian son:

- `tipoMovimiento` suma *entrada* y *salida*.
- La regla del motivo pasa a depender del origen: obligatorio en ajustes y compensatorios, y nulo en los movimientos generados por operaciones. Además, cada movimiento tiene a lo sumo un origen (una operación o el movimiento que corrige).

«Consumidor final» no se carga en el MVP: ver la sección 2.

## 7. Próximos pasos

1. **Modelo lógico** del subconjunto: esquema por intensión con el camino referencial. Sale casi directo de las tablas anteriores; STOCK queda con clave (idProducto, idDeposito) y MOVIMIENTO_STOCK referencia a STOCK con esa clave compuesta.
2. **`sql/01_schema.sql`** con las restricciones de la sección 5 y **`sql/02_datos_prueba.sql`** con el administrador y el depósito inicial.
3. **Proyecto Maven** organizado en capas (modelo, dao, servicio, ui, util).
