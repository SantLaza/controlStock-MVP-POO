-- =====================================================================
-- controlStock — MVP para POO (v0.1)
-- sql/04_datos_testeo.sql — datos realistas para el testeo manual
--
-- Se ejecuta con una cuenta administradora (por ejemplo, root) sobre una
-- base recién instalada: 00_reiniciar.sql, 01_schema.sql,
-- 02_datos_prueba.sql, 03_usuario_app.sql y después este script.
-- Si se ejecuta dos veces, falla por nombres duplicados y no inserta nada
-- (todo va dentro de una única transacción).
--
-- Escenario: un comercio mayorista con cinco depósitos, ocho categorías y
-- veinticuatro productos. Los saldos de STOCK no se cargan a mano: se
-- calculan como la suma de los movimientos (entradas menos salidas), igual
-- que los dejaría la aplicación.
--
-- Qué se busca probar con estos datos:
--
--   Categorías
--     Almacén          tiene productos activos: su baja se rechaza.
--     Librería         solo tiene productos inactivos: su baja se permite.
--     Mascotas         no tiene productos: baja permitida, sirve para altas.
--     Temporada        está inactiva: no se le pueden asignar productos.
--     Duplicados       "almacen" o "ALMACÉN" chocan con "Almacén" (la
--                      intercalación ignora mayúsculas y tildes).
--
--   Depósitos
--     Depósito Central, Depósito Norte y Sucursal Centro tienen
--                      existencias: su baja se rechaza.
--     Sucursal Centro  no tiene dirección (NULL).
--     Depósito Temporal está activo y todos sus saldos están en 0: su baja
--                      se permite.
--     Anexo Sur        está inactivo y tiene filas de STOCK en 0: no admite
--                      habilitaciones ni ajustes.
--
--   Productos
--     BEB-004          saldo 0 en todos sus depósitos: su baja se permite.
--     BAZ-003          existencias en un solo depósito: su baja se rechaza.
--     ALM-006          activo y no habilitado en ningún depósito.
--     LIB-001, LIB-002, TEM-001  inactivos: no admiten habilitaciones
--                      ni ajustes.
--     PER-004          precios en 0 (muestra sin cargo).
--     BAZ-002          nombre de 97 caracteres (formato de las tablas).
--     ALM-002          sin descripción (NULL).
--
--   Stock bajo (cantidad < stockMinimo, con stockMinimo > 0)
--     Aparecen:        ALM-001 en Central (8 de 20, faltante 12),
--                      BEB-002 en Central (32 de 36, faltante 4),
--                      LIM-001 en Norte (3 de 10, faltante 7),
--                      ALM-004 en Sucursal Centro (0 de 4, faltante 4),
--                      PER-003 en Sucursal Centro (0 de 6, faltante 6,
--                      habilitado y sin movimientos).
--     No aparecen:     ALM-002 en Central (15 de 15, igual al mínimo) y
--                      BEB-003 en Sucursal Centro (0 de 0, sin mínimo).
--
--   Totales
--     ALM-003          está en tres depósitos: 54 + 36 + 14 = 104.
--
-- La tabla AUDITORIA queda vacía: la aplicación todavía no la escribe.
-- =====================================================================

USE controlstock_mvp;

START TRANSACTION;

SET @admin = (SELECT idUsuario FROM USUARIO WHERE nombreUsuario = 'admin');

-- ---------------------------------------------------------------------
-- 1. Categorías

INSERT INTO CATEGORIA (nombreCategoria, estadoCategoria) VALUES
    ('Almacén',    'activo'),
    ('Bebidas',    'activo'),
    ('Limpieza',   'activo'),
    ('Perfumería', 'activo'),
    ('Bazar',      'activo'),
    ('Librería',   'activo'),
    ('Mascotas',   'activo'),
    ('Temporada',  'inactivo');

-- ---------------------------------------------------------------------
-- 2. Depósitos (Depósito Central ya existe, lo crea 02_datos_prueba.sql)

