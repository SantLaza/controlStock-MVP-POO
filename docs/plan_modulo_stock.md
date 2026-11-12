# Plan de implementación — Módulo Stock (Integrante B: Santiago)

**Fecha:** 11/11/2026  
**Dependencia:** base de código integrada (docs/base_codigo.md).  
**Objetivo:** habilitación de productos en depósitos, definición de stock mínimo, ajustes positivos y negativos.

---

## 1. Archivos a crear

| Capa | Archivo | Responsabilidad |
|---|---|---|
| dao/jdbc | `StockJdbcDao.java` | Implementar `StockDao` con JDBC |
| dao/jdbc | `MovimientoStockJdbcDao.java` | Implementar `MovimientoStockDao` con JDBC |
| servicio | `StockServicio.java` | Reglas de negocio de habilitación, mínimos y ajustes |

## 2. Archivos a modificar

| Archivo | Cambio |
|---|---|
| `ui/MenuStock.java` | Reemplazar el cascarón vacío por el menú completo |
| `Main.java` | Crear los DAOs, servicios y pasarlos a `MenuStock` |

---

## 3. Orden de implementación

### Etapa 1 — StockJdbcDao

Implementar cada método de la interfaz `StockDao`:

| Método | SQL | Notas |
|---|---|---|
| `insertar` | `INSERT INTO STOCK (idProducto, idDeposito, cantidad, stockMinimo) VALUES (?, ?, ?, ?)` | No tiene AUTO_INCREMENT (PK compuesta) |
| `actualizar` | `UPDATE STOCK SET cantidad = ?, stockMinimo = ? WHERE idProducto = ? AND idDeposito = ?` | |
| `buscarPorIds` | `SELECT ... WHERE idProducto = ? AND idDeposito = ?` | Devuelve `null` si no existe |
| `buscarPorIdsConBloqueo` | `SELECT ... WHERE idProducto = ? AND idDeposito = ? FOR UPDATE` | Bloquea la fila para concurrencia |
| `listarPorProducto` | `SELECT ... WHERE idProducto = ? ORDER BY idDeposito` | |
| `listarPorDeposito` | `SELECT ... WHERE idDeposito = ? ORDER BY idProducto` | |
| `listarStockBajo` | `SELECT ... WHERE cantidad < stockMinimo AND stockMinimo > 0` | Solo los que tienen mínimo definido |
| `existeConCantidadPositiva` | `SELECT 1 FROM STOCK WHERE idProducto = ? AND cantidad > 0 LIMIT 1` | Usado por Maestros para baja de producto |
| `existeConCantidadPositivaPorDeposito` | `SELECT 1 FROM STOCK WHERE idDeposito = ? AND cantidad > 0 LIMIT 1` | Usado por Maestros para baja de depósito |

### Etapa 2 — MovimientoStockJdbcDao

| Método | SQL | Notas |
|---|---|---|
| `insertar` | `INSERT INTO MOVIMIENTO_STOCK (idProducto, idDeposito, idUsuario, tipoMovimiento, cantidad, motivo, idMovimientoCorregido) VALUES (?, ?, ?, ?, ?, ?, ?)` | Usar `RETURN_GENERATED_KEYS` |
| `buscarPorId` | `SELECT ... WHERE idMovimiento = ?` | |
| `listarPorProductoDeposito` | `SELECT ... WHERE idProducto = ? AND idDeposito = ? ORDER BY fechaHoraMovimiento DESC` | |
| `listarTodos` | `SELECT ... ORDER BY fechaHoraMovimiento DESC` | |

### Etapa 3 — StockServicio

Este es el servicio más complejo del MVP. Controla transacciones y concurrencia.

**3.1 Habilitar producto en depósito**

```
habilitar(idProducto, idDeposito)
```

| Paso | Detalle |
|---|---|
| 1 | Abrir conexión, `setAutoCommit(false)` |
| 2 | `ProductoDao.buscarPorIdConBloqueo` → verificar que existe y `estaActivo()` |
| 3 | `DepositoDao.buscarPorIdConBloqueo` → verificar que existe y `estaActivo()` |
| 4 | Verificar que no exista ya un Stock para ese par (duplicado) |
| 5 | `StockDao.insertar` con cantidad 0 y stockMinimo 0 |
| 6 | Commit |

Orden de bloqueos: PRODUCTO → DEPOSITO (respeta el orden del plan).

**3.2 Definir stock mínimo**

```
definirMinimo(idProducto, idDeposito, nuevoMinimo)
```

| Paso | Detalle |
|---|---|
| 1 | Abrir conexión, `setAutoCommit(false)` |
| 2 | `StockDao.buscarPorIdsConBloqueo` → verificar que existe la habilitación |
| 3 | Validar que `nuevoMinimo >= 0` |
| 4 | `StockDao.actualizar` con el nuevo mínimo |
| 5 | Commit |

**3.3 Ajuste positivo**

```
ajustarPositivo(idProducto, idDeposito, cantidad, motivo, idUsuario)
```

| Paso | Detalle |
|---|---|
| 1 | Abrir conexión, `setAutoCommit(false)` |
| 2 | Validar: cantidad > 0, motivo no vacío después de trim |
| 3 | `StockDao.buscarPorIdsConBloqueo` → verificar que existe (producto habilitado en depósito) |
| 4 | Sumar la cantidad al saldo: `stock.aumentar(cantidad)` |
| 5 | `StockDao.actualizar` |
| 6 | Crear `MovimientoStock` con tipo `AJUSTE_POSITIVO` |
| 7 | `MovimientoStockDao.insertar` |
| 8 | Commit |

Los pasos 5 y 7 están en la misma transacción: si falla uno, se revierte todo.

**3.4 Ajuste negativo**

