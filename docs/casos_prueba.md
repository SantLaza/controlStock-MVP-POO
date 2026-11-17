# Casos de prueba — MVP con los datos de `04_datos_testeo.sql`

**Versión:** `main` posterior a las correcciones del 17/11 (H-01 a H-10).
**Objetivo:** recorrer todo el alcance del MVP y confirmar que las correcciones de `test_joaco.md` siguen funcionando. Cada caso tiene la entrada exacta y la salida esperada, copiada de los mensajes del código.

> **El orden importa.** Primero van las pruebas que solo leen o que se rechazan (la base no cambia), después las que escriben. Los valores esperados de cada bloque suponen que se hicieron los anteriores. Si se salta un caso que escribe, los siguientes pueden dar otros números.

---

## 0. Preparación

### 0.1 Reinstalar la base

Con la cuenta administradora (`root`), desde la raíz del repositorio:

```sql
source sql/00_reiniciar.sql
source sql/01_schema.sql
source sql/02_datos_prueba.sql
source sql/03_usuario_app.sql
source sql/04_datos_testeo.sql
```

El último `SELECT` del `04` tiene que devolver:

| categorias | depositos | productos | habilitaciones | movimientos |
|---|---|---|---|---|
| 8 | 5 | 24 | 40 | 57 |

### 0.2 Compilar y ejecutar

```bash
mvn package
```

```bash
mvn dependency:copy-dependencies -DincludeScope=runtime
```

```bash
java -cp "target/controlstock-mvp-0.1-SNAPSHOT.jar;target/dependency/*" controlstock.Main
```

### 0.3 Ids de referencia

Los asigna el `04` (los productos, en orden de código).

| Depósito | Id | Estado |
|---|---|---|
| Depósito Central | 1 | activo |
| Depósito Norte | 2 | activo |
| Sucursal Centro | 3 | activo, sin dirección |
| Depósito Temporal | 4 | activo, todo en 0 |
| Anexo Sur | 5 | **inactivo** |

| Categoría | Id | | Categoría | Id |
|---|---|---|---|---|
| Almacén | 1 | | Bazar | 5 |
| Bebidas | 2 | | Librería | 6 |
| Limpieza | 3 | | Mascotas | 7 |
| Perfumería | 4 | | Temporada | 8 (**inactiva**) |

| Id | Código | | Id | Código | | Id | Código |
|---|---|---|---|---|---|---|---|
| 1 | ALM-001 | | 9 | BAZ-003 | | 17 | LIM-002 |
| 2 | ALM-002 | | 10 | BEB-001 | | 18 | LIM-003 |
| 3 | ALM-003 | | 11 | BEB-002 | | 19 | LIM-004 |
| 4 | ALM-004 | | 12 | BEB-003 | | 20 | PER-001 |
| 5 | ALM-005 | | 13 | BEB-004 | | 21 | PER-002 |
| 6 | ALM-006 | | 14 | LIB-001 (**inactivo**) | | 22 | PER-003 |
| 7 | BAZ-001 | | 15 | LIB-002 (**inactivo**) | | 23 | PER-004 |
| 8 | BAZ-002 | | 16 | LIM-001 | | 24 | TEM-001 (**inactivo**) |

Los registros que se crean durante la prueba **no** van a tener ids consecutivos (25, 26…): InnoDB reserva ids de más en los `INSERT … SELECT`. Leer el id en el mensaje de confirmación o en el listado.

### 0.4 Consulta de control

Para confirmar que una tanda de rechazos no escribió nada (con la cuenta `controlstock_app` o `root`):

```sql
USE controlstock_mvp;
SELECT (SELECT COUNT(*) FROM MOVIMIENTO_STOCK) AS movimientos,
       (SELECT COUNT(*) FROM STOCK)            AS habilitaciones,
       (SELECT COUNT(*) FROM PRODUCTO)         AS productos,
       (SELECT COUNT(*) FROM CATEGORIA)        AS categorias,
       (SELECT COUNT(*) FROM DEPOSITO)         AS depositos;
```

---

## 1. Acceso

