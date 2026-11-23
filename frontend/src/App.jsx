import { useCallback, useEffect, useMemo, useState } from 'react';
import Editor from '@monaco-editor/react';
import {
  Activity, ArrowDownToLine, ArrowRight, BarChart3, BookOpen, Braces,
  Check, CheckCircle2, ChevronDown, ChevronRight, CircleAlert, Clock3,
  CloudOff, Code2, Command, Copy, Cpu, Database, FileCode2, FolderOpen,
  Gauge, GitBranch, HardDrive, Layers3, LoaderCircle, LockKeyhole,
  PanelLeftClose, Play, Plus, RefreshCw, RotateCcw, Save, Server,
  Settings2, ShieldCheck, Sparkles, SquareTerminal, Trash2, Workflow, X,
  Zap,
} from 'lucide-react';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis } from 'recharts';
import { api } from './lib/api';
import { localTemplates, initialInput } from './data/templates';

const DEFAULT_JAVA = `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from Java!");
    }
}`;

const sections = [
  { id: 'workspace', label: 'Workspace', icon: Code2 },
  { id: 'history', label: 'Execution history', icon: Clock3 },
  { id: 'analytics', label: 'Performance', icon: BarChart3 },
  { id: 'docs', label: 'Documentation', icon: BookOpen },
];

