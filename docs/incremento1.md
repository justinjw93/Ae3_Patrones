# Ae3 — Incremento 1 del proyecto

Integración de diseño orientado a objetos y patrones en el Sistema de gestión de tutorías
Diseño de Software (UCOM0310) · Semana 4 · PEL 4 - 2026

**Autor:** Justin Arreaga
**Repositorio:** https://github.com/justinjw93/Ae3_Patrones

**Historia del proyecto**

| Etapa | Entrega | Repositorio |
|---|---|---|
| Ae1 (Semana 2) | Análisis de dominio, diseño OO, cohesión/acoplamiento y SOLID | https://github.com/justinjw93/diseno_software |
| Ae2 (Semana 3) | Factory Method y Builder sobre ese diseño | https://github.com/justinjw93/Ae2_Patrones |
| **Ae3 (Semana 4)** | **Incremento 1: Strategy, Observer, Adapter y Facade** | **este repositorio** |

---

## 1. Estado inicial del incremento

Lo que existía en el repositorio **antes** de tocar una sola línea de este incremento.

### 1.1 Verificación de la línea base

```text
$ mvn clean test

[INFO] Tests run:  9 -- edu.uees.tutorias.builder.ReservaBuilderTest
[INFO] Tests run:  2 -- edu.uees.tutorias.factory.CreadorNotificadorSmsTest
[INFO] Tests run:  3 -- edu.uees.tutorias.factory.CreadorNotificadorTest
[INFO] Tests run:  6 -- edu.uees.tutorias.service.ServicioReservasTest
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

26 clases de producción y 20 pruebas verdes. El incremento **parte de un proyecto que compila**, no de cero.

### 1.2 Qué se recupera de Ae1

| Elemento de Ae1 | Dónde vive hoy |
|---|---|
| Análisis del dominio (estudiantes, docentes, horarios, reservas, reglas) | `docs/analisis.md` |
| Responsabilidades de las clases | `domain/` — `Reserva`, `HorarioTutoria`, `Usuario`, `Docente`, `Estudiante`, `Asignatura` |
| Abstracción y encapsulación | Estado privado; ningún `setEstado(...)` público en `Reserva` ni `HorarioTutoria` |
| Herencia vs. composición | Herencia solo en `Usuario → Docente/Estudiante`; el resto es composición |
| Cohesión y acoplamiento | `ServicioReservas` orquesta y delega reglas al dominio |
| Decisiones SOLID | DIP mediante `RepositorioReservas`, `RepositorioHorarios`, `Notificador` |
| Diagrama UML de clases | `docs/modelo-clases.puml` |
| Código base Java/Maven | `pom.xml`, JDK 21, JUnit 5 |

### 1.3 Qué se recupera de Ae2

Los dos patrones de la semana anterior siguen en el código. La actividad permite retirarlos si ya no están justificados; el análisis de este incremento concluye que **ambos se mantienen**, y que uno de ellos incluso gana un consumidor nuevo.

| Patrón | Problema que resuelve en mi proyecto | ¿Se mantiene? | Justificación |
|---|---|---|---|
| **Factory Method** | `App` escribía `new NotificadorConsola()` y quedaba amarrado a una clase concreta; además, la configuración propia de cada canal (remitente institucional, prefijo de auditoría, número de origen del SMS) se filtraba hacia el cliente. La jerarquía `CreadorNotificador` concentra esa decisión y esa configuración. | **Sí** | El punto de variación no desapareció: el sistema sigue teniendo cuatro canales y la expectativa de que aparezcan más. En este incremento el patrón **gana un consumidor**: el `ObservadorNotificacion` del Observer no instancia notificadores, le pide uno a un `CreadorNotificador`. Retirarlo obligaría a devolver los `new` concretos al código nuevo. |
| **Builder** | `Reserva` pasó de 4 a 9 datos entre obligatorios y opcionales: constructores telescópicos, tres `String` consecutivos intercambiables sin error de compilación, `null` explícitos y ningún punto único de validación. `ReservaBuilder` es hoy la única puerta de construcción y concentra `validar()`. | **Sí** | El número de datos no bajó y en este incremento **sube**: la tutoría virtual ya no recibe un enlace escrito a mano, sino una `SalaVirtual` producida por el Adapter. El Facade construye la reserva a través del builder, de modo que la validación cruzada (VIRTUAL exige enlace) sigue ocurriendo en un solo lugar. |

---

## 2. Problemas de diseño identificados

Cuatro problemas **reales y trazables** al código de la línea base. Cada uno cita el archivo donde se observa.

### Problema 1 — La notificación está cableada dentro del servicio

**Dónde:** `service/ServicioReservas.java`

Los cuatro casos de uso (`crearReserva`, `confirmarReserva`, `cancelarReserva`, `reprogramarReserva`) arman a mano el destinatario, el asunto y el cuerpo del mensaje, y llaman directamente a `notificador.notificar(...)`:

```java
public void confirmarReserva(String idReserva) {
    Reserva reserva = obtenerReserva(idReserva);
    reserva.confirmar();
    notificador.notificar(                       // <-- reacción cableada
            reserva.getEstudiante().getCorreo(),
            "Tutoria confirmada",
            "Tu reserva " + reserva.getId() + " fue confirmada.");
}
```

**Por qué es un problema.** El servicio conoce *quién* debe enterarse y *cómo* se redacta el aviso, cuando su responsabilidad es orquestar el caso de uso. Cada receptor nuevo —bitácora de auditoría, agenda del docente, panel de coordinación— obliga a **editar los cuatro métodos**. Es una violación directa del Open/Closed Principle: el servicio debería estar cerrado a modificación y hoy no lo está.

**Qué cambia:** la lista de interesados en un cambio de reserva.
**Qué permanece estable:** los casos de uso de `ServicioReservas`.

### Problema 2 — La política de cancelación está congelada en el dominio

**Dónde:** `domain/Reserva.java`

```java
public void cancelar() {
    if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CONFIRMADA) {
        throw new IllegalStateException(...);
    }
    this.estado = EstadoReserva.CANCELADA;
    this.horario.liberar();
}
```

**Por qué es un problema.** La única regla que existe es la transición de estado. El reglamento real de tutorías distingue casos que hoy no se pueden expresar: cancelar con más de 24 horas de anticipación es libre, hacerlo con menos genera una penalidad para el estudiante, y una cancelación originada por el docente no penaliza a nadie. Meter esas reglas dentro de `cancelar()` convertiría un método de dominio en una cadena de `if` sobre fechas y roles, mezclando *transición de estado* (responsabilidad de `Reserva`) con *política institucional* (que cambia por reglamento, no por diseño).

**Qué cambia:** la regla que decide si una cancelación procede y con qué penalidad.
**Qué permanece estable:** la transición `→ CANCELADA` y la liberación del horario.

### Problema 3 — El enlace de la tutoría virtual se escribe a mano

**Dónde:** `App.java` y `builder/ReservaBuilder.java`

```java
.virtualCon("https://meet.uees.edu.ec/tutoria-h003")   // App.java
```

**Por qué es un problema.** `enlaceVirtual` es un `String` que alguien teclea. No hay sala reservada en ningún proveedor, nadie garantiza que el enlace exista, y no hay código de acceso. Al integrar un proveedor real aparece el choque de interfaces: la API de Zoom recibe un `Map` de parámetros, espera el inicio en *epoch millis* y la duración en *segundos*, y devuelve una cadena con varios campos; el sistema, en cambio, razona con `LocalDateTime`, minutos y un objeto de dominio. Si el servicio hablara ese dialecto directamente, cambiar de proveedor obligaría a reescribirlo.

**Qué cambia:** el proveedor de videoconferencia y el formato de su API.
**Qué permanece estable:** el contrato `ProveedorVideoconferencia` que consume el sistema.

### Problema 4 — El cliente conoce demasiados pasos del flujo

**Dónde:** `App.java`

Para crear una tutoría, el cliente hoy: instancia dos repositorios, elige un `CreadorNotificador`, obtiene el `Notificador`, arma el `ServicioReservas`, guarda horarios, construye un `ReservaBuilder` con cinco llamadas encadenadas y recién entonces invoca al servicio. Con el proveedor de videoconferencia del Problema 3, el flujo suma dos pasos más (pedir la sala y trasladar su URL al builder).

**Por qué es un problema.** El cliente queda acoplado al **orden** de los pasos y a la existencia de cada colaborador. Cualquier reordenamiento interno rompe a todos los clientes. Falta una operación de alto nivel que exprese la intención del negocio —"crear una tutoría virtual"— en lugar de su procedimiento.

**Qué cambia:** los pasos internos del flujo de creación.
**Qué permanece estable:** la operación `crearTutoriaVirtual(...)` que ve el cliente.

---

## 3. Patrones seleccionados

Cuatro problemas, cuatro patrones, uno por problema. Ninguno se agrega "por completar la lista": cada uno se elige por la **pregunta de selección** que plantea la actividad.

| Problema | Pregunta de selección | Patrón |
|---|---|---|
| 1. Notificación cableada | ¿Quién debe enterarse cuando cambia una reserva? | **Observer** |
| 2. Política de cancelación congelada | ¿Qué regla cambia de forma independiente? | **Strategy** |
| 3. Enlace escrito a mano | ¿La interfaz externa no coincide con mi contrato? | **Adapter** |
| 4. Cliente con demasiados pasos | ¿El cliente conoce demasiados detalles? | **Facade** |

### 3.1 Plantilla de justificación

| Elemento | Patrón 1 · Strategy | Patrón 2 · Observer |
|---|---|---|
| **Problema real** | La regla de cancelación (anticipación y penalidad) está congelada dentro de `Reserva.cancelar()`. | `ServicioReservas` decide y redacta la notificación en sus cuatro casos de uso. |
| **Contexto** | El reglamento distingue cancelación anticipada, tardía y originada por el docente; el reglamento cambia por normativa, no por diseño. | Ante un cambio de reserva deben reaccionar el canal de mensajería, la bitácora de auditoría y la agenda del docente. |
| **Qué cambia** | El criterio que decide si la cancelación procede y con qué penalidad. | La lista de interesados y su reacción. |
| **Qué permanece estable** | La transición `→ CANCELADA` y la liberación del horario, que siguen en `Reserva`. | Los casos de uso de `ServicioReservas` y el evento que publica. |
| **Clases/interfaces implicadas** | `PoliticaCancelacion` (Strategy), `PoliticaCancelacionEstandar`, `PoliticaCancelacionConPenalidad`, `PoliticaCancelacionDocente` (ConcreteStrategies), `ResultadoCancelacion`, `ServicioReservas` (Context). | `ObservadorReserva` (Observer), `PublicadorReservas` (Subject), `ObservadorNotificacion`, `ObservadorBitacora`, `ObservadorAgendaDocente` (ConcreteObservers), `EventoReserva`. |
| **Principio SOLID relacionado** | **OCP** — se agrega una política sin tocar las existentes. **SRP** — `Reserva` conserva la transición; la política vive aparte. | **OCP** — un receptor nuevo es una clase nueva. **DIP** — el servicio depende de `ObservadorReserva`, no de `Notificador`. |
| **Beneficio esperado** | Reglas de cancelación intercambiables y probables una por una, sin condicionales en el dominio. | El servicio deja de conocer receptores; agregar auditoría no lo modifica. |
| **Costo/compromiso** | Tres clases más y una dependencia adicional en el constructor del servicio. Para una sola regla sería sobreingeniería; se justifica porque existen tres reales. | Indirección: leyendo `cancelarReserva` ya no se ve el correo saliendo. Se compensa con el registro explícito de observadores en el composition root. |
| **Cómo verificaré que funciona** | Pruebas por política (anticipada sin penalidad, tardía con penalidad, docente sin penalidad) y una prueba de que cambiar la estrategia cambia el resultado sin tocar `Reserva`. | Observador espía en pruebas que confirma un evento por caso de uso, y prueba de que registrar un segundo observador no altera al primero. |

| Elemento | Patrón 3 · Adapter | Patrón 4 · Facade |
|---|---|---|
| **Problema real** | La API del proveedor de videoconferencia no coincide con el contrato del sistema (mapa de parámetros, epoch millis, duración en segundos, respuesta en texto plano). | Crear una tutoría virtual exige al cliente conocer y ordenar seis colaboradores. |
| **Contexto** | La universidad puede cambiar de proveedor (Zoom hoy, Meet institucional mañana) sin que cambie el caso de uso. | `App` es hoy un procedimiento de 20 líneas que encadena repositorios, factory, builder y servicio. |
| **Qué cambia** | El proveedor externo y su formato de datos. | El orden y la cantidad de pasos internos del flujo. |
| **Qué permanece estable** | La interfaz `ProveedorVideoconferencia` y el tipo `SalaVirtual`. | La operación de alto nivel `crearTutoriaVirtual(...)`. |
| **Clases/interfaces implicadas** | `ProveedorVideoconferencia` (Target), `ZoomMeetingApi` / `MeetInstitucionalService` (Adaptees), `AdaptadorZoom`, `AdaptadorMeetInstitucional` (Adapters), `SalaVirtual`. | `GestionTutorias` (Facade) sobre `ServicioReservas`, `RepositorioHorarios`, `ProveedorVideoconferencia`, `ReservaBuilder` y `PublicadorReservas`. |
| **Principio SOLID relacionado** | **DIP** — el sistema depende de su propia abstracción, no del SDK. **ISP** — el contrato expone solo lo que el sistema necesita. | **SRP** — la fachada asume la coordinación del flujo; el servicio conserva los casos de uso. **LoD** — el cliente habla con un solo objeto. |
| **Beneficio esperado** | Cambiar de proveedor es cambiar de adaptador; el resto del sistema no se entera. | El cliente expresa intención en lugar de procedimiento; el flujo se reordena sin romperlo. |
| **Costo/compromiso** | Una clase de traducción por proveedor y la pérdida de las funciones exclusivas del SDK que el contrato no expone. | Riesgo de convertirse en clase Dios si absorbe lógica. Se evita: la fachada **solo coordina**, no decide reglas. |
| **Cómo verificaré que funciona** | Prueba de que el adaptador traduce correctamente minutos → segundos y `LocalDateTime` → epoch, y que dos adaptadores distintos satisfacen el mismo contrato. | Prueba de que `crearTutoriaVirtual` deja la reserva guardada, con enlace real y con los observadores notificados, en una sola llamada. |

---

## 4. Fases del incremento

El historial de Git sigue estas fases, un commit por fase.

| Fase | Contenido | Commit |
|---|---|---|
| 1 | Línea base verificada y análisis de problemas | `docs: registrar linea base y problemas de diseno del incremento 1` |
| 2 | Strategy — políticas de cancelación | `feat: aplicar strategy a las politicas de cancelacion` |
| 3 | Observer — eventos de la reserva | `feat: notificar cambios de reserva con observer` |
| 4 | Adapter — proveedor de videoconferencia | `feat: integrar proveedor de videoconferencia con adapter` |
| 5 | Facade — flujo de tutoría virtual | `feat: exponer el flujo de tutoria virtual con facade` |
| 6 | UML del incremento | `docs: actualizar UML del incremento 1` |
| 7 | README y decisiones de diseño | `docs: actualizar README y decisiones de diseno` |

Las secciones 5 a 9 (UML, evidencia de código, verificación final, conclusiones y declaración de IA) se completan al cerrar el incremento.
