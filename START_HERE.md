# 🚀 START HERE — PolyglotMesh

You have a full-stack **Java + GraalVM + React** development studio. It lets you write **Python** and **JavaScript** in a browser and execute them as guest languages in the **same Java process**.

## 3-minute checklist

**Install once:** Java **21+**, Maven **3.9+**, Node.js **20.19+ or 22.12+**. Internet is required for the first Maven/npm dependency download.

**Terminal 1 — backend**

```bat
cd /d C:\Projects\PolyglotMesh\backend
mvn spring-boot:run
```

**Terminal 2 — frontend**

```bat
cd /d C:\Projects\PolyglotMesh\frontend
npm install
npm run dev
```

**Open:** http://localhost:7070 (or the URL shown by Vite).

**Test:** Click **Dynamic pricing engine** → **Run code**. Check the Python output, JavaScript validation and final JSON result in the console. Change a price in **Input context** and run again. Save the script, then open **Execution history** and **Performance**.

**More help:** `docs/WINDOWS_SETUP.md` explains installing tools, setting PATH, running commands and troubleshooting. `docs/DEMO_PRESENTATION.md` gives a 5-minute project demonstration script.

**Build note:** The source was assembled and statically inspected, but not run end-to-end in the packaging environment (external dependency downloads unavailable). Run `mvn test` and `npm run build` locally. This is a **local development demo**, not a production-security sandbox.
