# First-day setup for all four members

1. Install GraalVM JDK 25 and set `JAVA_HOME` to GraalVM path, add `<GRAALVM_HOME>/bin` to PATH. Check `java -version` and `native-image --version`.
2. Install Apache Maven 3.9+; `mvn -version` must show the same GraalVM JDK.
3. Install Node.js 22.12+ and npm. Verify `node --version`, `npm --version`.
4. Install VS Code (full Visual Studio IDE not required). For Windows Native Image, install **Visual Studio Build Tools C++** (separate from VS Code).
5. Install Postman; import two JSON files from `postman/`.
6. Create/accept GitHub repo invitations and clone your team repository. Branches are `main`, `sampreeth`, `aastha`, `shagun`, `fazal`. Keep PR reviews mandatory for merging.
7. Assign task tracker from `TASK_BOARD.csv` in GitHub Projects, Trello, spreadsheet or equivalent; fill the evidence/status columns.
8. Build backend and frontend as in `README.md`, verify UI/health, run sample scripts.

Do not commit `.env`, secrets or H2 database contents. H2 test data lives under `backend/data/`.
