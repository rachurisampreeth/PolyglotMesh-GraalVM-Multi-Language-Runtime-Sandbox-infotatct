import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Editor from '@monaco-editor/react';
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis, Tooltip, CartesianGrid, Legend } from 'recharts';
import { api, post, put, del } from './api';

const starter = [
  { id: 'py', name: 'main.py', language: 'python', code: "print('Hello, Python!')\nresult = 5 + 7" },
  { id: 'js', name: 'main.js', language: 'javascript', code: "print('Hello, JavaScript!');\nresult = [2, 4, 6].reduce((a,b) => a+b, 0);" },
  { id: 'java', name: 'Example.java', language: 'java', code: '// Java syntax tab is editor-only.\npublic class Example { public static void main(String[] args) { System.out.println("Hello"); } }' },
];
const loadTabs = () => { try { const x = JSON.parse(localStorage.getItem('polyglotmesh.tabs')); return Array.isArray(x) && x.length ? x : starter; } catch { return starter; } };
const languageLabel = { python: 'Python', javascript: 'JavaScript', java: 'Java' };
const statusIcon = { OK: '✓', ERROR: '!', TIMEOUT: '⏱', SANDBOX_VIOLATION: '⛨' };
const fmt = (v) => typeof v === 'number' ? v.toFixed(2) : '—';

function Metric({ caption, value, hint }) {
  return <div className="metric"><div className="metric-label">{caption}</div><div className="metric-value">{value}</div><div className="metric-hint">{hint}</div></div>;
}
function CodePanel({ tabs, activeId, setActiveId, updateCode, addTab, renameTab, removeTab, editorRef }) {
  const active = tabs.find(t => t.id === activeId) || tabs[0];
  return <section className="pane editor-pane">
    <div className="pane-bar"><span className="pane-heading">CODE WORKSPACE</span><div className="pane-actions"><button onClick={renameTab} title="Rename tab">Rename</button><button onClick={removeTab} title="Close tab">×</button><button className="plus" onClick={addTab} title="New tab">＋ New</button></div></div>
    <div className="tabs" role="tablist" aria-label="Editor tabs">{tabs.map(t => <button key={t.id} role="tab" aria-selected={t.id === activeId} className={`tab ${t.id === activeId ? 'active' : ''}`} onClick={() => setActiveId(t.id)}><span className={`lang-dot ${t.language}`}></span>{t.name}</button>)}</div>
    <div className="editor-mount"><Editor path={active.name + active.id} language={active.language} theme="vs-dark" value={active.code} onChange={v => updateCode(v || '')} onMount={editor => { editorRef.current = editor; }} options={{ minimap: { enabled: false }, fontSize: 14, automaticLayout: true, wordWrap: 'on', padding: { top: 18 }, scrollBeyondLastLine: false, fontFamily: "'Cascadia Code', Consolas, monospace" }} /></div>
    <div className="editor-footer"><span>{languageLabel[active.language]} · UTF-8</span><span>Ctrl + Enter to run</span></div>
  </section>;
}
function ConsolePanel({ response, running, failure, setShowStack, showStack }) {
  const status = response?.status;
  return <section className="pane console-pane"><div className="pane-bar"><span className="pane-heading">CONSOLE OUTPUT</span><span className={`status ${status ? status.toLowerCase() : 'idle'}`}>{status ? `${statusIcon[status] || '•'} ${status}` : '● Ready'}</span></div>
    <div className="console-body" aria-live="polite">{running ? <div className="info"><span className="spinner"/> Running your code…</div> : failure ? <pre className="error-text">{failure}</pre> : !response ? <div className="empty-panel">Run your first snippet to see its output here.<br/><small>Python and JavaScript execute in isolated GraalVM contexts.</small></div> : <>
      {!!response.output && <pre className="stdout">{response.output}</pre>}
      {response.result !== null && response.result !== undefined && <div className="result-line"><span>result</span><pre>{typeof response.result === 'object' ? JSON.stringify(response.result, null, 2) : String(response.result)}</pre></div>}
      {response.error && <div className="error-box"><strong>{response.error}</strong>{response.stack?.length > 0 && <><button onClick={() => setShowStack(!showStack)}>{showStack ? 'Hide' : 'Show'} guest stack trace ▾</button>{showStack && <pre>{response.stack.join('\n')}</pre>}</>}</div>}
      {!!response.steps?.length && <div className="execution-steps"><b>Execution steps</b>{response.steps.map((s,i) => <div key={i}>{s.language} <span>{s.status} · {s.elapsedMs}ms</span></div>)}</div>}
      <div className="runtime-meta">Execution time: {response.elapsedMs} ms</div>
    </>}</div>
  </section>;
}