INSERT INTO DEPOSITO (nombreDeposito, direccion, estadoDeposito) VALUES
    ('Depósito Norte',    'Ruta Provincial 6 km 3,5, Parque Industrial', 'activo'),
    ('Sucursal Centro',   NULL,                                          'activo'),
    ('Depósito Temporal', 'Calle 25 de Mayo 1240, galpón 2',             'activo'),
    ('Anexo Sur',         'Av. Belgrano 3100',                           'inactivo');

-- ---------------------------------------------------------------------
-- 3. Productos
--    Se cargan en una tabla temporal con el nombre de la categoría y se
--    insertan buscando su id, para no depender de los autoincrementales.

CREATE TEMPORARY TABLE tmp_producto (
    codigo      VARCHAR(30),
    categoria   VARCHAR(50),
    nombre      VARCHAR(100),
    descripcion VARCHAR(255),
    compra      DECIMAL(12,2),
    venta       DECIMAL(12,2),
    estado      VARCHAR(10)
);

INSERT INTO tmp_producto VALUES
    ('ALM-001', 'Almacén',    'Yerba mate 1 kg',                 'Yerba mate con palo, paquete de 1 kg',      2850.00,  3990.00, 'activo'),
    ('ALM-002', 'Almacén',    'Azúcar común 1 kg',               NULL,                                         980.50,  1390.00, 'activo'),
    ('ALM-003', 'Almacén',    'Fideos tirabuzón 500 g',          'Pasta seca de sémola',                       720.00,  1050.00, 'activo'),
    ('ALM-004', 'Almacén',    'Aceite de girasol 1,5 L',         'Botella PET',                               2340.00,  3250.00, 'activo'),
    ('ALM-005', 'Almacén',    'Harina 0000 1 kg',                'Harina de trigo refinada',                   610.00,   890.00, 'activo'),
    ('ALM-006', 'Almacén',    'Arroz largo fino 1 kg',           'Producto nuevo, todavía sin habilitar',     1120.00,  1590.00, 'activo'),
    ('BEB-001', 'Bebidas',    'Agua mineral sin gas 2 L',        'Pack individual',                            450.00,   690.00, 'activo'),
    ('BEB-002', 'Bebidas',    'Gaseosa cola 2,25 L',             'Botella retornable',                        1580.00,  2290.00, 'activo'),
    ('BEB-003', 'Bebidas',    'Jugo de naranja 1 L',             'Jugo exprimido pasteurizado',               1350.00,  1990.00, 'activo'),
    ('BEB-004', 'Bebidas',    'Vino tinto Malbec 750 ml',        'Línea discontinuada por el proveedor',      4200.00,  6150.00, 'activo'),
    ('LIM-001', 'Limpieza',   'Lavandina 1 L',                   'Hipoclorito de sodio concentrado',           540.00,   820.00, 'activo'),
    ('LIM-002', 'Limpieza',   'Detergente concentrado 750 ml',   'Aroma limón',                               1290.00,  1890.00, 'activo'),
    ('LIM-003', 'Limpieza',   'Jabón en polvo 3 kg',             'Para lavado a máquina',                     5900.00,  8450.00, 'activo'),
    ('LIM-004', 'Limpieza',   'Esponja doble faz x3',            'Paquete de tres unidades',                   690.00,   990.00, 'activo'),
    ('PER-001', 'Perfumería', 'Shampoo 400 ml',                  'Para todo tipo de cabello',                 2150.00,  3190.00, 'activo'),
    ('PER-002', 'Perfumería', 'Jabón de tocador x3',             'Paquete de tres unidades de 90 g',          1180.00,  1750.00, 'activo'),
    ('PER-003', 'Perfumería', 'Pasta dental 90 g',               'Con flúor',                                  980.00,  1450.00, 'activo'),
    ('PER-004', 'Perfumería', 'Muestra de crema de manos 10 ml', 'Muestra promocional sin cargo',                 0.00,     0.00, 'activo'),
    ('BAZ-001', 'Bazar',      'Vaso de vidrio 300 ml x6',        'Caja de seis unidades',                     3600.00,  5200.00, 'activo'),
    ('BAZ-002', 'Bazar',      'Juego de ollas de acero inoxidable con tapa de vidrio templado y mangos antideslizantes, 5 piezas',
                                                                 'Apto para todo tipo de cocinas',           38500.00, 54900.00, 'activo'),
    ('BAZ-003', 'Bazar',      'Termo de acero 1 L',              'Doble pared, mantiene la temperatura 24 h', 12800.00, 18500.00, 'activo'),
    ('LIB-001', 'Librería',   'Cuaderno tapa dura 48 hojas',     'Rayado',                                    1450.00,  2100.00, 'inactivo'),
    ('LIB-002', 'Librería',   'Resma A4 500 hojas',              '75 g/m2',                                   6900.00,  9800.00, 'inactivo'),
    ('TEM-001', 'Temporada',  'Pan dulce con frutas 500 g',      'Producto de temporada navideña',            3900.00,  5900.00, 'inactivo');

