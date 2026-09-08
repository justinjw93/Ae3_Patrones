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

Cada fase compila y pasa las pruebas por si sola antes de commitear (Paso 7 de la actividad).

---

## 5. Diseño OO, cohesión, acoplamiento y SOLID

### 5.1 Cómo cambió el acoplamiento

| Clase | Dependencias antes del incremento | Dependencias después |
|---|---|---|
| `ServicioReservas` | `RepositorioReservas`, `RepositorioHorarios`, **`Notificador`** | `RepositorioReservas`, `RepositorioHorarios`, **`PublicadorReservas`**, **`PoliticaCancelacion`** |
| `Reserva` | `ReservaBuilder`, `HorarioTutoria`, enums | sin cambios |
| `App` | 8 clases concretas y el orden de sus llamadas | `GestionTutorias` + el cableado del composition root |

El servicio ganó una dependencia (la política) y perdió otra (el notificador), pero el cambio importante no es el número: es la **naturaleza**. Antes dependía de un mecanismo de mensajería y sabía redactar correos; ahora depende de una abstracción de publicación que no le impone receptores. El acoplamiento pasó de *contenido* a *datos*.

### 5.2 Cómo cambió la cohesión

`Reserva` volvió a tener una sola razón para cambiar: su ciclo de vida. La regla institucional —anticipación exigida, penalidad, quién origina la cancelación— cambia por normativa universitaria, no por diseño de software, y por eso vive en `policy/`.

`ServicioReservas` recuperó su papel de orquestador: los cuatro casos de uso hoy caben en tres o cuatro líneas cada uno, porque ya no redactan mensajes ni evalúan reglas de negocio.

### 5.3 SOLID en el incremento

| Principio | Evidencia concreta |
|---|---|
| **SRP** | `Reserva.cancelar()` ejecuta la transición; `PoliticaCancelacion.evaluar()` emite el veredicto. Dos motivos de cambio, dos clases |
| **OCP** | `ObservadorBitacora` y `ObservadorAgendaDocente` se agregaron **sin tocar** `ServicioReservas`. Lo mismo vale para `PoliticaCancelacionDocente` |
| **LSP** | `AdaptadorVideoconferenciaTest.dosProveedoresDistintosSeConsumenPorLaMismaInterfaz` recorre `List<ProveedorVideoconferencia>` sin mencionar clases concretas y ambas se comportan según el contrato |
| **ISP** | `ProveedorVideoconferencia` declara dos métodos, no el catálogo del SDK. El contrato lo dicta el consumidor |
| **DIP** | Ninguna clase fuera de `video/` importa `ZoomMeetingApi`. La fachada depende del Target, no del Adaptee |

### 5.4 El límite que evita la clase Dios

La actividad advierte contra las clases Dios y contra los patrones decorativos. `GestionTutorias` es la candidata natural a convertirse en una, así que se le puso un límite explícito y verificable:

- **no valida** la reserva → lo hace `ReservaBuilder.validar()`;
- **no decide** si una cancelación procede → lo hace la `PoliticaCancelacion`;
- **no elige** quién se entera → lo hace `PublicadorReservas`;
- **no habla** el dialecto de ningún proveedor → lo hace el `AdaptadorZoom`.

La fachada solo encadena llamadas, y el subsistema sigue siendo accesible por separado: `GestionTutoriasTest` construye el servicio directamente, sin pasar por ella.

---

## 6. UML actualizado

| Diagrama | Archivo | Qué muestra |
|---|---|---|
| **General del incremento** | `docs/uml-incremento1.puml` / `.png` | Las cuatro capas, los seis patrones, con generalización, realización de interfaces, dependencias, asociaciones y multiplicidades. La leyenda distingue qué vino de Ae1, Ae2 y Ae3 |
| Strategy | `docs/strategy-cancelacion.puml` / `.png` | Context, Strategy, tres ConcreteStrategies y la frontera con `Reserva` |
| Observer | `docs/observer-reservas.puml` / `.png` | Subject, Observer, tres ConcreteObservers y el puente hacia el Factory Method de Ae2 |
| Adapter | `docs/adapter-videoconferencia.puml` / `.png` | Target, dos Adapters y dos Adaptees marcados como código de terceros |
| Facade | `docs/facade-tutorias.puml` / `.png` | La fachada y el subsistema que coordina |
| Estado inicial | `docs/modelo-clases.puml` / `.png` | El modelo **antes** del incremento, para comparar |

