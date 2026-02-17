## Пример поискового запроса

Ищем документы по статусу + автору + периоду `created_at`:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM documents
WHERE status = 'DRAFT'
  AND lower(author) LIKE '%alex%'
  AND created_at BETWEEN '2026-01-01T00:00:00Z' AND '2026-12-31T00:00:00Z'
ORDER BY id DESC
LIMIT 20 OFFSET 0;
```

## Индексы
- `idx_documents_status` ускоряет отбор по `status`
- `idx_documents_author` помогает при частых фильтрах по `author` (для LIKE по `lower(author)` в проде лучше добавить функциональный индекс)
- `idx_documents_created_at` ускоряет отбор по периоду

Для максимально эффективного плана под этот запрос в PostgreSQL обычно добавляют составной индекс, например:

```sql
CREATE INDEX CONCURRENTLY idx_documents_status_created_at
ON documents(status, created_at);
```

А для `lower(author) LIKE '%...%'` — триграммный индекс (расширение `pg_trgm`).

