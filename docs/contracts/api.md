# HTTP API

Базовый префикс: `/api/v1`

Аутентификация:

- если на сервере задан `NOTES_API_KEY`, клиент обязан передавать заголовок `X-Notes-Api-Key`
- дополнительные заголовки для телеметрии и отладки:
  - `X-Client-Id`
  - `X-Client-Platform`
  - `X-Client-Version`

Все тела и ответы — `application/json`.

## GET /health

Назначение:
Проверка доступности сервера.

Ответ `200`:

```json
{
  "ok": true,
  "service": "notes-server",
  "serverTimeEpochMillis": 1750438800000
}
```

## GET /api/v1/snapshot

Назначение:
Получить полное авторитетное состояние сервера.

Ответ `200`:

```json
{
  "revision": 42,
  "serverTimeEpochMillis": 1750438800000,
  "notes": [
    {
      "id": "3dce4d7d-43de-4890-b8f8-0a0f3adce9df",
      "title": "Sprint ideas",
      "content": "# Idea",
      "pinned": true,
      "createdAt": 1750435200000,
      "updatedAt": 1750438800000
    }
  ],
  "timers": []
}
```

## PUT /api/v1/notes/{id}

Назначение:
Создать или обновить заметку. `id` в path является источником истины.

Тело:

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

Ответ `200`:
Объект заметки в актуальном серверном виде.

## DELETE /api/v1/notes/{id}

Назначение:
Удалить заметку.

Ответ `200`:

```json
{
  "ok": true,
  "id": "3dce4d7d-43de-4890-b8f8-0a0f3adce9df"
}
```

## PUT /api/v1/timers/{id}

Назначение:
Создать или обновить таймер/секундомер.

Тело:

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

Ответ `200`:
Объект таймера в актуальном серверном виде.

## DELETE /api/v1/timers/{id}

Назначение:
Удалить таймер.

Ответ `200`:

```json
{
  "ok": true,
  "id": "884f8b44-f7c4-40e4-bd3f-76658c841e65"
}
```

## Поведение клиентов

- После любого `PUT` или `DELETE` клиенту рекомендуется снова вызвать `GET /api/v1/snapshot`.
- `revision` нужен клиентам для понимания, изменилось ли общее состояние.
- Сервер не навязывает офлайн-очередь. Если Android-клиенту позже понадобится офлайн-режим, очередь операций должна жить на стороне клиента.
