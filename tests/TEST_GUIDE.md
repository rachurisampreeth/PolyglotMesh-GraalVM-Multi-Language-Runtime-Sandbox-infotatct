# Fazal's 20-Day QA Plan

This folder contains **test definitions**, not fabricated execution results. Record each actual result, screenshot and bug ID after running against a live backend/UI.

1. Install JDK, Node.js, Postman and VS Code; join the GitHub repo.
2. Confirm scripts in `samples/good` and README setup.
3. Import the Postman collection and environment. Test health, execute, data and snippets; fill `test_cases.csv`.
4–5. Run `scripts/api_regression.py` with a local backend to execute ten attack probes. **Be careful with huge allocations**; prefer the dedicated OS/container isolation for this stress case, or skip and record why. Do not run arbitrary code in production or expose the backend publicly.
6–7. Test five positive and five negative inputs in UI; save screenshots in your local evidence folder and record actual errors.
8–10. Populate mid-review deck placeholders with screenshots and actual test outputs; prepare demo script.
11–12. Record ten comparison iterations via `scripts/compare_ten.py`; verify both numeric prices match.
13. Capture JVM/native startup readings using `scripts/startup_benchmark.py` **only after native binary exists**.
14–15. Test metrics live polling, graph, history and security again.
16–17. Verify sample products/orders and H2 Save/Open/Edit/Delete/version conflicts.
18. Run `mvn clean test`, `npm run build`, Postman and UI regression; attach logs.
19–20. Finish README, update both decks, record the demo and submit. Actual upload requires human platform access.

### Expected behavior versus observed

- Guest syntax errors: HTTP 200 with `ERROR` and guest stack; UI shows error in red, collapsible trace.
- Infinite loop: eventually `TIMEOUT` or resource-exhausted status; next request succeeds.
- File/socket/host access: blocked; exact status may be `ERROR` or `SANDBOX_VIOLATION`, depending on guest exception behavior.
- Large allocations: the educational sandbox **does not guarantee containment**; monitor JVM memory and do not run this against an unisolated service.
- Java editor tab: syntax highlighting only; `Run` should show a user-friendly explanation.
- Mock comparison: both adapters must calculate equal numeric prices; neither latency is a valid proof of superior performance.
- Native executable: test **all** APIs, Python+JS, HashMap audit, sandbox and timeout; not only startup.
