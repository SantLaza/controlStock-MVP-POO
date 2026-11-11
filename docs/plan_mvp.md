# controlStock — Plan MVP para POO

**Integrantes:** Santiago Lazarte y Joaquin Curihuala.  
**Entrega objetivo:** 18/11/2026.  
**Primera versión completa e integrada:** 13/11/2026.  
**Corte de alcance:** 08/11/2026.  
**Última actualización del plan:** 05/11/2026 (ver la sección 12).

## 1. Objetivo

Desarrollar una aplicación de consola que permita administrar productos y controlar sus existencias en distintos depósitos.

El MVP deberá demostrar un recorrido completo: acceder al sistema, registrar maestros, habilitar un producto en un depósito, cargar y ajustar existencias y consultar el resultado.

Para POO, se acompañará el funcionamiento con un diseño de clases que permita explicar el encapsulamiento, las responsabilidades de cada clase, la colaboración entre objetos y el uso de interfaces.

La entrega será un **subconjunto priorizado de la V1** del proyecto, sin ser una versión del producto (ver `explicacion_DER_MVP.md`, sección 2). Las funciones y pruebas que queden pendientes conservarán su referencia a los requisitos del proyecto.

## 2. Tecnología

- Java 17 y Maven.
- JDBC y MySQL y **modo estricto** (`STRICT_TRANS_TABLES`).
- Interfaz de consola en español.
- JUnit 5 para pruebas de servicios.
- Git y GitHub, trabajando los dos sobre la rama `main` (sección 6).

La versión mínima de MySQL se indica en el README porque los `CHECK` se hacen cumplir desde 8.0.16. [Documentación oficial](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html). El modo estricto es necesario porque sin él MySQL acepta valores inválidos en los `ENUM` y solo emite una advertencia (`modelo_relacional_mvp.md`, sección 8.2).

## 3. Alcance por prioridad

| Prioridad | Funcionalidades |
|---|---|
| **Imprescindible** | Base ejecutable; inicio y cierre de sesión con un administrador; alta, modificación, listado y baja lógica de categorías, productos y depósitos; habilitación y mínimos; ajustes positivos y negativos en transacción; consultas de stock por depósito, total y stock bajo |
| **Importante** | Compensaciones; auditoría automática; historial de movimientos con filtros |
| **Si alcanza** | Reactivación; inclusión de inactivos en consultas; navegación al movimiento corregido; búsqueda avanzada de productos |

Las validaciones esenciales forman parte del núcleo:

- Códigos y nombres únicos según corresponda.
- Categoría activa al asignarla a un producto.
- Producto y depósito activos para habilitar o ajustar.
- Una única combinación de stock por producto y depósito.
- Cantidades enteras válidas y stock nunca negativo.
- Baja de productos y depósitos únicamente con existencias en cero.
- Conservación de al menos un depósito activo.
- Movimientos registrados que no se editan ni eliminan.

Compras, ventas, transferencias, gestión de clientes y proveedores, administración de usuarios, permisos por rol, reportes y exportaciones quedan fuera del MVP.

El **08/11**, ambos integrantes fijarán el alcance final según lo que ya funciona y el tiempo restante. Cada bloque adicional deberá poder completarse, integrarse y probarse antes del 13/11. Si entran las compensaciones pero no la reactivación, no se podrán corregir movimientos de productos o depósitos inactivos.

## 4. Diseño orientado a objetos

La aplicación seguirá el recorrido:

**Interfaz → Servicios → DAO → Base de datos**

Las clases del modelo representarán los conceptos del dominio.

| Componente | Responsabilidad |
|---|---|
| `modelo` | Representar Usuario, Categoría, Producto, Depósito, Stock y MovimientoStock, y los enumerados Estado, TipoMovimiento y Rol; encapsular sus datos y comportamientos |
| `dao` | Definir los contratos de persistencia (interfaces) y, en `dao.jdbc`, implementarlos con JDBC y consultas parametrizadas |
| `servicio` | Coordinar los casos de uso, validar reglas entre objetos, controlar las transacciones y los bloqueos, e informar los errores de negocio con `ReglaNegocioException` |
| `ui` | Leer datos, mostrar resultados y solicitar confirmaciones utilizando los servicios |
| `util` | Proveer conexión, configuración (lectura de `db.properties`) y utilidades compartidas |

Se entregará un **diagrama de clases consistente con el código**, con atributos relevantes, métodos, asociaciones e interfaces.

Los servicios dependerán de interfaces DAO, permitiendo utilizar implementaciones de prueba en memoria. La herencia y las clases abstractas se justificarán según las relaciones del dominio y la consigna de la materia.

