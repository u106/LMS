const { Layout, AuthCard, Field, ok, err, homeFor, mount } = window.UI;
const { useState, useEffect } = React;

function Login() {
  const [f, setF] = useState({ email: '', password: '' });
  const [busy, setBusy] = useState(false);
  useEffect(() => { if (Auth.token && Auth.me) location.replace(homeFor(Auth.me.role)); }, []);

  async function submit() {
    if (!f.email.trim() || !f.password) return err('Missing details', 'Enter your email and password.');
    setBusy(true);
    try { const me = await signIn(f.email.trim(), f.password); location.href = homeFor(me.role); }
    catch (e) { err("Couldn't sign in", e.message); setBusy(false); }
  }
  return (
    <Layout>
      <AuthCard title="Sign in" sub="Pick up where you left off." label="Sign in" busy={busy} onSubmit={submit}
                swap={<>New here? <a href="register.html" style={{ color: 'var(--primary)', fontWeight: 600 }}>Create an account</a></>}>
        <Field label="Email"><input type="email" autoComplete="email" placeholder="you@example.com" value={f.email} onChange={e => setF({ ...f, email: e.target.value })} /></Field>
        <Field label="Password"><input type="password" autoComplete="current-password" placeholder="••••••••" value={f.password} onChange={e => setF({ ...f, password: e.target.value })} /></Field>
      </AuthCard>
    </Layout>
  );
}
mount(Login);
