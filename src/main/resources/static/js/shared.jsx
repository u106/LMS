/* Shared React pieces: layout, nav, toasts, modal, cards, hooks.
   Exposed to page files through window.UI. */
const { useState, useEffect } = React;

const SPINES = ['#5B85FF', '#3FD996', '#E3A945', '#A97BF0', '#F26A75', '#3FC4D9'];
const spineFor = id => { let h = 0; for (const c of (id || '')) h = (h * 31 + c.charCodeAt(0)) >>> 0; return SPINES[h % SPINES.length]; };
const initials = n => (n || '?').trim().split(/\s+/).slice(0, 2).map(w => w[0]).join('').toUpperCase();
const homeFor = r => r === 'INSTRUCTOR' ? 'teaching.html' : r === 'ADMIN' ? 'people.html' : 'catalog.html';
const rowsOf = d => Array.isArray(d) ? d : (d?.content ?? []);

const toast = (title, msg, kind = 'info') =>
  window.dispatchEvent(new CustomEvent('toast', { detail: { title, msg, kind, id: Math.random() } }));
const ok  = (t, m) => toast(t, m, 'good');
const err = (t, m) => toast(t, m, 'bad');

/* fn() runs on mount and whenever deps change; reload() re-runs it. */
function useAsync(fn, deps) {
  const [s, setS] = useState({ data: null, error: null, loading: true });
  const [n, setN] = useState(0);
  useEffect(() => {
    let live = true;
    setS(x => ({ ...x, loading: true, error: null }));
    fn().then(data => live && setS({ data, error: null, loading: false }),
              e => live && setS({ data: null, error: e.message, loading: false }));
    return () => { live = false; };
  }, [...deps, n]);
  return { ...s, reload: () => setN(x => x + 1) };
}

function ToastHost() {
  const [list, setList] = useState([]);
  useEffect(() => {
    const h = e => {
      const t = e.detail;
      setList(l => [...l, t]);
      setTimeout(() => setList(l => l.filter(x => x.id !== t.id)), 4000);
    };
    window.addEventListener('toast', h);
    return () => window.removeEventListener('toast', h);
  }, []);
  return (
    <div className="toasts">
      {list.map(t => (
        <div key={t.id} className={'toast ' + t.kind}>
          <div className="tico">{t.kind === 'good' ? '✓' : t.kind === 'bad' ? '!' : 'i'}</div>
          <div className="ttxt"><div className="ttitle">{t.title}</div>{t.msg && <div className="tmsg">{t.msg}</div>}</div>
          <div className="tbar" />
        </div>
      ))}
    </div>
  );
}

function Nav() {
  const me = Auth.me, signed = !!Auth.token && !!me;
  const here = location.pathname.split('/').pop() || 'index.html';
  const links = [['catalog.html', 'Catalog']];
  if (me?.role === 'STUDENT')    links.push(['my-learning.html', 'My learning']);
  if (me?.role === 'INSTRUCTOR') links.push(['teaching.html', 'Teaching']);
  if (me?.role === 'ADMIN')      links.push(['people.html', 'People']);
  const signOut = () => { Auth.clear(); location.href = 'index.html'; };
  return (
    <nav className="nav"><div className="nav-in">
      <a className="brand" href={signed ? homeFor(me.role) : 'index.html'}>
        <div className="mark">RL</div><div className="brand-name">Rocket Learning</div>
      </a>
      <div className="nav-links">
        {links.map(([href, label]) => <a key={href} href={href} className={'nav-link' + (here === href ? ' on' : '')}>{label}</a>)}
      </div>
      <div className="nav-right">
        {signed ? (<>
          <div className="who">
            <div className="avatar">{initials(me.name)}</div>
            <div className="who-txt"><div className="who-name">{me.name}</div><div className="who-role">{me.role}</div></div>
          </div>
          <button className="btn sm" onClick={signOut}>Sign out</button>
        </>) : (<>
          <a className="btn sm" href="login.html">Sign in</a>
          <a className="btn primary sm" href="register.html">Get started</a>
        </>)}
      </div>
    </div></nav>
  );
}

/* roles = optional allow-list; others are redirected (UI guard only - the API enforces the real rules). */
function Layout({ roles, children }) {
  const me = Auth.me, signed = !!Auth.token && !!me;
  const allowed = !roles || (signed && roles.includes(me.role));
  const [up, setUp] = useState(null);
  useEffect(() => {
    const h = e => setUp(e.detail);
    window.addEventListener('apistatus', h);
    if (!allowed) location.replace(signed ? homeFor(me.role) : 'login.html');
    return () => window.removeEventListener('apistatus', h);
  }, []);
  if (!allowed) return null;
  return (<>
    <Nav />
    <main><div className="view on">{children}</div></main>
    <footer className="foot"><div className="foot-in">
      <span>Rocket Learning — React client for the Spring Boot LMS API</span>
      <div className={'status ' + (up === null ? '' : up ? 'up' : 'down')}>
        <span className="led" /><span>{up === null ? 'Checking API…' : up ? 'API connected' : 'API unreachable'}</span>
      </div>
    </div></footer>
    <ToastHost />
  </>);
}

function Modal({ open, onClose, title, sub, children, foot }) {
  useEffect(() => {
    if (!open) return;
    const k = e => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', k);
    return () => document.removeEventListener('keydown', k);
  }, [open]);
  if (!open) return null;
  return (
    <div className="scrim on" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal-head"><h3>{title}</h3>{sub && <p>{sub}</p>}</div>
        <div className="modal-body">{children}</div>
        <div className="modal-foot">{foot}</div>
      </div>
    </div>
  );
}

