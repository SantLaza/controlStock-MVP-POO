# Plan de implementación — Módulo Maestros (Integrante A: Joaquín)

**Fecha:** 11/11/2026  
**Dependencia:** base de código integrada (docs/base_codigo.md).  
**Objetivo:** alta, modificación, listado y baja lógica de Categorías, Productos y Depósitos.

---

## 1. Archivos a crear

| Capa | Archivo | Responsabilidad |
|---|---|---|
| dao/jdbc | `CategoriaJdbcDao.java` | Implementar `CategoriaDao` con JDBC |
| dao/jdbc | `ProductoJdbcDao.java` | Implementar `ProductoDao` con JDBC |
| dao/jdbc | `DepositoJdbcDao.java` | Implementar `DepositoDao` con JDBC |
| servicio | `CategoriaServicio.java` | Reglas de negocio de categorías |
| servicio | `ProductoServicio.java` | Reglas de negocio de productos |
| servicio | `DepositoServicio.java` | Reglas de negocio de depósitos |

## 2. Archivo a modificar

| Archivo | Cambio |
|---|---|
| `ui/MenuMaestros.java` | Reemplazar el cascarón vacío por el menú completo con submenús |
| `Main.java` | Crear los DAOs, servicios y pasarlos a `MenuMaestros` |

## 2.1 Estado de los maestros: `EntidadMaestra`

`Categoria`, `Producto` y `Deposito` extienden la clase abstracta `EntidadMaestra`, que guarda el estado. Ya no existen `getEstadoCategoria()` / `setEstadoCategoria()` (ni los de Producto y Depósito):

| Método | Uso |
|---|---|
| `getEstado()` | Leer el estado (para el `INSERT`/`UPDATE`: `getEstado().getValor()`) |
| `estaActivo()` | Validar en el servicio que la entidad esté activa |
| `darDeBaja()` | Pasar a inactivo. Lanza `IllegalStateException` si ya estaba inactiva |
| `reactivar()` | Pasar a activo (reactivación es "si alcanza"; no hace falta en el menú) |
| `getNombre()` | Nombre de la entidad, para mensajes y listados |

No hay setter público de estado, a propósito: la única forma de cambiarlo es con `darDeBaja()` o `reactivar()`. Los nombres de las columnas en SQL **no cambian** (`estadoCategoria`, `estadoProducto`, `estadoDeposito`).

**Mapeo en el DAO.** Toda entidad nace activa. Al leer una fila, si la columna dice inactivo se aplica la baja:

```java
Categoria c = new Categoria();
c.setIdCategoria(rs.getInt("idCategoria"));
c.setNombreCategoria(rs.getString("nombreCategoria"));
if (Estado.desdeValor(rs.getString("estadoCategoria")) == Estado.INACTIVO) {
    c.darDeBaja();
}
```

**Baja en el servicio.** Dentro de la transacción, en este orden:
1. Buscar con bloqueo (`buscarPorIdConBloqueo`). Si no existe o `!estaActivo()`, lanzar `ReglaNegocioException` con el mensaje para el usuario.
2. Validar las reglas que dependen de otras entidades (productos activos, existencias, último depósito activo).
3. `entidad.darDeBaja()`.
4. `dao.actualizar(conn, entidad)` y `commit`.

---

## 3. Orden de implementación

Implementar en este orden permite probar cada pieza antes de pasar a la siguiente.

### Etapa 1 — Categorías (la más simple, sirve para validar el patrón)

**3.1 CategoriaJdbcDao**

Implementar cada método de la interfaz `CategoriaDao`:

| Método | SQL | Notas |
|---|---|---|
| `insertar` | `INSERT INTO CATEGORIA (nombreCategoria, estadoCategoria) VALUES (?, ?)` | Usar `RETURN_GENERATED_KEYS` y asignar el id al objeto |
| `actualizar` | `UPDATE CATEGORIA SET nombreCategoria = ?, estadoCategoria = ? WHERE idCategoria = ?` | |
| `buscarPorId` | `SELECT ... FROM CATEGORIA WHERE idCategoria = ?` | Devuelve `null` si no existe |
| `buscarPorIdConBloqueo` | `SELECT ... FROM CATEGORIA WHERE idCategoria = ? FOR UPDATE` | Para operaciones transaccionales |
| `listarActivas` | `SELECT ... FROM CATEGORIA WHERE estadoCategoria = 'activo' ORDER BY nombreCategoria` | |
| `listarTodas` | `SELECT ... FROM CATEGORIA ORDER BY nombreCategoria` | |

