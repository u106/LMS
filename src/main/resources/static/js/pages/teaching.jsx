const { Layout, CourseList, CourseModal, Empty, useAsync, rowsOf, mount } = window.UI;
const { useState } = React;

function Teaching() {
  const q = useAsync(() => api('/courses/me?page=0&size=48'), []);
  const [open, setOpen] = useState(false);
  const list = rowsOf(q.data);
  const newBtn = <button className="btn primary" onClick={() => setOpen(true)}>+ New course</button>;
  return (
    <Layout roles={['INSTRUCTOR']}>
      <div className="phead">
        <div><h2>Your courses{!q.loading && <span className="count">{list.length}</span>}</h2><p>Drafts stay private until you publish them.</p></div>
        {newBtn}
      </div>
      <CourseList q={q} list={list} empty={<Empty icon="✎" title="You haven't created a course yet" action={newBtn}>Start with a title and a short description — you can add lessons next.</Empty>} />
      <CourseModal open={open} course={null} onClose={() => setOpen(false)} onSaved={q.reload} />
    </Layout>
  );
}
mount(Teaching);
