# controlStock — MVP para Programacion Orientada a Objetos

**Autores:** Santiago Lazarte y Joaquin Curihuala

Aplicacion de consola para administrar productos y controlar sus existencias en distintos depositos. Desarrollada como entrega para la materia Programacion Orientada a Objetos, este MVP demuestra un recorrido completo: acceso al sistema, gestion de maestros, habilitacion de productos en depositos, ajustes de stock y consultas.

> Este repositorio contiene solo la entrega de POO. Comparte el dominio y la documentacion de referencia con el producto controlStock, pero es un desarrollo independiente. El modelo de datos es un subconjunto del modelo completo.

## Tecnologias

| Componente | Tecnologia |
|---|---|
| Lenguaje | Java 17 |
| Build | Maven |
| Base de datos | MySQL 8.0.16 o superior |
| Acceso a datos | JDBC con consultas parametrizadas |
| Hash de contrasenas | BCrypt (jBCrypt 0.4) |
| Pruebas | Programa de concurrencia contra la base (`PruebaConcurrencia`, con `main`) |

## Arquitectura

La aplicacion sigue una arquitectura en capas con inversion de dependencias: los servicios dependen de interfaces DAO, no de implementaciones concretas.

```
Interfaz (ui) -> Servicios (servicio) -> DAO (dao) -> Base de datos
```

| Capa | Responsabilidad |
|---|---|
| `modelo` | Clases del dominio (Usuario, Categoria, Producto, Deposito, Stock, MovimientoStock, Auditoria), clase abstracta EntidadMaestra y enumerados (Estado, Rol, TipoMovimiento, Accion, EntidadAfectada) |
| `dao` | Interfaces de persistencia; en `dao/jdbc`, implementaciones con JDBC |
| `servicio` | Logica de negocio, validaciones, transacciones y control de concurrencia (ReglaNegocioException para errores de dominio) |
| `ui` | Menus de consola organizados por modulo (Principal, Maestros, Stock, Consultas) |
| `util` | Conexion a la base de datos (ConexionDB) y carga de configuracion (Configuracion) |

## Estructura del repositorio

```
controlStock-MVP-POO/
├── pom.xml                          Proyecto Maven
├── db.properties.example            Plantilla de configuracion (sin credenciales)
├── docs/
│   ├── plan_mvp.md                  Plan del MVP: alcance, calendario y acuerdos
│   ├── explicacion_DER_MVP.md       Explicacion del DER
│   ├── explicacion_modelo_logico_MVP.md
│   ├── modelo_relacional_mvp.md     Derivacion a tablas y camino referencial
│   ├── base_codigo.md               Decisiones de la base de codigo
│   ├── plan_modulo_maestros.md      Plan de implementacion: maestros (Joaquin)
│   ├── plan_modulo_stock.md         Plan de implementacion: stock (Santiago)
│   ├── der_mvp.drawio               DER del MVP (draw.io)
│   ├── der_mvp.png                  DER del MVP (imagen)
│   ├── modelo_logico_mvp.drawio     Modelo logico (draw.io)
│   └── modelo_logico_MVP.png        Modelo logico (imagen)
├── sql/
│   ├── 00_reiniciar.sql             Borra la base (solo para reinstalar)
│   ├── 01_schema.sql                Crea la base y las 7 tablas
│   ├── 02_datos_prueba.sql          Administrador y datos iniciales
│   └── 03_usuario_app.sql           Cuenta de la aplicacion con permisos minimos
├── src/test/java/controlstock/
│   └── PruebaConcurrencia.java      Pruebas de concurrencia contra la base (docs/plan_modulo_stock.md, seccion 5)
└── src/main/java/controlstock/
    ├── Main.java                    Punto de entrada
    ├── modelo/
    │   ├── EntidadMaestra.java      Clase abstracta (estado activo/inactivo)
    │   ├── Categoria.java
    │   ├── Producto.java
    │   ├── Deposito.java
    │   ├── Stock.java
    │   ├── MovimientoStock.java
    │   ├── Usuario.java
    │   ├── Auditoria.java
    │   ├── Estado.java              Enum: activo / inactivo
    │   ├── Rol.java                 Enum: administrador, encargado, operador
    │   ├── TipoMovimiento.java      Enum: ajuste positivo / negativo
    │   ├── Accion.java              Enum: alta, modificacion, baja logica
    │   └── EntidadAfectada.java     Enum: categoria, producto, deposito, stock, usuario
    ├── dao/
    │   ├── CategoriaDao.java
    │   ├── ProductoDao.java
    │   ├── DepositoDao.java
    │   ├── StockDao.java
    │   ├── MovimientoStockDao.java
    │   ├── UsuarioDao.java
    │   ├── AuditoriaDao.java
    │   └── jdbc/
    │       ├── CategoriaJdbcDao.java
    │       ├── ProductoJdbcDao.java
    │       ├── DepositoJdbcDao.java
    │       ├── StockJdbcDao.java
    │       ├── MovimientoStockJdbcDao.java
    │       └── UsuarioJdbcDao.java
    ├── servicio/
    │   ├── CategoriaServicio.java
    │   ├── ProductoServicio.java
    │   ├── DepositoServicio.java
    │   ├── StockServicio.java
    │   ├── ConsultaServicio.java    Consultas de stock (solo lectura)
    │   ├── LineaStock.java          Fila de consulta: stock + producto + deposito
    │   ├── SesionServicio.java
    │   └── ReglaNegocioException.java
    ├── ui/
    │   ├── MenuPrincipal.java
    │   ├── MenuMaestros.java
    │   ├── MenuStock.java
    │   └── MenuConsultas.java
    └── util/
        ├── ConexionDB.java
        └── Configuracion.java
```

