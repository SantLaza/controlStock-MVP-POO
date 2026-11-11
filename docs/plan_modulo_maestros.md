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
| `contarActivos` | `SELECT COUNT(*) FROM DEPOSITO WHERE estadoDeposito = 'activo'` |

**3.5 DepositoServicio**

| Método | Reglas de negocio |
|---|---|
| `crear(nombre, direccion)` | Nombre no vacío. Dirección puede ser null o no vacía. Duplicado → 1062. |
| `modificar(id, nuevoNombre, nuevaDireccion)` | Depósito debe existir. Mismas validaciones que crear. |
| `darDeBaja(id)` | Depósito debe existir y estar activo. **No debe tener stock con cantidad > 0** (usar `StockDao.existeConCantidadPositivaPorDeposito`). **Debe quedar al menos un depósito activo** (usar `contarActivos` ≥ 2 antes de dar de baja). |
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
| `darDeBaja(id)` | Producto debe existir y estar activo. **No debe tener stock con cantidad > 0** en ningún depósito (usar `StockDao.existeConCantidadPositiva`). |
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

| Lo que necesito de Santiago | Para qué | Mientras tanto |
|---|---|---|
| `StockJdbcDao.existeConCantidadPositiva()` | Baja de producto | Asumir false (no hay stock cargado) o crear un stub |
| `StockJdbcDao.existeConCantidadPositivaPorDeposito()` | Baja de depósito | Asumir false o crear un stub |

Ambos métodos son simples (un SELECT COUNT). Si Santiago no los tiene aún, se pueden implementar primero como stubs y después reemplazar.

## 6. Estimación

| Etapa | Tiempo estimado |
|---|---|
| Etapa 1 — Categorías (DAO + servicio + prueba) | 2-3 horas |
| Etapa 2 — Depósitos (DAO + servicio + prueba) | 2-3 horas |
| Etapa 3 — Productos (DAO + servicio + prueba) | 3-4 horas |
| Etapa 4 — MenuMaestros + integrar en Main | 2-3 horas |
| **Total** | **9-13 horas** |
