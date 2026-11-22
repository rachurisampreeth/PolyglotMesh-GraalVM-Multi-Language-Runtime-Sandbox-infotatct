# PolyglotMesh — 5-Minute Demonstration and Viva Guide

## One-line project introduction

“PolyglotMesh is a Java-based development platform that runs Python and JavaScript scripts inside the same JVM using GraalVM, with a React Monaco code editor, a REST API, persistence and live execution metrics.”

## Problem statement (30 seconds)

“In a conventional architecture, a Java application may call Python and JavaScript microservices to complete small tasks. That creates extra deployment work and network overhead. For operations that can safely run inside a host process, GraalVM's polyglot runtimes let Java execute guest languages directly.”

**Be precise:** This does not prove GraalVM is faster in every workload. We are demonstrating in-process integration and avoiding interservice REST calls; the browser-to-Java API remains HTTP.

## Architecture explanation (45 seconds)

“React and Monaco form the frontend IDE. When the user presses Run, Spring Boot WebFlux receives the code and input. Java constructs a restricted GraalVM polyglot Context. It passes input as a Java Map wrapped for guest access, runs GraalPy, then forwards the Python result to GraalJS. Java captures output and elapsed time, saves metadata to an H2 file and returns it to the browser.”

Point to the **Execution flow** panel while explaining.

## Live demo (2 minutes)

1. Launch backend and frontend before the presentation; verify **Runtime online**.
2. Select **Dynamic pricing engine**. Show the `pricing.py` tab and its `result` assignment.
3. Show `validate.js`; explain the `python_result` binding provided by the Java backend.
4. Point at **Input context**. Default data: unit price INR 1499, quantity 3, coupon `SAVE10`.
5. Press **Run code**. Explain the expected discounted payable amount INR 4047.30.
6. Point at the console and switch to **Trace** to show the Java, Python, JS stages.
7. Edit JSON to `"quantity": 2` and run again; the amount changes.
8. Press Save; open Saved scripts. Then show Execution history and Performance.

## Context restriction demo (30 seconds)

Open **Permission check** and run. It tries to read a Java host class from JavaScript. The expected error demonstrates that host class lookup is disabled. **Do not describe this as proof against all attacks**; it is one guardrail in a local demonstration.

## Technical highlights (30 seconds)

- `Context.newBuilder(...).allowHostAccess(HostAccess.NONE)` creates a restricted guest context.
- `ProxyObject.fromMap` sends data across language boundaries.
- `context.getBindings("python").getMember("result")` reads the Python output.
- `context.getBindings("js").putMember("python_result", ...)` passes it to JS.
- `Spring Boot WebFlux + Reactor boundedElastic` handles HTTP without running guest code on the event loop.
- `H2 + JPA` retains user-created scripts and execution metadata.

## Viva: likely questions

**Q: What is GraalVM?**
A: A JVM ecosystem with a polyglot embedding API that can execute supported guest languages such as GraalPy and GraalJS inside a Java host process.

**Q: What is a polyglot Context?**
A: An object describing an execution environment and its permissions, used to evaluate guest code and exchange bindings.

**Q: Where does Java communicate with Python?**
A: In `PolyglotExecutionService.java` through `context.getBindings("python")` and `context.eval("python", source)`.

**Q: Are Python and JavaScript running in separate Docker containers?**
A: No. Both execute within the Java process through GraalVM guest language runtimes.

**Q: Why choose WebFlux?**
A: To expose a reactive API; blocking execution and persistence are moved to elastic worker threads, while guest code uses a bounded executor.

**Q: Why H2 instead of MongoDB?**
A: H2 lets the full create/save/load/delete workflow run without a separate database service. MongoDB was in the original project plan but is not necessary to demonstrate the key runtime functionality.

**Q: What about Ruby and GraalVM Native Image?**
A: Those are future work, not part of the runnable core here. Python and JavaScript already establish the multi-language runtime concept.

**Q: Is your sandbox secure enough for arbitrary public users?**
A: No. It has host access restrictions, timeouts and output caps, but not hardened isolate/process boundaries or hard CPU/memory quotas; it is a local educational prototype.

**Q: What is stored in the database?**
A: Saved scripts and execution metadata (success/failure, duration, time, result preview). Full stdout and unsaved input data are not persisted in execution history.

**Q: Can you guarantee zero latency?**
A: No. The goal is avoiding a *network hop between guest operations*, not making execution time zero. The measured timing includes compilation/warmup and other overhead.

## Final review: what is completed?

**Core completed in source:** Java WebFlux API, GraalVM GraalPy/GraalJS integration, Java↔Python↔JavaScript pipeline, restricted Context, React + Monaco editor, run console, API templates, H2 persistence, run history and metrics.

**Clearly scoped out:** Ruby, native-image compilation, MongoDB, hardened untrusted-code multitenant security, and a rigorous comparative benchmark. You can present this as a substantial working prototype once you run the checks listed in `docs/TESTING.md`.
