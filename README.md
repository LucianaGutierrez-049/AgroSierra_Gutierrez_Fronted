# AgroCampo Gutierrez

Aplicación Kotlin Multiplatform y Compose del caso AgroSierra. Abrir esta carpeta completa en Android Studio.
Backend independiente: `C:\Users\lucia\Documents\AgroAPI_Gutierrez` (abrir allí en IntelliJ IDEA).

## Ejecutar

1. Ejecutar `./mvnw.cmd spring-boot:run` en el backend con Java 21.
2. Iniciar un emulador Android API 34 o superior.
3. Ejecutar la configuración androidApp en Android Studio.

El cliente apunta a `http://10.0.2.2:8081/api/v1/`. Esta dirección alcanza el equipo anfitrión desde el emulador Android.
Para un teléfono físico cambiar ApiConfig por la IP LAN del equipo y permitir el puerto 8081 en su red local.
HTTP sin TLS se habilita para la demostración local; antes de publicar usar HTTPS y deshabilitar tráfico en claro.

Usuarios semilla: tecnico1, jefe1 y admin; contraseña Agro2026*.

## Arquitectura

shared/commonMain: domain/model, domain/repository, domain/usecase; data/remote/dto, data/local,
data/repository; presentation y di. androidApp: Android Keystore, driver SQLite y conectividad.
Ktor es singleton inyectado por Koin. El dominio no incluye anotaciones de serialización.
La interfaz observa exclusivamente consultas SQLDelight. No se incrustan listados de negocio en la app.

## Sesión y privilegios

El JWT y la sesión se cifran mediante AES/GCM con una clave no exportable de Android Keystore.
La aplicación restaura sesiones vigentes. Un 401 borra el almacenamiento seguro; un 403 conserva la sesión.
El backend verifica firma, expiración y privilegios. Los botones aplican los privilegios recibidos del servicio.
La información local se separa por usuario; cerrar sesión conserva los pendientes del propietario original.

## Offline y sincronización

Una labor se guarda primero en SQLite con ID negativo, marca pendiente y clienteId idempotente.
PUT y DELETE también persisten en la cola local. La sincronización serializada envía la cola antes de descargar.
Las descargas recorren todas las páginas y conservan las modificaciones pendientes si el servidor las rechaza.
Los conflictos se muestran en cada fila y se corrigen con el formulario. Los pendientes no se descartan al salir.
Hay sincronización manual, al recuperar Internet y al volver a la aplicación.

## Verificación

`./gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest`
commonTest contiene pruebas de DTO, traducción de errores, registro offline, orden de sincronización,
sesión 401/403, DELETE 204 y aislamiento entre usuarios; AndroidHostTest aporta SQLite JDBC en memoria.

## Demostración

Ingresar con tecnico1, sincronizar, abrir un lote y activar modo avión. Registrar una labor y comprobar
la etiqueta pendiente. Recuperar conexión y sincronizar: la fila definitiva reemplaza la provisional.
Con jefe1 probar el cierre de un lote con cosecha y el rechazo de otro sin cosecha.
Probar el rechazo 403 directamente en el API con un token técnico para demostrar la seguridad del servidor.

## Pendientes de entrega

Validar ejecución en emulador y capturas reales antes de generar el PDF de evidencias.
No se fabrican fechas, ramas, PR ni historial de desarrollo de sesiones anteriores.
La rama de cambio SC se crea al recibir la asignación del docente.

## Repositorio

https://github.com/LucianaGutierrez-049/AgroSierra_Gutierrez_Fronted.git
