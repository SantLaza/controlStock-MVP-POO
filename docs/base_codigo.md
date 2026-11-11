# Base de código — Registro de decisiones y estructura

**Fecha:** 11/11/2026  
**Estado:** Integrada y compilando.

## 1. Qué se armó

La base de código es el conjunto de archivos compartidos que se construyen antes de repartir el trabajo entre los integrantes (plan_mvp.md, sección 6.3). Incluye:

- **Proyecto Maven** (`pom.xml`) con Java 17, MySQL Connector/J 8.3.0, jBCrypt 0.4 y JUnit 5.10.2.
- **Configuración** (`db.properties.example`) como plantilla sin contraseña.
- **Capa modelo** — 7 clases y 5 enumerados que representan el dominio.
- **Capa DAO** — 7 interfaces que definen los contratos de persistencia.
- **Capa DAO JDBC** — Implementación de `UsuarioJdbcDao` (necesaria para el acceso).
- **Capa servicio** — `ReglaNegocioException` con traducción de errores MySQL, y `SesionServicio` (inicio y cierre de sesión).
- **Capa UI** — `MenuPrincipal` (completo y final), `MenuMaestros`, `MenuStock` y `MenuConsultas` (cascarones vacíos).
- **Capa util** — `Configuracion` (lectura de `db.properties`) y `ConexionDB` (conexión y verificación de modo estricto).
- **Main** — Punto de entrada que arma todas las dependencias y lanza el menú principal.
- **Script SQL** — `02_datos_prueba.sql` con el administrador (admin / admin123) y el Depósito Central.

## 2. Estructura de archivos creados

```
pom.xml
db.properties.example
sql/02_datos_prueba.sql
src/main/java/controlstock/
├── Main.java
├── modelo/
│   ├── Estado.java              (enum: activo, inactivo)
│   ├── Rol.java                 (enum: administrador, encargado_deposito, operador)
│   ├── TipoMovimiento.java      (enum: ajuste_positivo, ajuste_negativo)
│   ├── Accion.java              (enum: alta, modificacion, baja_logica)
│   ├── EntidadAfectada.java     (enum: categoria, producto, deposito, stock, usuario)
│   ├── Usuario.java
│   ├── Categoria.java
│   ├── Producto.java
│   ├── Deposito.java
│   ├── Stock.java
│   ├── MovimientoStock.java
│   └── Auditoria.java
├── dao/
│   ├── UsuarioDao.java          (interfaz)
│   ├── CategoriaDao.java        (interfaz)
│   ├── ProductoDao.java         (interfaz)
│   ├── DepositoDao.java         (interfaz)
│   ├── StockDao.java            (interfaz)
│   ├── MovimientoStockDao.java  (interfaz)
│   ├── AuditoriaDao.java        (interfaz)
│   └── jdbc/
│       └── UsuarioJdbcDao.java  (implementación JDBC)
├── servicio/
│   ├── ReglaNegocioException.java
│   └── SesionServicio.java
├── ui/
│   ├── MenuPrincipal.java       (completo)
│   ├── MenuMaestros.java        (cascarón vacío)
│   ├── MenuStock.java           (cascarón vacío)
│   └── MenuConsultas.java       (cascarón vacío)
└── util/
    ├── Configuracion.java
    └── ConexionDB.java
```

## 3. Decisiones tomadas

| Decisión | Elección | Motivo |
|---|---|---|
| Paquete base | `controlstock` | Más simple para un MVP; sin prefijo de organización |
| Hash de contraseñas | BCrypt (jBCrypt 0.4) | Incluye la sal en el hash, uso directo con un solo método |
| Contraseña del admin de prueba | `admin123` | Solo para desarrollo y demo |
| Conexión a la base | `Connection` se pasa como parámetro a los DAO | El servicio controla la transacción: abre la conexión, llama a los DAO con ella, y hace commit o rollback |
| Verificación de modo estricto | Se comprueba una sola vez al inicializar `ConexionDB` | Si falta `STRICT_TRANS_TABLES`, la aplicación no arranca |
| Enumerados Java | Cada uno con un campo `valor` que coincide con el ENUM de MySQL | `desdeValor(String)` convierte lo que viene de la base al enum de Java |
| Traducción de errores MySQL | Método estático `ReglaNegocioException.desdeSQL()` | Traduce los códigos 1062, 1452, 3819, 1142, 1213 y 1205 a mensajes en español |
| MenuPrincipal | Completo y final, conectado a los tres submenús | Ningún integrante necesita modificarlo (plan_mvp.md, sección 6.3) |

## 4. Convenciones para los DAO JDBC

- Cada método recibe un `Connection conn` como primer parámetro.
- Los métodos de inserción usan `Statement.RETURN_GENERATED_KEYS` y asignan el id generado al objeto.
- Los métodos `buscarPorIdConBloqueo` usan `SELECT ... FOR UPDATE`.
- Los métodos `buscarPorIdsConBloqueo` en StockDao también usan `FOR UPDATE`.
- El método privado `mapear(ResultSet)` convierte una fila en un objeto del modelo.

## 5. Convenciones para los servicios

- El servicio obtiene una conexión con `ConexionDB.obtenerConexion()`.
- Para operaciones transaccionales: `conn.setAutoCommit(false)`, operaciones, `conn.commit()` o `conn.rollback()` en el `finally`.
- Ante un deadlock (error 1213): `rollback` y un reintento.
- El orden de bloqueo es siempre: CATEGORIA → PRODUCTO → DEPOSITO → STOCK.
- El usuario activo se obtiene de `SesionServicio.getUsuarioActual()` y se pasa al servicio cuando se necesita (por ejemplo, para auditoría).

## 6. Cómo probar la base

1. Ejecutar los scripts SQL en orden: `01_schema.sql`, `02_datos_prueba.sql`, `03_usuario_app.sql`.
2. Cambiar la contraseña de la cuenta `controlstock_app` en MySQL.
3. Copiar `db.properties.example` como `db.properties` y completar la contraseña.
4. Compilar: `mvn compile`.
5. Ejecutar: `mvn exec:java -Dexec.mainClass=controlstock.Main` (o desde el IDE).
6. Iniciar sesión con usuario `admin` y contraseña `admin123`.

## 7. Próximos pasos

Según el reparto (plan_mvp.md, sección 6.2):

- **Integrante A (Maestros):** implementar `CategoriaJdbcDao`, `ProductoJdbcDao`, `DepositoJdbcDao`, los servicios de cada uno y llenar `MenuMaestros`.
- **Integrante B (Stock):** implementar `StockJdbcDao`, `MovimientoStockJdbcDao`, los servicios de stock y llenar `MenuStock`.
- **Quien libere primero:** implementar `MenuConsultas` con las consultas de stock por depósito, total y stock bajo.

Cada integrante trabaja en sus propios archivos. Solo necesitan actualizar `Main.java` para crear y cablear sus nuevos servicios y DAOs.