Método privado `mapear(ResultSet)` para convertir fila → objeto.

**3.2 CategoriaServicio**

| Método | Reglas de negocio | Bloqueos |
|---|---|---|
| `crear(nombre)` | Nombre no vacío después de trim. Duplicado lo detecta la base (error 1062 → `ReglaNegocioException`). | Ninguno (INSERT simple) |
| `modificar(id, nuevoNombre)` | Categoría debe existir. Nombre no vacío. Duplicado → 1062. | `buscarPorIdConBloqueo` (FOR UPDATE) |
| `darDeBaja(id)` | Categoría debe existir y estar activa. No debe tener productos activos asignados. | `buscarPorIdConBloqueo` |
| `listarActivas()` | — | Ninguno |
| `listarTodas()` | — | Ninguno |

Patrón transaccional para `crear`, `modificar` y `darDeBaja`:
```
conn = ConexionDB.obtenerConexion()
conn.setAutoCommit(false)
try {
    // operación
    conn.commit()
} catch (SQLException e) {
    conn.rollback()
    throw ReglaNegocioException.desdeSQL(e)
} finally {
    conn.close()
}
```

Nota: para `darDeBaja` se necesita saber si hay productos activos en esa categoría. Dos opciones:
- Agregar un método en `ProductoDao`: `existeActivoPorCategoria(conn, idCategoria)`
- O hacer un SELECT directo en el servicio.

Recomendación: agregar el método en `ProductoDao` e implementarlo en `ProductoJdbcDao`.

**3.3 Probar Categorías**

Antes de seguir con Productos, verificar que funciona:
- Alta → aparece en listar.
- Alta duplicada → error.
- Modificar nombre → se refleja.
- Baja → estado cambia a inactivo.

### Etapa 2 — Depósitos

**3.4 DepositoJdbcDao**

Igual que Categoría, con el agregado:

| Método | SQL |
|---|---|
| `insertar` | `INSERT INTO DEPOSITO (nombreDeposito, direccion, estadoDeposito) VALUES (?, ?, ?)` |
| `actualizar` | `UPDATE DEPOSITO SET nombreDeposito = ?, direccion = ?, estadoDeposito = ? WHERE idDeposito = ?` |
| `buscarPorId` | `SELECT ... WHERE idDeposito = ?` |
| `buscarPorIdConBloqueo` | `SELECT ... WHERE idDeposito = ? FOR UPDATE` |
| `listarActivos` | `SELECT ... WHERE estadoDeposito = 'activo' ORDER BY nombreDeposito` |
| `listarTodos` | `SELECT ... ORDER BY nombreDeposito` |
| `contarActivos` | `SELECT COUNT(*) FROM DEPOSITO WHERE estadoDeposito = 'activo' FOR UPDATE` |

**3.5 DepositoServicio**

| Método | Reglas de negocio |
|---|---|
| `crear(nombre, direccion)` | Nombre no vacío. Dirección puede ser null o no vacía. Duplicado → 1062. |
| `modificar(id, nuevoNombre, nuevaDireccion)` | Depósito debe existir. Mismas validaciones que crear. |
| `darDeBaja(id)` | Depósito debe existir y estar activo. **No debe tener stock con cantidad > 0** (usar `StockDao.existeConCantidadPositivaPorDeposito`). **Debe quedar al menos un depósito activo** (usar `contarActivos` ≥ 2 antes de dar de baja). Orden de bloqueo (§5.4): `contarActivos` como primera sentencia, luego el depósito y por último el STOCK con `FOR SHARE`. |
| `listarActivos()` | — |
| `listarTodos()` | — |

Importante: la baja de depósito necesita `StockDao`. Como Santiago implementa `StockJdbcDao`, para probar mientras tanto se puede:
- Crear una implementación temporal que devuelva `false` en `existeConCantidadPositivaPorDeposito`.
- O esperar a que Santiago tenga `StockJdbcDao` listo.

