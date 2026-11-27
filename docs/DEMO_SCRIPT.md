# Mid-Review and Final Demo Script (presenter's guide)

**Do not claim a demonstration succeeded until tested on the recipient's machine.**

1. Open frontend and backend terminal; show `/api/health` is UP and the React backend status indicator is green.
2. Open Python tab: print `Hello`, run, explain real backend execution and console output.
3. Open JavaScript tab; run arithmetic; point out per-language code retention, highlighting and Ctrl+Enter. Java tab is editor-only.
4. Enter invalid Python (`1 / 0`) and show error color and guest stack expansion.
5. Run `open('blocked.txt','w')`; show no file is created and the guest is denied.
6. Submit `while True: pass`; demonstrate timeout/statement limit and recovery with normal code.
7. Select **HashMap Audit**: show the Java readback value changed from `1000` to `1500` via Python using a shared Map proxy, with no JSON serialization between Java and Python.
8. Select **Run Pricing Pipeline**: Java supplies `p2` product, Python computes discounted price, Java verifies it, JS checks stock and nonnegative price.
9. Open **MongoMock Explorer**: show products/orders and `db.find('products')` template.
10. Open **Script Library**: Save a snippet, load, edit, save with version, delete; show persisted H2 data and version conflict if needed.
11. Open **Execution Analytics**: run 10-sample comparison; mention mock REST delay bias, matching price values, polling and saved comparison history.
12. For Native Image: if **actual full native build succeeded**, show log, executable, test commands and recorded startup CSV; otherwise explicitly explain blockers and show the JS-only fallback labeled *contingency*, not complete native Spring app.
13. Wrap up with test-case sheet, bug register, release checklist and remaining manual review tasks.

### Video capture checklist

Record your own screen, hide names/paths/tokens, demonstrate the program from scratch, ensure visible port 7071 and 7070, show genuine output and test results, upload to an approved platform yourself. Do not upload passwords or private data.
