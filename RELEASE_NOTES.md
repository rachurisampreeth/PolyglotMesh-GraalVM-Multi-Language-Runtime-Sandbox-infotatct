# Release notes — v1.0 (November 22, 2026)

## Included in the archive

- Spring Boot 3/WebFlux Java 21 backend with GraalVM Polyglot/GraalPy/GraalJS dependencies.
- In-process Java → Python → JavaScript pipeline with primitive input-data binding.
- React 19 + Monaco Editor multi-tab IDE and responsive dark-themed dashboard.
- Local H2 script save/reload/delete, execution history, real end-to-end latency metrics.
- Restrictive host permissions, best-effort cancellation and capped output.
- Windows startup scripts, developer documentation, API docs and automated Java test source.

## Checks performed while packaging

- Java compiler parser checked all Java source files (syntax only).
- TypeScript compiler parser checked JSX and JS syntax (syntax only).
- `pom.xml` XML and frontend `package.json` JSON syntax checked.
- ZIP contents and archive CRC integrity checked.

## Not verified in packaging environment

- Full Java Maven compile / JUnit integration tests / actual GraalPy and GraalJS evaluation.
- npm install, Vite production build, browser interaction, OS-specific installations.

Network access to Maven/npm registries was not available in the packaging runtime. **Run** `mvn test`, `mvn package`, `npm install`, and `npm run build` on your own Windows machine before submitting the project as a verified build. A successful syntax check is not a substitute for an end-to-end run.

## Deliberate scope differences from the brief

Ruby, GraalVM native-image compilation, MongoDB and a production-hardened multi-user sandbox are omitted. H2 is used for saved scripts instead. The primary two-language execution workflow and surrounding IDE/backend/database features are implemented in source.
