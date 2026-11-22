# Architecture and Project Brief Mapping

## Intended problem

The Infotact brief identifies an enterprise Java application that needs small Python and JavaScript components, but would incur deployment complexity and network latency if every small operation became a REST/gRPC microservice. PolyglotMesh demonstrates an alternative: using GraalVM guest language runtimes **inside the Java host process** and a single browser-facing API.

## Components

**1. React + Monaco IDE** (`frontend/src/App.jsx`): a browser editor for single-language and multi-stage pipeline execution. It owns draft code and JSON input, sends requests to the backend, and displays actual stdout, result bindings, errors and step timings.

**2. Spring Boot WebFlux controller** (`backend/.../api/WorkspaceController.java`): implements API routing, result persistence, metrics, saved snippets, CORS and health. Long/possibly-blocking operations are scheduled on Reactor bounded elastic rather than the HTTP event-loop thread.

**3. PolyglotExecutionService** (`backend/.../service/PolyglotExecutionService.java`): keeps an `Engine`, creates a **new restricted Context per run**, disables host class access, host I/O, thread/process/native access, handles stdout capture, executes Python/JS, retrieves the top-level `result` variable, and applies a best-effort timeout. A two-thread executor with a bounded waiting queue limits accidental overload.

**4. Spring Data JPA + H2** (`backend/.../persistence/`): stores scripts and execution metadata in a file on the same machine. No external database is needed. (The project brief mentions MongoDB; H2 was chosen deliberately for a complete, installable prototype.)

**5. TemplateCatalog** (`backend/.../service/TemplateCatalog.java`): provides a pricing pipeline, Python data processing, JS validation, and an access-restriction example.

## Detailed execution sequence

```text
User clicks Run
   │
   ▼
React: POST /api/execute JSON
   │
   ▼
WebFlux controller → bounded elastic scheduler
   │
   ▼
PolyglotExecutionService validates mode, language, source and input
   │
   ▼
Thread pool receives job; time limit starts
   │
   ▼
GraalVM Engine → new Context(host access: none, IO: none)
   │
   ├── Java wraps a Map as a guest ProxyObject
   │     └── GraalPy bindings: input_data
   │
   ├── GraalPy evaluates pythonCode
   │     └── Java retrieves Python global: result
   │
   ├── Java inserts a primitive string binding: python_result
   │     └── GraalJS evaluates javascriptCode
   │
   └── Java retrieves JS global: result and captured stdout
   │
   ▼
Context closed, elapsedMs calculated
   │
   ▼
H2 stores a short execution metadata record
   │
   ▼
JSON response → React console, trace, history, chart
```

In **single** mode the same engine evaluates one language only. In **pipeline** mode Python and JavaScript execute sequentially in a context that supports both languages. Data is transferred via **Java/Polyglot bindings**, not network requests.

## Project brief scope mapping

| Brief requirement | Current implementation | Why |
| --- | --- | --- |
| GraalVM Polyglot Engine / Truffle | Core dependency and Context integration | Essential project objective |
| Python + JavaScript within Java JVM | Direct `context.eval` calls | Main deliverable |
| Ruby within Java | Not implemented | Scope control; two guest runtimes already prove polyglot design |
| Context isolation, restrict host resources | Restrictive permissions + bounded executor/timeout | Local-demo guardrails, not formal sandbox |
| Spring Boot 3 WebFlux execution API | Complete | Browser ↔ Java communication |
| React + Monaco multi-tab IDE | Complete | Language-aware code editor |
| Java HashMap into Python and result back to Java | `ProxyHashMap.from` + `getMember("result")` | Mid-project review deliverable |
| Real timing metrics | Per-stage and end-to-end timings with history/chart | Useful observability |
| Native Image AOT startup comparison | Not implemented | Platform-specific, complex integration |
| MongoDB binding and saved scripts | H2 + JPA alternative | Same user feature without extra service |
| Persist scripts | CRUD endpoints and UI | Fully usable local workflow |

## Design choices

- **WebFlux + blocking execution isolation:** Not evaluating guest code on the Reactor event loop avoids tying up HTTP workers; the project uses both Reactor elastic scheduling and a bounded Java execution queue.
- **Runtime output:** guest `print` goes to a capped output stream. The top-level `result` is separately retrieved. This is easier to explain and test than trying to infer the last expression in two languages.
- **Result type:** result is sent to the UI as a string. The JS pipeline template uses `JSON.stringify` so structured JSON is readable and not dependent on fragile cross-language object conversion.
- **Simple JSON inputs:** only primitive, flat JSON key/value pairs are supported. This intentionally limits the host-object exposure surface.
- **H2 on disk:** reliable local persistence without Docker/MongoDB. Database file is created in the backend working directory.
- **Metrics are honest:** `elapsedMs` includes context init and language execution. `heapDeltaKb` is a *process-wide* approximate delta, not a guest memory allocation measurement.

## Improvements beyond this version

If extending into a production system, use dedicated isolated worker processes/containers with OS-level memory/CPU limits, authentication and authorization, rate limiting, per-tenant quotas and an audited filesystem/runtime policy. For GraalVM's stronger `SandboxPolicy.ISOLATED` or `UNTRUSTED`, use the right language isolate artifacts, resource limits and compatible GraalVM build; do not simply rename the current permissions as hardened isolation. Add genuine benchmark harnesses and native-image CI as separate workstreams.
