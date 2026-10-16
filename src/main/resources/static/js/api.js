/* Plain JS (no JSX): session storage + fetch wrapper shared by every page. */
const API = '/api/v1';

const Auth = {
  get token() { return localStorage.getItem('lms_token'); },
  get me()    { return JSON.parse(localStorage.getItem('lms_me') || 'null'); },
  save(token, me) { localStorage.setItem('lms_token', token); localStorage.setItem('lms_me', JSON.stringify(me)); },
  clear() { localStorage.removeItem('lms_token'); localStorage.removeItem('lms_me'); },
};

const apiStatus = up => window.dispatchEvent(new CustomEvent('apistatus', { detail: up }));

async function api(path, opts = {}) {
  const headers = { ...(opts.headers || {}) };
  // FormData sets its own multipart boundary - never set Content-Type for it.
  if (opts.body && !(opts.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  if (Auth.token) headers['Authorization'] = 'Bearer ' + Auth.token;

  let res;
  try { res = await fetch(API + path, { ...opts, headers }); }
  catch { apiStatus(false); throw new Error("Can't reach the API. Is the app running on localhost:8080?"); }
  apiStatus(true);
  if (res.status === 204) return null;

  const text = await res.text();
  let payload = null;
  try { payload = text ? JSON.parse(text) : null; } catch {}

  if (!res.ok) {
    if (res.status === 401 && Auth.token) {
      Auth.clear(); location.href = 'login.html';
      throw new Error('Your session expired. Sign in again.');
    }
    const detail = payload?.details?.length ? payload.details.join(', ') : null;
    throw new Error(detail || payload?.message || `Request failed (${res.status})`);
  }
  return payload?.data !== undefined ? payload.data : payload;
}

async function signIn(email, password) {
  const auth = await api('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
  localStorage.setItem('lms_token', auth.accessToken);
  try {
    const me = await api('/users/me');           // role comes from the server
    Auth.save(auth.accessToken, me);
    return me;
  } catch (e) { Auth.clear(); throw e; }
}

Object.assign(window, { API, Auth, api, signIn });