### Etapa 3 — Productos

**3.6 ProductoJdbcDao**

| Método | SQL |
|---|---|
| `insertar` | `INSERT INTO PRODUCTO (codigoProducto, idCategoria, nombreProducto, descripcion, precioReferenciaCompra, precioReferenciaVenta, estadoProducto) VALUES (?, ?, ?, ?, ?, ?, ?)` |
| `actualizar` | `UPDATE PRODUCTO SET codigoProducto = ?, idCategoria = ?, nombreProducto = ?, descripcion = ?, precioReferenciaCompra = ?, precioReferenciaVenta = ?, estadoProducto = ? WHERE idProducto = ?` |
| `buscarPorId` | `SELECT ... WHERE idProducto = ?` |
| `buscarPorIdConBloqueo` | `SELECT ... WHERE idProducto = ? FOR UPDATE` |
| `buscarPorCodigo` | `SELECT ... WHERE codigoProducto = ?` |
| `listarActivos` | `SELECT ... WHERE estadoProducto = 'activo' ORDER BY nombreProducto` |
| `listarTodos` | `SELECT ... ORDER BY nombreProducto` |

Método extra a agregar a la interfaz `ProductoDao`:
```java
boolean existeActivoPorCategoria(Connection conn, int idCategoria) throws SQLException;
```
SQL: `SELECT 1 FROM PRODUCTO WHERE idCategoria = ? AND estadoProducto = 'activo' LIMIT 1`

**3.7 ProductoServicio**

| Método | Reglas de negocio |
|---|---|
| `crear(codigo, idCategoria, nombre, descripcion, precioCompra, precioVenta)` | Código y nombre no vacíos. Precios ≥ 0. **La categoría debe existir y estar activa** (bloqueo `buscarPorIdConBloqueo` sobre CATEGORIA, luego INSERT en PRODUCTO). Duplicados de código → 1062. |
| `modificar(id, ...)` | Producto debe existir. Si se cambia la categoría, la nueva debe estar activa. |
| `darDeBaja(id)` | Producto debe existir y estar activo. **No debe tener stock con cantidad > 0** en ningún depósito (usar `StockDao.existeConCantidadPositiva`, con `FOR SHARE` después del bloqueo del producto, §5.4). |
| `listarActivos()` | — |
| `listarTodos()` | — |

Orden de bloqueos (según plan_mvp.md sección 5): CATEGORIA → PRODUCTO → DEPOSITO → STOCK.

### Etapa 4 — MenuMaestros

**3.8 Estructura del menú**

```
--- Maestros ---
1. Categorías
2. Productos
3. Depósitos
0. Volver

--- Categorías ---
1. Alta
2. Modificar
3. Listar
4. Dar de baja
0. Volver

--- Productos ---
1. Alta
2. Modificar
3. Listar
4. Dar de baja
0. Volver

--- Depósitos ---
1. Alta
2. Modificar
3. Listar
4. Dar de baja
0. Volver
```

**3.9 Constructor de MenuMaestros**

Cambiar de:
```java
public MenuMaestros(Scanner scanner)
```
A:
```java
public MenuMaestros(Scanner scanner, CategoriaServicio categoriaServicio,
                    ProductoServicio productoServicio, DepositoServicio depositoServicio)
```

**3.10 Actualizar Main.java**

Agregar la creación de los DAOs y servicios nuevos:
```java
CategoriaDao categoriaDao = new CategoriaJdbcDao();
ProductoDao productoDao = new ProductoJdbcDao();
DepositoDao depositoDao = new DepositoJdbcDao();
StockDao stockDao = new StockJdbcDao(); // cuando Santiago lo tenga

CategoriaServicio categoriaServicio = new CategoriaServicio(categoriaDao, productoDao);
ProductoServicio productoServicio = new ProductoServicio(productoDao, categoriaDao, stockDao);
DepositoServicio depositoServicio = new DepositoServicio(depositoDao, stockDao);

MenuMaestros menuMaestros = new MenuMaestros(scanner, categoriaServicio, productoServicio, depositoServicio);
```

---

## 4. Validaciones críticas a no olvidar

