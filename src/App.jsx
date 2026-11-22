import { useState } from 'react';
import './App.css';
import MonacoEditor from './components/MonacoPythonEditor';

function App() {
  const [activeLang, setActiveLang] = useState('python');
  const [pythonCode, setPythonCode] = useState(`def greet(name):\n    print("Hello, " + name)\n\n    greet("PolyglotMesh")`);
  const [javaCode, setJavaCode] = useState(`public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello PolyglotMesh");\n    }\n}`);
  const [jsCode, setJsCode] = useState(`function greet(name) {\n  console.log("Hello, " + name);\n}\n\n  greet("PolyglotMesh");`);

  const getCode = () => {
    switch (activeLang) {
      case 'java':
        return javaCode;
      case 'javascript':
        return jsCode;
      default:
        return pythonCode;
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
      default:
        setPythonCode(value);
    }
  };

  return (
    <div className="app-container" style={{ padding: '1rem', backgroundColor: '#1e1e1e', minHeight: '100vh', color: '#fff' }}>
      {/* Language Tabs */}
      <div className="tabs" style={{ marginBottom: '1rem', display: 'flex', gap: '0.5rem' }}>
        {['python', 'java', 'javascript'].map((lang) => (
          <button
            key={lang}
            onClick={() => setActiveLang(lang)}
            style={{
              padding: '0.5rem 1rem',
              background: activeLang === lang ? '#333' : '#555',
              color: '#fff',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
            }}
          >
            {lang.charAt(0).toUpperCase() + lang.slice(1)}
          </button>
        ))}
      </div>
      {/* Monaco Editor */}
      <MonacoEditor language={activeLang} code={getCode()} onChange={setCode} />
    </div>
  );
}

export default App;
