# PolyglotMesh — Developer Implementation Guide

This guide explains **how the delivered code works, how you can modify it, and how to implement the main features yourself**. The easiest workflow is to run the existing system first (`WINDOWS_SETUP.md`) and then make one change at a time.

## Stage 1 — Java + Spring Boot API

1. Install Java 21+ and Maven 3.9+.
2. Inspect `backend/pom.xml`. The project imports the Spring Boot 3 parent and includes WebFlux (HTTP API), Spring Data JPA (database persistence), H2 (embedded database), and GraalVM Polyglot, Python, and JS artifacts.
3. Inspect `backend/src/main/java/com/polyglotmesh/PolyglotMeshApplication.java`, the Spring Boot entry point.
4. Run `mvn spring-boot:run` from `backend/`; test `GET /api/health`.
5. Inspect `backend/src/main/java/com/polyglotmesh/api/WorkspaceController.java` for all API endpoints. The controller exposes a `Mono<ExecuteResponse>` and schedules execution on `Schedulers.boundedElastic()` so guest code isn't evaluated on the HTTP event loop.

**Modify the port:** update `server.port` in `backend/src/main/resources/application.properties` or set `PORT`. The frontend's Vite proxy must use the same port.

## Stage 2 — Embed GraalVM languages

The central implementation is `backend/src/main/java/com/polyglotmesh/service/PolyglotExecutionService.java`.

**Language selection:** In single mode the Java API normalizes the language to `python` or `javascript` and creates a Context permitted to use `python` or `js`. In pipeline mode the Context supports **both** languages.

**Context permissions:**

```java
Context context = Context.newBuilder("python", "js")
    .engine(engine)
    .allowHostAccess(HostAccess.NONE)
    .allowHostClassLookup(name -> false)
    .allowIO(IOAccess.NONE)
    .allowCreateThread(false)
    .allowNativeAccess(false)
    .allowCreateProcess(false)
    .build();
```

The actual source also routes stdout/stderr into a capped stream. These options are useful restrictions **but are not sufficient for a hardened public service**.

**Python evaluation:**

```java
Map<Object,Object> pythonInput = new LinkedHashMap<>();
pythonInput.putAll(input);
context.getBindings("python").putMember("input_data", ProxyHashMap.from(pythonInput));
context.eval("python", pythonCode);
Value pythonResult = context.getBindings("python").getMember("result");
```

**Python → JS transfer:**

```java
context.getBindings("js").putMember("python_result", pythonResult.toString());
context.eval("js", javascriptCode);
Value jsResult = context.getBindings("js").getMember("result");
```

These are **direct API calls in Java**, with no separate Python web service and no JS microservice.

### Add your own pipeline algorithm

In React, select **Pipeline**. Replace pricing.py with:

```python
# Java sent quantity and unitPrice
subtotal = input_data['unitPrice'] * input_data['quantity']
result = round(subtotal * 1.18, 2)  # Example: add 18% tax
print('Tax-inclusive total:', result)
```

Replace validate.js with:

```javascript
const total = Number(python_result);
const eligible = total <= 5000;
result = JSON.stringify({eligible, total});
print('Eligible?', eligible);
```

Set Input context to:

```json
{"unitPrice": 1000, "quantity": 3}
```

Run. Expected Python result: `3540.0`. JavaScript returns `eligible: true` and `total: 3540`.

## Stage 3 — React + Monaco IDE

1. Inspect `frontend/package.json` for React, Vite, `monaco-editor`, `@monaco-editor/react`, Recharts and Lucide icons.
2. `frontend/src/main.jsx` configures Monaco to use **bundled local worker scripts**, rather than downloading the editor from a CDN.
3. `frontend/src/App.jsx` holds the workspace, examples, Monaco editor tabs, input JSON, execution console, history, metrics, and saved scripts.
4. `frontend/src/lib/api.js` is the single API client. It calls endpoints such as `POST /api/execute`.
5. `frontend/src/data/templates.js` contains built-in *offline preview* templates that show code even before the backend is running. The canonical templates are returned from `GET /api/templates` once the API is online.
6. `frontend/src/styles.css` implements the dark IDE theme and responsive layout.

**How to add a new editor button:** Find the `editor-toolbar` JSX in `App.jsx`, add your button, bind its click handler, and define state/logic above the `return` statement. Vite hot reloads React changes during `npm run dev`.

**How to change colors:** Update colors in `frontend/src/styles.css`, particularly `.primary-button`, `.page-heading h1 span`, `.editor-card`, `.sidebar` and `.topbar`.

