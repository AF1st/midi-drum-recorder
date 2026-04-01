# 🥁 MIDI Drum Recorder

Android приложение для записи ударных паттернов и экспорта в MIDI.

## Возможности

- 🥁 Две кнопки: **Kick** и **Snare**
- ⏺️ Запись ударов с точными временными метками
- 📁 Экспорт в стандартный `.mid` файл (MIDI Type 1)
- 🎵 Поддержка General MIDI drum channel (канал 10)
- 📊 Визуализация записанного паттерна
- 🔊 Тактильная обратная связь при нажатии

## Технологии

- **Kotlin** — основной язык
- **Jetpack Compose** — современный декларативный UI
- **Coroutines** — асинхронные операции
- **Material Design 3** — дизайн-система
- **MediaStore API** — сохранение файлов в Downloads
- Нативная генерация MIDI без внешних библиотек

## Сборка

```bash
# Клонировать репозиторий
git clone https://github.com/AF1st/midi-drum-recorder.git
cd midi-drum-recorder

# Сборка debug APK
./gradlew assembleDebug
```

APK будет в: `app/build/outputs/apk/debug/app-debug.apk`

## Использование

1. Нажми **REC** для начала записи
2. Стучи по кнопкам **KICK** и **SNARE** в нужном ритме
3. Нажми **STOP** для остановки
4. Нажми **Export MIDI** для сохранения файла
5. Файл появится в папке `Downloads/MidiDrumRecorder/`

## MIDI маппинг

| Инструмент | MIDI Note | GM название |
|------------|-----------|-------------|
| Kick       | 36        | Bass Drum 1 |
| Snare      | 38        | Acoustic Snare |

## Требования

- Android 8.0+ (API 26+)
- Разрешение на запись файлов (запрашивается автоматически)
