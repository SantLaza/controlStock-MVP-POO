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
| Pruebas | JUnit 5 |

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

- Java 17 o superior
- Maven 3.8 o superior
- MySQL 8.0.16 o superior con `STRICT_TRANS_TABLES` habilitado

Para verificar el modo estricto:
```sql
SELECT @@GLOBAL.sql_mode;
```
El resultado debe incluir `STRICT_TRANS_TABLES`.

## Uso rapido

Compilar y ejecutar:

```bash
mvn package -q
java -cp "target/controlstock-mvp-0.1-SNAPSHOT.jar:target/dependency/*" controlstock.Main
```

Usuario de prueba: `admin` / `admin123`.

La aplicacion requiere que la base de datos este creada y configurada previamente. Consultar los scripts en `sql/` y el archivo `db.properties.example` para los datos de conexion.

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
