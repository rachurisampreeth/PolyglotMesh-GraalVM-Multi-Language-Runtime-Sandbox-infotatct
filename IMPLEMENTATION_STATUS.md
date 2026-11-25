# Implementation readiness — code versus proof

**THIS IS A SOURCE DELIVERY, NOT A 100% VALIDATED RELEASE.**

| Subsystem / source task | Source provided | Tested here |
|---|---|---|
| Spring Boot WebFlux REST API | Yes | No (Maven unavailable in build environment) |
| GraalVM Python/JS + output capture | Yes | No |
| Restricted contexts + timeout + statement limit + capped output | Yes | No |
| Shared Engine/fresh Context | Yes | No |
| Java Map audit and guest `db.find` | Yes | No |
| Mock REST equivalent pricing and comparison history | Yes | No |
| Products/orders data | Yes | No |
| H2/JPA snippet CRUD + optimistic version check | Yes | No |
| React/Vite/Monaco UI | Yes | JSX parse only, browser build not verified |
| Console errors/spinner/JSON input/tabs/Ctrl+Enter | Yes | JSX parse only |
| Recharts metrics polling, Save/Load, data panel | Yes | JSX parse only |
| Postman collection, samples, docs, test matrix | Yes | JSON/CSV syntax validated |
| Spring native-image Maven/AOT configuration | Yes | Native compilation not executed |
| Full Windows/Linux native executable | Not included | Not produced |
| Smaller JS-only native fallback source | Yes | Native compilation not executed |
| Real benchmarks, screenshots, recorded video, actual GitHub merges/tags | Templates/scripts only | Not performed |
| Slides and task board | Prepared | Requires real evidence/rehearsal |

## Known distinctions from original schedule

- **Java** tab does not compile Java scripts; requested only syntax highlighting in frontend.
- Python and JavaScript execute in GraalVM; guest objects get a strictly limited `db.find` proxy.
- The original comparison requirement is implemented as the same pricing formula via Python execution versus local HTTP Java service, with transparent warning about unfair timing variables.
- An `audit` execution mode rather than an extra endpoint demonstrates Java HashMap readback; API contract documents it.
- Status mapping uses uppercase `OK`, `ERROR`, `TIMEOUT`, `SANDBOX_VIOLATION`; guest runtime errors may be classed as `ERROR` even if blocked.
- Full public-facing zero-trust isolation requires stronger OS level controls than are implemented here.

## What remains to reach true 100%

Build, run, test and fix any issues reported on matching GraalVM; generate and verify a **full** Spring Boot native image; record real startup metrics; demonstrate all 80 tasks with screenshots and peer review; complete manual GitHub invitations/merges/tags and final presentation/video submission. Only then mark the plan 100%.
