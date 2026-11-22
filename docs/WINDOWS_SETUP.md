# PolyglotMesh — Complete Windows 10/11 Installation and Usage Guide

This guide assumes you have extracted the zip to `C:\Projects\PolyglotMesh`. Substitute your actual folder path wherever needed. The commands work in **Command Prompt (cmd.exe)**; PowerShell commands are marked separately.

## 1. What you will run

| Component | Technology | Default local URL |
| --- | --- | --- |
| Frontend website | React + Monaco Editor + Vite | `http://localhost:7070` |
| Backend API | Java 21 + Spring Boot 3 WebFlux | `http://localhost:7071` |
| GraalPy/GraalJS | Library within Java process | No separate port |
| Persistent database | Embedded H2 file | No separate port |

There is **no Docker, MongoDB, Python installation, or JavaScript server required** for guest language execution. Maven downloads the embedded language runtimes; Node.js is only used to run/build the web frontend.

## 2. Install and verify prerequisites

### Java 21 or later

Use your existing Java 21 installation if present. In CMD:

```bat
java -version
javac -version
```

If the Java version is older than 21 or commands are not recognized, install a JDK (for example Eclipse Temurin 21) and set `JAVA_HOME` and `%JAVA_HOME%\bin` in your Windows `PATH`. Open a **new** Command Prompt after changing PATH.

**Optional for better polyglot performance:** install a matching GraalVM JDK and set `JAVA_HOME` to its directory. The source also supports ordinary JDKs but compiler/JIT compatibility varies; interpreter mode may be much slower. Do not claim sub-millisecond performance without measuring on your machine.

### Maven

Check:

```bat
mvn -version
```

If `mvn` is not recognized, install Apache Maven 3.9 or later. Extract Maven (for example `C:\Tools\apache-maven-3.9.x`) and add its `bin` folder to Windows PATH. Verify again in a **new** terminal. The project deliberately does not include an unverified Maven wrapper binary.

### Node.js + npm

Check:

```bat
node -v
npm -v
```

Use Node 20.19+ or Node 22.12+ for Vite 6 and its dependencies. If unavailable, install the current Node LTS release and reopen CMD.

### Network requirements

First backend launch downloads Spring Boot, GraalPy, GraalJS and H2 dependencies from Maven Central. The Python runtime can be a **large** download. First frontend launch downloads React, Vite, Monaco and chart libraries from npm. Subsequent runs use local caches.

## 3. Extract and inspect

Unzip the deliverable to `C:\Projects\PolyglotMesh`. Confirm:

```bat
cd /d C:\Projects\PolyglotMesh

dir
```

You should see the `backend`, `frontend`, `docs`, and `scripts` directories.

## 4. Start the backend

Open **CMD window #1** and enter:

```bat
cd /d C:\Projects\PolyglotMesh\backend
mvn spring-boot:run
```

Leave the window open. On a successful launch, Spring Boot logs that the HTTP server is running at **127.0.0.1:7071**. A data directory is created in the backend folder automatically when H2 initializes.

Open this address in a browser:

```text
http://localhost:7071/api/health
```

The JSON response should look similar to:

```json
{
  "status": "UP",
  "service": "PolyglotMesh",
  "runtime": "GraalVM Polyglot",
  "languages": ["python", "javascript"],
  "security": "restricted-local-demo",
  "timestamp": "2026-11-22T...Z"
}
```

The timestamp changes on each request.

## 5. Start the website

Open **CMD window #2**:

```bat
cd /d C:\Projects\PolyglotMesh\frontend
npm install
npm run dev
```

The development server prints a **Local** URL. It should be `http://127.0.0.1:7070` or `http://localhost:7070`. The frontend requires port **7070** to be free. If it is occupied, close the process using that port and restart Vite. The website should show **Runtime online** in the upper-right corner.

Vite forwards frontend `/api` requests to the Java backend automatically; no CORS/URL edits are necessary for this default setup.

## 6. Run the project's main use case

The default page opens on **Dynamic pricing engine**, a pipeline with a Java host, Python pricing logic and JavaScript validation.

**Inspect the code:** The **pricing.py** editor tab contains the pricing calculation. The **validate.js** tab contains the validation rule. The **Input context** panel provides data injected by the Java backend:

```json
{
  "product": "Starter Plan",
  "unitPrice": 1499,
  "quantity": 3,
  "coupon": "SAVE10"
}
```

**Click Run code.** The execution happens on the **Java backend**, not in the browser. Expected logic:

1. Java receives the request, creates a Java Map and a restricted GraalVM Context.
2. GraalPy evaluates `1499 * 3 = 4497`, gives 10% discount, and sets `result = 4047.3`.
3. Java reads the Python `result` binding and forwards it to GraalJS as `python_result`.
4. JavaScript checks the amount is positive and under 100000. It produces a JSON result string with `approved: true`.
5. Java records timing/status metadata in H2 and returns the console output/result to the browser.

**Change values:** Modify `unitPrice` to `2000`, `quantity` to `2`, and click Run again; the expected discounted price is `3600.0`. Change coupon to `NOCOUPON`; expected price is `4000.0`.

**View trace:** The console's **Trace** tab shows the Java binding, Python stage and JavaScript stage timings. Timing includes runtime costs and can vary greatly by machine or warm-up state.

## 7. Other examples

In the left sidebar under Quick Start:

