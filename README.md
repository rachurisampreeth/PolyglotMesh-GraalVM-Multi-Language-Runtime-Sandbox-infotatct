# PolyglotMesh — GraalVM Multi-Language Runtime Studio

**A full-stack, local-first development studio for executing Python and JavaScript inside a Java application.**

Based on the Infotact Solutions **Project 1: “PolyglotMesh”: GraalVM Multi-Language Runtime Sandbox** brief. The delivered implementation concentrates on the brief's *core working features*: GraalVM language execution, a Java-to-Python-to-JavaScript data pipeline, a restricted execution context, a Spring Boot API, a React + Monaco IDE, persisted scripts, execution history, and real timing metrics.

> **Status and honesty:** This archive contains complete backend and frontend **source code**, configuration, automated test source and setup documentation. It is not a bundled runnable Windows executable: dependencies must be downloaded by Maven/npm on the first run. The build and Java runtime could **not be compiled/run in the environment used to prepare this archive** because Maven packages could not be downloaded. Run `mvn test` and `npm run build` on your internet-connected machine before presenting it as verified. The app intentionally defaults to localhost only. Do not deploy it as a public arbitrary-code sandbox.

## Features

| Feature | Delivered? | Where |
| --- | --- | --- |
| Java 21+, Spring Boot 3, WebFlux REST API | Yes | `backend/` |
| Execute real Python code using GraalPy | Yes, implemented in source | `PolyglotExecutionService.java` |
| Execute real JavaScript code using GraalJS | Yes, implemented in source | `PolyglotExecutionService.java` |
| Java Map → Python result → JavaScript result, same JVM | Yes | `POST /api/execute` with `mode=pipeline` |
| Restricted guest permissions | Partially — host/IO blocked, timeout and output cap | `PolyglotExecutionService.java` |
| React IDE, Monaco syntax highlighting | Yes | `frontend/` |
| Real console output, structured result and error | Yes | REST + UI |
| Save/reload/delete scripts | Yes | JPA with embedded H2 |
| Run history and latency dashboard | Yes | `/api/history`, `/api/metrics` + chart |
| Example projects, API documentation and Windows scripts | Yes | `backend/TemplateCatalog.java`, `docs/`, `scripts/` |
| Ruby guest language | Not included | Prioritized Python + JavaScript |
| GraalVM Native Image AOT executable | Not included | Needs a dedicated, tested native-image toolchain |
| MongoDB integration | Not included | Replaced by embedded H2 for easy setup |
| Multi-tenant hardened untrusted-code sandbox | **Not included** | See `docs/SECURITY.md` |

**Important:** “same JVM” means no REST call *between Python and JavaScript*. The browser still calls the Java backend via HTTP. In-process execution does **not** guarantee zero latency or sub-millisecond execution, particularly on GraalPy cold starts.

---

## Quick start — Windows (Command Prompt)

### Requirements

- **JDK 21 or newer**; a matching **GraalVM JDK** is recommended for better performance, but standard OpenJDK can run embedded language dependencies in interpreter mode.
- **Apache Maven 3.9+** (`mvn -v`).
- **Node.js 20.19+ or 22.12+** and npm (`node -v`, `npm -v`).
- Internet access **on the first build** to download Maven/npm dependencies.

### 1. Unzip the project

Extract `PolyglotMesh_Complete_Project.zip` to a normal local folder such as `C:\Projects\PolyglotMesh`.

### 2. Run the Java backend

Open **Command Prompt 1**:

```bat
cd /d C:\Projects\PolyglotMesh\backend
mvn spring-boot:run
```

**Verify:** Open **http://localhost:7071/api/health** in your browser. You should get JSON containing `"status":"UP"` and `"languages":["python","javascript"]`.

### 3. Run the React dashboard

Open **Command Prompt 2**:

```bat
cd /d C:\Projects\PolyglotMesh\frontend
npm install
npm run dev
```

**Verify:** Open **http://localhost:7070**. This project requires port 7070 to be free. The top-right status should say **Runtime online**.

### 4. Demonstrate the full pipeline

1. Click **Dynamic pricing engine** in Quick Start.
2. Keep **Pipeline** selected. Click the **pricing.py** and **validate.js** tabs to inspect both languages.
3. Click **Run code**. A Java Map is bound into GraalPy; Python calculates a discounted price; Java forwards that value to GraalJS for approval.
4. The console should display lines similar to:

   ```text
   Python pricing result: INR 4047.3
   JS fraud-check approved: true
   RESULT
   {"approved":true,"payable":4047.3,"currency":"INR","source":"GraalPy → GraalJS"}
   ```

5. Edit the **Input context** JSON (`unitPrice`, `quantity`, `coupon`) and run again.
6. Click **Save** and name the script. It appears in **Saved scripts** after saving.
7. Open **Execution history** and **Performance** to inspect persisted runs.

### 5. Run the checks

```bat
cd /d C:\Projects\PolyglotMesh\backend
mvn test
mvn package

cd /d C:\Projects\PolyglotMesh\frontend
npm run build
```

The Java release JAR will be in `backend/target/polyglotmesh-backend-1.0.0.jar`. Run it using:

