# PolyglotMesh API Reference

Base URL: `http://127.0.0.1:7071/api`

The Vite frontend proxies `/api` to this backend during `npm run dev`.

## GET /health

Reports server availability. Example:

```json
{
  "status": "UP",
  "service": "PolyglotMesh",
  "runtime": "GraalVM Polyglot",
  "languages": ["python", "javascript"],
  "security": "restricted-local-demo",
  "timestamp": "..."
}
```

## POST /execute

Content type: `application/json`.

**Python single-language example:**

```json
{
  "mode": "single",
  "language": "python",
  "code": "result = sum([5, 10, 15])\nprint('Sum:', result)",
  "input": {}
}
```

**JavaScript single-language example:**

```json
{
  "mode": "single",
  "language": "javascript",
  "code": "result = input_data.price * 2; print('Result:', result);",
  "input": {"price": 25}
}
```

**Full mixed-language pipeline example:**

```json
{
  "mode": "pipeline",
  "language": "python",
  "pythonCode": "result = round(input_data['unitPrice'] * input_data['quantity'] * 0.9, 2)\nprint('Python result:', result)",
  "javascriptCode": "const approved = Number(python_result) > 0; result = JSON.stringify({approved, amount: Number(python_result)}); print('JS approved:', approved);",
  "input": {"unitPrice": 100, "quantity": 2}
}
```

**Example response structure:**

```json
{
  "id": "generated-uuid",
  "status": "success",
  "mode": "pipeline",
  "language": "python → javascript",
  "output": "Python result: 180.0\nJS approved: true\n",
  "result": "{\"approved\":true,\"amount\":180}",
  "error": "",
  "elapsedMs": 153,
  "heapDeltaKb": 42,
  "steps": [
    {"name": "Java input binding", "language": "java", "elapsedMs": 0, "status": "success"},
    {"name": "GraalPy calculation", "language": "python", "elapsedMs": 140, "status": "success"},
    {"name": "GraalJS validation", "language": "javascript", "elapsedMs": 8, "status": "success"}
  ],
  "timestamp": "..."
}
```

Numbers above are **illustrations, not measured results**. First-run GraalPy may take seconds, not milliseconds.

On guest failure, the HTTP request may still return `200 OK` with `"status":"error"` and an `error` string. An invalid input body produces `400`; queue saturation returns `429`. A timeout returns `"status":"timeout"` with a cancellation explanation.

### Important conventions

- **Top-level `result`**: a guest script should assign `result = ...` (Python or JavaScript). This is what Java returns in the `result` field.
- **Console output**: `print(...)` is collected into `output`. JavaScript uses GraalJS's built-in `print`; Python uses Python's built-in `print`.
- **Python input**: `input_data['unitPrice']`.
- **JavaScript input**: `input_data.unitPrice` (or `input_data['unitPrice']`).
- **Pipeline handoff**: Java reads Python's `result`, converts it to a string, then provides `python_result` to GraalJS. Use `Number(python_result)` for numeric values.
- Inputs are flat JSON objects with at most 24 keys; values must be strings (up to 1000 characters), numbers, booleans or null. Nested arrays/maps are rejected.
- There is no special sandbox-capable `import fs`, `Java.type(...)` or network API. Host access is denied.

### Test from PowerShell

Start the backend, then run:

```powershell
$body = @{
  mode = 'single'
  language = 'python'
  code = "result = 12 * 3`nprint('Hello', result)"
  input = @{}
} | ConvertTo-Json

Invoke-RestMethod -Uri 'http://127.0.0.1:7071/api/execute' `
  -Method Post -ContentType 'application/json' -Body $body
```

## GET /templates

Returns four built-in example templates: `pricing-engine`, `python-hello`, `javascript-hello` and `blocked-access`.

## Snippet CRUD

**Create:** `POST /snippets`:

```json
{
  "title": "My experiment",
  "mode": "single",
  "language": "python",
  "code": "result = 42",
  "pythonCode": "",
  "javascriptCode": ""
}
```

Returns `201` with the saved entity and UUID. `GET /snippets` lists saved entities (most recently updated first). `PUT /snippets/{id}` updates one with the same JSON fields. `DELETE /snippets/{id}` returns `204` (no body). Saved script input JSON is **not** persisted in this version.

## GET /history?limit=20

Returns recent execution log metadata. `limit` is clamped to 1–100. Log entries contain `id`, `timestamp`, `status`, `mode`, `language`, `elapsedMs` and a short `summary`. Code and full stdout are **not** stored in run history.

## GET /metrics

Returns `total`, `successes`, `failures`, `successRate`, `averageMs`, and `recent` (the most recent 12 execution log entries). Average is calculated over the **latest 40** records and includes cold starts.
