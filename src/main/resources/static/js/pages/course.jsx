const { Layout, Modal, Field, Btn, Pill, Empty, CourseModal, useAsync, ok, err, spineFor, initials, rowsOf, mount } = window.UI;
const { useState, useEffect } = React;

const courseId = new URLSearchParams(location.search).get('id');
const fmtSize = b => b == null ? '' : b < 1024 ? b + ' B' : b < 1048576 ? (b / 1024).toFixed(0) + ' KB' : (b / 1048576).toFixed(1) + ' MB';
const shortName = n => !n ? 'file' : n.length > 24 ? n.slice(0, 21) + '…' : n;

/* run an API action with consistent toasts */
async function call(fn, okTitle, okMsg, errTitle) {
  try { await fn(); if (okTitle) ok(okTitle, okMsg); return true; }
  catch (e) { err(errTitle, e.message); return false; }
}

/* lesson === null: closed. lesson.id present: edit. otherwise: create. */
function LessonModal({ lesson, nextPos, onClose, onSaved }) {
  const [f, setF] = useState({ title: '', position: 1, content: '' });
  const [busy, setBusy] = useState(false);
  useEffect(() => { if (lesson) setF({ title: lesson.title || '', position: lesson.position || nextPos, content: lesson.content || '' }); }, [lesson]);
  const set = k => e => setF({ ...f, [k]: e.target.value });

  async function save() {
    const position = parseInt(f.position, 10);
    if (!f.title.trim()) return err('Title required', 'Give the lesson a title before saving.');
    if (!position || position < 1) return err('Invalid position', 'Position must be 1 or higher.');
    setBusy(true);
    const body = JSON.stringify({ title: f.title.trim(), content: f.content.trim(), position });
    const done = await call(() => api(`/courses/${courseId}/lessons${lesson.id ? '/' + lesson.id : ''}`, { method: lesson.id ? 'PUT' : 'POST', body }),
      lesson.id ? 'Lesson updated' : 'Lesson added', f.title, "Couldn't save the lesson");
    setBusy(false);
    if (done) { onSaved(); onClose(); }
  }
  return (
    <Modal open={!!lesson} onClose={onClose} title={lesson?.id ? 'Edit lesson' : 'Add a lesson'} sub="Lessons appear in position order."
           foot={<><button className="btn ghost" onClick={onClose}>Cancel</button><Btn className="btn primary" busy={busy} onClick={save}>Save lesson</Btn></>}>
      <Field label="Title"><input value={f.title} onChange={set('title')} placeholder="What is dependency injection?" autoFocus /></Field>
      <Field label="Position"><input type="number" min="1" value={f.position} onChange={set('position')} /></Field>
      <Field label="Content"><textarea value={f.content} onChange={set('content')} placeholder="The lesson body." /></Field>
    </Modal>
  );
}

/* target = { lessonId, replace } or null */
function UploadModal({ target, onClose, onDone }) {
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => { setFile(null); }, [target]);

  async function upload() {
    const form = new FormData();
    form.append('file', file);
    setBusy(true);
    const done = await call(() => api(`/courses/${courseId}/lessons/${target.lessonId}/file`, { method: 'POST', body: form }),
      'File uploaded', `${file.name} is attached.`, "Couldn't upload");
    setBusy(false);
    if (done) { onDone(); onClose(); }
  }
  return (
    <Modal open={!!target} onClose={onClose} title={target?.replace ? 'Replace file' : 'Upload a file'}
           sub={target?.replace ? 'The current file will be replaced.' : 'mp4, webm, pdf, png or jpg.'}
           foot={<><button className="btn ghost" onClick={onClose}>Cancel</button><Btn className="btn primary" busy={busy} disabled={!file} onClick={upload}>Upload</Btn></>}>
      <label className="dropzone">
        <div className="dz-ico">↑</div>
        <div className="dz-main">Choose a file</div>
        <div className="dz-name">{file ? file.name : 'No file chosen'}</div>
        <input type="file" style={{ display: 'none' }} accept=".mp4,.webm,.pdf,.png,.jpg,.jpeg" onChange={e => setFile(e.target.files[0] || null)} />
      </label>
    </Modal>
  );
}

function Lesson({ l, owned, onOpen, onUpload, onStrip, onEdit, onDelete }) {
  return (
    <div className="lesson">
      <div className="lpos">{l.position}</div>
      <div className="lbody">
        <b>{l.title}</b>
        <p>{l.content || 'No content yet.'}</p>
        {l.hasFile
          ? <div className="lfile"><button className="filechip" onClick={onOpen} title={l.fileName}><span className="fi">▶</span>{shortName(l.fileName)} · {fmtSize(l.fileSize)}</button></div>
          : owned && <div className="lfile"><span className="filechip empty">No file</span></div>}
      </div>
      {owned && (
        <div className="lacts">
          <button className="btn sm" onClick={onUpload}>{l.hasFile ? 'Replace' : 'Upload'}</button>
          {l.hasFile && <button className="btn sm" onClick={onStrip}>Remove file</button>}
          <button className="btn sm" onClick={onEdit}>Edit</button>
          <button className="btn danger sm" onClick={onDelete}>Delete</button>
        </div>
      )}
    </div>
  );
}

