# Control LG

Control remoto infrarrojo para televisores LG, usando el emisor IR del celular.
Funciona sin wifi, sin internet y sin Bluetooth.

- Kotlin + Jetpack Compose (Material 3, tema oscuro). minSdk 21, targetSdk 35.
- Protocolo NEC a 38 kHz (`Nec.kt`, con test unitario).
- Códigos de fábrica en `LgCodes.kt`; también se editan dentro de la app.

## Qué trae

- Control completo: encendido, entrada, cruceta con OK, volumen y canal (se repiten al mantener
  presionado), Atrás/Home/Salir, apps, ajustes de la tele y teclado numérico desplegable.
- Perfiles de códigos: «LG estándar (2008 en adelante)» y «LG alternativo» (Encender y Apagar por separado).
- Asistente «Probar mi tele» al primer uso.
- Editor de códigos con botón «Probar» y «Restaurar valores de fábrica».
- Botones de apps ocultables (Netflix, Prime Video, Disney+, YouTube).
- Importar / Exportar la configuración como archivo JSON.

Disney+ y YouTube no traen código de fábrica (cambia según el modelo): se agregan en el editor.

## Compilar

Con Gradle 8.9 y JDK 17:

    gradle assembleDebug

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Sin Android Studio: al subir el proyecto a GitHub, el workflow `.github/workflows/build.yml`
compila el APK y lo deja como artifact descargable (`control-lg-debug`).
