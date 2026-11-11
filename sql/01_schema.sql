-- =====================================================================
-- controlStock — MVP para POO (v0.1)
-- sql/01_schema.sql — creación de la base de datos y de las 7 tablas
--
-- Base: modelo relacional del MVP v0.3 (docs/modelo_relacional_mvp.md),
--       que sale del modelo lógico v0.1 y del DER del MVP v0.1.
--
-- Orden de ejecución de los scripts:
--   00_reiniciar.sql   (solo para reinstalar: borra la base)
--   01_schema.sql      (este: crea la base y las tablas)
--   02_datos_prueba.sql
--   03_usuario_app.sql (cuenta con permisos mínimos para la aplicación)
--
-- Orden de creación: primero las tablas referenciadas y después las que
-- las referencian (camino referencial):
--   CATEGORIA, DEPOSITO, USUARIO, PRODUCTO, STOCK, MOVIMIENTO_STOCK, AUDITORIA
--
-- Convención de nombres de restricciones:
--   ak_  clave alternativa (UNIQUE)     fk_  clave foránea
--   chk_ restricción CHECK              (la PK no lleva nombre: en MySQL
--                                        siempre se llama PRIMARY)
-- =====================================================================

CREATE DATABASE controlstock_mvp
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE controlstock_mvp;

-- ---------------------------------------------------------------------
-- CATEGORIA