Los diagramas se escriben en PlantUML y se previsualizan y exportan con el plugin *PlantUML integration* de IntelliJ IDEA.

**Coherencia UML–Java.** Cada clase e interfaz del diagrama existe en el código con ese nombre exacto y en ese paquete, y cada relación del diagrama corresponde a un campo, un parámetro de constructor o un `implements` real. Los estereotipos (`<<Strategy>>`, `<<Adaptee>>`, `<<ConcreteObserver>>`) coinciden con los roles GoF documentados en el javadoc de cada clase.

---

## 7. Evidencia de código

### 7.1 Strategy — el Context consulta antes de actuar

```java
// ServicioReservas.java
public ResultadoCancelacion cancelarReserva(String idReserva,
                                            PoliticaCancelacion politica,
                                            LocalDateTime momentoSolicitud) {
    Reserva reserva = obtenerReserva(idReserva);

    ResultadoCancelacion resultado = politica.evaluar(reserva, momentoSolicitud);
    if (!resultado.permitida()) {
        throw new IllegalStateException("La reserva " + reserva.getId()
                + " no puede cancelarse. " + resultado.motivo());
    }

    reserva.cancelar();                                     // la transicion sigue en el dominio
    publicador.publicar(new EventoReserva(TipoEventoReserva.CANCELADA, reserva,
            momentoSolicitud, politica.nombre() + ": " + resultado.motivo()));
    return resultado;
}
```

El servicio no contiene ni una regla de anticipación ni de penalidad: solo sabe que hay una política a la que consultar. `momentoSolicitud` se recibe como parámetro —y no se lee de `now()`— para que las reglas sean reproducibles en pruebas.

### 7.2 Observer — el hecho, no la orden

```java
// ServicioReservas.java
public void confirmarReserva(String idReserva) {
    Reserva reserva = obtenerReserva(idReserva);
    reserva.confirmar();
    publicador.publicar(EventoReserva.de(TipoEventoReserva.CONFIRMADA, reserva,
            "Confirmada para el " + reserva.getHorario().inicio()));
}
```

Comparado con la línea base, desaparecieron el destinatario, el asunto y el cuerpo del mensaje. El servicio anuncia *"la reserva se confirmó"*, no *"envía un correo"*.

El `PublicadorReservas` aísla el fallo de cada receptor, porque el hecho ya ocurrió:

```java
// PublicadorReservas.java
for (ObservadorReserva observador : List.copyOf(observadores)) {
    try {
        observador.alOcurrir(evento);
    } catch (RuntimeException fallo) {
        fallos.add(observador.nombre() + " fallo ante " + evento.tipo() + ": " + fallo.getMessage());
    }
}
```

### 7.3 El punto donde se encuentran Ae2 y Ae3

```java
// ObservadorNotificacion.java
public class ObservadorNotificacion implements ObservadorReserva {

    private final CreadorNotificador creadorNotificador;   // Factory Method de Ae2
    ...
    case CONFIRMADA -> creadorNotificador.enviarNotificacion(
            evento.correoEstudiante(), "Tutoria confirmada",
            "Tu reserva " + evento.reserva().getId() + " fue confirmada.");
}
```

Este observador no escribe `new NotificadorCorreo()`. Le pide el canal al Creator del incremento anterior, y por eso el Factory Method **se mantiene**: perdió un consumidor (el servicio) y ganó otro.

### 7.4 Adapter — toda la incompatibilidad, en un solo archivo

```java
// AdaptadorZoom.java
Map<String, Object> parametros = new LinkedHashMap<>();
parametros.put("topic", titulo);
parametros.put("start_time", inicio.atZone(zona).toInstant().toEpochMilli());  // fecha local -> epoch UTC
parametros.put("duration", duracionMinutos * SEGUNDOS_POR_MINUTO);            // minutos -> segundos

String respuesta = api.scheduleMeeting(parametros);        // "meeting_id=...;join_url=...;passcode=..."

Map<String, String> campos = descomponer(respuesta);
return new SalaVirtual(campos.getOrDefault("join_url", ""),
                       campos.getOrDefault("passcode", ""), nombre());
```

Se usa **object adapter** (composición) y no class adapter (herencia) por dos razones: el SDK es código de terceros que no conviene extender, y la composición permite sustituirlo por un doble en las pruebas.

### 7.5 Facade — la intención, no el procedimiento

