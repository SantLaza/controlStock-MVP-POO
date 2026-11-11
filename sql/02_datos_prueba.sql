-- =====================================================================
-- controlStock — MVP para POO (v0.1)
-- sql/02_datos_prueba.sql — administrador, depósito inicial y datos
--                           para la demo
--
-- Se ejecuta después de 01_schema.sql y antes de 03_usuario_app.sql,
-- con una cuenta administradora (por ejemplo, root).
--
-- Contenido:
--   1. Usuario administrador (admin / admin123)
--   2. Depósito Central (depósito inicial activo)
--
-- La contraseña "admin123" está hasheada con BCrypt (costo 10).
-- Para la demo y las pruebas, usar: usuario "admin", contraseña "admin123".
-- =====================================================================

USE controlstock_mvp;

-- 1. Administrador
--    Contraseña en texto plano: admin123
INSERT INTO USUARIO (nombreUsuario, hashContrasena, rol, estadoUsuario)
VALUES ('admin',
        '$2a$10$Cq1..4H87ISxJDLjh98vpu.NUHhEg5.LDxUpH7kpW31b6NZNcwM8G',
        'administrador',
        'activo');

-- 2. Depósito Central
INSERT INTO DEPOSITO (nombreDeposito, direccion, estadoDeposito)
VALUES ('Depósito Central', 'Dirección del depósito principal', 'activo');
