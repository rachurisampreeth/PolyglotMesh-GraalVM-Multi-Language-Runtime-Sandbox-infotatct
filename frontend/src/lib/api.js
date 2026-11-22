const BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '');

async function request(path, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 35000);
  try {
    const response = await fetch(`${BASE}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      signal: controller.signal,
    });
    if (response.status === 204) return null;
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.error || body.detail || `HTTP ${response.status}`);
    return body;
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('Request timed out. Check the backend console.');
    throw error;
  } finally {
    clearTimeout(timer);
  }
}
export const api = {
  health: () => request('/health'),
  templates: () => request('/templates'),
  execute: (body) => request('/execute', { method: 'POST', body: JSON.stringify(body) }),
  snippets: () => request('/snippets'),
  saveSnippet: (body) => request('/snippets', { method: 'POST', body: JSON.stringify(body) }),
  updateSnippet: (id, body) => request(`/snippets/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteSnippet: (id) => request(`/snippets/${id}`, { method: 'DELETE' }),
  history: () => request('/history?limit=40'),
  metrics: () => request('/metrics'),
};
