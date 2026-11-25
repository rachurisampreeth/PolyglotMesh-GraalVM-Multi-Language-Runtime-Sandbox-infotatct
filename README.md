# PolyglotMesh · 20-Day Full-Stack Project

**Greenfield implementation** of the four-person original plan: GraalVM Polyglot execution, guarded Python + JS sandbox, React/Monaco multi-tab IDE, Spring Boot WebFlux backend, HashMap no-JSON audit, metrics/comparison, MongoMock data, H2 script management, test assets, Postman collection, native-image setup, benchmark tooling and presentation material.

> **Delivery status**: Source files and materials are provided in this ZIP, but builds, native executable, UI rendering, API test outcomes, GitHub team access, screenshots, presentations and actual review submissions **have not been verified or performed in the recipient's environment**. See `IMPLEMENTATION_STATUS.md` and `RELEASE_CHECKLIST.md`. The presence of a feature's code is not proof it works on your machine. The native `.exe` is not included.

## System diagram

```text
React + Vite + Monaco + Recharts (localhost:7070)
          |
          | REST /api/...
          v
Spring Boot 3.5 WebFlux (localhost:7071)
  |-- API controller (boundedElastic for blocking work)
  |-- PolyglotRunner -> shared GraalVM Engine
  |                      + fresh restricted Context per execution
  |                      + bounded worker pool + timeout + statement/output limits
  |                      + GraalPy / GraalJS
  |-- MongoMock (read-only products/orders + guest db.find)
  |-- Price comparison service -> Python execution + HTTP localhost:/mock/price
  |-- H2 + JPA: script CRUD, execution log, comparison history
```

## Requirements

- Windows 10/11, Linux or macOS; **GraalVM JDK 25** (recommended for the selected Polyglot dependencies 25.3.4.1), Maven 3.9+, Node.js 20.19+ or 22.12+, npm.
- For Windows **native image**: GraalVM Native Image + Microsoft Visual Studio C++ Build Tools (MSVC) and Windows SDK; open **x64 Native Tools Command Prompt**. Native builds are also possible on Linux with supported compiler tools. Building GraalPy + Spring Boot into a native image is resource-intensive; full compatibility needs on-machine verification.
- **JDK 21** can compile the source target but may run Polyglot 25.3 in *interpreter fallback* and is **not** a matching optimized runtime. Align build/runtime GraalVM 25 for native/AOT work.

## Run in two terminals

### Terminal 1 (backend)

```bat
cd backend
mvn clean test
mvn spring-boot:run
```

Health: `http://localhost:7071/api/health`. To use another port set environment `PORT`, but update `MOCK_BASE_URL` and `frontend/.env` to match.

### Terminal 2 (frontend)

```bat
cd frontend
npm install
npm run dev
```

Open `http://localhost:7070`.

**Optional**: Copy `frontend/.env.example` to `.env` and set `VITE_API_BASE`. React Monaco code executes only in Python and JavaScript. Java tab is for highlighting and editing, not arbitrary Java compilation.

## Test from CMD

```bat
curl.exe http://localhost:7071/api/health
curl.exe -X POST http://localhost:7071/api/execute -H "Content-Type: application/json" -d "{\"mode\":\"single\",\"language\":\"javascript\",\"code\":\"result = 2 + 2;\"}"
```

Or import `postman/PolyglotMesh.postman_collection.json` and `postman/PolyglotMesh.local.postman_environment.json` into Postman. The test scripts in `samples/good/` and `samples/attacks/` are for a local test instance only.

## Native build / JVM vs native comparison

```bat
cd backend
mvn -Pnative native:compile
```

Run the generated executable from `backend/target/`. On Windows use MSVC x64 native command prompt. See `docs/NATIVE_BUILD.md` (compiler setup, JDK compatibility, standalone fallback, reflection guidance). **Do not treat a successful package/JAR build as a successful native image build.** After building, test Python, JavaScript, audit, timeout and REST endpoints in the binary, then use `scripts/startup_benchmark.py` to gather actual startup readings in `docs/results/startup.csv`.

## Where things are

| Path | What it contains |
|---|---|
| `backend/` | Spring WebFlux, GraalVM Polyglot, H2/JPA, comparison and tests |
| `frontend/` | React, Monaco, Recharts, tabs, JSON input, errors, Save/Load, metrics, MongoMock |
| `fallback-native-js/` | Smaller native JS-only CLI as contingency (not full Spring Boot) |
| `postman/` | Importable collection and local environment |
| `samples/` | Five+ working scripts, ten attack scripts, JSON sample dataset |
| `tests/` | Scenario CSV + documented expected/actual and automation tools |
| `scripts/` | JVM/native startup measurement, API regression, comparison automation |
| `docs/` | Setup, architecture, native steps, security, presentations notes, test instructions |
| `slides/` | Prepared presentation decks with **evidence placeholders** |
| `TASK_BOARD.csv` | 80 original tasks assigned to four members |
| `RELEASE_CHECKLIST.md` | Human and technical acceptance gates |

## Team workflow

Branches `main`, `sampreeth`, `shagun`, `aastha`, `fazal` need to be created in your own GitHub repository; invitations and PRs require the owner's access. `CONTRACTS.md` defines the shared interface. This archive does **not** automatically create a remote, invite members, merge branches, make releases or upload a video.

## Security disclosure

This is an **educational local demo**. GraalVM Context restrictions alone do **not** guarantee complete isolation of malicious code, especially against resource exhaustion or platform vulnerabilities. For genuinely untrusted external workloads, run each execution in a separate OS process/container with filesystem, network, CPU, memory and process limits and access controls. Do not expose port 7071 to the public Internet without a proper security architecture.

## Performance disclosure

The mock endpoint intentionally waits 40 ms (configurable). The adapters implement the same numeric price formula but different runtimes, so results do not prove native/Python is faster than HTTP. JVM vs native startup measurements are not prefilled with invented numbers.