CREATE TABLE CATEGORIA (
    idCategoria      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombreCategoria  VARCHAR(50)  NOT NULL,
    estadoCategoria  ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',

    PRIMARY KEY (idCategoria),
    CONSTRAINT ak_categoria_nombre UNIQUE (nombreCategoria),
    CONSTRAINT chk_categoria_nombre CHECK (CHAR_LENGTH(TRIM(nombreCategoria)) > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- DEPOSITO

CREATE TABLE DEPOSITO (
    idDeposito      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombreDeposito  VARCHAR(50)  NOT NULL,
    direccion       VARCHAR(150) NULL,
    estadoDeposito  ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',

    PRIMARY KEY (idDeposito),
    CONSTRAINT ak_deposito_nombre UNIQUE (nombreDeposito),
    CONSTRAINT chk_deposito_nombre CHECK (CHAR_LENGTH(TRIM(nombreDeposito)) > 0),
    CONSTRAINT chk_deposito_direccion
        CHECK (direccion IS NULL OR CHAR_LENGTH(TRIM(direccion)) > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- USUARIO

CREATE TABLE USUARIO (
    idUsuario       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombreUsuario   VARCHAR(30)  NOT NULL,
    hashContrasena  VARCHAR(255) NOT NULL,
    rol             ENUM('administrador', 'encargado_deposito', 'operador') NOT NULL,
    estadoUsuario   ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',

    PRIMARY KEY (idUsuario),
    CONSTRAINT ak_usuario_nombre UNIQUE (nombreUsuario),
    CONSTRAINT chk_usuario_nombre CHECK (CHAR_LENGTH(TRIM(nombreUsuario)) > 0),
    CONSTRAINT chk_usuario_hash   CHECK (CHAR_LENGTH(TRIM(hashContrasena)) > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- PRODUCTO

CREATE TABLE PRODUCTO (
    idProducto              INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    codigoProducto          VARCHAR(30)   NOT NULL,
    idCategoria             INT UNSIGNED  NOT NULL,
    nombreProducto          VARCHAR(100)  NOT NULL,
    descripcion             VARCHAR(255)  NULL,
    precioReferenciaCompra  DECIMAL(12,2) NOT NULL,
    precioReferenciaVenta   DECIMAL(12,2) NOT NULL,
    estadoProducto          ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',

    PRIMARY KEY (idProducto),
    CONSTRAINT ak_producto_codigo UNIQUE (codigoProducto),
    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (idCategoria) REFERENCES CATEGORIA (idCategoria),
    CONSTRAINT chk_producto_codigo CHECK (CHAR_LENGTH(TRIM(codigoProducto)) > 0),
    CONSTRAINT chk_producto_nombre CHECK (CHAR_LENGTH(TRIM(nombreProducto)) > 0),
    CONSTRAINT chk_producto_descripcion
        CHECK (descripcion IS NULL OR CHAR_LENGTH(TRIM(descripcion)) > 0),
    CONSTRAINT chk_producto_precio_compra CHECK (precioReferenciaCompra >= 0),
    CONSTRAINT chk_producto_precio_venta  CHECK (precioReferenciaVenta >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- STOCK (entidad débil: PK compuesta por las claves de sus dueñas)

CREATE TABLE STOCK (
    idProducto   INT UNSIGNED NOT NULL,
    idDeposito   INT UNSIGNED NOT NULL,
    cantidad     INT          NOT NULL DEFAULT 0,
    stockMinimo  INT          NOT NULL DEFAULT 0,

    PRIMARY KEY (idProducto, idDeposito),
    CONSTRAINT fk_stock_producto
        FOREIGN KEY (idProducto) REFERENCES PRODUCTO (idProducto),
    CONSTRAINT fk_stock_deposito
        FOREIGN KEY (idDeposito) REFERENCES DEPOSITO (idDeposito),
    CONSTRAINT chk_stock_cantidad CHECK (cantidad >= 0),
    CONSTRAINT chk_stock_minimo   CHECK (stockMinimo >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- MOVIMIENTO_STOCK
poo_java
CREATE TABLE MOVIMIENTO_STOCK (
    idMovimiento           INT UNSIGNED NOT NULL AUTO_INCREMENT,
    idProducto             INT UNSIGNED NOT NULL,
    idDeposito             INT UNSIGNED NOT NULL,
    idUsuario              INT UNSIGNED NOT NULL,
    fechaHoraMovimiento    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipoMovimiento         ENUM('ajuste_positivo', 'ajuste_negativo') NOT NULL,
    cantidad               INT          NOT NULL,
    motivo                 VARCHAR(255) NOT NULL,
    idMovimientoCorregido  INT UNSIGNED NULL,

    PRIMARY KEY (idMovimiento),
    -- Una sola FK de dos columnas: exige que el PAR producto–depósito
    -- exista en STOCK (que el producto esté habilitado en ese depósito).
    CONSTRAINT fk_movimiento_stock
        FOREIGN KEY (idProducto, idDeposito) REFERENCES STOCK (idProducto, idDeposito),
    CONSTRAINT fk_movimiento_usuario
        FOREIGN KEY (idUsuario) REFERENCES USUARIO (idUsuario),
    -- Autorreferencia (relación CORRIGE): el compensatorio apunta al original.
    CONSTRAINT fk_movimiento_corregido
        FOREIGN KEY (idMovimientoCorregido) REFERENCES MOVIMIENTO_STOCK (idMovimiento),
    CONSTRAINT chk_movimiento_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_movimiento_motivo   CHECK (CHAR_LENGTH(TRIM(motivo)) > 0)
    -- "Un movimiento no se corrige a sí mismo" (idMovimientoCorregido <> idMovimiento)
    -- NO va como CHECK: MySQL no permite usar una columna AUTO_INCREMENT en un
    -- CHECK. Se valida en el servicio.
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- AUDITORIA

CREATE TABLE AUDITORIA (
    idAuditoria         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    idUsuario           INT UNSIGNED NOT NULL,
    fechaHoraAuditoria  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accion              ENUM('alta', 'modificacion', 'baja_logica') NOT NULL,
    entidadAfectada     ENUM('categoria', 'producto', 'deposito', 'stock', 'usuario') NOT NULL,
    registroAfectado    VARCHAR(30)  NOT NULL,
    descripcionCambio   VARCHAR(500) NOT NULL,

    PRIMARY KEY (idAuditoria),
    CONSTRAINT fk_auditoria_usuario
        FOREIGN KEY (idUsuario) REFERENCES USUARIO (idUsuario),
    -- registroAfectado NO es FK: guarda como texto el id del registro
    -- afectado, que puede ser de cualquier tabla.
    CONSTRAINT chk_auditoria_registro    CHECK (CHAR_LENGTH(TRIM(registroAfectado)) > 0),
    CONSTRAINT chk_auditoria_descripcion CHECK (CHAR_LENGTH(TRIM(descripcionCambio)) > 0)
) ENGINE = InnoDB;