| # | Entrada | Resultado esperado | OK |
|---|---|---|---|
| A-01 | `admin` / `admin123` | «¡Bienvenido, admin (administrador)!» y el menú principal | ☐ |
| A-02 | `admin` / `malaclave` | «Error: Usuario o contraseña incorrectos.» y vuelve a pedir usuario | ☐ |
| A-03 | `noexiste` / `x` | El mismo mensaje que A-02 (no revela si el usuario existe) | ☐ |
| A-04 | enter / enter | El mismo mensaje que A-02 | ☐ |
| A-05 | `ADMIN` / `admin123` | Ingresa como `admin` | ☐ |
| A-06 | `  admin  ` / `admin123` | Ingresa (se recortan los espacios) | ☐ |
| A-07 | En el menú principal: `9`, después `abc` | «Opción no válida. Ingresá un número entre 0 y 4.» las dos veces | ☐ |
| A-08 | Opción `4` | «Sesión cerrada.» y vuelve a pedir usuario | ☐ |
| A-09 | `salir` como usuario | «¡Hasta luego!» y termina | ☐ |
| A-10 | Volver a abrir, ingresar y elegir `0` | «¡Hasta luego!» y termina | ☐ |
| A-11 | Volver a abrir y, en cualquier menú, `Ctrl+Z` y enter (Windows) | «Se cerró la entrada. ¡Hasta luego!», **sin** traza de excepción (H-07) | ☐ |

---

## 2. Consultas (solo lectura)

Menú principal → `3`.

### 2.1 Stock por depósito (opción 1)

| # | Id | Resultado esperado | OK |
|---|---|---|---|
| C-01 | `1` | «Stock en Depósito Central:», **19 filas** ordenadas por id. Controlar: ALM-001 8 / 20 / 12 **BAJO**; ALM-002 15 / 15 / 0 sin marca; ALM-003 54; BEB-001 96; BEB-002 32 / 36 / 4 **BAJO**; BEB-004 0; BAZ-003 0; PER-004 100. **No** aparecen LIB-001 ni TEM-001 (inactivos) | ☐ |
| C-02 | `1` (misma tabla) | BAZ-002 se ve recortado: «Juego de ollas de acero i...» y las columnas no se desarman | ☐ |
| C-03 | `2` | «Stock en Depósito Norte:», **7 filas**. LIM-001 3 / 10 / 7 **BAJO**. TEM-001 no aparece | ☐ |
| C-04 | `3` | «Stock en Sucursal Centro:», **5 filas**. ALM-004 0 / 4 / 4 **BAJO**; PER-003 0 / 6 / 6 **BAJO**; BEB-003 0 / 0 / 0 **sin** marca | ☐ |
| C-05 | `4` | «Stock en Depósito Temporal:», 4 filas (BEB-001, BEB-002, BEB-004, LIM-003), todas con saldo 0 | ☐ |
| C-06 | `5` | La lista de depósitos activos no lo muestra. Respuesta: «Error: El depósito 'Anexo Sur' está inactivo.» | ☐ |
| C-07 | `99` | «Error: No existe el depósito con id 99.» | ☐ |
| C-08 | `abc` | «Ingresá un número entero.» y vuelve a pedir | ☐ |

### 2.2 Stock total de un producto (opción 2)

| # | Id | Resultado esperado | OK |
|---|---|---|---|
| C-09 | `3` (ALM-003) | Central 54, Norte 36, Sucursal Centro 14. «Stock total: 104» | ☐ |
| C-10 | `1` (ALM-001) | Central 8 **BAJO**, Norte 25. «Stock total: 33» | ☐ |
| C-11 | `5` (ALM-005) | Solo Central 50 (no suma Anexo Sur, inactivo). «Stock total: 50» | ☐ |
| C-12 | `6` (ALM-006) | «El producto no está habilitado en ningún depósito activo. Stock total: 0.» | ☐ |
| C-13 | `8` (BAZ-002) | El encabezado muestra el nombre completo de 97 caracteres. «Stock total: 5» | ☐ |
| C-14 | `14` (LIB-001) | «Error: El producto 'Cuaderno tapa dura 48 hojas' está inactivo.» | ☐ |
| C-15 | `99` | «Error: No existe el producto con id 99.» | ☐ |

### 2.3 Stock bajo (opción 3)

| # | Resultado esperado | OK |
|---|---|---|
| C-16 | **Exactamente 5 filas**, en este orden: ALM-001 Central 8 / 20 / 12 · ALM-004 Sucursal Centro 0 / 4 / 4 · BEB-002 Central 32 / 36 / 4 · LIM-001 Norte 3 / 10 / 7 · PER-003 Sucursal Centro 0 / 6 / 6. **No** aparecen ALM-002 Central (15 de 15) ni BEB-003 Sucursal Centro (mínimo 0) | ☐ |

---

## 3. Stock: rechazos (la base no cambia)

