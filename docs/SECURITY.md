# Sandbox Security Review (educational only)

| Control | Implementation | Important limitation |
|---|---|---|
| File I/O | `IOAccess.NONE` | Test actual GraalPy/JS behavior on the target runtime. |
| Host class access | `allowHostClassLookup(false)` | Guest Proxy objects are intentionally limited to safe operations. |
| Host native/process/thread | `allowNativeAccess(false)`, `allowCreateProcess(false)`, `allowCreateThread(false)` | Does not create an OS sandbox. |
| Java host access | `HostAccess.NONE` | `db.find()` and the HashMap audit use explicit Graal proxy APIs, not general Java reflection. |
| Network | `IOAccess.NONE`, no guest direct network | Java host's comparison HTTP client is separate and authorized. |
| Infinite loops | Future timeout + `context.close(true)` + `ResourceLimits.statementLimit` | Certain blocking host operations can resist cancellation; worker saturation requires monitoring. |
| Output spam | bounded `CappedStream` buffers | Truncates only; it does not enforce a total guest heap budget. |
| Concurrency | 4-worker bounded pool, 16 pending | Prevents unlimited queued scripts; users may still overwhelm the JVM without OS quotas. |
| Code size | ≤16,000 UTF-16 characters | Input JSON size limits should additionally be configured at gateway. |
| Audit log | ID, language, status and timing only | Does not persist guest source or input in H2 by default. |

**Known residual risk:** The `huge_list.py` stress case may exhaust Java process memory. Run it only inside isolated resource-limited environment; the QA helper intentionally **skips** it. This exercise does not claim production-safe multi-tenant code execution.

**Recommended production architecture**: authenticated API, per-request ephemeral container/VM or sandboxed OS process, unprivileged user, readonly mount, no network policy, cgroup CPU/RAM/IO limits, process watchdog, strict H2/data access policy, rate limiting, secretless execution, observability, and independent security review.

Any demonstration of a denied action should validate absence of side effects, not merely an error label.
