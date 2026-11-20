import React from 'react';
import Editor from '@monaco-editor/react';

// Default Python code snippet
const defaultCode = `def greet(name):
    print("Hello, " + name)

greet("PolyglotMesh")`;

const MonacoPythonEditor = () => {
  return (
    <div className="editor-container" style={{ height: '70vh', width: '100%' }}>
      <Editor
        height="100%"
        defaultLanguage="python"
        defaultValue={defaultCode}
        theme="vs-dark"
        options={{
          lineNumbers: 'on',
          tabSize: 4,
          insertSpaces: true,
          automaticLayout: true,
          scrollBeyondLastLine: false,
        }}
      />
    </div>
  );
};

export default MonacoPythonEditor;