const Field = ({ label, hint, children }) =>
  <div className="field"><label>{label}</label>{children}{hint && <div className="hint">{hint}</div>}</div>;

function Btn({ busy, className = 'btn', children, ...p }) {
  return <button {...p} className={className} disabled={busy || p.disabled}>{busy && <span className="spin" />} {children}</button>;
}

const Pill = ({ status }) => status === 'PUBLISHED'
  ? <span className="pill live">Published</span> : <span className="pill draft">Draft</span>;

const Empty = ({ icon, title, children, action }) => (
  <div className="empty" style={{ gridColumn: '1/-1' }}>
    <div className="ico">{icon}</div><h3>{title}</h3><p>{children}</p>{action}
  </div>
);
const Skeletons = ({ n = 6 }) => <div className="grid">{Array.from({ length: n }, (_, i) => <div key={i} className="skel" />)}</div>;

const CourseCard = ({ c }) => (
  <a className="card" href={'course.html?id=' + c.id}>
    <div className="card-top" style={{ background: spineFor(c.id) }} />
    <div className="card-body">
      <h3>{c.title}</h3>
      <p className="desc">{c.description || 'No description yet.'}</p>
      <div className="card-foot">
        <div className="by"><div className="avatar">{initials(c.instructorName)}</div><span>{c.instructorName || 'Unknown'}</span></div>
        <div style={{ marginLeft: 'auto' }}><Pill status={c.status} /></div>
      </div>
    </div>
  </a>
);

function CourseList({ q, list, empty }) {
  if (q.loading) return <Skeletons />;
  if (q.error) return <div className="grid"><Empty icon="⚠" title="Couldn't load courses"
    action={<button className="btn" onClick={q.reload}>Try again</button>}>{q.error}</Empty></div>;
  return list.length ? <div className="grid">{list.map(c => <CourseCard key={c.id} c={c} />)}</div> : <div className="grid">{empty}</div>;
}

/* Create (course = null) or edit a course. */
function CourseModal({ open, course, onClose, onSaved }) {
  const blank = { title: '', description: '', status: 'DRAFT' };
  const [f, setF] = useState(blank);
  const [busy, setBusy] = useState(false);
  useEffect(() => { if (open) setF(course ? { title: course.title, description: course.description || '', status: course.status } : blank); }, [open]);
  const set = k => e => setF({ ...f, [k]: e.target.value });

  async function save() {
    if (!f.title.trim()) return err('Title required', 'Give the course a title before saving.');
    setBusy(true);
    try {
      const body = { title: f.title.trim(), description: f.description.trim(), ...(course ? { status: f.status } : {}) };
      const saved = await api(course ? '/courses/' + course.id : '/courses', { method: course ? 'PUT' : 'POST', body: JSON.stringify(body) });
      ok(course ? 'Course updated' : 'Course created', course ? saved.title : `"${saved.title}" was saved as a draft.`);
      onSaved(saved); onClose();
    } catch (e) { err("Couldn't save the course", e.message); }
    finally { setBusy(false); }
  }
  return (
    <Modal open={open} onClose={onClose} title={course ? 'Edit course' : 'New course'}
           sub={course ? 'Changes apply as soon as you save.' : "It'll be saved as a draft."}
           foot={<><button className="btn ghost" onClick={onClose}>Cancel</button><Btn className="btn primary" busy={busy} onClick={save}>Save course</Btn></>}>
      <Field label="Title"><input value={f.title} onChange={set('title')} placeholder="Spring Security in depth" autoFocus /></Field>
      <Field label="Description"><textarea value={f.description} onChange={set('description')} placeholder="What will someone be able to do after this course?" /></Field>
      {course && <Field label="Visibility">
        <select value={f.status} onChange={set('status')}>
          <option value="DRAFT">Draft — only you can see it</option>
          <option value="PUBLISHED">Published — listed in the catalog</option>
        </select></Field>}
    </Modal>
  );
}

/* Cartoon image that quietly disappears (or shows `fallback`) if the file is missing. */
function Art({ src, alt = '', className = '', fallback = null }) {
  const [bad, setBad] = useState(false);
  if (bad) return fallback;
  return <img className={'art ' + className} src={src} alt={alt} loading="lazy" onError={() => setBad(true)} />;
}

function Banner({ title, sub }) {
  return (
    <div className="banner">
      <div className="banner-txt"><h2>{title}</h2><p>{sub}</p></div>
      <Art className="banner-art" src="/img/banner-students.jpg" alt="Students studying together" />
    </div>
  );
}
/* Shared card for the login / register pages. */
function AuthCard({ title, sub, label, busy, onSubmit, swap, children }) {
  return (
    <div className="auth-split">
      <Art className="auth-art" src="/img/auth-students.jpg" alt="Two students learning together" />
      <div className="auth-wrap"><form className="auth" onSubmit={e => { e.preventDefault(); onSubmit(); }}>
        <h2>{title}</h2><p className="sub">{sub}</p>{children}
        <Btn className="btn primary lg" style={{ width: '100%' }} busy={busy} type="submit">{label}</Btn>
        <div className="swap">{swap}</div>
      </form></div>
    </div>
  );
}

const mount = C => ReactDOM.createRoot(document.getElementById('root')).render(<C />);

window.UI = { Art, Banner, Layout, Modal, Field, Btn, Pill, Empty, Skeletons, CourseCard, CourseList, CourseModal, AuthCard,
              useAsync, ok, err, toast, spineFor, initials, homeFor, rowsOf, mount };