**Keyboard shortcut:** `Ctrl+Enter` (Windows) or `⌘+Enter` (macOS) runs the current script in the workspace.

## Stage 4 — Persistence in H2

The `Snippet` and `ExecutionLog` entities are under `backend/.../persistence/`.

- A snippet contains a title, language/mode, and script contents.
- Execution history contains a UUID, date/time, status, language/mode, wall time, and a short result/error preview. It intentionally does not store full script source or input for every run.
- `SnippetRepository` and `ExecutionLogRepository` are Spring Data JPA repositories.
- The H2 file is created under `backend/data/`.

**Add a saved input field (optional):** Introduce `inputJson` on `Snippet`, add it to `SnippetRequest`, update `WorkspaceController.apply()`, then pass `inputText` from React's `saveSnippet` method. Hibernate `ddl-auto=update` will attempt to add the column on the next restart. Back up existing H2 data before schema changes.

## Stage 5 — Console, errors and metrics

The runner captures JavaScript/Python `print()` output from the context's `out` stream. It collects the guest's `result` variable separately, returns the status/error string, and records execution steps.

The UI:

- Shows captured stdout and `result` in **Execution console**.
- Displays per-stage elapsed times under **Trace**.
- Reads the persisted execution metadata under **Execution history**.
- Draws a time-series chart under **Performance**.

**Important measurement caveat:** End-to-end `elapsedMs` includes validation, queueing, context creation, runtime warm-up, script evaluation, and Java result wrapping. Individual stage timers show only a portion. The JVM heap delta metric is approximate and process-wide.

## Stage 6 — Validate the app

```bat
cd /d C:\Projects\PolyglotMesh\backend
mvn test
mvn package

cd /d C:\Projects\PolyglotMesh\frontend
npm install
npm run build
```

The `PolyglotExecutionServiceTest` class includes JavaScript, Python, pipeline bridging, host class denial, and invalid-input tests. `docs/TESTING.md` provides a complete manual acceptance checklist.

## Stage 7 — Extending toward the original full brief

These are **enhancements**, not requirements to run the supplied end product:

**Ruby support:** Add a compatible, verified `org.graalvm.polyglot:ruby` artifact, confirm it works with your Java/GraalVM versions, then update input validation, runtime dispatch, UI language mapping and templates. Check license and platform support first.

**MongoDB instead of H2:** Install MongoDB locally or use a managed instance; add Spring Data MongoDB starter, replace the JPA entities/repositories with Mongo document repositories, and update configuration. Use Java as the safe data access layer — do not expose database credentials to guest scripts.

**AOT / Native Image:** Create a separate build target and test GraalPy/GraalJS resource inclusion on Windows or Linux. Do not assume a normal Spring Boot native-image build bundles guest language runtimes correctly; GraalVM requires dedicated native embedding configuration.

**Stronger sandbox:** Use an isolated process per execution or the GraalVM `SandboxPolicy.ISOLATED`/`UNTRUSTED` with supported isolate artifacts, CPU/heap quotas, engine/host compatibility, auditing and aggressive security tests. Code from strangers should never execute in this demo server as-is.

**Comparative latency benchmark:** Run repeatable warm vs warm experiments, with controlled payloads, real separate REST service baselines, p50/p95/p99 latencies and memory use. Do not label an artificial `sleep` as a measured REST benchmark.

## Source map — where to change what

| You want to change | File |
| --- | --- |
| API routes | `backend/.../api/WorkspaceController.java` |
| Allowed execution modes / actual script invocation | `backend/.../service/PolyglotExecutionService.java` |
| Built-in server templates | `backend/.../service/TemplateCatalog.java` |
| Database schema | `backend/.../persistence/Snippet.java` and `ExecutionLog.java` |
| Spring port / timeout | `backend/src/main/resources/application.properties` |
| React UI pages / buttons | `frontend/src/App.jsx` |
| Dark theme | `frontend/src/styles.css` |
| Default editor content | `frontend/src/data/templates.js` |
| Browser ↔ backend requests | `frontend/src/lib/api.js` |
| Vite backend proxy | `frontend/vite.config.js` |
| Automated JVM behavior tests | `backend/src/test/java/com/polyglotmesh/` |

**Minimum viable submission:** Show the pricing pipeline returning a result, change input values, demonstrate host class blocking, save a script, and show execution history and metrics. This covers the central brief without requiring Ruby, MongoDB or AOT.