Cada módulo tendrá su propio submenú (`MenuMaestros`, `MenuStock`, `MenuConsultas`). El menú principal se arma en la base de código, ya conectado a los tres submenús aunque estén vacíos, para que después nadie necesite modificarlo (sección 6.3).

## 5. Acuerdos de diseño

**Base de datos.** El diseño está cerrado y documentado en `docs/`: DER del MVP, modelo lógico y modelo relacional (dominios, claves, restricciones, normalización y control de concurrencia). Los scripts de `sql/` lo implementan (sección 6.4).

**Restricciones de movimientos.** En el MVP todos los movimientos son ajustes, así que no hace falta la restricción de origen del modelo completo:

| Campo | Regla |
|---|---|
| `tipoMovimiento` | Ajuste positivo o ajuste negativo |
| `cantidad` | Entero mayor que cero |
| `motivo` | Obligatorio: no nulo y no vacío después de quitar espacios |
| `idMovimientoCorregido` | Opcional; solo lo tienen los compensatorios |
| `idProducto`, `idDeposito` | Referencia compuesta a STOCK |

Que un movimiento no se corrija a sí mismo se valida en el servicio, porque MySQL no permite usar una columna `AUTO_INCREMENT` en un `CHECK`.

**Transacciones.** El servicio controlará la transacción. Todos los DAO participantes utilizarán la misma conexión: el cambio de saldo y el movimiento se guardarán juntos o se revertirán juntos.

**Concurrencia.** Validar dentro de una transacción no alcanza: un `SELECT` común no bloquea y dos sesiones pueden validar a la vez sobre los mismos datos. Los casos de uso que comprueban un estado o un saldo y después escriben usan lecturas bloqueantes (`FOR SHARE` / `FOR UPDATE`), siempre en el orden **CATEGORIA → PRODUCTO → DEPOSITO → STOCK**. La tabla de bloqueos por caso de uso está en `modelo_relacional_mvp.md`, sección 5.4. Ante un deadlock (error 1213) se hace `ROLLBACK` y se reintenta una vez.

**Auditoría.** Si se incluye ese bloque, el cambio y su auditoría se guardarán en la misma transacción. Preparar una tabla o interfaz no contará como auditoría implementada.

**Conexión y credenciales.**

- La aplicación se conecta con la cuenta `controlstock_app`, que no puede borrar en ninguna tabla y en MOVIMIENTO_STOCK y AUDITORIA solo puede leer e insertar. Nunca se conecta como `root`.
- La configuración de conexión va en `db.properties`, que está en el `.gitignore`. En el repositorio se versiona `db.properties.example`, con las mismas claves y sin contraseña.
- Al conectarse, la clase de conexión comprueba el modo estricto; si falta, la aplicación no arranca.

**Contratos compartidos.** Se definen en la base de código, antes de repartir el trabajo: nombres de clases y paquetes, identificadores, errores de negocio (`ReglaNegocioException` y la traducción de los errores de MySQL 1062, 1452, 3819, 1142, 1213 y 1205) y cómo llega el usuario de la sesión a los servicios. Usar importes decimales exactos (`BigDecimal`), contraseñas con hash y sal y configuración sin credenciales versionadas.

## 6. Organización del trabajo

### 6.1 Una sola rama

Por el tiempo disponible, los dos trabajan directamente en `main`, sin ramas de funcionalidad ni pull requests. Las fusiones no desaparecen: se hacen en cada `git pull`, pero son chicas y frecuentes. Para que funcione:

- **Antes de empezar:** `git switch main` y `git pull`.
- **Mientras se trabaja:** commits chicos, cada uno con un avance que compile. En `git add` se nombran los archivos uno por uno, para no subir `db.properties` ni archivos del IDE.
- **Para subir:** `git pull` y después `git push`, por lo menos una vez por día.
- `git config pull.rebase true` en las dos computadoras, para que el historial quede en línea recta.
- **Nunca** `git push --force` sobre `main`.
- **Revisión:** cada uno lee los commits del otro en GitHub (pestaña Commits). Antes del corte del 08/11 y antes del 13/11 se hace una revisión conjunta.

### 6.2 Reparto

