# PolyglotMesh: Team Contract

## Team and ownership
- Leader (Sampreeth): GraalVM engine, sandbox, interop, Native Image, pricing demo, merges
- Shagun: Spring Boot WebFlux API, persistence, metrics endpoints, mock data
- Aastha: React + Monaco frontend
- Fazal: testing, samples, documentation, slides, demo video

## Ports and setup
- Backend: http://localhost:7070 (Java 17+, GraalVM, Spring Boot 3, WebFlux)
  - set in application.properties: server.port=7070
- Frontend: http://localhost:7071 (Vite + React)
  - set in vite.config.js: server: { port: 7071, strictPort: true }
- Backend allows CORS from http://localhost:7071

## API 1: Execute code
POST /api/execute

Request:
{ "language": "python" | "js", "code": "print('Hello')", "input": { "any": "json" } }

Response:
{
  "status": "OK" | "ERROR" | "TIMEOUT" | "SANDBOX_VIOLATION",
  "output": "Hello\n",
  "error": null,
  "stackTrace": null,
  "durationMs": 3.2
}

- `input` is optional and is available inside scripts as a variable named `input`.
- Backend always returns HTTP 200 with a status field for script problems.
  HTTP 400 is only for invalid requests (unknown language, empty code, code too large).

## API 2: Saved scripts
- GET    /api/scripts            -> [{ id, name, language, code, updatedAt }]
- GET    /api/scripts/{id}
- POST   /api/scripts            body: { name, language, code }
- PUT    /api/scripts/{id}       body: { name, language, code }
- DELETE /api/scripts/{id}

## API 3: Metrics comparison
POST /api/metrics/compare   body: { language, code, input }
Response: { "polyglotMs": 0.8, "restMs": 52.4, "speedup": 65.5 }
GET /api/metrics/history -> last N comparison results (newest first)

## API 4: Mock data (Week 4)
- GET /api/data/collections           -> ["products", "orders"]
- GET /api/data/{collection}          -> sample documents

## Java tab decision
Truffle runs Python and JS, not Java. The Java tab is for host bindings:
- It displays the Java objects that scripts can call (documentation + editable notes).
- Java itself runs as backend code and is exposed to scripts.
- Objects available inside Python/JS: `input` (request data) and, from Week 4,
  `db` with `db.find(collection, filterJson)`.

## Sandbox rules (what user scripts must NOT do)
- No file system access, no network sockets, no native access, no creating threads.
- No host class lookup.
- Execution timeout, statement limit and output size cap apply to every run.
- Each request uses a fresh Context; one shared Engine for speed.

## Git rules
- Everyone works only on their own branch (sampreeth, shagun, aastha, fazal).
- Nobody commits or pushes directly to main; the leader decides when branches are merged.
- Everyone commits every working day with a clear message.
- Daily check-in at a fixed time. If a day's tasks aren't done, finish them before starting the next day.