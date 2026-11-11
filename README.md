# controlStock — MVP para Programación Orientada a Objetos


**Autores:** Santiago Lazarte y Joaquin Curihuala

> Este repositorio contiene solo la entrega de POO. Comparte el dominio y la documentación de referencia con el producto controlStock, pero es un desarrollo independiente: el producto se desarrolla y documenta en un repositorio aparte. El modelo de datos de este MVP es un subconjunto del modelo completo.

## Alcance previsto


**Imprescindible**

- [ ] Inicio y cierre de sesión con un usuario administrador precargado.
- [ ] Alta, modificación, listado y baja lógica de categorías, productos y depósitos.
- [ ] Habilitación de productos en depósitos y definición del stock mínimo de cada uno.
- [ ] Ajustes de stock positivos y negativos, con motivo obligatorio y sin permitir stock negativo.
- [ ] Consulta de stock por depósito, stock total de un producto y listado de productos con stock bajo.

**Importante**

- [ ] Movimientos compensatorios para corregir ajustes, sin modificar ni borrar el movimiento original.
- [ ] Historial de movimientos con filtros.
- [ ] Auditoría automática de las altas, modificaciones y bajas de categorías, productos y depósitos, de las habilitaciones y de los cambios de stock mínimo.

## Tecnologías

- Java 17 y Maven
- MySQL 8.0.16 o superior, con acceso por JDBC
- Interfaz de consola

## Instalación de la base

1. Comprobá que el servidor está en modo estricto: `SELECT @@GLOBAL.sql_mode;` tiene que incluir `STRICT_TRANS_TABLES`
2. Con una cuenta administradora (por ejemplo, `root`), ejecutá en orden los scripts de `sql/`:
   - `01_schema.sql`: crea la base `controlstock_mvp` y sus tablas.
   - `02_datos_prueba.sql`: carga el administrador y los datos iniciales (pendiente).
   - `03_usuario_app.sql`: crea la cuenta `controlstock_app` que usa la aplicación y le deja exactamente sus permisos (si la cuenta ya existía con permisos de más, se los quita).

3. Pone tu contraseña a la cuenta de la aplicación con una consulta que **no** se guarde en el repositorio:
   `ALTER USER 'controlstock_app'@'localhost' IDENTIFIED BY 'tu_contraseña';`
   y escribí esa contraseña en tu `db.properties` local, que está en el `.gitignore` y no se versiona. Si la cuenta ya existía, `03_usuario_app.sql` no le cambia la contraseña.
   ```

## Estructura del repositorio

```
docs/   plan del MVP, DER (der_mvp.drawio y der_mvp.png) y su explicación
sql/    scripts de creación de la base de datos y de carga de datos iniciales
src/    código fuente, organizado en capas: modelo, dao, servicio, ui y util
```

## Estado

En desarrollo. Fecha de entrega: 18/11/2026.