| Responsable | Bloque | Archivos |
|---|---|---|
| Ambos | Base de código | `pom.xml`, `util/`, enumerados, `ReglaNegocioException`, `Main`, `MenuPrincipal`, acceso (inicio y cierre de sesión) y `sql/02_datos_prueba.sql` |
| Integrante A | Maestros | Categoría, Producto y Depósito en `modelo`, `dao`, `dao/jdbc`, `servicio`, y `MenuMaestros` |
| Integrante B | Stock | Stock y MovimientoStock en `modelo`, `dao`, `dao/jdbc`, `servicio`, y `MenuStock` (habilitación, mínimos, ajustes y validaciones) |
| Quien libere capacidad primero | Consultas | `MenuConsultas` y las consultas de stock por depósito, total y stock bajo |

Santiago y Joaquin definirán quién ocupa A y B según disponibilidad. Los paquetes van por capa, pero los archivos van por módulo: repartiendo por módulo, cada uno trabaja en sus propios archivos y Git junta los cambios sin conflictos.

### 6.3 Archivos compartidos

`pom.xml`, `util/`, `Main`, `MenuPrincipal`, los enumerados, `ReglaNegocioException`, los scripts de `sql/`, el README y los documentos de `docs/` los usan los dos. Para no pisarse:

1. **Se arman primero, en la base de código**, antes de que cada uno empiece su módulo. El menú principal queda ya conectado a los tres submenús vacíos.
2. Después, **los modifica uno solo a la vez, avisándole antes al otro**.

### 6.4 Estructura del repositorio

```
controlStock-MVP-POO/
├── pom.xml
├── README.md
├── .gitignore
├── db.properties.example        ← plantilla sin contraseña (se versiona)
├── docs/                        ← plan, DER, modelo lógico, modelo relacional, diagrama de clases, pruebas
├── sql/
│   ├── 00_reiniciar.sql         ← borra la base (solo para reinstalar)
│   ├── 01_schema.sql            ← crea la base y las 7 tablas
│   ├── 02_datos_prueba.sql      ← administrador, depósito inicial y datos de la demo
│   └── 03_usuario_app.sql       ← cuenta controlstock_app con permisos mínimos
└── src/
    ├── main/java/controlstock/
    │   ├── Main.java
    │   ├── modelo/
    │   ├── dao/        (interfaces)
    │   │   └── jdbc/   (implementaciones)
    │   ├── servicio/
    │   ├── ui/
    │   └── util/
    └── test/java/controlstock/
        └── servicio/   (pruebas JUnit con DAO en memoria)
```

Los scripts se ejecutan con una cuenta administradora y con detención ante el primer error, en el orden que indica el README. El `.gitignore` ignora `db.properties`, `target/` y las carpetas de configuración del IDE que use cada uno.

## 7. Calendario

| Fechas | Hito | Estado |
|---|---|---|
| **26/10–05/11** | Documentación alineada; DER, modelo lógico y modelo relacional; scripts `00`, `01` y `03` probados | Hecho |
| **05–07/11** | Base de código: Maven, conexión, configuración, contratos compartidos, menú principal y acceso; `02_datos_prueba.sql`; instalación desde cero en las dos computadoras. Al quedar integrada, empiezan maestros y stock en paralelo | Pendiente |
| **08/11** | Fijar el alcance final y registrar pendientes | Pendiente |
| **08–11/11** | Integrar maestros, stock y consultas imprescindibles, y los bloques seleccionados; ejecutar una primera secuencia completa | Pendiente |
| **12–13/11** | Cerrar pruebas y ejecutar la demo completa | Pendiente |
| **14–17/11** | Corregir defectos, comprobar instalación y ensayar la presentación | Pendiente |
| **18/11** | Entregar la versión etiquetada `v0.1-mvp` | Pendiente |

La integración será continua. El 13/11 deberá funcionar todo el alcance elegido.

El calendario original preveía la base integrada para el 31/10 y maestros, stock y consultas entre el 01 y el 07/11. La base de datos quedó lista el 05/11, pero el código empieza recién ahora. Por eso, el corte del 08/11 se hará con la base de código recién integrada, y los bloques importantes solo deberían sumarse si el núcleo imprescindible avanza según lo previsto.

## 8. Pruebas prioritarias

Se documentarán los casos de lo imprescindible, vinculados con sus requisitos y reglas de negocio, en `docs/pruebas.md`.

Las verificaciones principales serán:

- Instalación desde cero con los scripts en las dos computadoras.
- Acceso válido, acceso rechazado y cierre de sesión.
- Maestros válidos, duplicados y restricciones de baja.
- Habilitación duplicada y mínimos inválidos.
- Ajustes válidos, cantidades inválidas, motivo vacío y stock insuficiente.
- Reversión completa ante un fallo entre actualización de saldo y registro del movimiento.
- Concurrencia:
  - Con saldo 3, dos ajustes simultáneos de −2 permiten uno y rechazan el otro; saldo final 1 y un solo movimiento nuevo.
  - Con dos depósitos activos, dos bajas simultáneas de depósitos distintos permiten una y rechazan la otra; queda un depósito activo.
  - Un ajuste y la baja del mismo producto, al mismo tiempo, nunca dejan un producto inactivo con existencias.
