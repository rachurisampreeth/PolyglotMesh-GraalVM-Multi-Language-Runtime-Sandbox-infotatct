# Test Plan and Acceptance Criteria

## Automated tests

From `backend/`:

```text
mvn test
mvn package
```

Tests in `backend/src/test` cover template availability, JavaScript evaluation, Python evaluation, the Java → Python → JavaScript handoff, and denial of Java host-class access. All GraalVM dependencies must be downloaded first. The test timeout is deliberately larger than the web UI default to allow initial GraalPy warmup.

From `frontend/`:

```text
npm install
npm run build
```

This checks that the React/Vite bundle and local Monaco workers can be built. Automated browser interaction tests are **not included**; run the manual checks below.

## Manual acceptance checklist

| Test | Action | Expected result |
| --- | --- | --- |
| API health | Open `http://localhost:7071/api/health` | JSON `status: UP` |
| Frontend startup | `npm run dev` and open Vite URL | Studio UI loads, status `Runtime online` |
| Python language | Open Python data processing template and run | stdout `Computed mean: 25.0`, result `25.0` |
| JavaScript language | Open JS validation template and run | JSON subtotal / positive quantity |
| Pipeline | Open pricing engine and run | stdout for both stages and `approved: true` |
| Data editing | Change unitPrice from 1499 to 2000, quantity from 3 to 2 | discounted price 3600.0 |
| Coupon removed | Change coupon to `NOCOUPON` | calculated price 4000.0 |
| Pipeline trace | Open console `Trace` | Java, Python and JS stages displayed |
| Host access restricted | Open Permission check example and run | Error trying to access `Java.type`, not a successful private property read |
| Script save | Edit and press Save | Script appears under Saved scripts |
| Script reload | Open a different example, then click saved script | Previous code reappears |
| Script update | Reopen a saved script, edit and Save | Same script updates |
| Script delete | Hover saved row, click trash, confirm | Script disappears |
| History | Open Execution history after a run | Recent row with duration, status and timestamp |
| Analytics | Open Performance after several runs | Counts and latency chart use persisted data |
| Backend offline | Stop Java backend and refresh studio | Runtime offline state; run shows actionable error |
| Large output | Run loop producing >32 KB printed text | Captured output is truncated, UI remains responsive |
| Timeout | Run `while True: pass` in Python | Eventually returns cancellation/error; **run only locally** |
| Restart persistence | Stop and restart backend | Saved snippets and metadata remain in H2 |

## Smoke test examples

**Python script**:

```python
result = 2 ** 8
print('Computed:', result)
```

Expected stdout contains `Computed: 256`; result is `256`.

**JavaScript script**:

```javascript
result = JSON.stringify({hello: 'GraalJS', ok: true});
print('JavaScript running inside JVM');
```

Expected stdout contains `JavaScript running inside JVM`; result contains `"ok":true`.

**Pipeline stage A (Python)**:

```python
result = input_data['quantity'] * 5
print('From Python:', result)
```

**Pipeline stage B (JavaScript)**:

```javascript
result = 'JS received ' + python_result;
print(result);
```

With `{ "quantity": 4 }`, expect `JS received 20`.

## Security test notes

A failed `Java.type` call checks one restricted capability only. It is **not** proof of sandbox completeness. The timeout is best effort and hard memory/CPU quotas are absent. Do not test with aggressive memory-exhaustion code on a machine with important unsaved work.

## Local verification record — 24 November 2026

The following backend checks were completed locally:

- `mvn test` completed with **BUILD SUCCESS**.
- `GET /api/health` returned `status: UP`.
- The health response identified GraalVM Polyglot with Python and JavaScript support.
- `POST /api/mock-rest/execute` returned the expected echoed input.
- `POST /api/compare-metrics` returned timing statistics for both execution paths.
- `ComparisonServiceTest` passed.

**Not yet verified in this record:** A fresh frontend `npm run build` and automated browser interaction tests.

These results describe the local development environment and should not be interpreted as proof of production-grade sandbox security.
