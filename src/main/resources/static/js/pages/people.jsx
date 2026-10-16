const { Layout, Empty, useAsync, initials, mount } = window.UI;

function People() {
  const q = useAsync(() => api('/users'), []);
  const list = q.data || [];
  return (
    <Layout roles={['ADMIN']}>
      <div className="phead">
        <div><h2>People{!q.loading && <span className="count">{list.length}</span>}</h2><p>Everyone registered on the platform.</p></div>
      </div>
      {q.loading ? <div className="skel" style={{ height: 240 }} /> : q.error ? <Empty icon="⚠" title="Couldn't load people">{q.error}</Empty> :
        <div className="tbl-wrap"><table className="tbl">
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th></tr></thead>
          <tbody>{list.map(u => (
            <tr key={u.id || u.email}>
              <td><div className="by"><div className="avatar">{initials(u.name)}</div><span>{u.name}</span></div></td>
              <td className="mono">{u.email}</td>
              <td><span className="pill role">{u.role}</span></td>
              <td style={{ color: 'var(--muted)' }}>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}</td>
            </tr>))}</tbody>
        </table></div>}
    </Layout>
  );
}
mount(People);