```bat
cd /d C:\Projects\PolyglotMesh\backend
java -jar target\polyglotmesh-backend-1.0.0.jar
```

For the complete walkthrough, see **`docs/WINDOWS_SETUP.md`** and **`docs/IMPLEMENTATION_GUIDE.md`**. The `scripts/` folder contains optional `.bat` shortcuts.

---

## Architecture

```text
              Browser / React + Monaco (7070)
                           |
                        HTTP /api
                           v
                Java 21 / Spring Boot (7071)
                   |               |
              WebFlux API       H2 + JPA
                   |          snippets / logs
                   v
             PolyglotExecutionService
                   |
             GraalVM Polyglot Context
                   |
      Java Map ---+----> GraalPy (Python)
                                 |
                        result via Java binding
                                 |
                                 v
                           GraalJS (JS)
                                 |
                                 v
                        result + console + timing
```

A fresh GraalVM `Context` is created for each call; it is not reused between users. The Spring Boot service restricts guest host access and file/network/process/thread operations. Runs have a 10-second wall-clock timeout and 32 KiB output cap. **These are guardrails, not a formally secure sandbox or a guest heap/CPU quota.**

### Stack

- Backend: Java 21, Spring Boot **3.5.16**, Spring WebFlux, Spring Data JPA, H2, GraalVM Polyglot SDK / GraalPy / GraalJS **25.3.4.1**.
- Frontend: React **19**, Vite **6**, Monaco Editor, Recharts, Lucide icons, CSS.
- Database: `backend/data/polyglotmesh.mv.db` (generated on first backend launch).

### API endpoints

| Method | URL | Description |
| --- | --- | --- |
| GET | `/api/health` | Check backend |
| GET | `/api/templates` | Built-in templates |
| POST | `/api/execute` | Run Python, JS or pipeline |
| GET | `/api/snippets` | List saved scripts |
| POST | `/api/snippets` | Save script |
| PUT | `/api/snippets/{id}` | Update saved script |
| DELETE | `/api/snippets/{id}` | Delete saved script |
| GET | `/api/history?limit=20` | Recent execution metadata |
| GET | `/api/metrics` | Totals, success rate and recent latency |

See **`docs/API.md`** for request/response bodies and test commands.

---

## Project structure

```text
PolyglotMesh/
├── START_HERE.md                      friendly setup summary
├── README.md                          project readme
├── backend/
│   ├── pom.xml                        Java dependencies
│   └── src/
│       ├── main/java/com/polyglotmesh/
│       │   ├── api/                    API routes, DTOs, errors, CORS
│       │   ├── persistence/            JPA entities/repositories
│       │   ├── service/                execution engine, templates
│       │   └── PolyglotMeshApplication.java
│       ├── main/resources/application.properties
│       └── test/java/...               JUnit tests
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/                          React IDE, Monaco, styles, API client
├── docs/
│   ├── WINDOWS_SETUP.md                installation and troubleshooting
│   ├── ARCHITECTURE.md                 components and design decisions
│   ├── IMPLEMENTATION_GUIDE.md         extend and modify the application
│   ├── API.md                          sample requests and responses
│   ├── SECURITY.md                     security limitations
│   ├── TESTING.md                      manual test checklist
│   └── DEMO_PRESENTATION.md            demo and viva prompts
└── scripts/                            Windows and Linux start scripts
```

## Configuration

Backend configuration is in `backend/src/main/resources/application.properties`:

| Property / variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `7071` | Backend port |
| `SERVER_ADDRESS` | `127.0.0.1` | Localhost-only binding — keep this default |
| `EXECUTION_TIMEOUT_MS` | `10000` | Execution wall-clock timeout |
| `mesh.execution.max-code-chars` | `16000` | Per-stage source limit |
| `mesh.execution.max-output-bytes` | `32768` | Captured console size limit |

Frontend uses Vite's development proxy (`/api` → `http://127.0.0.1:7071`). For a separately deployed frontend, set `VITE_API_BASE` at build time, e.g. `VITE_API_BASE=http://localhost:7071/api`; adjust backend CORS if necessary, but **do not expose the code execution API publicly**.

## Known limitations

1. **Native Image AOT** is deliberately omitted. GraalPy and polyglot native-image support need additional platform-specific work and cannot be assumed to work from this source as-is.
2. **MongoDB** is replaced with **H2**; your saved scripts still persist between runs, with no external database installation.
3. **Ruby** is not in this build. The complete Python + JavaScript pathway is implemented.
4. **No hard memory cap or guest CPU quota**: JVM heap delta is a diagnostic estimate only. The 10-second timeout is best effort and not a denial-of-service proof.
5. Python and JS execution occurs in the same Java process, but **not** with an isolated Native Image sandbox. Run locally with trusted scripts; do not accept arbitrary untrusted users.
6. First-time GraalPy initialization can be slow. Warm up with a simple Python run before performance demonstrations.
7. No dependency binaries or `node_modules` are included in the zip. Maven and npm downloads occur on the first setup; a network connection is needed.

## Attribution

Project concept: Infotact Solutions project brief, “Real-World Applications – Hands-on System Building”, Project 1. This implementation is an independent learning/reference deliverable. See `docs/ARCHITECTURE.md` for what was kept, substituted and intentionally omitted.
