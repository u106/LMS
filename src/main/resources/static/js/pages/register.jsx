const { Layout, AuthCard, Field, ok, err, homeFor, mount } = window.UI;
const { useState, useEffect } = React;

function Register() {
  const [f, setF] = useState({ name: '', email: '', password: '' });
  const [busy, setBusy] = useState(false);
  useEffect(() => { if (Auth.token && Auth.me) location.replace(homeFor(Auth.me.role)); }, []);
  const set = k => e => setF({ ...f, [k]: e.target.value });

  async function submit() {
    if (!f.name.trim() || !f.email.trim() || !f.password) return err('Missing details', 'Fill in every field to continue.');
    if (f.password.length < 8) return err('Weak password', 'Use at least 8 characters.');
    setBusy(true);
    try {
      await api('/users/register', { method: 'POST', body: JSON.stringify({ name: f.name.trim(), email: f.email.trim(), password: f.password }) });
      const me = await signIn(f.email.trim(), f.password);
      location.href = homeFor(me.role);
    } catch (e) { err("Couldn't create account", e.message); setBusy(false); }
  }
  return (
    <Layout>
      <AuthCard title="Create your account" sub="Everyone starts as a student." label="Create account" busy={busy} onSubmit={submit}
                swap={<>Already have an account? <a href="login.html" style={{ color: 'var(--primary)', fontWeight: 600 }}>Sign in</a></>}>
        <Field label="Full name"><input autoComplete="name" placeholder="Ada Lovelace" value={f.name} onChange={set('name')} /></Field>
        <Field label="Email"><input type="email" autoComplete="email" placeholder="you@example.com" value={f.email} onChange={set('email')} /></Field>
        <Field label="Password" hint="Minimum 8 characters."><input type="password" autoComplete="new-password" placeholder="At least 8 characters" value={f.password} onChange={set('password')} /></Field>
      </AuthCard>
    </Layout>
  );
}
mount(Register);
