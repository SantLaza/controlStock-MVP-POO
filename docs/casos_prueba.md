# Casos de prueba — Presentación del MVP

**Objetivo:** mostrar en unos 8 a 10 minutos qué se puede hacer con el sistema, con una sola historia encadenada que recorre los tres módulos (Maestros, Stock y Consultas) y las principales reglas de negocio.

> **El orden importa.** Cada caso supone que se hicieron los anteriores. Si se salta uno, los números siguientes pueden cambiar.

---

## 0. Preparación (antes de presentar)

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

### 0.3 Datos que se usan en la demo

| Id | Código | Producto | Situación inicial |
|---|---|---|---|
| 3 | ALM-003 | Fideos tirabuzón 500 g | Habilitado en 3 depósitos |
| 6 | ALM-006 | Arroz largo fino 1 kg | Producto nuevo, sin habilitar en ningún depósito |
| 9 | BAZ-003 | Termo de acero 1 L | Tiene 4 unidades en Depósito Norte |

| Id | Depósito |
|---|---|
| 1 | Depósito Central |
| 2 | Depósito Norte |
| 3 | Sucursal Centro |

Usuario de la demo: `admin` / `admin123` (es el único usuario cargado).

---

## 1. Guion de la presentación

### Menús de referencia

- **Principal:** 1 Maestros · 2 Stock · 3 Consultas · 4 Cerrar sesión · 0 Salir
- **Maestros:** 1 Categorías · 2 Productos · 3 Depósitos → dentro de cada uno: 1 Alta · 2 Modificar · 3 Listar · 4 Dar de baja
- **Stock:** 1 Habilitar · 2 Stock mínimo · 3 Ajuste positivo · 4 Ajuste negativo
- **Consultas:** 1 Stock por depósito · 2 Stock total de un producto · 3 Productos con stock bajo

### 1.1 Acceso

| # | Qué se muestra | Entrada | Resultado esperado | OK |
|---|---|---|---|---|
| P-01 | Login seguro | `admin` / `malaclave` | «Error: Usuario o contraseña incorrectos.» No revela si el usuario existe | ☐ |
| P-02 | Ingreso correcto | `admin` / `admin123` | «¡Bienvenido, admin (administrador)!» y el menú principal | ☐ |

### 1.2 Consultas: la situación actual

| # | Qué se muestra | Entrada | Resultado esperado | OK |
|---|---|---|---|---|
| P-03 | Stock consolidado de un producto en todos los depósitos | `3` → `2` → `3` (ALM-003) | Central 54, Norte 36, Sucursal Centro 14. «Stock total: 104» | ☐ |
| P-04 | Alerta de reposición | `3` → `3` | **5 filas**: ALM-001 Central 8 / 20 / 12 · ALM-004 Sucursal Centro 0 / 4 / 4 · BEB-002 Central 32 / 36 / 4 · LIM-001 Norte 3 / 10 / 7 · PER-003 Sucursal Centro 0 / 6 / 6 | ☐ |

### 1.3 Maestros: validación

| # | Qué se muestra | Entrada | Resultado esperado | OK |
|---|---|---|---|---|
| P-05 | No permite duplicados (ignora mayúsculas y tildes) | `1` → `1` → `1` (Categorías → Alta) → `ALMACÉN` | «Error: Ya existe una categoría con el nombre 'ALMACÉN' (no se distinguen mayúsculas ni tildes).» | ☐ |

### 1.4 Stock: ciclo completo de un producto nuevo

Se trabaja con **ALM-006 (Arroz largo fino 1 kg)** en **Depósito Central**.

