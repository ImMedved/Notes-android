# Итоговый технологический стек

## Обязательный стек

- `Jetpack Compose`
  Основной UI toolkit приложения.
- `Material 3`
  Используется как база для expressive UI-языка: акцентная типографика, контрастные поверхности, большие hero-блоки и motion-first композиции.
- `Navigation Compose`
  Навигация между разделами `Notes`, `Timers`, `Sync` и editor-экранами.
- `Compose Animation`
  Применяется для `AnimatedContent`, `AnimatedVisibility`, плавных смен режимов редактора и статусов синхронизации.
- `Coil 3`
  Используется для загрузки и рендера декоративных Compose-illustration assets в hero-секциях.

## Выбранный дополнительный стек

- `MVVM`
  Отдельные `ViewModel` для `notes`, `timers`, `settings`.
- `Room`
  Локальная база данных и источник истины для offline-first сценария.
- `Retrofit + OkHttp`
  HTTP-клиент для синхронизации с backend snapshot API.
- `Coroutines`
  Асинхронные операции, sync, редакторы, widgets.
- `Flow`
  Реактивные потоки из Room/DataStore в UI.
- `WorkManager`
  Периодическая и моментальная background-синхронизация.
- `DataStore Preferences`
  Хранение настроек синхронизации и режима темы.

## Пока не внедрено

- `Hilt`
  Архитектурно проект к нему готов, но текущая версия оставляет ручной composition root через `NotesApplication`.
- `Glance`
  Виджеты пока остаются на `AppWidgetProvider` + `RemoteViews`, чтобы не ломать уже рабочую механику обновления таймера/заметки.

## Версии и ориентиры

- `Navigation Compose`: Android Developers указывает `2.9.8`
- `Coil Compose`: официальная документация Coil показывает `io.coil-kt.coil3:coil-compose:3.5.0`
- `Material 3 in Compose`: Android Developers рекомендует стандартную зависимость `androidx.compose.material3:material3`

См. также:

- [Архитектура](./architecture.md)
- [Структура проекта](./project-structure.md)
- [Android-клиент](./android-client.md)