- **Python data processing** — calculates the mean of a Python list and prints `25.0`.
- **JavaScript validation** — reads Java-supplied values and returns a JSON subtotal.
- **Permission check** — deliberately attempts to access a Java class; it should produce an error because host class lookup is disabled. A failure here is the **expected** outcome.

In **Single** mode, click `main.py` or `main.js` to change the editor language. In **Pipeline** mode, the two tabs represent different steps of one API call. Custom scripts should assign a top-level variable named `result` to display a structured return value; `print(...)` messages appear in the console.

## 8. Save, load, edit and delete scripts

Press **Save** at the editor's upper-right. Enter any title and confirm. The code is saved in the backend's embedded H2 database; its title appears under **Saved scripts** in the left sidebar. Click it to reload the code later. Click Save again to update the same script. Hover over the title to reveal the delete button. Script deletion is permanent.

**Important:** Script input JSON is not saved with custom snippets in this version. The editor starts with example input; customize it before running. Example templates load their own input context.

## 9. Review metrics

Open **Execution history** to inspect recorded run ID metadata, language, mode, duration, status and short result preview. Open **Performance** to see counts, success rate, average duration of the latest 40 runs, and a latency chart. Data is stored locally in `backend/data/`.

Metrics are **wall-clock time** and approximate **JVM-wide heap change**, not isolated per-language CPU or heap quotas. GraalPy cold starts often skew the first run. Avoid comparing one cold Python run to one warm JavaScript run as though they were equivalent.

## 10. Build distributable output

**Backend**:

```bat
cd /d C:\Projects\PolyglotMesh\backend
mvn clean test
mvn package
java -jar target\polyglotmesh-backend-1.0.0.jar
```

**Frontend**:

```bat
cd /d C:\Projects\PolyglotMesh\frontend
npm run build
npm run preview
```

Vite creates `frontend/dist/`. The preview command prints a local address (often port 4173). For a preview on a different port, use `VITE_API_BASE` when building, or use a local reverse proxy, because the Vite *development* proxy is only enabled for `npm run dev`. The standard recommended demonstration setup is `npm run dev` + `mvn spring-boot:run`.

## 11. Shortcut scripts

You may double-click `scripts/windows-start-all.bat` to open the backend and frontend in separate terminal windows. Or run `scripts/windows-start-backend.bat` and `scripts/windows-start-frontend.bat` individually. The frontend launcher runs `npm install` if `node_modules` is missing; the backend launcher expects Maven in PATH. The shortcuts do not change Windows environment variables.

## 12. Common errors

### `mvn is not recognized`

Maven is not installed or its `bin` folder is missing from Windows PATH. Install it and restart CMD; rerun `mvn -version`.

### Java version 17 / unsupported class file version

The backend uses Java 21. Ensure `java -version`, `javac -version` and `mvn -version` all point to a JDK 21+ installation. Reset `JAVA_HOME` if Maven uses another JDK.

### `Port 7071 was already in use`

Another backend is running on port 7071. Stop that process. Alternatively, set `PORT=8082` before starting the backend and edit `frontend/vite.config.js` proxy target to `http://127.0.0.1:8082`.

### `Runtime offline`

Check backend terminal for errors and open `http://localhost:7071/api/health` directly. The website can render without a backend, but **executing or saving code requires** a running backend. If you changed the server port, update Vite proxy configuration.

### GraalPy initialization error / Maven missing artifacts

Make sure the Maven download finished. Check online connectivity and try `mvn -U clean package`. If a Java VM/compiler compatibility warning appears, prefer the **matching GraalVM JDK** version. Try a JavaScript example first, then Python. Do not disable restricted host permissions merely to hide an error; understand the missing guest capability.

### First Python run is slow or times out

The first GraalPy invocation initializes a sizeable runtime. You may increase `EXECUTION_TIMEOUT_MS` for local development, for example before starting the backend:

```bat
set EXECUTION_TIMEOUT_MS=20000
mvn spring-boot:run
```

This is **not** a stronger security setup; do not run unknown scripts.

### `npm install` fails

Verify Node.js and npm versions, then retry with internet access. If package dependencies were only partly downloaded, remove `frontend/node_modules` and run `npm install` again.

### Monaco editor is blank

Inspect the browser's DevTools console for failed imports/workers and restart Vite. Monaco and its worker files are bundled by Vite; it does not need a Monaco CDN at runtime. Google Fonts may fail offline, but system fallback fonts render normally.

### H2 database file is locked

Do not run two copies of the Java backend against the same `backend/data/` directory. Stop duplicates. For a full local reset, shut down the backend, delete `backend/data/`, and restart (this **deletes all saved scripts/history**).

## 13. Practical development workflow

1. Write Python in `pricing.py` and assign `result`.
2. Write JavaScript in `validate.js`; use `python_result` provided by Java, then assign JS `result`.
3. Set sample data in Input context.
4. Run and fix errors from the console. Errors do not crash the whole IDE.
5. Save finished snippets and review history/analytics.
6. If modifying backend Java files, restart Maven; Vite updates the React frontend automatically.
7. Run `mvn test` and `npm run build` before a demonstration.

**For security considerations:** `docs/SECURITY.md`. **For the exact JSON API:** `docs/API.md`. **For a ready demo script:** `docs/DEMO_PRESENTATION.md`.
