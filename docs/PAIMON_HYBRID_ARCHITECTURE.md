# Android Guardian: arquitectura Paimon híbrida

**Fase:** AG-PAIMON-001  
**TIMER total estimado:** 1–2 días efectivos para esta primera base; integración de chat remoto, memoria útil y avatar animado son fases posteriores.  
**Rama autorizada:** `android-guardian/mvp-001`  
**Restricción de publicación:** no modificar `main`, no fusionar PR sin autorización expresa.

## Objetivo

Transformar Android Guardian en una compañera cálida con personalidad de Paimon: ayudar a ordenar el día, recordar tareas, conversar cuando haga falta y ejecutar acciones locales simples con límites explícitos. El avatar anime es parte de la experiencia futura, pero no debe retrasar la base funcional ni convertirse en un recurso pesado.

## Principio central: híbrido, no dependiente del modelo

La app no debe necesitar IA para hacer lo básico:

1. **Capa local determinista:** recordatorios, rutinas, estado de batería, ayuda, clima solicitado por la persona y comandos conocidos. Las órdenes se clasifican con código Kotlin local.
2. **Capa de conversación opcional:** una interfaz `AssistantChatProvider` permitirá conectar un proveedor remoto en una fase posterior. Debe recibir una petición acotada y devolver texto; no obtiene acceso directo a servicios Android.
3. **Capa de acciones autorizadas:** solo recibe acciones tipadas de una lista permitida. La IA nunca debe ejecutar Kotlin arbitrario, shell, URLs arbitrarias ni acciones del teléfono directamente.
4. **Memoria local explícita:** almacenar preferencias y tareas que la persona elija recordar. No guardar audio bruto ni enviar el historial entero por defecto.
5. **Presentación:** UI Compose accesible con personaje, respuesta visible, control de voz, indicador de conexión/ocupación y acciones sugeridas. El avatar se carga como recurso liviano; animaciones deben poder desactivarse.

## Reglas para no trabarse ni divagar

- Ninguna llamada de red, escaneo de archivos, inferencia de modelo o lectura grande de almacenamiento en el hilo principal.
- Usar coroutines con cancelación y timeouts; para trabajo diferible y persistente, WorkManager con restricciones apropiadas. Reservar AlarmManager para alarmas y recordatorios que lo justifiquen.
- La UI debe poder cancelar una petición y seguir usando acciones locales aunque el proveedor remoto falle.
- Limitar cada respuesta remota a una tarea. En la futura capa de herramientas: máximo de tres llamadas por turno, lista cerrada de herramientas y prohibición de reintentos recursivos. Al alcanzar el límite, explicar qué quedó pendiente.
- Aplicar timeout de red, límites de entrada/salida y manejo visible de errores. No reintentar automáticamente operaciones que puedan duplicar efectos.
- El modo sin conexión debe informar qué capacidades siguen disponibles, sin fingir que la IA respondió.
- No mostrar “hecho” hasta que la acción local confirme éxito. Las acciones ambiguas deben pedir aclaración en vez de adivinar.

## Límites de seguridad y permisos

- Conservar los permisos mínimos que ya existen. No añadir Accesibilidad, lectura de notificaciones, contactos, SMS, llamadas ni micrófono de fondo para esta fase.
- El reconocimiento de voz actual usa el reconocedor instalado por Android y depende de su disponibilidad/políticas. No activar una palabra de vigilancia permanente en esta fase.
- Clima y ubicación continúan bajo acción explícita y permiso de ubicación; no conservar coordenadas en la memoria del asistente.
- Borrar archivos, enviar mensajes, realizar llamadas, cambiar ajustes del sistema o controlar apps externas requerirá permisos apropiados y confirmación visible. No se permitirá a la IA realizar estas acciones por su cuenta.
- No prometer limpieza de caché privada de otras apps, aumento mágico de RAM ni ejecución persistente garantizada frente a las restricciones del fabricante.
- No incrustar una clave secreta de proveedor dentro del APK: una clave distribuida en un cliente puede extraerse. Para uso propio, una opción BYOK debe ser explícita, explicar el riesgo y permitir borrarla; una integración para distribución necesita un backend seguro con límites de gasto y protección de abuso.
- Mostrar cuándo un mensaje se enviará a un proveedor remoto. No enviar ubicación, recordatorios privados ni memoria personal salvo que sea necesario y consentido.

