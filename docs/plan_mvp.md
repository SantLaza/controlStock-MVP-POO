# controlStock — Plan MVP para POO

**Integrantes:** Santiago Lazarte y Joaquin Curihuala.  
**Entrega objetivo:** 18/11/2026.  
**Primera versión completa e integrada:** 13/11/2026.  
**Corte de alcance:** 08/11/2026.

## 1. Objetivo

Desarrollar una aplicación de consola que permita administrar productos y controlar sus existencias en distintos depósitos.

El MVP deberá demostrar un recorrido completo: acceder al sistema, registrar maestros, habilitar un producto en un depósito, cargar y ajustar existencias y consultar el resultado.

Para POO, se acompañará el funcionamiento con un diseño de clases que permita explicar el encapsulamiento, las responsabilidades de cada clase, la colaboración entre objetos y el uso de interfaces.

La entrega será un **subconjunto priorizado de la V1** del proyecto, sin ser una versión del producto (ver `explicacion_DER_MVP.md`, sección 2). Las funciones y pruebas que queden pendientes conservarán su referencia a los requisitos del proyecto.

## 2. Tecnología

- Java 17 y Maven.
- JDBC y MySQL **8.0.16 o superior**, con motor InnoDB.
- Interfaz de consola en español.
- JUnit 5 para pruebas de servicios.
- Git y GitHub, con ramas cortas y revisión mediante pull requests.

La versión mínima de MySQL se indica en el README porque los `CHECK` se hacen cumplir desde 8.0.16. [Documentación oficial](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html).

## 3. Alcance por prioridad

| Prioridad | Funcionalidades |
|---|---|
| **Imprescindible** | Base ejecutable; inicio y cierre de sesión con un administrador; alta, modificación, listado y baja lógica de categorías, productos y depósitos; habilitación y mínimos; ajustes positivos y negativos en transacción; consultas de stock por depósito, total y stock bajo |
| **Importante** | Compensaciones; auditoría automática; historial de movimientos con filtros |
| **Si alcanza** | Reactivación; inclusión de inactivos en consultas; navegación al movimiento corregido; búsqueda avanzada de productos |

Las validaciones esenciales forman parte del núcleo:

- Códigos y nombres únicos según corresponda.
- Categoría activa al asignarla a un producto.
- Producto y depósito activos para habilitar o ajustar.
- Una única combinación de stock por producto y depósito.
- Cantidades enteras válidas y stock nunca negativo.
- Baja de productos y depósitos únicamente con existencias en cero.
- Conservación de al menos un depósito activo.
- Movimientos registrados que no se editan ni eliminan.

Compras, ventas, transferencias, gestión de clientes y proveedores, administración de usuarios, permisos por rol, reportes y exportaciones quedan fuera del MVP.

El **08/11**, ambos integrantes fijarán el alcance final según lo que ya funciona y el tiempo restante. Cada bloque adicional deberá poder completarse, integrarse y probarse antes del 13/11. Si entran las compensaciones pero no la reactivación, no se podrán corregir movimientos de productos o depósitos inactivos.

## 4. Diseño orientado a objetos

La aplicación seguirá el recorrido:

**Interfaz → Servicios → DAO → Base de datos**

Las clases del modelo representarán los conceptos del dominio.

| Componente | Responsabilidad |
|---|---|
| `modelo` | Representar Usuario, Categoría, Producto, Depósito, Stock y MovimientoStock; encapsular sus datos y comportamientos |
| `dao` | Definir contratos de persistencia e implementarlos con JDBC y consultas parametrizadas |
| `servicio` | Coordinar los casos de uso, validar reglas entre objetos y controlar las transacciones |
| `ui` | Leer datos, mostrar resultados y solicitar confirmaciones utilizando los servicios |
| `util` | Proveer conexión, configuración y utilidades compartidas |

Se entregará un **diagrama de clases consistente con el código**, con atributos relevantes, métodos, asociaciones e interfaces.

Los servicios dependerán de interfaces DAO, permitiendo utilizar implementaciones de prueba. La herencia y las clases abstractas se justificarán según las relaciones del dominio y la consigna de la materia.

Cada módulo tendrá su propio submenú. Se acordará un responsable de integrar las opciones en el menú principal.

## 5. Acuerdos previos a la base

**Disponibilidad.** Antes del 28/10, cada integrante indicará cuántas horas semanales puede dedicar. El calendario se contrastará con esa capacidad y una estimación por bloque.

**Restricciones de movimientos.** En el MVP todos los movimientos son ajustes, así que no hace falta la restricción de origen del modelo completo:

| Campo | Regla |
|---|---|
| `tipoMovimiento` | Ajuste positivo o ajuste negativo |
| `cantidad` | Entero mayor que cero |
| `motivo` | Obligatorio: no nulo y no vacío después de quitar espacios |
| `idMovimientoCorregido` | Opcional; solo lo tienen los compensatorios |
| `idProducto`, `idDeposito` | Referencia compuesta a STOCK |

Que un movimiento no se corrija a sí mismo se valida en el servicio, porque MySQL no permite usar una columna `AUTO_INCREMENT` en un `CHECK`.

**Transacciones.** El servicio controlará la transacción. Todos los DAO participantes utilizarán la misma conexión: el cambio de saldo y el movimiento se guardarán juntos o se revertirán juntos.

