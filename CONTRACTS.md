# PolyglotMesh API Contracts (v1.0)

Base URL: `http://localhost:7071`. JSON request/response, CORS allowed for React on port 7070. Backend execution is Python/JavaScript only; Java is syntax-highlighted but not executed. **These are the canonical contracts** for all team branches.

## Execution / sandbox

`POST /api/execute`:

```json
{"mode":"single","language":"python","code":"print('Hi')\nresult=1+2","input":{"name":"Lab"}}
```

Modes: `single`, `audit` (mutate the **same Java HashMap** from Python), `pipeline` (Java MongoMock product -> GraalPy price -> GraalJS stock validation). In pipeline mode, optional `pythonCode`/`javascriptCode` override the default guest scripts, `input` supplies `productId`, `quantity`, `discountPercent`.

Response fields:

```json
{
 "id":"<uuid>","status":"OK","mode":"single","language":"python",
 "output":"Hi\n","result":3,"error":"","stack":[],
 "steps":[{"language":"python","status":"OK","result":3,"elapsedMs":10}],
 "elapsedMs":15,"timestamp":"<UTC instant>"
}
```

Statuses (uppercase): `OK`, `ERROR`, `TIMEOUT`, `SANDBOX_VIOLATION`. The sandbox may classify some restricted operations as `ERROR` when a guest runtime raises its own permission-related exception; **blocked execution**, not a particular label, is the primary security assertion. `TIMEOUT` also covers statement-limit exhaustion. `input` and `db` are limited guest proxies, not unrestricted Java host objects. `db.find('products')`/`db.find('orders')` reads mock collections.

`POST /api/audit` runs the HashMap audit and logs the result (alias for `/api/execute` with `mode=audit`).

`GET /api/health` -> `{ "status":"UP",... }`.

`GET /api/templates` -> list of `{id,title,language,code}`.

## History / metrics

`GET /api/history?limit=20` -> recent execution log array (H2-backed).

`GET /api/metrics` -> `{sampleSize,successes,failures,successRate,averageMs,recent}` from last 100 executions.

`GET /api/metrics/history` -> last 100 persisted comparison results.

`POST /api/compare-metrics`:

```json
{"pricing":{"productId":"p2","quantity":2,"discountPercent":10},"iterations":10}
```

Returns `{id,result:{graalvmPrice,restPrice},equalResults,metrics:[{adapter,measurementType,averageMs,minMs,maxMs,sampleMs}],caveat,timestamp}`. The mock REST endpoint calculates the **same pricing formula** as the GraalPy path but includes configurable artificial HTTP delay. Its output is an illustrative comparison, **not a scientific benchmark**.

`POST /mock/price` takes a bare pricing request and responds `{productId,unitPrice,quantity,discountPercent,total,implementation}`; artificial delay defaults to 40ms.

## Data (read-only)

`GET /api/data/collections` -> `["products","orders"]` in unspecified order.

`GET /api/data/products`, `GET /api/data/orders` -> sample JSON documents.

`POST /api/data/query`: `{ "collection":"products", "filter":{"id":"p2"} }` -> matching list. At most three exact-match filter keys; unknown collection -> HTTP 400.

## Snippet CRUD (H2)

`GET /api/snippets` -> snippets ordered newest first.

`POST /api/snippets` -> HTTP 201 with `{id,title,language,code,version,updatedAt}`. Body `{ "title":"Greeting", "language":"python", "code":"print(1)" }`.

`PUT /api/snippets/{id}` -> updated object, requires `expectedVersion` equal to stored version. Mismatch -> HTTP 409, missing -> HTTP 404.

`DELETE /api/snippets/{id}` -> HTTP 204, missing -> HTTP 404.

Errors: input validation -> HTTP 400 `{error:"..."}`; other server errors -> HTTP 500 with generic message; guest code errors are encoded in a **HTTP 200** execution response with `status` and guest stack to avoid crashing UI.

## Resource defaults

`EXECUTION_TIMEOUT_MS=10000`, `STATEMENT_LIMIT=100000`, `OUTPUT_LIMIT=32768`, `CODE_LIMIT=16000`, `MAX_WORKERS=4`, `MAX_QUEUE=16`. Restricted Java host lookup, filesystem, subprocesses, OS threads, native access, and host access. **A JVM context is not a production isolation boundary**; see SECURITY.md.
