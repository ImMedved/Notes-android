# Базовая структура проекта

## Текущая структура

```text
app/
  src/main/java/com/notes/notesandroid/
    data/
      local/db/
      model/
      preferences/
      remote/
      NotesRepository.kt
      SyncScheduler.kt
    feature/
      notes/
      timers/
      settings/
    navigation/
      PaperNotesApp.kt
    sync/
      SyncWorker.kt
    ui/
      components/
      theme/
    util/
    widget/
    MainActivity.kt
    NotesApplication.kt
```

## Что где живет

- `data/model`
  Предметные модели, режимы темы, sync state, timer modes.
- `data/local/db`
  Room entities, DAO, database, converters.
- `data/preferences`
  DataStore и настройки приложения.
- `data/remote`
  Retrofit API и factory.
- `feature/notes`
  Экран списка заметок, note editor, notes VM.
- `feature/timers`
  Экран таймеров, timer editor, timers VM.
- `feature/settings`
  Экран sync/theme настроек.
- `navigation`
  `NavHost`, top-level destinations, screen wiring.
- `ui/components`
  Переиспользуемые expressive card/hero/banner блоки.
- `ui/theme`
  Цветовые схемы, типографика и темная/светлая тема.
- `widget`
  Домашние виджеты заметок и таймеров.

## Рекомендуемая следующая эволюция

Если проект станет больше, рекомендуемая Gradle-модульность:

```text
app
core-model
core-data
core-designsystem
feature-notes
feature-timers
feature-settings
```

Текущий код уже разложен так, чтобы такой переход был механическим, а не архитектурным.
