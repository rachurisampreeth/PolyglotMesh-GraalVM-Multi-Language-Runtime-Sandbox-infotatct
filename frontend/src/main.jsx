import React from 'react';
import { createRoot } from 'react-dom/client';
import * as monaco from 'monaco-editor';
import { loader } from '@monaco-editor/react';
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import JSONWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import TSWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
import App from './App';
import './styles.css';

// Bundle Monaco workers locally instead of loading the editor from a CDN.
self.MonacoEnvironment = {
  getWorker(_id, label) {
    if (label === 'json') return new JSONWorker();
    if (label === 'typescript' || label === 'javascript') return new TSWorker();
    return new EditorWorker();
  },
};
loader.config({ monaco });

createRoot(document.getElementById('root')).render(
  <React.StrictMode><App /></React.StrictMode>
);