```java
// GestionTutorias.java
public Reserva crearTutoriaVirtual(Estudiante estudiante, String idHorario, String motivo) {
    HorarioTutoria horario = obtenerHorario(idHorario);

    SalaVirtual sala = proveedorVideoconferencia.crearSala(
            tituloDe(horario, motivo), horario.inicio(), horario.duracionEnMinutos());

    ReservaBuilder builder = new ReservaBuilder()
            .conEstudiante(estudiante)
            .virtualCon(sala.url())
            .conMotivo(motivo);

    if (sala.exigeCodigo()) {
        builder.conObservaciones("Codigo de acceso " + sala.codigoAcceso() + " (" + sala.proveedor() + ")");
    }

    return servicioReservas.crearReserva(builder, idHorario);
}
```

Del lado del cliente, esos seis pasos son una línea:

```java
Reserva tutoria = gestionTutorias.crearTutoriaVirtual(estudiante, "H002", "Consulta sobre normalizacion");
```

---

## 8. Verificación

### 8.1 Compilación y pruebas

```text
$ mvn clean test

[INFO] Tests run:  9 -- edu.uees.tutorias.builder.ReservaBuilderTest
[INFO] Tests run:  7 -- edu.uees.tutorias.event.PublicadorReservasTest
[INFO] Tests run:  7 -- edu.uees.tutorias.facade.GestionTutoriasTest
[INFO] Tests run:  2 -- edu.uees.tutorias.factory.CreadorNotificadorSmsTest
[INFO] Tests run:  3 -- edu.uees.tutorias.factory.CreadorNotificadorTest
[INFO] Tests run:  8 -- edu.uees.tutorias.policy.PoliticaCancelacionTest
[INFO] Tests run: 11 -- edu.uees.tutorias.service.ServicioReservasTest
[INFO] Tests run:  5 -- edu.uees.tutorias.video.AdaptadorVideoconferenciaTest
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

De 20 pruebas en la línea base a **52** al cerrar el incremento. 45 clases de producción y 8 de prueba.

### 8.2 Cómo se verificó cada patrón

| Patrón | Prueba que lo demuestra | Qué comprueba |
|---|---|---|
| **Strategy** | `elMismoEscenarioProduceVeredictosDistintosSegunLaEstrategia` | El mismo escenario tardío produce tres veredictos distintos; lo único que cambia es la estrategia |
| **Strategy** | `cancelarComoDocenteNoPenalizaAunqueSeaTarde` | Cambiar la estrategia en la llamada cambia el resultado sin tocar `ServicioReservas` ni `Reserva` |
| **Observer** | `cadaCasoDeUsoPublicaUnEventoATodosLosObservadores` | Un hecho por caso de uso, idéntico para todos los registrados |
| **Observer** | `elFalloDeUnObservadorNoImpideQueLosDemasReaccionen` | Un canal caído no impide que la bitácora registre |
| **Observer + Factory** | `elObservadorDeNotificacionPideElCanalAlFactoryMethod` | El observador no instancia canales |
| **Adapter** | `elAdaptadorTraduceMinutosASegundosYFechaLocalAEpochUtc` | La traducción real que llega al SDK: 60 min → 3600 s, `LocalDateTime` → epoch millis |
| **Adapter** | `dosProveedoresDistintosSeConsumenPorLaMismaInterfaz` | Dos APIs incompatibles, un solo código cliente |
| **Facade** | `unaSolaLlamadaCreaLaTutoriaVirtualCompleta` | Reserva guardada, horario ocupado y enlace emitido en una llamada |
| **Facade** | `cambiarDeProveedorNoCambiaLaOperacionDeAltoNivel` | Sustituir Zoom por Meet no cambia el cliente |
| **Facade** | `laTutoriaPresencialNoPideSalaAlProveedor` | La fachada no llama al proveedor cuando no corresponde |

### 8.3 Ejecución de la aplicación

`mvn compile exec:java -Dexec.mainClass="edu.uees.tutorias.App"` recorre los seis patrones. Extracto:

```text
### Facade: una operacion de alto nivel ###
Proveedor de videoconferencia: Zoom
  Modalidad: VIRTUAL
  Enlace emitido por el proveedor: https://uees.zoom.us/j/2705291540
  Observaciones: Codigo de acceso R0B3K8 (Zoom)

### Observer: un hecho, varios receptores ###
Observadores registrados: 3
Asientos en bitacora: 3
Agenda del docente tras la cancelacion: []

