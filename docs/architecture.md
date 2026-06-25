# Архитектура

## Подход

Проект переведен на `clean-ish` Android-архитектуру с четким разделением ответственности:

- `UI / Presentation`
  Compose-экраны и `ViewModel`.
- `Data`
  Room, DataStore, Retrofit, sync orchestration.
- `System`
  WorkManager и widgets.

Главный принцип: `offline first`.

Это означает:

- пользовательское действие сначала фиксируется локально в Room
- UI сразу обновляется из локальной базы
- background sync позже отправляет изменения на сервер
- после push клиент снова тянет полный `snapshot`

## Источник истины

Единственный локальный источник истины:

- таблица `notes`
- таблица `timers`
- таблица `sync_metadata`

Настройки приложения не смешиваются с основной предметной моделью и живут отдельно в `DataStore Preferences`.

## Sync flow

Текущий поток синхронизации:

1. Пользователь меняет заметку или таймер.
2. Изменение сохраняется в Room и помечается как `pending`.
3. `WorkManager` получает задачу на ближайшую синхронизацию.
4. Worker отправляет локальные pending-операции на сервер.
5. После успешных `PUT/DELETE` клиент вызывает `GET /api/v1/snapshot`.
6. Локальный cache полностью обновляется серверным снимком.

## MVVM

Каждый крупный раздел имеет свой state holder:

- `feature/notes/NotesViewModel`
- `feature/timers/TimersViewModel`
- `feature/settings/SettingsViewModel`

Это упрощает:

- независимую эволюцию экранов
- переиспользование логики
- дальнейший вынос в отдельные Gradle-модули

## Feature-first структура

Текущая реализация уже следует feature-first подходу внутри `app`:

- `feature/notes`
- `feature/timers`
- `feature/settings`
- `navigation`
- `data`
- `widget`

То есть код уже разделен по продуктовым зонам, а не по типам файлов.

## Граница для будущей модульности

Следующий естественный шаг без переписывания логики:

- вынести `data` в `core-data`
- вынести `ui/theme` и `ui/components` в `core-designsystem`
- вынести `feature/notes`, `feature/timers`, `feature/settings` в отдельные `feature-*` модули

Архитектурно проект к этому уже подготовлен.
