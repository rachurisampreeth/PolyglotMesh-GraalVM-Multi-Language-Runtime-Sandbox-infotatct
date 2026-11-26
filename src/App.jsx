
import { useState } from 'react';
import './App.css';
import MonacoEditor from './components/MonacoPythonEditor';

function App() {
  const [activeLang, setActiveLang] = useState('python');

  const [languageNames, setLanguageNames] = useState({
    python: 'Python',
    java: 'Java',
    javascript: 'JavaScript',
    c: 'C'
  });

  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState('Ready');
  const [output, setOutput] = useState('');
  const [jsonInput, setJsonInput] = useState('{\n  "name": "PolyglotMesh"\n}');
  const [showErrorDetails, setShowErrorDetails] = useState(false);
  const [errorDetails, setErrorDetails] = useState('');

  const [pythonCode, setPythonCode] = useState(`def greet(name):
    print("Hello, " + name)

greet("PolyglotMesh")`);

  const [javaCode, setJavaCode] = useState(`public class Main {
    public static void main(String[] args) {
        System.out.println("Hello PolyglotMesh");
    }
}`);

  const [jsCode, setJsCode] = useState(`function greet(name) {
    console.log("Hello, " + name);
}

greet("PolyglotMesh");`);

  const [cCode, setCCode] = useState(`#include <stdio.h>

int main() {
    printf("Hello PolyglotMesh");
    return 0;
}`);

  const getCode = () => {
    switch (activeLang) {
      case 'java': return javaCode;
      case 'javascript': return jsCode;
      case 'c': return cCode;
      default: return pythonCode;
    }
  };

  const setCode = (value) => {
    switch (activeLang) {
      case 'java':
        setJavaCode(value);
        break;
      case 'javascript':
        setJsCode(value);
        break;
      case 'c':
        setCCode(value);
        break;
      default:
        setPythonCode(value);
    }
  };

  const renameTab = () => {
    const newName = prompt('Enter new tab name:');

    if (newName && newName.trim()) {
      setLanguageNames({
        ...languageNames,
        [activeLang]: newName.trim()
      });
    }
  };

  // Copy console output
  const copyOutput = async () => {
    if (!output) return;

    try {
      await navigator.clipboard.writeText(output);
      alert('Output copied successfully!');
    } catch (error) {
      alert('Unable to copy output.');
    }
  };

  const runCode = () => {
    const code = getCode();

    if (!code.trim()) {
      setOutput('Please enter code before running.');
      setStatus('Error');
      return;
    }

    let parsedInput;

    try {
      parsedInput = JSON.parse(jsonInput);
    } catch (error) {
      setOutput('Invalid JSON: ' + error.message);
      setStatus('Error');
      return;
    }

    setRunning(true);
    setStatus('Running');
    setErrorDetails('');
    setShowErrorDetails(false);
    setOutput('');

    setTimeout(() => {
      try {
        let result;

        switch (activeLang) {
          case 'java':
            result = `${languageNames[activeLang]} output:\nHello PolyglotMesh`;
            break;
          case 'javascript':
            result = `${languageNames[activeLang]} output:\nHello, PolyglotMesh`;
            break;
          case 'c':
            result = `${languageNames[activeLang]} output:\nHello PolyglotMesh`;
            break;
          default:
            result = `${languageNames[activeLang]} output:\nHello, PolyglotMesh`;
        }

        setOutput(
          result + '\n\nJSON Input: ' +
          JSON.stringify(parsedInput, null, 2)
        );

        setStatus('Ready');
      } catch (err) {
        setStatus('Error');
        setErrorDetails(err.stack || err.message);
        setShowErrorDetails(true);
        setOutput(`Error: ${err.message}`);
      } finally {
        setRunning(false);
      }
    }, 1000);
  };

  return (
    <div className="app-container">

      <header className="app-header">
        <div className="brand">
          <div className="brand-icon">PM</div>
          <div>
            <h1>PolyglotMesh</h1>
            <p>Multi-Language Code Editor</p>
          </div>
        </div>

        <div className="header-tag">
          <span className="online-dot"></span>
          Frontend Demo
        </div>
      </header>

      <main className="workspace">

        <section className="editor-panel">

          <div className="panel-top">
            <div>
              <h2>Code Editor</h2>
              <p>Write and manage your code</p>
            </div>

            <div className={`status-badge ${status.toLowerCase()}`}>
              <span className="status-dot"></span>
              {status}
            </div>
          </div>

          <div className="demo-message">
            <span>ⓘ</span>
            Backend is not connected yet. Running in Demo Mode.
          </div>

          <div className="editor-toolbar">
            <div className="tabs">
              {['python', 'java', 'javascript', 'c'].map((lang) => (
                <button
                  key={lang}
                  className={`tab-button ${activeLang === lang ? 'active' : ''}`}
                  onClick={() => setActiveLang(lang)}
                >
                  {languageNames[lang]}
                </button>
              ))}
            </div>

            <button className="rename-button" onClick={renameTab}>
              ✎ Rename Tab
            </button>
          </div>

          <div className="monaco-wrapper">
            <MonacoEditor
              language={activeLang}
              code={getCode()}
              onChange={setCode}
            />
          </div>

          <div className="editor-footer">
            <span>Editing: {languageNames[activeLang]}</span>

            <button
              className="run-button"
              onClick={runCode}
              disabled={running}
            >
              {running ? '◌ Running...' : '▶ Run Code'}
            </button>
          </div>

        </section>

        <aside className="console-panel">

          <div className="panel-top">
            <div>
              <h2>Console Output</h2>
              <p>View your program output</p>
            </div>
            <span className="console-icon">⌘</span>
          </div>

          <div className="language-info">
            <span className="language-indicator"></span>
            Active Language: <strong>{languageNames[activeLang]}</strong>
          </div>

          <div className="section-heading">
            <h3>JSON Input</h3>
            <span>INPUT</span>
          </div>

          <textarea
            className="json-input"
            value={jsonInput}
            onChange={(e) => setJsonInput(e.target.value)}
            placeholder="Enter JSON input here..."
            rows={6}
          />

          <div className="output-heading">
            <h3>Program Output</h3>

            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                className="clear-button"
                onClick={copyOutput}
                disabled={!output || running}
              >
                Copy Output
              </button>

              <button
                className="clear-button"
                onClick={() => setOutput('')}
                disabled={running}
              >
                Clear
              </button>
            </div>
          </div>

          <div className={`console-output ${output ? 'has-output' : ''}`}>
            {output || 'Your output will appear here after running the code.'}
          </div>

          {errorDetails && (
            <div className="error-details">
              <button
                className="error-toggle"
                onClick={() => setShowErrorDetails(!showErrorDetails)}
              >
                {showErrorDetails ? 'Hide Error Details −' : 'Show Error Details +'}
              </button>

              {showErrorDetails && (
                <pre>{errorDetails}</pre>
              )}
            </div>
          )}

          <div className="console-footer">
            <span className="online-dot"></span>
            Demo Environment
          </div>

        </aside>

      </main>

      <footer className="app-footer">
        <span>PolyglotMesh</span>
        <span>Multi-Language Development Workspace</span>
      </footer>

    </div>
  );
}

export default App;