Menú principal → `2`. Desde las correcciones H-10, el menú muestra la lista de productos activos y de depósitos válidos antes de pedir cada id, y rechaza en el momento lo que no está en la lista.

| # | Opción y entrada | Resultado esperado | OK |
|---|---|---|---|
| S-01 | 1 (Habilitar) → producto `14` | «No hay un producto activo con ese id.» y vuelve a pedir. LIB-001 no figura en la lista | ☐ |
| S-02 | 1 → producto `99` | El mismo mensaje que S-01 | ☐ |
| S-03 | 1 → producto `0` | «Operación cancelada.» | ☐ |
| S-04 | 1 → producto `1` (ALM-001) | Lista solo «[3] Sucursal Centro» y «[4] Depósito Temporal» (donde todavía no está) | ☐ |
| S-05 | … → depósito `1`, después `5` | «Ese depósito no está en la lista.» las dos veces. Con `0`: «Operación cancelada.» | ☐ |
| S-06 | 2 (Mínimo) → producto `6` (ALM-006) | «El producto no está habilitado en ningún depósito activo. Habilitalo primero (opción 1).» | ☐ |
| S-07 | 2 → producto `5` (ALM-005) | Solo lista «[1] Depósito Central — saldo 50, mínimo 10». Con depósito `5`: «Ese depósito no está en la lista.» (H-03). Salir con `0` | ☐ |
| S-08 | 4 (Ajuste negativo) → producto `12` (BEB-003) → depósito `3` | «El saldo es 0: no hay unidades para restar.» | ☐ |
| S-09 | 4 → producto `1` → depósito `1` (saldo 8) → cantidades `0`, `-5` | «La cantidad del ajuste debe ser mayor a 0.» las dos veces | ☐ |
| S-10 | … → cantidades `abc`, `99999999999` | «Ingresá un número entero.» las dos veces | ☐ |
| S-11 | … → cantidad `9` → motivo `   ` (solo espacios) | «El motivo es obligatorio.» y vuelve a pedir | ☐ |
| S-12 | … → motivo de **256** caracteres (ver nota) | «Error: El motivo no puede superar los 255 caracteres.» | ☐ |
| S-13 | 4 → `1` → `1` → cantidad `9` → motivo `Prueba` | «Error: Stock insuficiente. Saldo actual: 8» | ☐ |
| S-14 | 3 (Ajuste positivo) → producto `2` (ALM-002) → depósito `2` (saldo 12) → cantidad `2147483647` → motivo `Prueba` | «Error: No se puede aumentar 2147483647: el saldo superaría el máximo permitido.» | ☐ |
| S-15 | Opción `7` en el menú de Stock | «Opción no válida. Ingresá un número entre 0 y 4.» | ☐ |
| S-16 | Consulta de control (0.4) | **57** movimientos, **40** habilitaciones. Repetir C-10: ALM-001 sigue en 8 y 25 | ☐ |

**Nota S-12:** para generar el texto, en PowerShell: `('a' * 256) | Set-Clipboard` y pegarlo en la consola de la aplicación.

---

## 4. Stock: operaciones que escriben (recorrido de la demo)

| # | Opción y entrada | Resultado esperado | OK |
|---|---|---|---|
| S-17 | 1 (Habilitar) → `6` (ALM-006) | Lista los 4 depósitos activos (1 a 4). Elegir `1` → «Producto habilitado en el depósito (saldo 0, mínimo 0).» | ☐ |
| S-18 | 1 → `6` otra vez | La lista ya no muestra el depósito 1 (solo 2, 3 y 4). Salir con `0` | ☐ |
| S-19 | 2 (Mínimo) → `6` → `1` → mínimo `-1` | «El stock mínimo no puede ser negativo.» y vuelve a pedir | ☐ |
| S-20 | … → mínimo `5` | «Mínimo actualizado a 5. Saldo actual: 0.» y «AVISO - Stock bajo: el mínimo es 5, faltan 5 unidades.» | ☐ |
| S-21 | 3 (Ajuste positivo) → `6` → `1` → `10` → motivo `  Ingreso de prueba  ` | «Ajuste registrado. Saldo actual: 10.» **sin** aviso | ☐ |
| S-22 | 4 (Ajuste negativo) → `6` → `1` → `7` → motivo `Venta de prueba` | «Ajuste registrado. Saldo actual: 3.» y «AVISO - Stock bajo: el mínimo es 5, faltan 2 unidades.» | ☐ |
| S-23 | 4 → `6` → `1` → `4` → motivo `Prueba` | «Error: Stock insuficiente. Saldo actual: 3» | ☐ |
| S-24 | 4 → `1` (ALM-001) → `1` → `8` → motivo `Salida total` | «Ajuste registrado. Saldo actual: 0.» y «AVISO - Stock bajo: el mínimo es 20, faltan 20 unidades.» | ☐ |
| S-25 | 4 → `1` → `1` | «El saldo es 0: no hay unidades para restar.» | ☐ |
| S-26 | Consultas → 2 → `6` | Central 3 / 5 / 2 **BAJO**. «Stock total: 3» | ☐ |
| S-27 | Consultas → 3 | **6 filas**: ALM-001 Central 0 / 20 / 20 · ALM-004 Sucursal Centro · **ALM-006 Central 3 / 5 / 2** · BEB-002 Central · LIM-001 Norte · PER-003 Sucursal Centro | ☐ |
| S-28 | Salir de la aplicación, volver a entrar y repetir S-26 | Mismo resultado (los datos persisten) | ☐ |