INSERT INTO PRODUCTO (codigoProducto, idCategoria, nombreProducto, descripcion,
                      precioReferenciaCompra, precioReferenciaVenta, estadoProducto)
SELECT t.codigo, c.idCategoria, t.nombre, t.descripcion, t.compra, t.venta, t.estado
FROM tmp_producto t
JOIN CATEGORIA c ON c.nombreCategoria = t.categoria
ORDER BY t.codigo;

-- ---------------------------------------------------------------------
-- 4. Habilitaciones (STOCK) con su mínimo; la cantidad arranca en 0 y se
--    calcula en el paso 6 a partir de los movimientos.

CREATE TEMPORARY TABLE tmp_stock (
    codigo   VARCHAR(30),
    deposito VARCHAR(50),
    minimo   INT
);

INSERT INTO tmp_stock VALUES
    ('ALM-001', 'Depósito Central',  20),
    ('ALM-001', 'Depósito Norte',    10),
    ('ALM-002', 'Depósito Central',  15),
    ('ALM-002', 'Depósito Norte',     0),
    ('ALM-003', 'Depósito Central',  24),
    ('ALM-003', 'Depósito Norte',    12),
    ('ALM-003', 'Sucursal Centro',    6),
    ('ALM-004', 'Depósito Central',  12),
    ('ALM-004', 'Sucursal Centro',    4),
    ('ALM-005', 'Depósito Central',  10),
    ('ALM-005', 'Anexo Sur',          0),
    ('BEB-001', 'Depósito Central',  48),
    ('BEB-001', 'Depósito Norte',    24),
    ('BEB-001', 'Depósito Temporal',  0),
    ('BEB-002', 'Depósito Central',  36),
    ('BEB-002', 'Depósito Temporal',  0),
    ('BEB-003', 'Depósito Central',  12),
    ('BEB-003', 'Sucursal Centro',    0),
    ('BEB-004', 'Depósito Central',   0),
    ('BEB-004', 'Depósito Temporal',  0),
    ('LIM-001', 'Depósito Central',  20),
    ('LIM-001', 'Depósito Norte',    10),
    ('LIM-002', 'Depósito Central',  15),
    ('LIM-003', 'Depósito Central',   8),
    ('LIM-003', 'Depósito Temporal',  0),
    ('LIM-004', 'Depósito Central',   0),
    ('PER-001', 'Depósito Central',  10),
    ('PER-001', 'Sucursal Centro',    5),
    ('PER-002', 'Depósito Norte',     0),
    ('PER-003', 'Depósito Central',  12),
    ('PER-003', 'Sucursal Centro',    6),
    ('PER-004', 'Depósito Central',   0),
    ('BAZ-001', 'Depósito Central',   4),
    ('BAZ-001', 'Anexo Sur',          0),
    ('BAZ-002', 'Depósito Central',   2),
    ('BAZ-003', 'Depósito Central',   0),
    ('BAZ-003', 'Depósito Norte',     2),
    ('LIB-001', 'Depósito Central',   0),
    ('TEM-001', 'Depósito Central',   0),
    ('TEM-001', 'Depósito Norte',     0);