## Arquitectura propuesta

```
Compose UI / avatar Paimon
          |
AssistantController (estado, cancelación, timeout)
          |
          +-- LocalCommandRouter (determinista, probado)
          |
          +-- AssistantChatProvider (opcional, remoto)
          |
ActionPolicy (lista permitida + confirmación)
          |
Android capability adapters
(Reminders / Weather / Battery / Storage / future routines)
          |
Local persistent storage (preferencias y tareas elegidas)
```

El parser actual es un paso inicial hacia `LocalCommandRouter`: produce un `AssistantDecision` tipado y no ejecuta acciones. `MainActivity` aún mantiene funciones del sistema; una fase posterior debe extraerlas a adaptadores, sin reescribir todo en un solo cambio.

## Fases sugeridas

### AG-PAIMON-001: comandos locales fiables
**Estimación:** 1–2 días efectivos.
- Router determinista separado de UI.
- Unknown/empty input no ejecuta acciones.
- Pruebas unitarias de frases en español, acentos y prioridad de recordatorios.
- Documentación del contrato de seguridad y errores.
- Sin API keys ni permisos nuevos.

### AG-PAIMON-002: experiencia de compañera
**Estimación:** 2–3 días efectivos.
- Chat local simple con estados idle/loading/error/cancelled.
- Respuestas y sugerencias locales; estructura de personalidad configurable.
- Avatar anime liviano y accesible, con fallback si falla la imagen y sin bloquear el lienzo/pantalla.
- Historial con borrado manual; sin envío remoto todavía.
- Pruebas de recomposición, rotación de pantalla y estados vacíos.

### AG-PAIMON-003: proveedor remoto opcional
**Estimación:** 2–4 días efectivos más configuración y validación.
- Interfaz `AssistantChatProvider`, timeout/cancelación y manejo de cuota/red.
- Consentimiento antes de enviar mensajes a la nube.
- Configuración de proveedor sin secretos incrustados en el APK.
- No se habilitan herramientas del dispositivo desde respuestas del modelo.
- Pruebas con servidor falso: éxito, demora, desconexión, respuestas malformadas y cancelación.

### AG-PAIMON-004: memoria y rutinas seguras
**Estimación:** 2–4 días efectivos.
- Preferencias opt-in y rutinas repetibles; editar, pausar y borrar.
- Resumen local simple basado en hechos registrados, no inferencias médicas.
- Confirmación previa para acciones sensibles.
- Pruebas de persistencia, reinicio, zona horaria y restricciones de batería.

## Criterios de aceptación de esta primera fase

- Las frases conocidas se clasifican en la intención esperada.
- Se toleran acentos y variaciones comunes en español.
- Las frases explícitas de recordatorio conservan su contenido, sin programar una alarma desde el parser.
- Entrada vacía o desconocida devuelve `UNKNOWN`; no ejecuta acciones ni llama a red.
- Se compila APK debug y pasan las pruebas unitarias por CI.
- Se informa el SHA del commit y el enlace real del workflow. La compilación no sustituye las pruebas en el Xiaomi físico.

## Riesgos que siguen abiertos

- La exactitud de órdenes por voz depende del reconocedor del dispositivo.
- Los permisos y las políticas de batería del fabricante pueden retrasar avisos.
- La conexión remota añade latencia, cuota, privacidad y dependencia de red.
- Las pruebas automatizadas no certifican UX, pronunciación, consumo real ni el comportamiento en segundo plano del Xiaomi 24116RACCG.