```
ajustarNegativo(idProducto, idDeposito, cantidad, motivo, idUsuario)
```

Igual que el positivo, con estas diferencias:

| Paso | Detalle |
|---|---|
| 4 | **Verificar que el saldo alcanza:** `stock.getCantidad() >= cantidad`. Si no alcanza → `ReglaNegocioException("Stock insuficiente. Saldo actual: " + stock.getCantidad())` |
| 4b | Restar: `stock.disminuir(cantidad)` |
| 6 | Tipo `AJUSTE_NEGATIVO` |

**3.5 Manejo de deadlocks**

En `ajustarPositivo` y `ajustarNegativo`, envolver la transacción con reintento:

```java
int intentos = 0;
while (true) {
    try (Connection conn = ConexionDB.obtenerConexion()) {
        conn.setAutoCommit(false);
        try {
            // operación completa
            conn.commit();
            return;
        } catch (SQLException e) {
            conn.rollback();
            if (ReglaNegocioException.esDeadlock(e) && intentos < 1) {
                intentos++;
                continue; // reintentar una vez
            }
            throw ReglaNegocioException.desdeSQL(e);
        }
    }
}
```

### Etapa 4 — MenuStock

**Estructura del menú:**

```
--- Stock ---
1. Habilitar producto en depósito
2. Definir stock mínimo
3. Ajuste positivo
4. Ajuste negativo
0. Volver
```

Para cada opción, el menú debe:
1. Mostrar listas de referencia (productos activos, depósitos activos) para que el usuario sepa qué ids usar.
2. Pedir los datos por consola.
3. Llamar al servicio.
4. Mostrar resultado o error.

**Constructor de MenuStock:**

Cambiar de:
```java
public MenuStock(Scanner scanner)
```
A:
```java
public MenuStock(Scanner scanner, StockServicio stockServicio,
                 SesionServicio sesionServicio)
```

Necesita `SesionServicio` para obtener el `idUsuario` del usuario logueado al registrar movimientos.

**Para mostrar listas de referencia** (productos y depósitos activos), el menú puede recibir también los DAOs de consulta o los servicios de maestros. Alternativas:
- Recibir `ProductoServicio` y `DepositoServicio` (si Joaquín ya los tiene).
- Recibir `ProductoDao` y `DepositoDao` directamente y hacer consultas de solo lectura.

Recomendación: recibir los servicios de Joaquín cuando estén listos. Mientras tanto, pedir los ids directamente sin mostrar lista.

### Etapa 5 — Integrar en Main.java

```java
StockDao stockDao = new StockJdbcDao();
MovimientoStockDao movimientoStockDao = new MovimientoStockJdbcDao();

StockServicio stockServicio = new StockServicio(
    stockDao, movimientoStockDao, productoDao, depositoDao);

MenuStock menuStock = new MenuStock(scanner, stockServicio, sesionServicio);
```

---

## 4. Validaciones críticas a no olvidar

- [ ] Producto y depósito activos para habilitar (`buscarPorIdConBloqueo` en ambos).
- [ ] Habilitación única por par producto-depósito (duplicado → 1062 de la base).
- [ ] Cantidad de ajuste: entero > 0.
- [ ] Motivo: no nulo y no vacío después de trim.
- [ ] Stock nunca negativo: verificar saldo antes del ajuste negativo (con bloqueo FOR UPDATE).
- [ ] Saldo y movimiento en la misma transacción: se guardan juntos o se revierten juntos.
- [ ] Orden de bloqueos: PRODUCTO → DEPOSITO → STOCK (nunca al revés).
- [ ] Deadlock (error 1213): rollback + un reintento.
- [ ] Consultas parametrizadas siempre.

## 5. Concurrencia — Casos a probar

Del plan_mvp.md sección 8:

| Caso | Resultado esperado |
|---|---|
| Saldo 3, dos ajustes simultáneos de −2 | Uno pasa (saldo 1), el otro se rechaza. Un solo movimiento nuevo. |
| Dos depósitos activos, dos bajas simultáneas | Una pasa, la otra se rechaza. Queda un depósito activo. |
| Ajuste y baja del mismo producto al mismo tiempo | Nunca dejar un producto inactivo con existencias. |

Estos se verifican gracias a `FOR UPDATE`: la segunda sesión espera a que la primera termine y después ve el estado actualizado.

## 6. Dependencias con el módulo Maestros

| Lo que necesito de Joaquín | Para qué | Mientras tanto |
|---|---|---|
| `ProductoJdbcDao.buscarPorIdConBloqueo()` | Verificar producto activo al habilitar/ajustar | Implementar el método yo mismo (es un SELECT simple) o esperar |
| `DepositoJdbcDao.buscarPorIdConBloqueo()` | Verificar depósito activo al habilitar/ajustar | Idem |
| `ProductoServicio.listarActivos()` | Mostrar lista de productos en el menú | Pedir id directamente sin mostrar lista |
| `DepositoServicio.listarActivos()` | Mostrar lista de depósitos en el menú | Idem |

Los primeros dos son más urgentes. Si Joaquín los tiene antes, perfecto. Si no, Santiago puede implementar `ProductoJdbcDao` y `DepositoJdbcDao` con solo esos dos métodos (el resto lo completa Joaquín).

## 7. Estimación

| Etapa | Tiempo estimado |
|---|---|
| Etapa 1 — StockJdbcDao | 2-3 horas |
| Etapa 2 — MovimientoStockJdbcDao | 1-2 horas |
| Etapa 3 — StockServicio (habilitación + mínimos + ajustes + deadlock) | 4-5 horas |
| Etapa 4 — MenuStock | 2-3 horas |
| Etapa 5 — Integración en Main | 30 min |
| **Total** | **10-14 horas** |