INSERT INTO STOCK (idProducto, idDeposito, cantidad, stockMinimo)
SELECT p.idProducto, d.idDeposito, 0, t.minimo
FROM tmp_stock t
JOIN PRODUCTO p ON p.codigoProducto = t.codigo
JOIN DEPOSITO d ON d.nombreDeposito = t.deposito
ORDER BY p.idProducto, d.idDeposito;

-- ---------------------------------------------------------------------
-- 5. Movimientos (todos del administrador, entre el 10 y el 16/11/2026).
--    En cada par producto–depósito las salidas son posteriores a las
--    entradas, así que ningún saldo intermedio queda negativo.

CREATE TEMPORARY TABLE tmp_movimiento (
    codigo   VARCHAR(30),
    deposito VARCHAR(50),
    fecha    DATETIME,
    tipo     VARCHAR(20),
    cantidad INT,
    motivo   VARCHAR(255)
);

INSERT INTO tmp_movimiento VALUES
    -- Carga inicial de inventario
    ('ALM-001', 'Depósito Central',  '2026-11-10 08:30:00', 'ajuste_positivo',  40, 'Carga inicial de inventario'),
    ('ALM-001', 'Depósito Norte',    '2026-11-10 08:35:00', 'ajuste_positivo',  25, 'Carga inicial de inventario'),
    ('ALM-002', 'Depósito Central',  '2026-11-10 08:40:00', 'ajuste_positivo',  30, 'Carga inicial de inventario'),
    ('ALM-002', 'Depósito Norte',    '2026-11-10 08:45:00', 'ajuste_positivo',  12, 'Carga inicial de inventario'),
    ('ALM-003', 'Depósito Central',  '2026-11-10 08:50:00', 'ajuste_positivo',  60, 'Carga inicial de inventario'),
    ('ALM-003', 'Depósito Norte',    '2026-11-10 08:55:00', 'ajuste_positivo',  36, 'Carga inicial de inventario'),
    ('ALM-003', 'Sucursal Centro',   '2026-11-10 09:00:00', 'ajuste_positivo',  18, 'Carga inicial de inventario'),
    ('ALM-004', 'Depósito Central',  '2026-11-10 09:05:00', 'ajuste_positivo',  24, 'Carga inicial de inventario'),
    ('ALM-004', 'Sucursal Centro',   '2026-11-10 09:10:00', 'ajuste_positivo',   6, 'Carga inicial de inventario'),
    ('ALM-005', 'Depósito Central',  '2026-11-10 09:15:00', 'ajuste_positivo',  50, 'Carga inicial de inventario'),
    ('ALM-005', 'Anexo Sur',         '2026-11-10 09:20:00', 'ajuste_positivo',  10, 'Carga inicial de inventario'),
    ('BEB-001', 'Depósito Central',  '2026-11-10 09:25:00', 'ajuste_positivo', 120, 'Carga inicial de inventario'),
    ('BEB-001', 'Depósito Norte',    '2026-11-10 09:30:00', 'ajuste_positivo',  48, 'Carga inicial de inventario'),
    ('BEB-001', 'Depósito Temporal', '2026-11-10 09:35:00', 'ajuste_positivo',  24, 'Carga inicial de inventario'),
    ('BEB-002', 'Depósito Central',  '2026-11-10 09:40:00', 'ajuste_positivo',  72, 'Carga inicial de inventario'),
    ('BEB-002', 'Depósito Temporal', '2026-11-10 09:45:00', 'ajuste_positivo',  24, 'Carga inicial de inventario'),
    ('BEB-003', 'Depósito Central',  '2026-11-10 09:50:00', 'ajuste_positivo',  24, 'Carga inicial de inventario'),
    ('BEB-004', 'Depósito Central',  '2026-11-10 09:55:00', 'ajuste_positivo',  12, 'Carga inicial de inventario'),
    ('BEB-004', 'Depósito Temporal', '2026-11-10 10:00:00', 'ajuste_positivo',   6, 'Carga inicial de inventario'),
    ('LIM-001', 'Depósito Central',  '2026-11-10 10:05:00', 'ajuste_positivo',  48, 'Carga inicial de inventario'),
    ('LIM-001', 'Depósito Norte',    '2026-11-10 10:10:00', 'ajuste_positivo',  15, 'Carga inicial de inventario'),
    ('LIM-002', 'Depósito Central',  '2026-11-10 10:15:00', 'ajuste_positivo',  30, 'Carga inicial de inventario'),
    ('LIM-003', 'Depósito Central',  '2026-11-10 10:20:00', 'ajuste_positivo',  16, 'Carga inicial de inventario'),
    ('LIM-004', 'Depósito Central',  '2026-11-10 10:25:00', 'ajuste_positivo',  40, 'Carga inicial de inventario'),
    ('PER-001', 'Depósito Central',  '2026-11-10 10:30:00', 'ajuste_positivo',  20, 'Carga inicial de inventario'),
    ('PER-001', 'Sucursal Centro',   '2026-11-10 10:35:00', 'ajuste_positivo',  10, 'Carga inicial de inventario'),
    ('PER-002', 'Depósito Norte',    '2026-11-10 10:40:00', 'ajuste_positivo',  30, 'Carga inicial de inventario'),
    ('PER-003', 'Depósito Central',  '2026-11-10 10:45:00', 'ajuste_positivo',  24, 'Carga inicial de inventario'),
    ('PER-004', 'Depósito Central',  '2026-11-10 10:50:00', 'ajuste_positivo', 100, 'Carga inicial de inventario'),
    ('BAZ-001', 'Depósito Central',  '2026-11-10 10:55:00', 'ajuste_positivo',  10, 'Carga inicial de inventario'),
    ('BAZ-002', 'Depósito Central',  '2026-11-10 11:00:00', 'ajuste_positivo',   5, 'Carga inicial de inventario'),
    ('BAZ-003', 'Depósito Central',  '2026-11-10 11:05:00', 'ajuste_positivo',   6, 'Carga inicial de inventario'),
    ('BAZ-003', 'Depósito Norte',    '2026-11-10 11:10:00', 'ajuste_positivo',   4, 'Carga inicial de inventario'),
    ('LIB-001', 'Depósito Central',  '2026-11-10 11:15:00', 'ajuste_positivo',  30, 'Carga inicial de inventario'),
    ('TEM-001', 'Depósito Central',  '2026-11-10 11:20:00', 'ajuste_positivo',  40, 'Carga inicial de inventario'),

    -- Salidas y correcciones posteriores
    ('ALM-005', 'Anexo Sur',         '2026-11-11 16:00:00', 'ajuste_negativo',  10, 'Traslado manual al Depósito Central antes del cierre del anexo'),
    ('LIB-001', 'Depósito Central',  '2026-11-11 16:30:00', 'ajuste_negativo',  30, 'Devolución a proveedor por discontinuación de la línea'),
    ('TEM-001', 'Depósito Central',  '2026-11-12 09:00:00', 'ajuste_negativo',  38, 'Salida por venta mayorista, remito 0001-00004521'),
    ('TEM-001', 'Depósito Central',  '2026-11-12 09:10:00', 'ajuste_negativo',   2, 'Producto vencido'),
    ('BEB-004', 'Depósito Central',  '2026-11-12 11:00:00', 'ajuste_negativo',  12, 'Devolución a proveedor por discontinuación de la línea'),
    ('BEB-004', 'Depósito Temporal', '2026-11-12 11:10:00', 'ajuste_negativo',   6, 'Devolución a proveedor por discontinuación de la línea'),
    ('BEB-001', 'Depósito Temporal', '2026-11-13 08:00:00', 'ajuste_negativo',  24, 'Vaciado del depósito temporal, traslado manual al Central'),
    ('BEB-002', 'Depósito Temporal', '2026-11-13 08:10:00', 'ajuste_negativo',  24, 'Vaciado del depósito temporal, traslado manual al Central'),
    ('ALM-003', 'Depósito Central',  '2026-11-13 10:00:00', 'ajuste_negativo',   6, 'Mercadería dañada en la descarga'),
    ('ALM-004', 'Depósito Central',  '2026-11-13 10:15:00', 'ajuste_negativo',   3, 'Rotura de envases'),
    ('LIM-003', 'Depósito Central',  '2026-11-13 10:30:00', 'ajuste_negativo',   2, 'Envases rotos'),
    ('BAZ-001', 'Depósito Central',  '2026-11-13 10:45:00', 'ajuste_negativo',   1, 'Rotura en manipulación'),
    ('ALM-001', 'Depósito Central',  '2026-11-14 12:00:00', 'ajuste_negativo',  32, 'Salida por venta mayorista, remito 0001-00004533'),
    ('ALM-002', 'Depósito Central',  '2026-11-14 12:10:00', 'ajuste_negativo',  15, 'Salida por venta mayorista, remito 0001-00004533'),
    ('BEB-001', 'Depósito Central',  '2026-11-14 12:20:00', 'ajuste_negativo',  24, 'Salida por venta mayorista, remito 0001-00004534'),
    ('BEB-002', 'Depósito Central',  '2026-11-14 12:30:00', 'ajuste_negativo',  40, 'Salida por venta mayorista, remito 0001-00004534'),
    ('LIM-001', 'Depósito Central',  '2026-11-14 12:40:00', 'ajuste_negativo',  10, 'Salida por venta mayorista, remito 0001-00004535'),
    ('LIM-001', 'Depósito Norte',    '2026-11-15 09:00:00', 'ajuste_negativo',  12, 'Salida por venta mayorista, remito 0002-00000871'),
    ('ALM-003', 'Sucursal Centro',   '2026-11-15 17:00:00', 'ajuste_negativo',   4, 'Venta en mostrador'),
    ('ALM-004', 'Sucursal Centro',   '2026-11-15 17:10:00', 'ajuste_negativo',   6, 'Venta en mostrador'),
    ('BAZ-003', 'Depósito Central',  '2026-11-16 09:30:00', 'ajuste_negativo',   6, 'Salida por venta mayorista, remito 0001-00004540'),
    ('PER-001', 'Depósito Central',  '2026-11-16 18:00:00', 'ajuste_negativo',   1, 'Diferencia de recuento físico');

