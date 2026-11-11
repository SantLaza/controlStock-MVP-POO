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

La aplicacion sigue una arquitectura en capas:

```
Interfaz (ui) -> Servicios (servicio) -> DAO (dao) -> Base de datos
```

| Capa | Responsabilidad |
|---|---|
| `modelo` | Clases del dominio y enumerados (Usuario, Categoria, Producto, Deposito, Stock, MovimientoStock, Auditoria) |
| `dao` | Interfaces de persistencia; en `dao/jdbc`, implementaciones con JDBC |
| `servicio` | Logica de negocio, validaciones, transacciones y control de concurrencia |
| `ui` | Menus de consola organizados por modulo |
| `util` | Conexion a la base de datos y configuracion |

Los servicios dependen de interfaces DAO, no de implementaciones concretas.

## Estructura del repositorio

```
controlStock-MVP-POO/
├── pom.xml                      Proyecto Maven
├── db.properties.example        Plantilla de configuracion (sin credenciales)
├── docs/
│   ├── plan_mvp.md              Plan del MVP: alcance, calendario y acuerdos
│   ├── explicacion_DER_MVP.md   Explicacion del DER
│   ├── explicacion_modelo_logico_MVP.md
│   ├── base_codigo.md           Decisiones de la base de codigo
│   ├── plan_modulo_maestros.md  Plan de implementacion: maestros
│   ├── plan_modulo_stock.md     Plan de implementacion: stock
│   ├── der_mvp.drawio           DER del MVP (draw.io)
│   ├── der_mvp.png              DER del MVP (imagen)
│   └── modelo_logico_MVP.png    Modelo logico (imagen)
├── sql/
│   ├── 00_reiniciar.sql         Borra la base (solo para reinstalar)
│   ├── 01_schema.sql            Crea la base y las 7 tablas
│   ├── 02_datos_prueba.sql      Administrador y datos iniciales
│   └── 03_usuario_app.sql       Cuenta de la aplicacion con permisos minimos
└── src/main/java/controlstock/
    ├── Main.java
    ├── modelo/                  Clases del dominio y enumerados
    ├── dao/                     Interfaces de persistencia
    │   └── jdbc/                Implementaciones JDBC
    ├── servicio/                Logica de negocio
    ├── ui/                      Menus de consola
    └── util/                    Conexion y configuracion
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

## Instalacion

### Base de datos

Con una cuenta administradora (por ejemplo, `root`), ejecutar en orden:

```
sql/01_schema.sql
sql/02_datos_prueba.sql
sql/03_usuario_app.sql
```

Luego, asignar una contrasena a la cuenta de la aplicacion (en una consulta que no se guarde en el repositorio):

```sql
ALTER USER 'controlstock_app'@'localhost' IDENTIFIED BY 'la_contrasena_elegida';
```

### Configuracion local

Copiar la plantilla y completar la contrasena:

```bash
cp db.properties.example db.properties
```

Editar `db.properties` con la contrasena asignada en el paso anterior. Este archivo esta en el `.gitignore` y no se versiona.

### Compilacion y ejecucion

```bash
mvn compile exec:java -Dexec.mainClass=controlstock.Main
```

Usuario de prueba: `admin` / `admin123`

## Alcance funcional

**Nucleo (imprescindible)**

- Inicio y cierre de sesion con un usuario administrador precargado.
- Alta, modificacion, listado y baja logica de categorias, productos y depositos.
- Habilitacion de productos en depositos y definicion del stock minimo.
- Ajustes de stock positivos y negativos, con motivo obligatorio y sin stock negativo.
- Consulta de stock por deposito, stock total y listado de productos con stock bajo.

**Bloques adicionales (si el tiempo lo permite)**

- Movimientos compensatorios para corregir ajustes.
- Historial de movimientos con filtros.
- Auditoria automatica de cambios sobre maestros.

## Seguridad

- La aplicacion se conecta con la cuenta `controlstock_app`, que tiene permisos minimos: no puede ejecutar DELETE en ninguna tabla, y en MOVIMIENTO_STOCK y AUDITORIA solo puede leer e insertar.
- Las contrasenas se almacenan con hash BCrypt (sal incluida).
- Las credenciales de conexion no se versionan (`db.properties` en `.gitignore`).
- Todas las consultas usan sentencias parametrizadas.

## Documentacion

| Documento | Contenido |
|---|---|
| `docs/plan_mvp.md` | Alcance, tecnologia, acuerdos de diseno, reparto del trabajo y calendario |
| `docs/explicacion_DER_MVP.md` | Origen del DER, entidades, relaciones y restricciones |
| `docs/explicacion_modelo_logico_MVP.md` | Derivacion del DER a tablas y camino referencial |
| `docs/base_codigo.md` | Decisiones de la base de codigo: paquetes, convenciones y contratos |
| `docs/plan_modulo_maestros.md` | Plan de implementacion del modulo de maestros |
| `docs/plan_modulo_stock.md` | Plan de implementacion del modulo de stock |