## Requisitos previos

- JDK 17 o superior
- Maven 3.8 o superior
- MySQL 8.0.16 o superior con `STRICT_TRANS_TABLES` habilitado, y una cuenta administradora (por ejemplo, `root`) para crear la base

Para verificar el modo estricto:
```sql
SELECT @@GLOBAL.sql_mode;
```
El resultado debe incluir `STRICT_TRANS_TABLES`. Si no lo incluye, la aplicacion no arranca y explica el motivo.

## Instalacion y ejecucion

Todos los comandos se ejecutan desde la raiz del repositorio.

### 1. Crear la base de datos

Abrir el cliente de MySQL con la cuenta administradora:

```bash
mysql -u root -p --default-character-set=utf8mb4
```

y ejecutar los scripts en este orden:

```sql
source sql/01_schema.sql
source sql/02_datos_prueba.sql
source sql/03_usuario_app.sql
ALTER USER 'controlstock_app'@'localhost' IDENTIFIED BY 'elegir_una_contrasena';
```

- `01` crea la base `controlstock_mvp` y sus 7 tablas; `02` carga el administrador y el Deposito Central; `03` crea la cuenta `controlstock_app`, con la que se conecta la aplicacion (permisos minimos, ver [Seguridad](#seguridad)).
- `03` crea la cuenta con una contrasena provisoria y vencida: el `ALTER USER` le pone la definitiva. Evitar acentos y la letra enie (ver el paso 2).
- Si `mysql` no se reconoce como comando, en Windows esta en `C:\Program Files\MySQL\MySQL Server 8.0\bin`: agregar esa carpeta al `PATH`, o abrir y ejecutar los tres scripts y el `ALTER USER`, en el mismo orden, desde MySQL Workbench.

**Reinstalar desde cero** (borra todos los datos): ejecutar `source sql/00_reiniciar.sql` y despues `01`, `02` y `03`. El `ALTER USER` no hace falta: la cuenta `controlstock_app` no se borra y conserva su contrasena.

### 2. Configurar la conexion

Copiar la plantilla como `db.properties`:

```bash
cp db.properties.example db.properties
```

(en PowerShell: `Copy-Item db.properties.example db.properties`)

y completar `db.contrasena` con la contrasena del `ALTER USER`. El archivo se lee como ISO-8859-1, por eso la contrasena no debe llevar acentos ni enie. `db.properties` esta en el `.gitignore`: nunca se sube al repositorio.

La aplicacion busca `db.properties` en este orden y usa el primero que encuentra:

1. La ruta indicada con `-Ddb.config`, por ejemplo `java -Ddb.config=C:\ruta\db.properties ...`.
2. La carpeta desde donde se ejecuta.
3. La raiz del proyecto, que deduce de la ubicacion de las clases compiladas (`target/classes` o el `.jar` en `target/`).

Por eso funciona aunque se ejecute desde el IDE o desde otra carpeta. Si no lo encuentra, el mensaje de error lista las rutas donde busco.

### 3. Compilar

```bash
mvn package
mvn dependency:copy-dependencies -DincludeScope=runtime
```

El primero genera `target/controlstock-mvp-0.1-SNAPSHOT.jar`. Ese jar no incluye las bibliotecas, por eso el segundo copia MySQL Connector/J y jBCrypt en `target/dependency/`.

### 4. Ejecutar

El separador del classpath depende del sistema operativo.

En Windows (PowerShell, cmd o Git Bash), con `;`:

```bash
java -cp "target/controlstock-mvp-0.1-SNAPSHOT.jar;target/dependency/*" controlstock.Main
```

En Linux y macOS, con `:`:

```bash
java -cp "target/controlstock-mvp-0.1-SNAPSHOT.jar:target/dependency/*" controlstock.Main
```

Usuario de prueba: `admin` / `admin123`.

## Alcance funcional

**Implementado**

- Inicio y cierre de sesion con un usuario administrador precargado.
- Alta, modificacion, listado y baja logica de categorias, productos y depositos.
- Habilitacion de productos en depositos y definicion del stock minimo.
- Ajustes de stock positivos y negativos, con motivo obligatorio y sin stock negativo.
- Consulta de stock por deposito, stock total y listado de productos con stock bajo.

**Pendiente**

- Movimientos compensatorios para corregir ajustes.
- Historial de movimientos con filtros.
- Auditoria automatica de cambios sobre maestros.

## Seguridad

- La aplicacion se conecta con la cuenta `controlstock_app`, que tiene permisos minimos: no puede ejecutar DELETE en ninguna tabla, y en MOVIMIENTO_STOCK y AUDITORIA solo puede leer e insertar.
- Las contrasenas se almacenan con hash BCrypt (sal incluida).
- Las credenciales de conexion no se versionan (`db.properties` en `.gitignore`).
- Todas las consultas usan sentencias parametrizadas.

## Autores

| Integrante | Modulo principal |
|---|---|
| Joaquin Curihuala | Maestros: categorias, productos y depositos (DAO, servicios y menu) |
| Santiago Lazarte | Stock: habilitacion, ajustes, movimientos y consultas (DAO, servicios y menu) |
