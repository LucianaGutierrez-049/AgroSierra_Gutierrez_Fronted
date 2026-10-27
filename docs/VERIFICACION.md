# Verificación de AgroCampo

27 de octubre de 2026. Comando ejecutado:
`./gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest --console=plain`.
Resultado: BUILD SUCCESSFUL; 9 pruebas, 0 fallos, 0 errores.
Siete pruebas nuevas de AgroOfflineTest y dos pruebas iniciales de la plantilla.
Las pruebas nuevas verifican DTO, errores, registro offline, sincronización sin duplicados,
sesión 401/403, eliminación 204 y separación de datos por usuario.
APK generado en androidApp/build/outputs/apk/debug/androidApp-debug.apk (excluido de Git).
La comprobación visual y las capturas de ejecución se documentan por separado al completarse.
