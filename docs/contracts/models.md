# Модели данных

## Note

```json
{
  "id": "3dce4d7d-43de-4890-b8f8-0a0f3adce9df",
  "title": "Sprint ideas",
  "content": "# Idea\n\n- add android client",
  "pinned": true,
  "createdAt": 1750435200000,
  "updatedAt": 1750438800000
}
```

Поля:

- `id` — UUID строки, создается клиентом.
- `title` — короткий заголовок.
- `content` — Markdown-текст.
- `pinned` — закрепление вверху списка.
- `createdAt` — Unix epoch в миллисекундах.
- `updatedAt` — Unix epoch в миллисекундах.

## TimerEntry

```json
{
  "id": "884f8b44-f7c4-40e4-bd3f-76658c841e65",
  "name": "Workout",
  "mode": "COUNTDOWN",
  "durationMillis": 5400000,
  "startedAt": 1750438800000,
  "accumulatedMillis": 120000,
  "running": true,
  "createdAt": 1750435200000,
  "updatedAt": 1750438800000
}
```

Поля:

- `mode` — `COUNTDOWN` или `STOPWATCH`.
- `durationMillis` — общая длина для countdown; для stopwatch можно хранить `31536000000`.
- `startedAt` — момент текущего старта, если таймер запущен.
- `accumulatedMillis` — накопленное время до текущего запуска.
- `running` — активен ли таймер прямо сейчас.

## ServerSnapshot

```json
{
  "revision": 42,
  "serverTimeEpochMillis": 1750438800000,
  "notes": [],
  "timers": []
}
```

Поля:

- `revision` — глобальная монотонно растущая ревизия состояния.
- `serverTimeEpochMillis` — текущее серверное время.
- `notes` — полный список заметок.
- `timers` — полный список таймеров.

## Ошибка API

```json
{
  "error": {
    "code": "unauthorized",
    "message": "X-Notes-Api-Key is invalid or missing."
  }
}
```