export default function App() {
  const [tabs, setTabs] = useState(loadTabs);
  const [activeId, setActiveId] = useState('py');
  const [section, setSection] = useState('studio');
  const [jsonInput, setJsonInput] = useState('{"productId":"p2","quantity":2,"discountPercent":10}');
  const [response, setResponse] = useState(null);
  const [running, setRunning] = useState(false);
  const [failure, setFailure] = useState('');
  const [healthy, setHealthy] = useState(null);
  const [showStack, setShowStack] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [snippets, setSnippets] = useState([]);
  const [loadedSnippet, setLoadedSnippet] = useState({});
  const [stats, setStats] = useState(null);
  const [history, setHistory] = useState([]);
  const [compareResult, setCompareResult] = useState(null);
  const [comparisonBusy, setComparisonBusy] = useState(false);
  const [comparisonIterations, setComparisonIterations] = useState(10);
  const [dataTab, setDataTab] = useState('products');
  const [records, setRecords] = useState([]);
  const [dataError, setDataError] = useState('');
  const editorRef = useRef(null);
  const active = tabs.find(t => t.id === activeId) || tabs[0];

  useEffect(() => { localStorage.setItem('polyglotmesh.tabs', JSON.stringify(tabs)); }, [tabs]);
  const fetchSnippets = useCallback(() => api('/api/snippets').then(setSnippets).catch(() => {}), []);
  const refresh = useCallback(() => {
    api('/api/health').then(() => setHealthy(true)).catch(() => setHealthy(false));
    api('/api/metrics').then(setStats).catch(() => {});
    api('/api/metrics/history').then(setHistory).catch(() => {});
  }, []);
  useEffect(() => {
    refresh(); fetchSnippets();
    api('/api/templates').then(setTemplates).catch(() => {});
    const timer = setInterval(refresh, 5000);
    return () => clearInterval(timer);
  }, [refresh, fetchSnippets]);
  useEffect(() => {
    api(`/api/data/${dataTab}`).then(rows => { setRecords(rows); setDataError(''); }).catch(e => setDataError(e.message));
  }, [dataTab, section]);
  const updateCode = (code) => setTabs(prev => prev.map(t => t.id === activeId ? { ...t, code } : t));
  const addTab = () => {
    const language = prompt('Choose language: python, javascript or java', 'python');
    if (!language || !['python', 'javascript', 'java'].includes(language.toLowerCase().trim())) return;
    const lang = language.toLowerCase().trim();
    const name = prompt('File name', lang === 'python' ? 'untitled.py' : lang === 'java' ? 'Untitled.java' : 'untitled.js');
    if (!name) return;
    const id = crypto.randomUUID();
    setTabs(old => [...old, { id, name: name.slice(0, 70), language: lang, code: '' }]); setActiveId(id);
  };
  const renameTab = () => { const name = prompt('New tab name', active.name); if (name) setTabs(old => old.map(t => t.id === activeId ? { ...t, name: name.slice(0, 70) } : t)); };
  const removeTab = () => { if (tabs.length === 1) return; const next = tabs.filter(t => t.id !== activeId); setTabs(next); setActiveId(next[0].id); };
  const run = useCallback(async (mode = 'single') => {
    const tab = tabs.find(t => t.id === activeId) || tabs[0];
    if (mode === 'single' && tab.language === 'java') { setFailure('Java is available for editing and highlighting only. Use Python or JavaScript for execution.'); return; }
    let input;
    try { input = JSON.parse(jsonInput); if (!input || Array.isArray(input) || typeof input !== 'object') throw new Error('Must be an object'); }
    catch (e) { setFailure(`Invalid JSON input: ${e.message}`); return; }
    if (mode === 'single' && !tab.code.trim()) { setFailure('Enter some code before running.'); return; }
    setRunning(true); setFailure(''); setResponse(null); setShowStack(false);
    try {
      const payload = mode === 'single' ? { mode, language: tab.language, code: tab.code, input } : { mode, input };
      setResponse(await post('/api/execute', payload)); refresh();
    } catch (e) { setFailure(e.message); setHealthy(false); }
    finally { setRunning(false); }
  }, [tabs, activeId, jsonInput, refresh]);
  useEffect(() => {
    const keydown = e => { if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); run(); } };
    window.addEventListener('keydown', keydown);
    return () => window.removeEventListener('keydown', keydown);
  }, [run]);
  const save = async () => {
    try {
      const name = prompt('Save this snippet as', active.name); if (!name) return;
      const payload = { title: name, language: active.language, code: active.code };
      const editing = loadedSnippet[active.id];
      const saved = editing ? await put(`/api/snippets/${editing.id}`, { ...payload, expectedVersion: editing.version }) : await post('/api/snippets', payload);
      setLoadedSnippet(prev => ({ ...prev, [active.id]: saved })); await fetchSnippets(); alert('Saved successfully');
    } catch (e) { alert(`Could not save: ${e.message}`); }
  };
  const load = s => {
    const tab = { id: crypto.randomUUID(), name: s.title, language: s.language, code: s.code };
    setTabs(prev => [...prev, tab]); setLoadedSnippet(prev => ({ ...prev, [tab.id]: s })); setActiveId(tab.id); setSection('studio');
  };
  const deleteSnippet = async s => {
    if (!confirm(`Delete ${s.title}?`)) return;
    try { await del(`/api/snippets/${s.id}`); setLoadedSnippet(prev => Object.fromEntries(Object.entries(prev).filter(([, val]) => val.id !== s.id))); fetchSnippets(); }
    catch (e) { alert(e.message); }
  };
  const compare = async () => {
    let payload;
    try { payload = JSON.parse(jsonInput); }
    catch { alert('Enter valid pricing JSON in the Input panel first'); return; }
    setComparisonBusy(true);
    try { const result = await post('/api/compare-metrics', { pricing: payload, iterations: Number(comparisonIterations) }); setCompareResult(result); refresh(); }
    catch (e) { alert(`Comparison failed: ${e.message}`); }
    finally { setComparisonBusy(false); }
  };
  const chart = useMemo(() => compareResult ? compareResult.metrics.map(m => ({ name: m.adapter, average: +m.averageMs.toFixed(2), min: +m.minMs.toFixed(2), max: +m.maxMs.toFixed(2) })) : [], [compareResult]);
  return <div className="app-shell">
    <aside className="sidebar"><div className="brand"><span className="brand-mark">◈</span><div><b>POLYGLOT<span>MESH</span></b><small>RUNTIME STUDIO</small></div></div>
      <div className="sidebar-label">WORKSPACE</div>
      {[['studio','⌘','Code Studio'], ['metrics','▥','Analytics'], ['data','▤','MongoMock Data'], ['library','▧','Script Library']].map(([key, icon, name]) => <button key={key} className={`nav-item ${section === key ? 'selected' : ''}`} onClick={() => setSection(key)}><span>{icon}</span>{name}</button>)}
      <div className="sidebar-label sections-label">QUICK TEMPLATES</div>
      {templates.slice(0, 6).map(t => <button className="template-item" key={t.id} onClick={() => { const id = crypto.randomUUID(); setTabs(prev => [...prev, { id, name: t.title + (t.language === 'python' ? '.py' : '.js'), language: t.language, code: t.code }]); setActiveId(id); setSection('studio'); }}>{t.title}</button>)}
      <div className="sidebar-label sections-label">SAVED SCRIPTS</div>
      {snippets.slice(0, 4).map(s => <button className="template-item" key={s.id} title={`Open version ${s.version}`} onClick={() => load(s)}>▧ {s.title}</button>)}
      <div className="sidebar-bottom"><div className="server-dot"><span className={healthy ? 'online' : 'offline'}></span>{healthy === null ? 'Checking backend' : healthy ? 'Backend connected' : 'Backend offline'}</div><small>GraalVM · Spring WebFlux</small></div>
    </aside>
    <main className="main-content">
      <header className="topbar"><div><div className="crumb">PROJECT / POLYGLOT EXECUTION</div><h1>{section === 'studio' ? 'Code Studio' : section === 'metrics' ? 'Execution Analytics' : section === 'data' ? 'MongoMock Explorer' : 'Script Library'}</h1></div><div className="header-right"><span className="version-pill">v1.0 · Lab</span><span className="avatar">PM</span></div></header>
      {healthy === false && <div className="backend-warning">⚠ Backend is unreachable at localhost:7071. Start <code>mvn spring-boot:run</code> from the backend folder.</div>}
      {section === 'studio' && <>
        <div className="hero-strip"><div><strong>Write in any language. Execute safely.</strong><p>Python and JavaScript execution · Java syntax highlighting · restricted contexts · hard limits</p></div><div className="hero-tag">LIVE SANDBOX</div></div>
        <div className="studio-toolbar"><div className="toolbar-caption">EDITOR & EXECUTION</div><div className="action-group"><button className="secondary" onClick={() => run('audit')}>HashMap Audit</button><button className="secondary" onClick={() => run('pipeline')}>Run Pricing Pipeline</button><button className="secondary" onClick={save}>Save Script</button><button className="primary" disabled={running} onClick={() => run()}>{running ? '◌ Running…' : '▶ Run Code'}</button></div></div>
        <div className="split"><CodePanel {...{ tabs, activeId, setActiveId, updateCode, addTab, renameTab, removeTab, editorRef }}/><ConsolePanel {...{ response, running, failure, setShowStack, showStack }}/></div>
        <section className="input-panel"><div className="pane-bar"><span className="pane-heading">INPUT DATA <span className="tiny-note">JSON OBJECT</span></span><span className="muted">Available as <code>input</code> in guest scripts</span></div><textarea aria-label="JSON input data" value={jsonInput} onChange={e => setJsonInput(e.target.value)} spellCheck={false}/></section>
      </>}
      {section === 'metrics' && <>
        <div className="metric-grid"><Metric caption="RECENT EXECUTIONS" value={stats?.sampleSize ?? '—'} hint="Latest 100 runs"/><Metric caption="SUCCESS RATE" value={stats ? `${fmt(stats.successRate)}%` : '—'} hint="Within recorded sample"/><Metric caption="AVG EXECUTION" value={stats ? `${fmt(stats.averageMs)} ms` : '—'} hint="Recent sample"/><Metric caption="COMPARISONS" value={history.length} hint="Saved benchmark records"/></div>
        <section className="card"><div className="card-top"><div><h2>Adapter Comparison</h2><p>Same price formula in GraalPy and local Java HTTP mock. Results vary with warm-up, delay and overhead.</p></div><div className="action-group"><label>Iterations <input aria-label="Comparison iterations" type="number" min="1" max="20" value={comparisonIterations} onChange={e => setComparisonIterations(e.target.value)} /></label><button className="primary" onClick={compare} disabled={comparisonBusy}>{comparisonBusy ? 'Running…' : '▶ Compare'}</button></div></div>
          {chart.length ? <><div className="chart-area"><ResponsiveContainer width="100%" height="100%"><BarChart data={chart} margin={{top:20, right:30, bottom:15,left:0}}><CartesianGrid strokeDasharray="3 3" stroke="#1f3547"/><XAxis dataKey="name" stroke="#8da4ba"/><YAxis stroke="#8da4ba" unit="ms"/><Tooltip contentStyle={{background:'#102335',border:'1px solid #29445b',color:'#fff'}}/><Legend/><Bar dataKey="average" name="Average (ms)" fill="#46d5c4" radius={[6,6,0,0]}/><Bar dataKey="min" name="Minimum (ms)" fill="#7388ff" radius={[6,6,0,0]}/></BarChart></ResponsiveContainer></div><div className="comparison-note">Values match: <b>{String(compareResult.equalResults)}</b> · {compareResult.caveat}</div></> : <div className="chart-placeholder">Run a comparison to display timing bars. The pricing JSON comes from Code Studio's Input Data panel.</div>}
        </section><section className="card"><h2>Recorded Comparison History</h2><div className="table-wrap"><table><thead><tr><th>Time</th><th>Iterations</th><th>GraalPy avg</th><th>REST avg</th><th>Equal</th></tr></thead><tbody>{history.map(h=><tr key={h.id}><td>{new Date(h.createdAt).toLocaleString()}</td><td>{h.iterations}</td><td>{fmt(h.graalAverageMs)} ms</td><td>{fmt(h.restAverageMs)} ms</td><td>{h.equalResults ? '✓' : '✕'}</td></tr>)}{!history.length && <tr><td colSpan="5">No comparisons recorded yet.</td></tr>}</tbody></table></div></section>
      </>}
      {section === 'data' && <section className="card"><div className="card-top"><div><h2>Data collections</h2><p>Read-only in-memory MongoMock. Guest scripts can call <code>db.find('products')</code>.</p></div><div className="action-group">{['products','orders'].map(t => <button key={t} className={t === dataTab ? 'primary' : 'secondary'} onClick={() => setDataTab(t)}>{t}</button>)}</div></div>{dataError && <p className="error-text">{dataError}</p>}<div className="document-grid">{records.map(row => <pre key={row.id} className="document-card">{JSON.stringify(row, null, 2)}</pre>)}{!records.length && <p>No documents</p>}</div></section>}
      {section === 'library' && <section className="card"><div className="card-top"><div><h2>Saved snippets</h2><p>H2-backed saved scripts with optimistic version checking and validated deletion.</p></div><button className="secondary" onClick={fetchSnippets}>↻ Refresh</button></div><div className="library-grid">{snippets.map(s => <div className="library-card" key={s.id}><span className="tag">{s.language}</span><h3>{s.title}</h3><small>v{s.version} · {new Date(s.updatedAt).toLocaleString()}</small><div><button className="secondary" onClick={() => load(s)}>Open / Edit</button><button className="danger" onClick={() => deleteSnippet(s)}>Delete</button></div></div>)}{!snippets.length && <p>No saved snippets yet. Save a script from Code Studio.</p>}</div></section>}
      <footer className="footnote">PolyglotMesh · Educational demonstration only. Restricted contexts are not a substitute for OS-level isolation.</footer>
    </main>
  </div>;
}
