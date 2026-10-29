# controlStock — MVP para Programación Orientada a Objetos

Producto mínimo de un sistema de control de stock para una PYME, desarrollado para la entrega de la materia Programación Orientada a Objetos (UNPSJB, Sede Esquel) del **18/11/2026**.

**Autores:** Santiago Lazarte y Joaquin Curihuala

> Este repositorio contiene solo la entrega de POO. Comparte el dominio y la documentación de referencia con el producto controlStock, pero es un desarrollo independiente: el producto se desarrolla y documenta en un repositorio aparte. El modelo de datos de este MVP es un subconjunto del modelo completo (ver [`docs/explicacion_DER_MVP.md`](docs/explicacion_DER_MVP.md)).

## Alcance previsto

El alcance final se fija el 08/11/2026. Cada bloque se marca cuando está terminado.

**Imprescindible**

- [ ] Inicio y cierre de sesión con un usuario administrador precargado.
- [ ] Alta, modificación, listado y baja lógica de categorías, productos y depósitos.
- [ ] Habilitación de productos en depósitos y definición del stock mínimo de cada uno.
- [ ] Ajustes de stock positivos y negativos, con motivo obligatorio y sin permitir stock negativo.
- [ ] Consulta de stock por depósito, stock total de un producto y listado de productos con stock bajo.

**Importante**

- [ ] Movimientos compensatorios para corregir ajustes, sin modificar ni borrar el movimiento original.
- [ ] Historial de movimientos con filtros.
- [ ] Auditoría automática de las altas, modificaciones y bajas de categorías, productos y depósitos, de las habilitaciones y de los cambios de stock mínimo.

## Tecnologías

- Java 17 y Maven
- MySQL 8.0.16 o superior, con acceso por JDBC
- Interfaz de consola

## Estructura del repositorio

```
docs/   plan del MVP, DER (der_mvp.drawio y der_mvp.png) y su explicación
sql/    scripts de creación de la base de datos y de carga de datos iniciales
src/    código fuente, organizado en capas: modelo, dao, servicio, ui y util
```

## Estado

En desarrollo. Fecha de entrega: 18/11/2026.