**Verificación en la base:**

```sql
SELECT idMovimiento, idProducto, idDeposito, idUsuario, tipoMovimiento, cantidad,
       CONCAT('[', motivo, ']') AS motivo
FROM MOVIMIENTO_STOCK
ORDER BY idMovimiento DESC
LIMIT 3;
```

| # | Resultado esperado | OK |
|---|---|---|
| S-29 | Total de movimientos: **60** (57 + 3). El rechazo de S-23 no dejó movimiento | ☐ |
| S-30 | Los 3 últimos: ALM-001 (1) −8, ALM-006 (6) −7 y ALM-006 +10, los tres con `idUsuario` = 1. El motivo de S-21 quedó como `[Ingreso de prueba]`, sin los espacios de los extremos | ☐ |

---

## 5. Maestros: rechazos

Menú principal → `1`.

### 5.1 Categorías

| # | Opción y entrada | Resultado esperado | OK |
|---|---|---|---|
| M-01 | Alta → enter | «Alta cancelada.» (H-02) | ☐ |
| M-02 | Alta → `0` | «Alta cancelada.» y **no** crea una categoría «0» (H-02) | ☐ |
| M-03 | Alta → `almacen` | «Error: Ya existe una categoría con el nombre 'almacen' (no se distinguen mayúsculas ni tildes).» | ☐ |
| M-04 | Alta → `ALMACÉN` | El mismo mensaje, con 'ALMACÉN' | ☐ |
| M-05 | Alta → `  Bebidas  ` | El mismo mensaje, con 'Bebidas' (sin espacios) | ☐ |
| M-06 | Alta → nombre de 51 caracteres | «Error: El nombre no puede superar los 50 caracteres.» | ☐ |
| M-07 | Modificar → `99` | «No se encontró una categoría con ese ID.» | ☐ |
| M-08 | Modificar → `7` (Mascotas) → `bebidas` | «Error: Ya existe una categoría con el nombre 'bebidas' (…).» | ☐ |
| M-09 | Dar de baja → `1` (Almacén) | «Error: No se puede dar de baja: tiene productos activos asignados.» | ☐ |
| M-10 | Dar de baja → `8` (Temporada) | «Error: La categoría 'Temporada' ya está inactiva.» | ☐ |
| M-11 | Dar de baja → `99` | «Error: No existe la categoría con id 99.» | ☐ |

### 5.2 Productos

