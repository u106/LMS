const { Layout, Art, homeFor, mount } = window.UI;
const { useEffect } = React;

function Landing() {
  useEffect(() => { if (Auth.token && Auth.me) location.replace(homeFor(Auth.me.role)); }, []);
  const spines = [['Spring Security in depth', 'Filters, JWT, authorization'], ['Designing REST APIs', 'Contracts, pagination, versioning'], ['Data modelling with JPA', 'Entities, relationships, migrations']];
  return (
    <Layout>
      <div className="hero">
        <div>
          <div className="eyebrow"><span className="led" /> Built on Spring Boot</div>
          <h1>Learn backend engineering, <em>one course at a time.</em></h1>
          <p>Courses written by instructors who build for a living. Browse the catalog free — sign in when you're ready to start.</p>
          <div className="hero-cta">
            <a className="btn primary lg" href="catalog.html">Browse courses</a>
            <a className="btn lg" href="register.html">Create an account</a>
          </div>
        </div>
        <Art className="hero-art" src="/img/hero-students.jpg" alt="Students learning together"
             fallback={<div className="shelf">{spines.map(([t, s]) => <div className="spine" key={t}><b>{t}</b><span>{s}</span></div>)}</div>} />
      </div>
    </Layout>
  );
}
mount(Landing);