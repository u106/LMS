const { Layout, Empty, Skeletons, useAsync, ok, err, spineFor, rowsOf, mount } = window.UI;

function MyLearning() {
  const q = useAsync(() => api('/enrollments/me?page=0&size=48'), []);
  const list = rowsOf(q.data);

  async function leave(e) {
    if (!confirm(`Unenroll from "${e.courseTitle}"?`)) return;
    try { await api('/enrollments/' + e.courseId, { method: 'DELETE' }); ok('Unenrolled', e.courseTitle); q.reload(); }
    catch (x) { err("Couldn't unenroll", x.message); }
  }
  return (
    <Layout roles={['STUDENT', 'INSTRUCTOR']}>
      <div className="phead">
        <div><h2>My learning{!q.loading && <span className="count">{list.length}</span>}</h2><p>Courses you're enrolled in.</p></div>
        <a className="btn" href="catalog.html">Browse catalog</a>
      </div>
      {q.loading ? <Skeletons n={3} /> : q.error ? <div className="grid"><Empty icon="⚠" title="Couldn't load enrollments">{q.error}</Empty></div> :
        list.length === 0 ? <div className="grid"><Empty icon="🎓" title="Nothing here yet" action={<a className="btn primary" href="catalog.html">Find a course</a>}>Enroll in a published course and it will show up here.</Empty></div> :
        <div className="grid">{list.map(e => (
          <div className="card" key={e.id} style={{ cursor: 'default' }}>
            <div className="card-top" style={{ background: spineFor(e.courseId) }} />
            <div className="card-body">
              <h3>{e.courseTitle}</h3>
              <p className="desc">Enrolled on {new Date(e.enrolledAt).toLocaleDateString()}</p>
              <div className="card-foot">
                <a className="btn primary sm" href={'course.html?id=' + e.courseId}>Open course</a>
                <button className="btn ghost sm" style={{ marginLeft: 'auto' }} onClick={() => leave(e)}>Unenroll</button>
              </div>
            </div>
          </div>))}</div>}
    </Layout>
  );
}
mount(MyLearning);
