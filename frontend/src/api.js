const ROOT = (import.meta.env.VITE_API_BASE || 'http://localhost:7071').replace(/\/$/, '');
export async function api(path, options = {}) {
  const res = await fetch(`${ROOT}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
  });
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.error || `HTTP ${res.status}`);
  return body;
}
export function post(path, payload) { return api(path, { method: 'POST', body: JSON.stringify(payload) }); }
export function put(path, payload) { return api(path, { method: 'PUT', body: JSON.stringify(payload) }); }
export function del(path) { return api(path, { method: 'DELETE' }); }