**Auditoría.** Si se incluye ese bloque, el cambio y su auditoría se guardarán en la misma transacción. Preparar una tabla o interfaz no contará como auditoría implementada.

**Contratos compartidos.** Definir nombres, identificadores, errores de negocio y cómo llega el usuario de la sesión a los servicios. Usar importes decimales exactos, contraseñas con hash y sal y configuración sin credenciales versionadas.

## 6. Ramas y reparto

| Rama | Contenido | Responsable propuesto |
|---|---|---|
| `docs/mvp` | DER, explicación, modelo lógico y diagrama de clases | Ambos |
| `feature/mvp-base` | Modelo lógico, SQL, Maven, conexión, configuración, transacciones y acceso | Ambos |
| `feature/maestros` | Categorías, productos y depósitos | Integrante A |
| `feature/stock` | Habilitación, mínimos, ajustes y validaciones | Integrante B |
| `feature/consultas` | Stock por depósito, total y stock bajo | Quien libere capacidad primero |

Santiago y Joaquin definirán quién ocupa A y B según disponibilidad.

Después de integrar la base, maestros y stock podrán avanzar en paralelo utilizando los contratos acordados y datos de prueba.

Cada rama partirá de `main` actualizado y volverá mediante un PR revisado por el otro integrante. Los bloques grandes podrán dividirse en entregas menores.

## 7. Calendario

| Fechas | Hito |
|---|---|
| **26–28/10** | Acordar horas y responsables; alinear documentación; preparar modelo lógico y restricciones |
| **29–31/10** | Integrar la base con acceso y comprobar la instalación desde cero |
| **01–07/11** | Integrar maestros, stock y consultas imprescindibles; ejecutar una primera secuencia completa |
| **08/11** | Fijar el alcance final y registrar pendientes |
| **09–10/11** | Completar los bloques seleccionados e integrar los submenús |
| **11–13/11** | Cerrar pruebas y ejecutar la demo completa |
| **14–17/11** | Corregir defectos, comprobar instalación y ensayar la presentación |
| **18/11** | Entregar la versión etiquetada `v0.1-mvp` |

La integración será continua. El 13/11 deberá funcionar todo el alcance elegido.

## 8. Pruebas prioritarias

Se documentarán los casos de lo imprescindible, vinculados con sus requisitos y reglas de negocio.

Las verificaciones principales serán:

- Acceso válido, acceso rechazado y cierre de sesión.
- Maestros válidos, duplicados y restricciones de baja.
- Habilitación duplicada y mínimos inválidos.
- Ajustes válidos, cantidades inválidas, motivo vacío y stock insuficiente.
- Reversión completa ante un fallo entre actualización de saldo y registro del movimiento.
- Concurrencia: con saldo 3, dos ajustes simultáneos de −2 permiten uno y rechazan el otro; saldo final 1 y un solo movimiento nuevo.
- Consultas correctas y persistencia después de reiniciar.

Los bloques importantes incluidos tendrán pruebas de su recorrido principal y sus rechazos críticos. La cobertura restante de RNF-22 quedará identificada como pendiente.

## 9. Demo de aceptación

1. **Imprescindible:** crear la base con los scripts, iniciar la aplicación y acceder como administrador. Mostrar un acceso rechazado.
2. **Imprescindible:** crear categoría, producto y un segundo depósito; demostrar modificación y listado.
3. **Imprescindible:** habilitar el producto en ambos depósitos. Definir mínimo 5 en el primero y 0 en el segundo. Rechazar una habilitación duplicada.
4. **Imprescindible:** cargar 10 unidades en el primero y 4 en el segundo mediante ajustes con motivo. Mostrar saldos 10 y 4 y total 14.
5. **Imprescindible:** ajustar −7 en el primero. Mostrar saldo 3, stock bajo y faltante 2.
6. **Imprescindible:** intentar ajustar −4. Mostrar el rechazo y que el saldo continúa en 3, sin registrar un movimiento.
7. **Si entra compensación:** registrar +7 vinculado al ajuste original. Mostrar saldo 10 y conservación del movimiento corregido.
8. **Si entra historial:** demostrar movimientos y filtros. La navegación al original dependerá del alcance opcional.
9. **Imprescindible:** rechazar bajas con existencias; ajustar los saldos vigentes a cero y demostrar bajas permitidas y rechazo de ajustes sobre inactivos.
10. **Si entra auditoría:** comprobar registros de maestros, habilitación y mínimos.
11. **Imprescindible:** cerrar sesión, reiniciar y comprobar persistencia.

El guion final omitirá los bloques que queden pendientes el 08/11.

## 10. Entregables y finalización

- Código fuente y proyecto Maven ejecutable.
- DER, modelo lógico y diagrama de clases.
- Scripts SQL e inicialización/datos de prueba.
- Configuración de ejemplo y README de instalación y ejecución.
- Casos de prueba y resultados de las verificaciones críticas.
- Lista de funciones implementadas y pendientes.
- Guion de demo y etiqueta `v0.1-mvp`.

El MVP estará terminado cuando el núcleo imprescindible esté integrado en `main`, las pruebas críticas pasen y **ambos integrantes puedan ejecutar la demo y explicar las decisiones de diseño**. Las evidencias académicas se completarán según la consigna de POO.
