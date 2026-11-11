
-- controlStock — MVP para POO (v0.1)
-- sql/03_usuario_app.sql — cuenta de MySQL que usa la aplicación Java
--
-- Se ejecuta con una cuenta administradora (por ejemplo, root), después
-- de 01_schema.sql. Se puede ejecutar más de una vez: cada vez quita TODOS
-- los permisos de la cuenta y le da exactamente los de este archivo, así
-- que si la cuenta ya existía con permisos de más, se los saca.
--
-- Si la cuenta ya existía, el script NO le cambia la contraseña: sigue
-- valiendo la que tenía.
--
-- ROLES: la cuenta tiene que estar SIN ROLES. El REVOKE de abajo quita los
-- permisos directos, pero no los roles asignados (GRANT rol TO cuenta), y
-- un rol puede darle permisos de más, como DELETE.
--
-- Para comprobar el resultado:
--   SHOW GRANTS FOR 'controlstock_app'@'localhost';
-- Tienen que aparecer solo USAGE ON *.* y los siete GRANT de abajo. Si
-- aparece una línea "GRANT `algun_rol`@`...` TO `controlstock_app`@`localhost`",
-- quitale ese rol:
--   REVOKE 'algun_rol'@'...' FROM 'controlstock_app'@'localhost';
-- Además, SELECT @@mandatory_roles; tiene que dar vacío (los roles
-- obligatorios se aplican a todas las cuentas).
--
-- Por qué existe: la aplicación no se conecta como root, sino con una
-- cuenta que solo tiene los permisos que necesita. Así la base respalda
-- dos reglas de negocio aunque el código tenga un error:
--   * Baja lógica: la aplicación no tiene DELETE en ninguna tabla.
--   * Historial inmutable: en MOVIMIENTO_STOCK y AUDITORIA solo puede
--     leer e insertar (sin UPDATE ni DELETE).
--
-- CONTRASEÑA: este archivo está versionado, así que NO lleva la contraseña
-- real. La cuenta se crea con una contraseña provisoria y VENCIDA: nadie
-- puede usarla hasta que cada uno ejecute, en una pestaña aparte de mysql que NO se guarde en el repositorio:
--
--   ALTER USER 'controlstock_app'@'localhost' IDENTIFIED BY 'tu_contraseña';
--
-- y ponga esa contraseña en su db.properties local (que tampoco se
-- versiona).
-- =====================================================================

CREATE USER IF NOT EXISTS 'controlstock_app'@'localhost'
    IDENTIFIED BY 'provisoria_cambiar'
    PASSWORD EXPIRE;

-- Parte de cero: quita cualquier permiso anterior, en cualquier nivel
-- (global, de base o de tabla). Si la cuenta es nueva, no hace nada.
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'controlstock_app'@'localhost';

-- Maestros y stock: leer, dar de alta y modificar (la baja es lógica:
-- un UPDATE del estado). El UPDATE también permite el SELECT ... FOR UPDATE
-- que usan los servicios para bloquear filas (FOR SHARE solo pide SELECT).
GRANT SELECT, INSERT, UPDATE ON controlstock_mvp.CATEGORIA TO 'controlstock_app'@'localhost';
GRANT SELECT, INSERT, UPDATE ON controlstock_mvp.PRODUCTO  TO 'controlstock_app'@'localhost';
GRANT SELECT, INSERT, UPDATE ON controlstock_mvp.DEPOSITO  TO 'controlstock_app'@'localhost';
GRANT SELECT, INSERT, UPDATE ON controlstock_mvp.STOCK     TO 'controlstock_app'@'localhost';

-- Usuarios: en el MVP solo se leen para iniciar sesión. El administrador
-- lo carga 02_datos_prueba.sql y no hay administración de usuarios.
GRANT SELECT ON controlstock_mvp.USUARIO TO 'controlstock_app'@'localhost';

-- Historial: solo leer e insertar.
GRANT SELECT, INSERT ON controlstock_mvp.MOVIMIENTO_STOCK TO 'controlstock_app'@'localhost';
GRANT SELECT, INSERT ON controlstock_mvp.AUDITORIA        TO 'controlstock_app'@'localhost';