INSERT INTO MOVIMIENTO_STOCK (idProducto, idDeposito, idUsuario, fechaHoraMovimiento,
                              tipoMovimiento, cantidad, motivo)
SELECT p.idProducto, d.idDeposito, @admin, t.fecha, t.tipo, t.cantidad, t.motivo
FROM tmp_movimiento t
JOIN PRODUCTO p ON p.codigoProducto = t.codigo
JOIN DEPOSITO d ON d.nombreDeposito = t.deposito
ORDER BY t.fecha;

-- ---------------------------------------------------------------------
-- 6. Saldos: cada cantidad es la suma de sus movimientos.

UPDATE STOCK s
JOIN (
    SELECT idProducto, idDeposito,
           SUM(CASE WHEN tipoMovimiento = 'ajuste_positivo' THEN cantidad
                    ELSE -cantidad END) AS saldo
    FROM MOVIMIENTO_STOCK
    GROUP BY idProducto, idDeposito
) m ON m.idProducto = s.idProducto AND m.idDeposito = s.idDeposito
SET s.cantidad = m.saldo;

DROP TEMPORARY TABLE tmp_producto;
DROP TEMPORARY TABLE tmp_stock;
DROP TEMPORARY TABLE tmp_movimiento;

COMMIT;

-- ---------------------------------------------------------------------
-- Resumen de lo cargado. Valores esperados:
-- 8 categorías, 5 depósitos, 24 productos, 40 habilitaciones y
-- 57 movimientos.

SELECT (SELECT COUNT(*) FROM CATEGORIA)        AS categorias,
       (SELECT COUNT(*) FROM DEPOSITO)         AS depositos,
       (SELECT COUNT(*) FROM PRODUCTO)         AS productos,
       (SELECT COUNT(*) FROM STOCK)            AS habilitaciones,
       (SELECT COUNT(*) FROM MOVIMIENTO_STOCK) AS movimientos;