### Strategy: tres reglas de cancelacion ###
- Anticipada    -> Fuera de plazo: la cancelacion exige 24 h de anticipacion y solo faltan 2 h.
- Con penalidad -> Cancelacion tardia: quedan 2 h y se aplica 50 % de penalidad.
- Del docente   -> Sin penalidad para el estudiante: cancelacion administrativa del docente.

### Adapter: dos proveedores, un solo contrato ###
- Zoom: https://uees.zoom.us/j/3033649459 (codigo O3GQRR, Zoom)
- Meet institucional: https://meet.uees.edu.ec/tutoria-13895a46 (Meet institucional)
```

---

## 9. Git/GitHub

**Repositorio:** https://github.com/justinjw93/Ae3_Patrones

El historial evidencia evolución progresiva: el incremento no llegó en un solo commit, y cada fase compila y pasa las pruebas por sí sola.

| Commit | Fase |
|---|---|
| `docs: registrar linea base y problemas de diseno del incremento 1` | Línea base verificada y análisis |
| `feat: aplicar strategy a las politicas de cancelacion` | Patrón 1 |
| `feat: notificar cambios de reserva con observer` | Patrón 2 |
| `feat: integrar proveedor de videoconferencia con adapter` | Patrón 3 |
| `feat: exponer el flujo de tutoria virtual con facade` | Patrón 4 |
| `docs: actualizar UML del incremento 1` | Diagramas |
| `docs: actualizar README y decisiones de diseno del incremento 1` | Documentación final |

Antes de estos, el repositorio conserva el historial completo de Ae1 y Ae2: el incremento se apoya sobre ese trabajo en lugar de reemplazarlo.

---

## 10. Conclusiones

**Los patrones se eligieron desde el problema, no desde la lista.** Los cuatro salieron de leer el código de la línea base: la notificación cableada en `ServicioReservas`, la regla congelada en `Reserva.cancelar()`, el enlace escrito a mano en `App` y el procedimiento de seis pasos que el cliente debía ordenar. Cada uno responde a una de las preguntas de selección de la actividad, y ninguno se agregó para completar un cupo.

**Los patrones se componen, no compiten.** El resultado más interesante del incremento es que el Factory Method de Ae2 no se retiró: cambió de consumidor. Antes lo usaba `App`, ahora lo usa `ObservadorNotificacion`. Del mismo modo, el Adapter le da sentido al Facade —sin proveedor externo, la fachada sería una envoltura fina— y la Strategy le da a la fachada algo real que elegir en `cancelarPorDocente`. Un patrón aislado se justifica peor que uno que encaja con los demás.

**El diseño previo fue lo que abarató los cambios.** Ninguno de los cuatro obligó a tocar los repositorios ni las reglas de transición de estado. El Observer encontró su lugar porque el servicio ya recibía colaboraciones por interfaz; el Strategy, porque `Reserva` ya protegía su estado; el Adapter, porque la inversión de dependencias ya era la norma. La inversión hecha en Ae1 se cobró aquí.

**Lo que costó.** Cuatro paquetes nuevos y 19 clases más. El costo real no es el número de archivos sino la indirección: leyendo `cancelarReserva` ya no se ve el correo salir, y hay que saber dónde se registran los observadores. Se compensó con dos decisiones: el composition root de `App` muestra explícitamente qué se registra y qué política rige, y cada clase documenta en su javadoc qué problema resuelve y qué rol GoF cumple.

**Lo que aprendí.** Que la pregunta útil no es "¿qué patrón aplico?" sino "¿qué cambia por su propia cuenta en este sistema?". Los cuatro puntos de variación —el reglamento de cancelación, la lista de interesados, el proveedor de video y el orden de los pasos— existían antes de conocer los patrones; lo que los patrones aportaron fue un lugar donde ponerlos.

---

## 11. Declaración de uso de inteligencia artificial

Para esta actividad utilicé herramientas de inteligencia artificial. Me apoyé en el modelo Claude para agilizar el análisis de los problemas de diseño de la línea base, la generación preliminar del código en Java, el diseño de los diagramas UML en PlantUML y la redacción de la documentación técnica.

Revisé, probé y adapté todo el contenido generado: el código fue inspeccionado y verificado mediante compilación y ejecución de pruebas con Maven y JDK 21 (52 pruebas, `BUILD SUCCESS`), y puedo explicar y justificar cada decisión de diseño presentada en este informe, incluyendo por qué se mantuvieron los patrones de Ae2, por qué se eligió cada patrón nuevo desde un problema concreto del código, y qué costo asume el sistema por cada uno.
