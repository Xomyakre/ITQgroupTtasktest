# ITQ Group test task — Documents service

## Требования
- Java 17
- Docker (для PostgreSQL)

## Запуск PostgreSQL

```bash
docker compose up -d
```

Параметры БД уже прописаны в `src/main/resources/application.properties`:
- db: `itqdb`
- user: `itquser`
- pass: `itqpass`

## Запуск сервиса

```bash
./gradlew bootRun
```

## API (основное)
- `POST /api/documents` — создать документ (всегда создаётся в `DRAFT`, номер генерируется)
- `GET /api/documents/{id}?includeHistory=true` — получить документ + история
- `GET /api/documents?page=0&size=20&sort=id,desc` — список документов (пагинация/сортировка)
- `GET /api/documents?ids=1&ids=2&ids=3&page=0&size=2` — пакетное получение по id
- `POST /api/documents/submit` — пакетный перевод `DRAFT -> SUBMITTED` (1..1000 id)
- `POST /api/documents/approve` — пакетный перевод `SUBMITTED -> APPROVED` (1..1000 id), с записью в реестр
- `GET /api/documents/search?status=DRAFT&author=al&from=2026-01-01T00:00:00Z&to=2026-12-31T00:00:00Z`
  - период трактуется **по `createdAt`**
- `POST /api/documents/test-concurrent-approve` — проверка конкурентного утверждения

### Пример payload для create

```json
{
  "initiator": "user1",
  "author": "Alex",
  "title": "My doc"
}
```

### Пример payload для submit/approve

```json
{
  "initiator": "user1",
  "ids": [1,2,3],
  "comment": "optional"
}
```

## Фоновые процессы
Включены по умолчанию:
- SUBMIT-worker: берёт `DRAFT` пачками по `app.batch-size` и вызывает пакетный submit
- APPROVE-worker: берёт `SUBMITTED` пачками по `app.batch-size` и вызывает пакетный approve

Управление:
- `app.workers.enabled=true|false`
- `app.batch-size=100`
- `app.workers.submit-fixed-delay-ms=5000`
- `app.workers.approve-fixed-delay-ms=5000`

По логам видно размер пачек, успешные обработки и время выполнения.

## Утилита генерации документов
Модуль `generator` читает JSON-конфиг и создаёт N документов через API.

Запуск:

```bash
./gradlew :generator:run --args="generator-config.json"
```

Если файл конфигурации не существует — он будет создан с дефолтными значениями.

## Тесты

```bash
./gradlew test
```