| # | Opción y entrada | Resultado esperado | OK |
|---|---|---|---|
| M-12 | Alta → código enter, y otra vez con `0` | «Alta cancelada.» las dos veces (H-02) | ☐ |
| M-13 | Alta → `PRU-001` → categoría `8`, después `99` | «No hay una categoría activa con ese ID.» las dos veces, en el momento (H-10). Con `0`: «Alta cancelada.» | ☐ |
| M-14 | Alta → `alm-001` → cat. `1` → nombre `Prueba` → desc. enter → precios `1` y `1` | «Error: Ya existe un producto con el código 'alm-001' (no se distinguen mayúsculas ni tildes).» | ☐ |
| M-15 | Alta → código de 31 caracteres → cat. `1` → nombre `Prueba` → precios `1` y `1` | «Error: El código no puede superar los 30 caracteres.» | ☐ |
| M-16 | Alta → `PRU-001` → cat. `1` → nombre de 101 caracteres → precios `1` y `1` | «Error: El nombre no puede superar los 100 caracteres.» | ☐ |
| M-17 | Alta → … → precio de compra `-5` | «El valor no puede ser negativo.» y vuelve a pedir | ☐ |
| M-18 | … → precio `1,5`, después `abc` | «Ingresá un número válido (usá punto como separador decimal).» | ☐ |
| M-19 | Alta `PRU-001`, cat. `1`, nombre `Prueba`, compra `1.555`, venta `1` | «Error: El precio de referencia de compra no puede tener más de 2 decimales.» (H-08) | ☐ |
| M-20 | Igual, compra `99999999999` | «Error: El precio de referencia de compra no puede superar 9999999999.99.» en castellano (H-06) | ☐ |
| M-21 | Modificar → `99` | «No se encontró un producto con ese ID.» | ☐ |
| M-22 | Modificar → `1` (ALM-001) → código `ALM-002`, el resto enter | «Error: Ya existe un producto con el código 'ALM-002' (…).» | ☐ |
| M-23 | Modificar → `1` → código enter → categoría `8` | «No hay una categoría activa con ese ID.» y vuelve a pedir. Enter conserva la 1; completar con enter y queda sin cambios | ☐ |
| M-24 | Dar de baja → `9` (BAZ-003, 4 en Norte) | «Error: No se puede dar de baja: tiene stock con cantidad mayor a 0.» | ☐ |
| M-25 | Dar de baja → `14` (LIB-001) | «Error: El producto 'Cuaderno tapa dura 48 hojas' ya está inactivo.» | ☐ |
| M-26 | Dar de baja → `99` | «Error: No existe el producto con id 99.» | ☐ |

### 5.3 Depósitos

| # | Opción y entrada | Resultado esperado | OK |
|---|---|---|---|
| M-27 | Alta → enter, y otra vez con `0` | «Alta cancelada.» las dos veces (H-02) | ☐ |
| M-28 | Alta → `deposito norte` → dirección enter | «Error: Ya existe un depósito con el nombre 'deposito norte' (…).» | ☐ |
| M-29 | Alta → nombre de 51 caracteres | «Error: El nombre no puede superar los 50 caracteres.» | ☐ |
| M-30 | Modificar → `99` | «No se encontró un depósito con ese ID.» | ☐ |
| M-31 | Dar de baja → `1` (Central) | «Error: No se puede dar de baja: tiene stock con cantidad mayor a 0.» | ☐ |
| M-32 | Dar de baja → `5` (Anexo Sur) | «Error: El depósito 'Anexo Sur' ya está inactivo.» | ☐ |
| M-33 | Consulta de control (0.4) | 8 categorías, 24 productos, 5 depósitos (ningún alta rechazada o cancelada quedó guardada) | ☐ |

---

## 6. Maestros: operaciones que escriben

| # | Opción y entrada | Resultado esperado | OK |
|---|---|---|---|
| M-34 | Categorías → Alta → `Juguetería` | «Categoría creada: [id] Juguetería (activo)» | ☐ |
| M-35 | Categorías → Modificar → `7` → `Mascotas y Accesorios` | «Categoría modificada: [7] Mascotas y Accesorios (activo)» | ☐ |
| M-36 | Productos → Alta → `MAS-001`, cat. `7`, `Alimento balanceado 1 kg`, desc. enter, precios `0` y `0` | «Producto creado: [MAS-001] Alimento balanceado 1 kg (activo)» | ☐ |
| M-37 | Productos → Alta → `MAS-002`, cat. `7`, `Collar regulable`, desc. `   `, compra `1.500`, venta `9999999999.99` | Creado. En Listar: «$1.50 / $9999999999.99» (H-08: 1.500 se acepta porque es exacto) | ☐ |
| M-38 | Productos → Modificar → `24` (TEM-001, categoría 8 inactiva) → todo enter salvo precio compra `4100` | «Producto modificado» (H-01: se conserva la categoría inactiva) | ☐ |
| M-39 | Productos → Modificar → `1` (ALM-001) → categoría `2`, el resto enter | Modificado. En Listar: «Cat: 2». Repetir con categoría `1` para devolverlo | ☐ |
| M-40 | Productos → Modificar → `3` (ALM-003) → descripción `-` | Modificado. Al volver a Modificar 3 se ve «Descripción: (sin descripción)» (H-09) | ☐ |
| M-41 | Productos → Dar de baja → `13` (BEB-004, 0 en Central y Temporal) | «Producto dado de baja.» | ☐ |
| M-42 | Stock → Habilitar | BEB-004 ya no figura en la lista de productos activos | ☐ |
| M-43 | Consultas → 1 → `4` (Temporal) | 3 filas (BEB-004 ya no aparece) | ☐ |
| M-44 | Categorías → Dar de baja → `6` (Librería, solo productos inactivos) | «Categoría dada de baja.» | ☐ |
| M-45 | Productos → Modificar → `14` (LIB-001) → precio venta `2200` | «Producto modificado» (H-01: categoría y producto inactivos, se permite) | ☐ |
| M-46 | Depósitos → Dar de baja → `4` (Temporal, todo en 0) | «Depósito dado de baja.» | ☐ |
| M-47 | Consultas → 2 → `10` (BEB-001) | Central 96 + Norte 48. «Stock total: 144» (no suma Temporal) | ☐ |
| M-48 | Consultas → 1 → `4` | «Error: El depósito 'Depósito Temporal' está inactivo.» | ☐ |
| M-49 | Depósitos → Alta → `Depósito Prueba` → dirección enter | «Depósito creado: [id] Depósito Prueba (activo)» (sin dirección) | ☐ |
| M-50 | Depósitos → Modificar → ese id → nombre enter → dirección `Calle Falsa 123` | «Depósito modificado: [id] Depósito Prueba — Calle Falsa 123 (activo)» | ☐ |
| M-51 | Depósitos → Modificar → ese id → nombre enter → dirección `-` | «Depósito modificado: [id] Depósito Prueba (activo)», sin dirección (H-09) | ☐ |
| M-52 | Depósitos → Dar de baja → ese id | «Depósito dado de baja.» | ☐ |