function formatTime(value) {
  if (!value) return '—';
  try { return new Date(value).toLocaleString(undefined, { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' }); }
  catch { return value; }
}
function Pill({ children, tone = 'neutral' }) { return <span className={`pill pill-${tone}`}>{children}</span>; }
function Logo() {
  return <div className="brandmark"><span className="brandmark-inner"><Braces size={23} strokeWidth={2.1}/></span></div>;
}
function RunIcon({ isRunning }) { return isRunning ? <LoaderCircle size={16} className="spin" /> : <Play size={16} fill="currentColor" />; }
function EmptyState({ icon: Icon, title, description }) {
  return <div className="empty-state"><div className="empty-icon"><Icon size={25} /></div>
    <h3>{title}</h3><p>{description}</p></div>;
}
function MetricTile({ icon: Icon, label, value, hint, color = 'cyan' }) {
  return <div className="metric-tile"><div className={`metric-icon ${color}`}><Icon size={19}/></div>
    <p>{label}</p><strong>{value}</strong><span>{hint}</span></div>;
}

export default function App() {
  const [section, setSection] = useState('workspace');
  const [templates, setTemplates] = useState(localTemplates);
  const [saved, setSaved] = useState([]);
  const [mode, setMode] = useState('pipeline');
  const [language, setLanguage] = useState('python');
  const [activeEditor, setActiveEditor] = useState('python');
  const [code, setCode] = useState('');
  const [singleCodes, setSingleCodes] = useState({ python: '', javascript: '', java: DEFAULT_JAVA });
  const [pythonCode, setPythonCode] = useState(localTemplates[0].pythonCode);
  const [javascriptCode, setJavascriptCode] = useState(localTemplates[0].javascriptCode);
  const [inputText, setInputText] = useState(JSON.stringify(initialInput, null, 2));
  const [workspaceName, setWorkspaceName] = useState('Dynamic pricing engine');
  const [currentTemplate, setCurrentTemplate] = useState('pricing-engine');
  const [currentSnippetId, setCurrentSnippetId] = useState(null);
  const [running, setRunning] = useState(false);
  const [run, setRun] = useState(null);
  const [history, setHistory] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [connection, setConnection] = useState('connecting');
  const [toast, setToast] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [consoleTab, setConsoleTab] = useState('output');
  const [inspectorTab, setInspectorTab] = useState('context');
  const editorHeight = '364px';

  const notify = useCallback((message, kind = 'info') => {
    setToast({ message, kind, nonce: Date.now() });
  }, []);
  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(() => setToast(null), 4300);
    return () => clearTimeout(id);
  }, [toast]);

  const refreshData = useCallback(async () => {
    try {
      await api.health();
      setConnection('online');
      const results = await Promise.allSettled([api.templates(), api.snippets(), api.history(), api.metrics()]);
      if (results[0].status === 'fulfilled' && Array.isArray(results[0].value)) setTemplates(results[0].value);
      if (results[1].status === 'fulfilled') setSaved(results[1].value);
      if (results[2].status === 'fulfilled') setHistory(results[2].value);
      if (results[3].status === 'fulfilled') setMetrics(results[3].value);
    } catch {
      setConnection('offline');
    }
  }, []);
  useEffect(() => { refreshData(); }, [refreshData]);

  function applyExample(t) {
    setMode(t.mode);
    setLanguage(t.language || 'python');
    setCode(t.code || '');
    if (t.mode === 'single' && (t.language === 'python' || t.language === 'javascript')) {
      setSingleCodes(prev => ({ ...prev, [t.language]: t.code || '' }));
    }
    setPythonCode(t.pythonCode || '');
    setJavascriptCode(t.javascriptCode || '');
    setActiveEditor(t.mode === 'pipeline' ? 'python' : (t.language || 'python'));
    setInputText(JSON.stringify(t.input || initialInput, null, 2));
    setWorkspaceName(t.title);
    setCurrentTemplate(t.id);
    setCurrentSnippetId(null);
    setRun(null);
    setConsoleTab('output');
    setSection('workspace');
  }
  function applySnippet(s) {
    setMode(s.mode);
    setLanguage(s.language || 'python');
    setCode(s.code || '');
    if (s.mode === 'single' && (s.language === 'python' || s.language === 'javascript')) {
      setSingleCodes(prev => ({ ...prev, [s.language]: s.code || '' }));
    }
    setPythonCode(s.pythonCode || '');
    setJavascriptCode(s.javascriptCode || '');
    setActiveEditor(s.mode === 'pipeline' ? 'python' : (s.language || 'python'));
    setWorkspaceName(s.title);
    setCurrentSnippetId(s.id);
    setCurrentTemplate(null);
    setRun(null);
    setSection('workspace');
    notify('Loaded saved script');
  }
  function changeMode(nextMode) {
    const t = templates.find((x) => x.mode === nextMode && (nextMode === 'pipeline' || x.language === language))
       || templates.find((x) => x.mode === nextMode);
    if (t) applyExample(t);
  }
  function selectSingleLanguage(nextLanguage) {
    if (nextLanguage === language) return;
    setSingleCodes(prev => ({ ...prev, [language]: code }));
    setLanguage(nextLanguage);
    setActiveEditor(nextLanguage);
    setCode(singleCodes[nextLanguage] || (nextLanguage === 'java' ? DEFAULT_JAVA : templates.find(t => t.id === `${nextLanguage}-hello`)?.code || ''));
    setRun(null);
  }

  const javaPreview = mode === 'single' && language === 'java';
  const editorLanguage = mode === 'pipeline' ? activeEditor : language;
  const editorValue = mode === 'pipeline'
    ? (activeEditor === 'python' ? pythonCode : javascriptCode)
    : code;
  const setEditorValue = (value) => {
    if (mode === 'single') {
      setCode(value || '');
      setSingleCodes(prev => ({ ...prev, [language]: value || '' }));
    }
    else if (activeEditor === 'python') setPythonCode(value || '');
    else setJavascriptCode(value || '');
  };

  async function execute() {
    if (running) return;
    if (javaPreview) return notify('Java is editor-only; select Python or JavaScript to execute', 'info');
    let input;
    try {
      input = JSON.parse(inputText);
      if (input === null || Array.isArray(input) || typeof input !== 'object') throw new Error();
    } catch {
      notify('Input context must be a valid JSON object', 'error');
      setInspectorTab('context');
      return;
    }
    setRunning(true);
    setRun(null);
    setConsoleTab('output');
    try {
      const result = await api.execute({ mode, language, code, pythonCode, javascriptCode, input });
      setRun(result);
      setConnection('online');
      if (result.status === 'success') notify(`Execution completed in ${result.elapsedMs} ms`, 'success');
      else notify(result.error || 'Execution failed', 'error');
      const [newHistory, newMetrics] = await Promise.allSettled([api.history(), api.metrics()]);
      if (newHistory.status === 'fulfilled') setHistory(newHistory.value);
      if (newMetrics.status === 'fulfilled') setMetrics(newMetrics.value);
    } catch (error) {
      setRun({ status: 'error', error: error.message, output: '', result: '', steps: [], elapsedMs: 0, heapDeltaKb: 0 });
      if (/fetch|network|failed to|timed out/i.test(error.message)) setConnection('offline');
      notify(error.message, 'error');
    } finally {
      setRunning(false);
    }
  }


  useEffect(() => {
    const keyHandler = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
        event.preventDefault();
        execute();
      }
    };
    window.addEventListener('keydown', keyHandler, true);
    return () => window.removeEventListener('keydown', keyHandler, true);
  });

  async function saveSnippet() {
    if (javaPreview) return notify('Java editing is available; backend script saving supports Python and JavaScript only', 'info');
    if (connection !== 'online') return notify('Start the Java backend to save scripts', 'error');
    const title = window.prompt('Name your script', workspaceName);
    if (title === null) return;
    if (!title.trim()) return notify('Enter a script name', 'error');
    const data = { title: title.trim().slice(0, 80), mode, language, code, pythonCode, javascriptCode };
    try {
      const entity = currentSnippetId ? await api.updateSnippet(currentSnippetId, data) : await api.saveSnippet(data);
      setCurrentSnippetId(entity.id);
      setWorkspaceName(entity.title);
      setCurrentTemplate(null);
      setSaved(await api.snippets());
      notify('Saved to the local H2 database', 'success');
    } catch (error) { notify(error.message, 'error'); }
  }

  async function deleteSnippet(s) {
    if (!window.confirm(`Delete "${s.title}" permanently?`)) return;
    try {
      await api.deleteSnippet(s.id);
      setSaved(await api.snippets());
      if (currentSnippetId === s.id) setCurrentSnippetId(null);
      notify('Script deleted');
    } catch (error) { notify(error.message, 'error'); }
  }

  const chartData = useMemo(() => (metrics?.recent || []).slice().reverse().map((row, i) => ({
    name: `${i + 1}`, ms: row.elapsedMs, status: row.status,
  })), [metrics]);
  const footerStatus = connection === 'online' ? 'GraalVM engine connected'
    : connection === 'connecting' ? 'Connecting to backend…' : 'Backend is offline';

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-brand"><Logo /><div><b>Polyglot<span>Mesh</span></b><small>GRAALVM RUNTIME STUDIO</small></div></div>
        <div className="topbar-middle"><span className="topbar-slash">/</span><span>personal-workspace</span><ChevronRight size={15}/><strong>{workspaceName}</strong></div>
        <div className="topbar-actions">
          <div className={`status-badge ${connection}`}><i/>{connection === 'online' ? 'Runtime online' : connection === 'offline' ? 'Runtime offline' : 'Connecting'}</div>
          <button className="icon-button" title="Refresh connection" onClick={refreshData}><RefreshCw size={17}/></button>
          <div className="avatar">PM</div>
        </div>
      </header>

      <div className="body-layout">
        <aside className={`sidebar ${sidebarOpen ? '' : 'sidebar-collapsed'}`}>
          <div className="sidebar-main">
            <div className="sidebar-section-label">PLATFORM <button onClick={() => setSidebarOpen(!sidebarOpen)} title="Toggle sidebar"><PanelLeftClose size={16}/></button></div>
            <nav className="side-nav">
              {sections.map(({ id, label, icon: Icon }) => (
                <button key={id} onClick={() => setSection(id)} className={`nav-link ${section === id ? 'active' : ''}`}>
                  <Icon size={18}/><span>{label}</span>{section === id && <div className="nav-active-bar"/>}
                </button>
              ))}
            </nav>
            <div className="sidebar-divider"/>
            <div className="sidebar-section-label">QUICK START <Sparkles size={15}/></div>
            <div className="templates-list">
              {templates.map((t) => (
                <button key={t.id} onClick={() => applyExample(t)} className={`template-link ${currentTemplate === t.id ? 'selected' : ''}`} title={t.title}>
                  <span className={`file-glyph ${t.mode === 'pipeline' ? 'multi' : t.language}`}>
                    {t.mode === 'pipeline' ? <Workflow size={15}/> : <FileCode2 size={15}/>}</span>
                  <span className="truncate"><b>{t.title}</b><small>{t.subtitle}</small></span>
                </button>
              ))}
            </div>
            <div className="sidebar-divider"/>
            <div className="sidebar-section-label">SAVED SCRIPTS <span>{saved.length}</span></div>
            <div className="saved-list">
              {saved.length ? saved.map(s => (
                <div key={s.id} className={`saved-row ${currentSnippetId === s.id ? 'selected' : ''}`}>
                  <button title={`Open ${s.title}`} onClick={() => applySnippet(s)}><FileCode2 size={15}/><span>{s.title}</span></button>
                  <button className="delete-small" title="Delete script" onClick={() => deleteSnippet(s)}><Trash2 size={14}/></button>
                </div>
              )) : <p className="no-saved">No saved scripts yet.<br/>Build something and hit Save.</p>}
            </div>
          </div>
          <div className="sidebar-footer">
            <div className="runtime-card"><div className="runtime-visual"><Cpu size={19}/><span className={connection === 'online' ? 'pulse' : ''}/></div>
              <b>One JVM. Any language.</b><p>Polyglot execution without microservice overhead.</p>
              <div className="runtime-card-bottom"><span>GraalVM Polyglot</span><span className="mini-tag">v25</span></div>
            </div>
            <div className="side-version"><ShieldCheck size={13}/> LOCAL DEVELOPMENT BUILD <span>v1.0</span></div>
          </div>
        </aside>

        <main className="main-area">
          {section === 'workspace' ? <>
            <div className="page-heading">
              <div><div className="eyebrow"><span className="eyebrow-dot"/> POLYGLOT DEVELOPMENT ENVIRONMENT</div>
                <h1>Build without <span>boundaries.</span></h1>
                <p>Write Python, execute JavaScript, and connect them inside a single Java Virtual Machine.</p>
              </div>
              <div className="heading-side"><div className="shield-badge"><ShieldCheck size={17}/> Host access restricted</div><span>POWERED BY GRAALVM + SPRING BOOT</span></div>
            </div>
            <div className="workspace-grid">
              <div className="workspace-center">
                <div className="editor-card">
                  <div className="editor-toolbar">
                    <div className="editor-card-title"><div className="editor-symbol"><Code2 size={17}/></div><div><strong>Code editor</strong><small>Write &amp; execute across runtimes</small></div></div>
                    <div className="toolbar-right">
                      <div className="mode-switch" aria-label="Execution mode">
                        <button className={mode === 'single' ? 'active' : ''} onClick={() => changeMode('single')}>Single</button>
                        <button className={mode === 'pipeline' ? 'active' : ''} onClick={() => changeMode('pipeline')}><Workflow size={13}/> Pipeline</button>
                      </div>
                      <button className="secondary-button save-button" disabled={javaPreview} title={javaPreview ? "Java is editor-only; saving to backend is not supported" : "Save script"} onClick={saveSnippet}><Save size={15}/> Save</button>
                      <button className="primary-button" disabled={running || javaPreview} title={javaPreview ? "Java syntax highlighting only; execution is not supported" : "Execute code"} onClick={execute}><RunIcon isRunning={running}/>{running ? 'Running…' : 'Run code'}<span className="keycap">Ctrl↵</span></button>
                    </div>
                  </div>
                  <div className="tabs-row">
                    {mode === 'pipeline' ? <>
                      <button className={`code-tab ${activeEditor === 'python' ? 'active' : ''}`} onClick={() => setActiveEditor('python')}><span className="lang-dot py"/> pricing.py <span className="code-pill">PY</span></button>
                      <button className={`code-tab ${activeEditor === 'javascript' ? 'active' : ''}`} onClick={() => setActiveEditor('javascript')}><span className="lang-dot js"/> validate.js <span className="code-pill">JS</span></button>
                    </> : <>
                       <button className={`code-tab ${language === 'python' ? 'active' : ''}`} onClick={() => selectSingleLanguage('python')}><span className="lang-dot py"/> main.py</button>
                       <button className={`code-tab ${language === 'javascript' ? 'active' : ''}`} onClick={() => selectSingleLanguage('javascript')}><span className="lang-dot js"/> main.js</button>
                       <button className={`code-tab ${language === 'java' ? 'active' : ''}`} onClick={() => selectSingleLanguage('java')}><span className="lang-dot java"/> Main.java <span className="code-pill">EDIT</span></button>
                    </>}
                    <div className="tab-spacer"/><span className="tabs-right"><span className="tabs-dot"/>UTF-8 <span>•</span> {editorLanguage === 'python' ? 'Python 3' : editorLanguage === 'java' ? 'Java 21' : 'JavaScript'}</span>
                  </div>
                  <div className="editor-wrap">
                    <Editor key={`${mode}-${editorLanguage}`} height={editorHeight} language={editorLanguage} value={editorValue} onChange={setEditorValue}
                      loading={<div className="editor-loading"><LoaderCircle className="spin" size={23}/> Loading Monaco editor…</div>}
                      theme="vs-dark" options={{ fontFamily: "'Cascadia Code', Consolas, 'Courier New', monospace", fontSize: 13.5, lineHeight: 25,
                        fontLigatures: true, minimap: { enabled: false }, scrollBeyondLastLine: false, renderLineHighlight: 'line',
                        automaticLayout: true, overviewRulerBorder: false, smoothScrolling: true, padding: { top: 18, bottom: 18 },
                        guides: { indentation: true }, bracketPairColorization: { enabled: true }, tabSize: 4,
                        lineNumbersMinChars: 3, folding: true, wordWrap: 'on' }} />
                  </div>
                  <div className="editor-footer"><span><span className="footer-dot"/> {mode === 'pipeline' ? 'GraalVM Polyglot Context' : editorLanguage === 'java' ? 'Java editor · syntax highlighting only' : editorLanguage === 'python' ? 'GraalPy Runtime' : 'GraalJS Runtime'}</span><span><ShieldCheck size={13}/> Restricted permissions <span className="footer-sep">|</span> {editorLanguage === 'python' ? 'Python' : editorLanguage === 'java' ? 'Java · editor only' : 'JavaScript'}</span></div>
                </div>

                <div className="console-card">
                  <div className="console-header"><div className="console-title"><SquareTerminal size={17}/> <strong>Execution console</strong> {run && <Pill tone={run.status === 'success' ? 'green' : 'red'}>{run.status === 'success' ? 'SUCCESS' : run.status.toUpperCase()}</Pill>}</div>
                    <div className="console-controls"><button className={consoleTab === 'output' ? 'active' : ''} onClick={() => setConsoleTab('output')}>Output</button><button className={consoleTab === 'steps' ? 'active' : ''} onClick={() => setConsoleTab('steps')}>Trace</button>
                      <button title="Copy output" onClick={() => { navigator.clipboard?.writeText((run?.output || '') + '\n' + (run?.result || '') + '\n' + (run?.error || '')); notify('Console copied'); }}><Copy size={14}/></button></div>
                  </div>
                  {consoleTab === 'output' ? <div className="console-content">
                    {!run ? <div className="console-placeholder"><span className="terminal-prompt">➜</span> <span>Click <b>Run code</b> to execute your script on the JVM.</span><i className="terminal-cursor"/></div> : <>
                      <div className="console-log">{run.output || <span className="muted">(No stdout generated)</span>}</div>
                      {run.result && <div className="console-result"><span className="result-label"><CheckCircle2 size={14}/> RESULT</span><pre>{run.result}</pre></div>}
                      {run.error && <div className="console-error"><CircleAlert size={15}/><pre>{run.error}</pre></div>}
                    </>}
                  </div> : <div className="console-content trace-content">
                    {!run?.steps?.length ? <div className="console-placeholder"><span className="terminal-prompt">➜</span> Stage traces appear after a run.</div> : run.steps.map((step, idx) => <div className="trace-row" key={idx}><span className="trace-index">0{idx+1}</span><CheckCircle2 size={15} className="green-text"/><b>{step.name}</b><span>{step.language}</span><strong>{step.elapsedMs}ms</strong></div>)}
                  </div>}
                  <div className="console-bottom"><span>{run ? `Completed in ${run.elapsedMs}ms` : 'Ready for execution'}</span><span>{run ? `JVM heap delta (approx.): ${run.heapDeltaKb} KB` : 'Output is limited to 32 KB'}</span></div>
                </div>
              </div>

              <aside className="inspector">
                <div className="inspector-card">
                  <div className="panel-heading"><span><Layers3 size={17}/> Execution flow</span><Pill tone="purple">LIVE</Pill></div>
                  <p className="panel-description">A single Java process orchestrates your language runtime.</p>
                  <div className="flow-list">
                    <div className="flow-item"><div className="flow-icon java"><Braces size={17}/></div><div><b>Java host</b><span>Spring Boot · API Gateway</span></div><span className="flow-number">01</span></div>
                    <div className="flow-connector"/>
                    <div className="flow-item"><div className="flow-icon python">Py</div><div><b>GraalPy</b><span>{mode === 'pipeline' || language === 'python' ? 'Python guest runtime' : 'Available runtime'}</span></div><span className="flow-number">02</span></div>
                    <div className="flow-connector"/>
                    <div className="flow-item"><div className="flow-icon javascript">JS</div><div><b>GraalJS</b><span>{mode === 'pipeline' || language === 'javascript' ? 'JavaScript guest runtime' : 'Available runtime'}</span></div><span className="flow-number">03</span></div>
                  </div>
                  <div className="no-network"><Zap size={15}/> <span>Guest languages run in-process.<br/><b>No interservice API calls</b></span></div>
                </div>

                <div className="inspector-card context-card">
                  <div className="panel-heading"><span><Database size={16}/> Input context</span><button className="small-text-button" onClick={() => setInputText(JSON.stringify(initialInput, null, 2))}><RotateCcw size={13}/> Reset</button></div>
                  <p className="panel-description">JSON object injected through Java host bindings.</p>
                  <textarea spellCheck="false" value={inputText} onChange={e => setInputText(e.target.value)} aria-label="Input JSON data"/>
                  <div className="context-foot"><span className="context-green-dot"/> Java Map → guest proxy <span>JSON</span></div>
                </div>

                <div className="inspector-card mini-stats-card">
                  <div className="panel-heading"><span><Gauge size={17}/> Last execution</span><span className="small-muted">METRICS</span></div>
                  <div className="metric-stat"><span>Wall time</span><b>{run ? `${run.elapsedMs} ms` : '— ms'}</b></div>
                  <div className="metric-stat"><span>Result</span><Pill tone={!run ? 'neutral' : run.status === 'success' ? 'green' : 'red'}>{run?.status || 'Not run'}</Pill></div>
                  <div className="metric-stat"><span>Host heap Δ <small>est.</small></span><b>{run ? `${run.heapDeltaKb} KB` : '— KB'}</b></div>
                  <div className="performance-note"><Activity size={15}/><span>Actual runtime metrics. Heap Δ is JVM-wide, not a per-script memory limit.</span></div>
                </div>
              </aside>
            </div>
          </> : null}

          {section === 'history' && <div className="subpage"><div className="subpage-header"><div><div className="eyebrow">OBSERVABILITY / EXECUTIONS</div><h1>Execution <span>history.</span></h1><p>Run metadata is persisted in an embedded H2 database.</p></div><button className="secondary-button" onClick={refreshData}><RefreshCw size={15}/> Refresh</button></div>
            <div className="data-panel"><div className="data-panel-header"><b>All recent runs</b><Pill>{history.length} entries</Pill></div>
              {history.length ? <div className="table-scroller"><table><thead><tr><th>STATUS</th><th>MODE</th><th>RUNTIME</th><th>DURATION</th><th>TIME</th><th>RESULT PREVIEW</th></tr></thead><tbody>{history.map(h => <tr key={h.id}><td><Pill tone={h.status === 'success' ? 'green' : 'red'}>{h.status}</Pill></td><td>{h.mode}</td><td><span className="mono-label">{h.language}</span></td><td>{h.elapsedMs} ms</td><td>{formatTime(h.timestamp)}</td><td className="truncate-cell" title={h.summary}>{h.summary || '—'}</td></tr>)}</tbody></table></div> : <EmptyState icon={Clock3} title="No executions yet" description="Run your first script in the Workspace to create history."/>}
            </div>
          </div>}

          {section === 'analytics' && <div className="subpage"><div className="subpage-header"><div><div className="eyebrow">OBSERVABILITY / ANALYTICS</div><h1>Runtime <span>performance.</span></h1><p>Understand execution time and results across recent runs.</p></div><button className="secondary-button" onClick={refreshData}><RefreshCw size={15}/> Refresh</button></div>
            <div className="metric-grid"><MetricTile icon={Play} label="Total executions" value={metrics?.total ?? 0} hint="Persisted locally"/><MetricTile icon={CheckCircle2} label="Success rate" value={`${metrics?.successRate ?? 0}%`} hint="All-time completion" color="green"/><MetricTile icon={Clock3} label="Avg. wall time" value={`${metrics?.averageMs ?? 0} ms`} hint="Last 40 executions" color="purple"/><MetricTile icon={CircleAlert} label="Failed / timed out" value={metrics?.failures ?? 0} hint="All recorded errors" color="orange"/></div>
            <div className="chart-card"><div className="chart-header"><div><b>Execution latency</b><p>Recent runs · wall-clock time (ms)</p></div><Pill tone="purple">GraalVM</Pill></div>
              {chartData.length ? <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><AreaChart data={chartData} margin={{ top: 8, right: 8, bottom: 8, left: -25 }}><defs><linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#9475ff" stopOpacity={.38}/><stop offset="100%" stopColor="#9475ff" stopOpacity={0}/></linearGradient></defs><CartesianGrid stroke="#233146" vertical={false} strokeDasharray="4 4"/><XAxis dataKey="name" tickLine={false} axisLine={false} tick={{fill:'#75859d', fontSize:11}}/><Tooltip contentStyle={{ background:'#161e30', border:'1px solid #33405a', borderRadius:10, color:'#fff' }}/><Area type="monotone" dataKey="ms" stroke="#a78bfa" strokeWidth={3} fill="url(#chartGradient)" activeDot={{r:5}}/></AreaChart></ResponsiveContainer></div> : <EmptyState icon={Activity} title="No latency data yet" description="Execute code to generate a real performance trend."/>}
            </div>
            <div className="analytics-note"><ShieldCheck size={18}/><div><strong>How to interpret the numbers</strong><p>Wall-clock time includes context setup and runtime warm-up. The measurements are genuine but are not an isolated per-language microbenchmark; run warmups before comparing languages.</p></div></div>
          </div>}

          {section === 'docs' && <div className="subpage docs-page"><div className="subpage-header"><div><div className="eyebrow">REFERENCE / GUIDE</div><h1>Know your <span>runtime.</span></h1><p>The essentials for developing with PolyglotMesh.</p></div></div>
            <div className="doc-grid"><div className="doc-block"><div className="doc-icon"><Workflow size={21}/></div><h2>What is PolyglotMesh?</h2><p>A Java Spring Boot WebFlux API uses the GraalVM Polyglot SDK to run GraalPy and GraalJS inside its own process. In pipeline mode, Java hands data to Python, retrieves a result, and forwards it to JavaScript without calling another service.</p></div>
              <div className="doc-block"><div className="doc-icon"><Server size={21}/></div><h2>Start the backend</h2><p>Install JDK 21+ and Maven 3.9+, then run the following from the backend directory.</p><code>mvn spring-boot:run</code><p>Expected health check: <strong>http://localhost:7071/api/health</strong></p></div>
              <div className="doc-block"><div className="doc-icon"><Code2 size={21}/></div><h2>Start the frontend</h2><p>Install Node.js 20.19+ or 22.12+, then use these commands inside the frontend directory.</p><code>npm install<br/>npm run dev</code><p>Open the URL Vite prints, normally <strong>http://localhost:7070</strong>.</p></div>
              <div className="doc-block"><div className="doc-icon"><LockKeyhole size={21}/></div><h2>Security limits</h2><p>Host Java classes, filesystem, process creation, native access and guest-created threads are disallowed. Execution is cancelled after a timeout and output is capped. <strong>This demonstration is not a hardened public code-execution service.</strong></p></div>
              <div className="doc-block"><div className="doc-icon"><Database size={21}/></div><h2>Save &amp; reload</h2><p>Press Save in the editor. Your script is stored via JPA in a local H2 file. Run history and metrics are stored as metadata, too. No MongoDB or Docker installation is required.</p></div>
              <div className="doc-block"><div className="doc-icon"><BookOpen size={21}/></div><h2>Full instructions</h2><p>See <strong>README.md</strong>, <strong>docs/WINDOWS_SETUP.md</strong>, and <strong>docs/ARCHITECTURE.md</strong> in the delivered zip for commands, troubleshooting, API examples, and a presentation walkthrough.</p></div>
            </div>
          </div>}

          <div className="main-footer"><span><span className={`live-dot ${connection}`}/>{footerStatus}</span><span>Spring Boot <span className="bullet">•</span> GraalVM Polyglot <span className="bullet">•</span> React + Monaco</span></div>
        </main>
      </div>
      {toast && <div className={`toast toast-${toast.kind}`} role="status">{toast.kind === 'error' ? <CircleAlert size={17}/> : <CheckCircle2 size={17}/>}<span>{toast.message}</span><button onClick={() => setToast(null)} aria-label="Close notification"><X size={14}/></button></div>}
    </div>
  );
}
