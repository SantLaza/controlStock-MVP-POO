
-- controlStock — MVP para POO (v0.1)
-- sql/00_reiniciar.sql — borra la base para reinstalar desde cero
--
-- este script BORRA la base controlstock_mvp con TODOS sus datos.
-- Usalo solo para reiniciar la demo o comprobar la instalación desde cero.
-- Después ejecutá, en orden: 01_schema.sql, 02_datos_prueba.sql y
-- 03_usuario_app.sql.
--
-- La cuenta controlstock_app no se borra: sus permisos quedan guardados
-- y vuelven a valer cuando 01_schema.sql crea las tablas de nuevo.

DROP DATABASE IF EXISTS controlstock_mvp;
