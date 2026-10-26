const { Layout, Banner,	 CourseList, Empty, useAsync, rowsOf, mount } = window.UI;
const { useState } = React;

function Catalog() {
  const [page, setPage] = useState(0);
  const [term, setTerm] = useState('');
  const q = useAsync(() => api(`/courses?page=${page}&size=12`), [page]);
  const t = term.trim().toLowerCase();
  const list = rowsOf(q.data).filter(c => !t || (c.title + ' ' + (c.description || '')).toLowerCase().includes(t));
  const pages = q.data?.totalPages || 1;
  return (
    <Layout>
	<Banner title="Learn better, together." sub="Pick a course, study with friends, and grow one lesson at a time." />
      <div className="phead">
        <div><h2>Course catalog{!q.loading && <span className="count">{list.length}</span>}</h2><p>Published courses, open to everyone.</p></div>
        <div className="toolbar"><input placeholder="Search this page…" value={term} onChange={e => setTerm(e.target.value)} /></div>
      </div>
      <CourseList q={q} list={list} empty={<Empty icon="📚" title="No courses found">Try a different search, or check back once instructors publish.</Empty>} />
      {pages > 1 && (
        <div className="pager">
          <button className="btn sm" disabled={page === 0} onClick={() => setPage(page - 1)}>← Prev</button>
          <span className="mono">Page {page + 1} / {pages}</span>
          <button className="btn sm" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next →</button>
        </div>
      )}
    </Layout>
  );
}
mount(Catalog);