| # | Qué se muestra | Entrada | Resultado esperado | OK |
|---|---|---|---|---|
| P-06 | Habilitar el producto en un depósito | `2` → `1` → `6` → `1` | La lista muestra solo productos activos y depósitos donde todavía no está. «Producto habilitado en el depósito (saldo 0, mínimo 0).» | ☐ |
| P-07 | Definir el stock mínimo | `2` → `6` → `1` → `5` | «Mínimo actualizado a 5. Saldo actual: 0.» y «AVISO - Stock bajo: el mínimo es 5, faltan 5 unidades.» | ☐ |
| P-08 | Ingreso de mercadería | `3` → `6` → `1` → `10` → motivo `Ingreso de proveedor` | «Ajuste registrado. Saldo actual: 10.» **sin** aviso | ☐ |
| P-09 | Salida (venta) | `4` → `6` → `1` → `7` → motivo `Venta mostrador` | «Ajuste registrado. Saldo actual: 3.» y «AVISO - Stock bajo: el mínimo es 5, faltan 2 unidades.» | ☐ |
| P-10 | No permite stock negativo | `4` → `6` → `1` → `4` → motivo `Prueba` | «Error: Stock insuficiente. Saldo actual: 3» | ☐ |

### 1.5 Consultas: el reporte refleja los cambios

| # | Qué se muestra | Entrada | Resultado esperado | OK |
|---|---|---|---|---|
| P-11 | El producto nuevo entra en la alerta | Principal → `3` → `3` | **6 filas**: aparece **ALM-006 Depósito Central 3 / 5 / 2** junto a las 5 de P-04 | ☐ |

### 1.6 Integridad de datos

| # | Qué se muestra | Entrada | Resultado esperado | OK |
|---|---|---|---|---|
| P-12 | No se puede dar de baja un producto con existencias | Principal → `1` → `2` → `4` (Productos → Dar de baja) → `9` (BAZ-003) | «Error: No se puede dar de baja: tiene stock con cantidad mayor a 0.» | ☐ |

### 1.7 Persistencia

| # | Qué se muestra | Entrada | Resultado esperado | OK |
|---|---|---|---|---|
| P-13 | Los datos quedan guardados en MySQL | Salir con `0`, volver a ejecutar, ingresar y `3` → `2` → `6` | Depósito Central 3 / 5 / 2 **BAJO**. «Stock total: 3» | ☐ |

---

## 2. Cierre opcional

### 2.1 Trazabilidad de movimientos

En MySQL:

```sql
USE controlstock_mvp;
SELECT idMovimiento, idProducto, idDeposito, idUsuario, tipoMovimiento, cantidad, motivo
FROM MOVIMIENTO_STOCK
ORDER BY idMovimiento DESC
LIMIT 2;
```

| # | Resultado esperado | OK |
|---|---|---|
| P-14 | Los dos últimos movimientos son de ALM-006 (`idProducto` 6) en Central: −7 «Venta mostrador» y +10 «Ingreso de proveedor», los dos con `idUsuario` = 1 (admin) | ☐ |
| P-15 | `SELECT COUNT(*) FROM MOVIMIENTO_STOCK;` devuelve **59** (57 + 2). El intento rechazado de P-10 no dejó registro | ☐ |

### 2.2 Concurrencia

Hacerlo **al final**, porque deja datos con prefijo `CONC-` en la base. Desde la raíz, en Git Bash:

```bash
R=~/.m2/repository
CP="$R/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar;$R/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"
javac -encoding UTF-8 --release 17 -cp "$CP" -d target/prueba $(find src/main/java src/test/java -name "*.java")
java -cp "target/prueba;$CP" controlstock.PruebaConcurrencia
```

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| P-16 | Saldo 3 y dos ajustes simultáneos de −2 | Pasa uno solo; el otro recibe «Stock insuficiente. Saldo actual: 1». Saldo final 1 y un solo movimiento | ☐ |
| P-17 | Fin del programa | Todas las comprobaciones OK | ☐ |

---

## 3. Después de la presentación

La demo deja cambios en la base. Para repetirla, reinstalar con `00` → `01` → `02` → `03` → `04` y confirmar 8 / 5 / 24 / 40 / 57 (sección 0.1).

La guía completa de pruebas (más de 100 casos, incluidos todos los rechazos y las correcciones H-01 a H-10) quedó en el historial de git, en el commit `9df03f0`:

```bash
git show 9df03f0:docs/casos_prueba.md
```
