# Security Model — Read Before Deploying

This is an **educational, local-only** polyglot IDE, **not** a production arbitrary-code execution sandbox.

## Implemented guardrails

- API binds to `127.0.0.1` by default instead of the public network.
- New `Context` for each run; guest variable state is not deliberately reused between executions.
- GraalVM guest context has `HostAccess.NONE`, disallows host class lookup, denies host IO/filesystem/network, process creation, native access and guest-created threads.
- Simple flat input object with a fixed key/value size cap rather than arbitrary Java objects or class references.
- Output is bounded to 32 KiB, source to 16,000 characters per code stage.
- Run jobs have a wall-clock timeout (default 10 seconds), and Java attempts context cancellation on timeout.
- Execution thread pool contains two workers plus a bounded queue of eight pending jobs.

## What this does NOT guarantee

- **Not a hardened security boundary.** The default GraalVM context policy and host permission flags are insufficient alone to safely execute hostile code in a public multi-user service. GraalVM offers stronger sandbox policies and isolate resource options, which require separate implementation and testing.
- **No reliable hard memory or CPU quota.** Java's heap delta is only a rough process-wide observation. A script that consumes huge memory may exhaust the JVM before a timeout is enforced.
- **Cancellation is best effort.** Native work or guest execution away from cancellation safepoints might not terminate instantly. JVM-wide denial of service is still possible.
- **No authentication, TLS, CSRF protection or user accounts.** Anyone able to reach the bound API could issue runs and view/save local scripts.
- **H2 and result previews are not encrypted at rest.** Do not submit private data or credentials through the editor.
- **Sandbox rules may differ between GraalVM versions or engines.** Always run negative tests and validate security settings for the exact dependency versions and platform.

## Appropriate use

Use on your own laptop, in a trusted local classroom demonstration, or in a private development environment for code you wrote yourself. Do **not** change `SERVER_ADDRESS` to `0.0.0.0`, publish port 7071, or host on a shared public server without a separate hardened architecture and security review.

For stronger isolation, investigate the GraalVM sandboxing guide and resource limits, purpose-built worker processes, OS-enforced quotas, permission allowlists, and a deliberately minimal network surface. Do not claim that a blocked `Java.type` attempt proves full security.

Useful reading:

- GraalVM security guide: https://www.graalvm.org/latest/security-guide/sandboxing/
- GraalVM Polyglot embedding guide: https://www.graalvm.org/reference-manual/embed-languages/
- GraalPy JVM embedding: https://graalpy.org/jvm-developers/docs/
