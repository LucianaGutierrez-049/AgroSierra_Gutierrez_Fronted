# Android verificado con AgroAPI y Oracle

Se declaró y solicitó ACCESS_LOCAL_NETWORK porque la app apunta a Android 17 API 37.
La documentación oficial está en https://developer.android.com/privacy-and-security/local-network-permission.

APK compilado y 9 pruebas sin fallos. El usuario técnico inicia sesión contra AgroAPI en el puerto 8081.
Se sincronizó el padrón, se abrió Las Palmeras, se activó modo avión y se deshabilitó WiFi.
Se registró una labor, se comprobó su marca pendiente y se restauró la conexión.
La consulta al servidor confirmó una única fila definitiva con id 41.
Las capturas reales y los resultados se encuentran en docs/evidencias.
El texto de la observación de evidencia es "Evidencia offline Gutierrez 2026".

No se alteraron datos de otras aplicaciones ni se borraron datos locales de AgroCampo.
