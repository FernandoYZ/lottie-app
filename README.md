# Lottie

App Android para registrar el ciclo menstrual. Todo se guarda en el teléfono: sin cuentas, sin servidor y sin conexión a internet.

## Qué hace

- Registro de menstruaciones desde el calendario.
- Estimación de la próxima menstruación, la ventana fértil y las fases del ciclo para los próximos 12 meses.
- Registro diario: flujo, estado de ánimo, síntomas, flujo vaginal, digestión, sexo, pruebas de embarazo y ovulación, agua, peso, temperatura basal y notas.
- Historial con la duración media del ciclo y de la menstruación.
- Recordatorios locales.
- Bloqueo con la huella o el PIN del teléfono, y opción para borrar todos los datos.

Las estimaciones son orientativas: no son un diagnóstico ni un método anticonceptivo.

## Requisitos

- Android 8.0 (API 26) o superior.
- Android Studio con JDK 17 o superior.

## Compilar

```bash
./gradlew assembleDebug        # genera el APK de depuración
./gradlew installDebug         # lo instala en un teléfono conectado
./gradlew testDebugUnitTest    # pruebas unitarias
```

## Tecnologías

Kotlin, Jetpack Compose (Material 3), Room, DataStore, WorkManager y androidx.biometric.
