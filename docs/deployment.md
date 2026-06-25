# Развертывание

## 1. Инфраструктура

Infra compose живет отдельно и поднимает общие сервисы.

Файлы:

- [deploy/infra/docker-compose.yml](D:/MyProjects/IdeaProjects/Notes/deploy/infra/docker-compose.yml)
- [deploy/infra/.env.example](D:/MyProjects/IdeaProjects/Notes/deploy/infra/.env.example)

Запуск на Arch:

```bash
cd deploy/infra
cp .env.example .env
docker compose up -d
```

Это создаст:

- PostgreSQL
- Redis
- внешнюю docker-сеть `notes-backend`

## 2. Сервер приложения

Файлы:

- [deploy/server/docker-compose.yml](D:/MyProjects/IdeaProjects/Notes/deploy/server/docker-compose.yml)
- [deploy/server/.env.example](D:/MyProjects/IdeaProjects/Notes/deploy/server/.env.example)
- [server/Dockerfile](D:/MyProjects/IdeaProjects/Notes/server/Dockerfile)

Запуск:

```bash
cd deploy/server
cp .env.example .env
docker compose up -d --build
```

## 3. Tailscale

На сервере:

```bash
sudo tailscale up
tailscale ip -4
```

В Windows-клиенте затем укажите:

- `http://<tailscale-ip>:8080`
- или `http://<magicdns-name>:8080`

## 4. Windows app-image

Сборка:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-exe.ps1
```

Готовый exe:

- [NotesWidgetClient.exe](D:/MyProjects/IdeaProjects/Notes/dist/NotesWidgetClient/NotesWidgetClient.exe)

## Локальный smoke-test на localhost

Если вы хотите быстро проверить всё на одном ПК без Tailscale:

1. Поднимите infra compose.
2. Поднимите server compose с `NOTES_PORT=8080`.
3. Запустите `NotesWidgetClient.exe`.
4. На вкладке `Sync` оставьте `http://127.0.0.1:8080`.
5. Если в `deploy/server/.env` задан `NOTES_API_KEY`, введите его в поле `API key`.

## 5. Резервное копирование

Минимум, что стоит бэкапить:

- PostgreSQL volume из infra compose
- `.env` файла сервера

Redis в текущей версии не является критичным для данных заметок и таймеров.