- [ ] Nombres y códigos: trim + no vacío antes de enviar a la base.
- [ ] Categoría activa al asignarla a un producto (verificar con bloqueo).
- [ ] Baja de categoría: sin productos activos.
- [ ] Baja de producto: sin stock con cantidad > 0 en ningún depósito.
- [ ] Baja de depósito: sin stock con cantidad > 0 Y al menos un depósito activo.
- [ ] Precios de producto: BigDecimal ≥ 0.
- [ ] Duplicados (código producto, nombre categoría, nombre depósito): dejar que la base lance 1062 y traducir.
- [ ] Consultas parametrizadas siempre (nunca concatenar strings en SQL).

## 5. Dependencias con el módulo Stock

| Lo que necesito de Santiago | Para qué | Estado |
|---|---|---|
| `StockJdbcDao.existeConCantidadPositiva()` | Baja de producto | Disponible (Santiago lo subió) |
| `StockJdbcDao.existeConCantidadPositivaPorDeposito()` | Baja de depósito | Disponible (Santiago lo subió) |

Santiago subió `StockJdbcDao`, `MovimientoStockJdbcDao` y `StockServicio` el 13/11. También creó `EntidadMaestra` y adaptó los tres modelos (`Categoria`, `Producto`, `Deposito`) para que la extiendan. Las dos dependencias están resueltas, no hacen falta stubs.

## 6. Estimación

| Etapa | Tiempo estimado |
|---|---|
| Etapa 1 — Categorías (DAO + servicio + prueba) | 2-3 horas |
| Etapa 2 — Depósitos (DAO + servicio + prueba) | 2-3 horas |
| Etapa 3 — Productos (DAO + servicio + prueba) | 3-4 horas |
| Etapa 4 — MenuMaestros + integrar en Main | 2-3 horas |
| **Total** | **9-13 horas** |

## 7. Progreso

| Fecha | Avance |
|---|---|
| 13/11/2026 | Etapa 1 — `CategoriaJdbcDao` y `CategoriaServicio` creados. Se agrego `existeActivoPorCategoria` a la interfaz `ProductoDao` (necesario para la baja de categoria). Compila correctamente. |
| 13/11/2026 | Etapa 2 — `DepositoJdbcDao` y `DepositoServicio` creados. La baja valida stock con cantidad > 0 (usa `StockDao` de Santiago) y que quede al menos un deposito activo (`contarActivos >= 2`). Se agrego `FOR UPDATE` a `contarActivos` y reintento por deadlock en `darDeBaja` para cubrir el caso de concurrencia de dos bajas simultaneas. |
| 13/11/2026 | Etapa 3 — `ProductoJdbcDao` y `ProductoServicio` creados. El DAO incluye `existeActivoPorCategoria`. El servicio bloquea la categoria (FOR UPDATE, orden CATEGORIA -> PRODUCTO) al crear y modificar, y verifica stock antes de la baja. |
| 13/11/2026 | Etapa 4 — `MenuMaestros` completo con submenus de Categorias, Productos y Depositos (alta, modificar, listar, dar de baja). `Main.java` actualizado: crea los DAOs y servicios de maestros y los conecta con el menu. Se corrigieron tildes en mensajes de error de `DepositoServicio` y `ProductoServicio`. Compila correctamente (39 archivos). |
| 17/11/2026 | Corrección de bloqueos según `modelo_relacional_mvp.md` §5.4, a partir de las observaciones de `plan_modulo_stock.md` §5.2. `DepositoServicio.darDeBaja` llama a `contarActivos` (FOR UPDATE sobre todos los depósitos activos) como primera sentencia de la transacción: dos bajas simultáneas piden las mismas filas en el mismo orden y una espera a la otra, sin deadlock. El reintento ante 1213 se conserva como protección. `existeConCantidadPositiva` y `existeConCantidadPositivaPorDeposito` de `StockJdbcDao` pasan a usar `FOR SHARE`, de modo que las bajas leen el saldo confirmado y esperan a un ajuste en curso. Cambio en `StockJdbcDao` acordado con Santiago. Verificado con `PruebaConcurrencia` (casos 1, 2 y 3, todas las comprobaciones OK). |