**Verificación en la base:**

```sql
SELECT codigoProducto, descripcion, precioReferenciaCompra, precioReferenciaVenta, estadoProducto
FROM PRODUCTO
WHERE codigoProducto IN ('MAS-001', 'MAS-002', 'ALM-003', 'TEM-001', 'LIB-001', 'BEB-004');
```

| # | Resultado esperado | OK |
|---|---|---|
| M-53 | MAS-001 y MAS-002 con `descripcion` NULL. MAS-001 0.00 / 0.00; MAS-002 1.50 / 9999999999.99. ALM-003 con descripción NULL. TEM-001 compra 4100.00. LIB-001 venta 2200.00. BEB-004 `inactivo` | ☐ |

---

## 7. Concurrencia

Correr **al final**, porque crea datos con prefijo `CONC-` que la aplicación no puede borrar. Desde la raíz, en Git Bash (los comandos están en `plan_modulo_stock.md`, sección 5.1):

```bash
R=~/.m2/repository
CP="$R/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar;$R/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"
javac -encoding UTF-8 --release 17 -cp "$CP" -d target/prueba $(find src/main/java src/test/java -name "*.java")
java -cp "target/prueba;$CP" controlstock.PruebaConcurrencia
```

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| X-01 | Saldo 3 y dos ajustes simultáneos de −2 | Uno pasa, el otro «Stock insuficiente. Saldo actual: 1». Saldo final 1, un solo movimiento | ☐ |
| X-02 | Dos depósitos activos y dos bajas simultáneas | Una pasa, la otra «debe quedar al menos un depósito activo» | ☐ |
| X-03 | Ajuste +5 y baja del mismo producto, en los dos órdenes | Exactamente una pasa; nunca queda un producto inactivo con existencias | ☐ |
| X-04 | Fin del programa | Todas las comprobaciones OK | ☐ |
| X-05 | Consultas → 3 en la aplicación | Si aparecen códigos `CONC-…` (15 caracteres), la columna Código se ensancha y las filas no se desplazan (H-05) | ☐ |

---

## 8. Al terminar

La prueba deja cambios en la base. **Antes de la demo, reinstalar** con `00` → `01` → `02` → `03` → `04` y confirmar 8 / 5 / 24 / 40 / 57 (sección 0.1).

## 9. Lo que no cubre esta guía

| Verificación | Motivo |
|---|---|
| Arranque rechazado sin `STRICT_TRANS_TABLES` | Requiere cambiar el `sql_mode` global con `root` |
| Usuario inactivo | No hay usuarios inactivos en los datos |
| Baja del último depósito activo desde la interfaz | Requiere vaciar el stock de todos los depósitos; lo cubre X-02 |
| Carrera de categoría en `ProductoServicio.modificar` | Requiere dos sesiones detenidas a mitad de la transacción |
| Auditoría, compensaciones e historial | Fuera del alcance del MVP |
