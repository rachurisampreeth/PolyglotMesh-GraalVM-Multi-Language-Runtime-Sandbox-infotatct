
import { useState } from 'react';
import './App.css';
import MonacoEditor from './components/MonacoPythonEditor';

function App() {
  // Language selection
  const [activeLang, setActiveLang] = useState('python');

  const [languageNames, setLanguageNames] = useState({
    python: 'Python',
    java: 'Java',
    javascript: 'JavaScript',
    c: 'C'
  });

  // Execution state
  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState('Ready');
  const [output, setOutput] = useState('');
  const [jsonInput, setJsonInput] = useState('{\n  "name": "PolyglotMesh"\n}');
  const [showErrorDetails, setShowErrorDetails] = useState(false);
  const [errorDetails, setErrorDetails] = useState('');

  // Code for each language
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

  // Active language
  const getActiveLanguage = () => activeLang;

  // Get code for selected language
  const getCode = () => {
    switch (activeLang) {
      case 'java':
        return javaCode;
      case 'javascript':
        return jsCode;
      case 'c':
        return cCode;
      default:
        return pythonCode;
    }
  };

  // Update code for selected language
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

  // Rename selected tab
  const renameTab = () => {
    const newName = prompt('Enter new tab name:');

    if (newName && newName.trim()) {
      setLanguageNames({
        ...languageNames,
        [activeLang]: newName.trim()
      });
    }
  };

  // Run button
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
    <div
      className="app-container"
      style={{
        padding: '1rem',
        backgroundColor: '#1e1e1e',
        minHeight: '100vh',
        color: '#fff'
      }}
    >
      <div className="split-pane">

        {/* Left / Main panel */}
        <div className="editor-panel">

          <div className="status-badge">
            Status: {status}
          </div>

          {/* Backend status message */}
          <div
            style={{
              background: '#3b2f1f',
              color: '#ffcc80',
              padding: '10px',
              borderRadius: '5px',
              marginBottom: '10px'
            }}
          >
            Backend is not connected yet. Running in Demo Mode.
          </div>

          <div
            className="tabs"
            style={{
              marginBottom: '1rem',
              display: 'flex',
              gap: '0.5rem'
            }}
          >
            {['python', 'java', 'javascript', 'c'].map((lang) => (
              <button
                key={lang}
                onClick={() => setActiveLang(lang)}
                style={{
                  padding: '0.5rem 1rem',
                  background: activeLang === lang ? '#333' : '#555',
                  color: '#fff',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                {languageNames[lang]}
              </button>
            ))}
          </div>

          <button
            onClick={renameTab}
            style={{
              marginBottom: '1rem',
              padding: '0.5rem 1rem',
              background: '#1976d2',
              color: '#fff',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            Rename Tab
          </button>

          <MonacoEditor
            language={activeLang}
            code={getCode()}
            onChange={setCode}
          />

          <button
            onClick={runCode}
            style={{
              marginTop: '0.5rem',
              padding: '0.5rem 1rem',
              background: '#4caf50',
              color: '#fff',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
            disabled={running}
          >
            {running ? 'Running…' : 'Run'}
          </button>
        </div>

        {/* Right / Console panel */}
        <div className="console-panel">

          <h2
            className="console-heading"
            style={{ marginTop: 0, marginBottom: '0.5rem' }}
          >
            Console Output
          </h2>

          <h3>JSON Input</h3>

          <p>Active Language: {getActiveLanguage()}</p>

          <textarea
            value={jsonInput}
            onChange={(e) => setJsonInput(e.target.value)}
            placeholder="Enter JSON input here..."
            rows={5}
            style={{
              width: '100%',
              padding: '10px',
              background: '#1e1e1e',
              color: '#fff',
              border: '1px solid #555',
              borderRadius: '5px',
              marginBottom: '10px'
            }}
          />

          <button
            onClick={() => setOutput('')}
            style={{
              marginBottom: '0.5rem',
              padding: '0.3rem 0.8rem',
              background: '#555',
              color: '#fff',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
            disabled={running}
          >
            Clear
          </button>

          <div className="console-output">
            {output || 'Output will appear here after execution.'}
          </div>

          {errorDetails && (
            <div className="error-details">
              <button
                onClick={() => setShowErrorDetails(!showErrorDetails)}
              >
                {showErrorDetails ? 'Hide Error Details' : 'Show Error Details'}
              </button>

              {showErrorDetails && (
                <pre>{errorDetails}</pre>
              )}
            </div>
          )}

        </div>
      </div>
    </div>
  );
}

export default App;
