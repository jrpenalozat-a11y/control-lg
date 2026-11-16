# Control LG

Control remoto infrarrojo para televisores LG, usando el emisor IR del celular.
Funciona sin wifi, sin internet y sin Bluetooth.

- Kotlin + Jetpack Compose (Material 3, tema oscuro). minSdk 21, targetSdk 35.
- Protocolo NEC a 38 kHz (`Nec.kt`, con test unitario).
- Códigos de fábrica en `LgCodes.kt`; también se editan dentro de la app.

## Descargar

Enlace directo a la última versión (se puede mandar a cualquiera):

https://github.com/jrpenalozat-a11y/control-lg/releases/latest/download/ControlLG.apk

Dentro de la app, el botón **Compartir** (arriba, al lado del engranaje) envía el APK por
WhatsApp, Bluetooth o Quick Share, sin internet.

## Qué trae

- **Varias teles en un solo control**: arriba se elige cuál manejar («LG cocina», «TV Pipi pieza»…).
  Cada tele guarda su perfil, sus códigos y sus botones de apps. Se agregan, renombran y borran en
  Configuración → Mis teles.
- Perfiles: LG estándar, LG alternativo, **Roku TV** (TCL, Hisense, AOC Roku…) y «Otra marca» (vacío).
- Botones de Netflix, Prime Video, Disney+, YouTube, Max y Spotify (ocultables por tele).
- **Buscador de código** en el Editor: prueba uno por uno los 256 códigos de una marca hasta que la
  tele responde, y lo guarda en ese botón. Sirve para las apps sin código de fábrica y para teles
  de otra marca (la AOC 32S5295 de la pieza parte con el perfil «Otra marca», sin códigos).

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