function CoursePage() {
  const me = Auth.me, signed = !!Auth.token && !!me;
  const c = useAsync(() => api('/courses/' + courseId), []);
  const ls = useAsync(() => api(`/courses/${courseId}/lessons`), []);
  const en = useAsync(() => signed ? api('/enrollments/me?page=0&size=100') : Promise.resolve(null), []);
  const [editing, setEditing] = useState(false);
  const [lessonModal, setLessonModal] = useState(null);
  const [upload, setUpload] = useState(null);

  const course = c.data;
  const lessons = ls.data || [];
  const owned = signed && course && me.id === course.instructorId;
  const enrolled = rowsOf(en.data).some(e => e.courseId === courseId);

  const setStatus = status => call(async () => {
    await api('/courses/' + courseId, { method: 'PUT', body: JSON.stringify({ title: course.title, description: course.description, status }) });
    c.reload();
  }, status === 'PUBLISHED' ? 'Course published' : 'Moved to draft', course.title, "Couldn't change visibility");

  const removeCourse = async () => {
    if (!confirm(`Delete "${course.title}"? Its lessons go too. This can't be undone.`)) return;
    call(async () => { await api('/courses/' + courseId, { method: 'DELETE' }); location.href = 'teaching.html'; }, 'Course deleted', course.title, "Couldn't delete the course");
  };
  const enroll   = () => call(async () => { await api('/enrollments', { method: 'POST', body: JSON.stringify({ courseId }) }); en.reload(); }, 'Enrolled', `You're in "${course.title}".`, "Couldn't enroll");
  const unenroll = () => confirm('Unenroll from this course?') && call(async () => { await api('/enrollments/' + courseId, { method: 'DELETE' }); en.reload(); }, 'Unenrolled', course.title, "Couldn't unenroll");
  const openFile = id => call(async () => { const r = await api(`/courses/${courseId}/lessons/${id}/file`); window.open(r.url, '_blank'); }, null, null, "Couldn't open the file");
  const stripFile = id => confirm('Remove the file from this lesson?') && call(async () => { await api(`/courses/${courseId}/lessons/${id}/file`, { method: 'DELETE' }); ls.reload(); }, 'File removed', 'The file was deleted from storage.', "Couldn't remove the file");
  const removeLesson = id => confirm('Delete this lesson?') && call(async () => { await api(`/courses/${courseId}/lessons/${id}`, { method: 'DELETE' }); ls.reload(); }, 'Lesson deleted', 'It was removed from this course.', "Couldn't delete the lesson");

  const back = <button className="back" onClick={() => history.length > 1 ? history.back() : location.href = 'catalog.html'}>← Back</button>;
  if (!courseId) return <Layout>{back}<Empty icon="⚠" title="No course selected">Open a course from the catalog.</Empty></Layout>;
  if (c.loading) return <Layout>{back}<div className="skel" style={{ height: 230 }} /></Layout>;
  if (c.error) return <Layout>{back}<Empty icon="⚠" title="Couldn't open this course">{c.error}</Empty></Layout>;

  const action = owned ? (<>
      {course.status === 'DRAFT' ? <button className="btn primary sm" onClick={() => setStatus('PUBLISHED')}>Publish</button>
                                 : <button className="btn sm" onClick={() => setStatus('DRAFT')}>Move to draft</button>}
      <button className="btn sm" onClick={() => setEditing(true)}>Edit</button>
      <button className="btn danger sm" onClick={removeCourse}>Delete</button></>)
    : !signed ? <a className="btn primary sm" href="login.html">Sign in to enroll</a>
    : course.status !== 'PUBLISHED' ? null
    : enrolled ? (<><span className="pill live">Enrolled</span><button className="btn ghost sm" onClick={unenroll}>Unenroll</button></>)
    : <button className="btn primary sm" onClick={enroll}>Enroll</button>;

  return (
    <Layout>
      {back}
      <div className="detail">
        <div className="detail-band" style={{ background: spineFor(course.id) }} />
        <div className="detail-in">
          <h2>{course.title}</h2>
          <p className="lede">{course.description || 'No description yet.'}</p>
          <div className="meta">
            <div className="by"><div className="avatar">{initials(course.instructorName)}</div><span>{course.instructorName || 'Unknown'}</span></div>
            <Pill status={course.status} />
            <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>{action}</div>
          </div>
        </div>
      </div>

      <div className="lessons">
        <div className="lessons-head">
          <h3>Lessons</h3>
          {owned && <button className="btn primary sm" onClick={() => setLessonModal({})}>+ Add lesson</button>}
        </div>
        {ls.loading ? <div style={{ padding: 22 }}><div className="skel" style={{ height: 62 }} /></div>
          : ls.error ? <div style={{ padding: 26, color: 'var(--danger)' }}>{ls.error}</div>
          : lessons.length === 0 ? <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--muted)' }}>{owned ? 'No lessons yet — add the first one.' : 'This course has no lessons yet.'}</div>
          : lessons.map(l => <Lesson key={l.id} l={l} owned={owned}
              onOpen={() => openFile(l.id)} onUpload={() => setUpload({ lessonId: l.id, replace: l.hasFile })}
              onStrip={() => stripFile(l.id)} onEdit={() => setLessonModal(l)} onDelete={() => removeLesson(l.id)} />)}
      </div>

      <CourseModal open={editing} course={course} onClose={() => setEditing(false)} onSaved={c.reload} />
      <LessonModal lesson={lessonModal} nextPos={lessons.length + 1} onClose={() => setLessonModal(null)} onSaved={ls.reload} />
      <UploadModal target={upload} onClose={() => setUpload(null)} onDone={ls.reload} />
    </Layout>
  );
}
mount(CoursePage);