- Arranque rechazado si el servidor no está en modo estricto.
- Consultas correctas y persistencia después de reiniciar.

Las restricciones de la base (claves, `CHECK`, permisos de la cuenta y carreras de concurrencia a nivel SQL) ya se verificaron sobre MySQL 8.0.46 (`modelo_relacional_mvp.md`, sección 9). Las pruebas de esta sección comprueban que la aplicación las respeta.

Los bloques importantes incluidos tendrán pruebas de su recorrido principal y sus rechazos críticos. La cobertura restante de RNF-22 quedará identificada como pendiente.

## 9. Demo de aceptación

1. **Imprescindible:** crear la base con los scripts (`01`, `02` y `03`), iniciar la aplicación y acceder como administrador. Mostrar un acceso rechazado.
2. **Imprescindible:** crear categoría, producto y un segundo depósito; demostrar modificación y listado.
3. **Imprescindible:** habilitar el producto en ambos depósitos. Definir mínimo 5 en el primero y 0 en el segundo. Rechazar una habilitación duplicada.
4. **Imprescindible:** cargar 10 unidades en el primero y 4 en el segundo mediante ajustes con motivo. Mostrar saldos 10 y 4 y total 14.
5. **Imprescindible:** ajustar −7 en el primero. Mostrar saldo 3, stock bajo y faltante 2.
6. **Imprescindible:** intentar ajustar −4. Mostrar el rechazo y que el saldo continúa en 3, sin registrar un movimiento.
7. **Si entra compensación:** registrar +7 vinculado al ajuste original. Mostrar saldo 10 y conservación del movimiento corregido.
8. **Si entra historial:** demostrar movimientos y filtros. La navegación al original dependerá del alcance opcional.
9. **Imprescindible:** rechazar bajas con existencias; ajustar los saldos vigentes a cero y demostrar bajas permitidas y rechazo de ajustes sobre inactivos.
10. **Si entra auditoría:** comprobar registros de maestros, habilitación y mínimos.
11. **Imprescindible:** cerrar sesión, reiniciar y comprobar persistencia.

Para ensayar la demo desde cero se ejecuta antes `00_reiniciar.sql`. El guion final omitirá los bloques que queden pendientes el 08/11.

## 10. Decisiones pendientes

Se cierran al empezar la base de código, porque afectan a los dos:

| Decisión | Opciones | Afecta a |
|---|---|---|
| Quién es el integrante A y quién el B | Según disponibilidad | Reparto (6.2) |
| Algoritmo de hash de contraseñas | BCrypt o PBKDF2 | `02_datos_prueba.sql` y el acceso |
| Nombre del paquete base | `controlstock` o `ar.edu.unpsjb.controlstock` | Todos los archivos Java |
| Carpetas del IDE a ignorar | Según el IDE de cada uno | `.gitignore` |

Si todavía no lo hicieron, cada integrante indica cuántas horas semanales puede dedicar hasta el 18/11, para contrastarlo con el calendario.

## 11. Entregables y finalización

- Código fuente y proyecto Maven ejecutable.
- DER, modelo lógico, modelo relacional y diagrama de clases.
- Scripts SQL de creación, datos de prueba, cuenta de la aplicación y reinicio.
- `db.properties.example` y README de instalación y ejecución.
- Casos de prueba y resultados de las verificaciones críticas.
- Lista de funciones implementadas y pendientes.
- Guion de demo y etiqueta `v0.1-mvp`.

El MVP estará terminado cuando el núcleo imprescindible esté integrado en `main`, las pruebas críticas pasen y **ambos integrantes puedan ejecutar la demo y explicar las decisiones de diseño**. Las evidencias académicas se completarán según la consigna de POO.

## 12. Cambios del plan

| Fecha | Cambios |
|---|---|
| 05/11/2026 | Se trabaja en una sola rama (`main`) en lugar de ramas con pull requests (2 y 6). Se agregan los acuerdos de concurrencia, conexión y credenciales (5), la organización de archivos y el reparto por módulo (6), las pruebas de concurrencia y de modo estricto (8) y las decisiones pendientes (10). Se actualiza el calendario con lo hecho hasta el 05/11 (7). Se suman el modelo relacional y los scripts a los entregables (11). |