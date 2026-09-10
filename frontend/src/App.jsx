import { useEffect, useState } from 'react';
import { Check, Circle, CircleCheck, Recycle, LogOut, Settings, ArrowLeft, House, TriangleAlert, Bell } from 'lucide-react';
import './index.css';

let csrf;
async function request(path, options = {}) {
  if (options.method && options.method !== 'GET') {
    if (!csrf) {
      const response = await fetch('/api/csrf');
      if (!response.ok) throw new Error('Kunne ikke koble til. Prøv igjen.');
      csrf = await response.json();
    }
    options.headers = { ...options.headers, [csrf.headerName]: csrf.token };
  }
  const response = await fetch(`/api${path}`, options);
  if (response.status === 401) {
    window.dispatchEvent(new Event('nb69-session-ended'));
    throw new Error(path === '/login' ? 'Feil brukernavn eller passord.' : 'Logg inn igjen for å fortsette.');
  }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.message || 'Handlingen kunne ikke utføres. Oppdater siden og prøv igjen.');
  }
  return response.status === 204 ? null : response.json();
}
const json = (method, body) => ({ method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
const people = [['eilif','Eilif'],['sigurd','Sigurd'],['andreas','Andreas'],['jorgen','Jørgen'],['erlend','Erlend']];
const when = value => new Intl.DateTimeFormat('nb-NO', { day:'numeric', month:'short', hour:'2-digit', minute:'2-digit', timeZone:'Europe/Oslo' }).format(new Date(value));

function Login({ onLogin }) {
  const [activate, setActivate] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');
  async function submit(event) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true);
    const data = new FormData(event.currentTarget);
    try {
      if (activate) {
        await request('/activate', json('POST', Object.fromEntries(data)));
        setActivate(false); setNotice('Passordet er klart. Logg inn med det nye passordet ditt.');
      } else {
        await request('/login', { method:'POST', body:new URLSearchParams(data) });
        csrf = null;
        onLogin(await request('/me'));
      }
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }
  return <main className="login"><div className="welcome-icon"><House size={27}/></div><p className="eyebrow">Nedre Bakklandet 69</p><h1>Velkommen hjem.</h1><p className="intro">Logg inn for å se ukens ansvar.</p>
    <form onSubmit={submit} className="panel login-form" key={String(activate)}>
      <h2>{activate ? 'Sett ditt eget passord' : 'Logg inn'}</h2>
      <label>Hvem er du?<select name="username" required>{people.map(([id,name]) => <option key={id} value={id}>{name}</option>)}</select></label>
      {activate && <label>Aktiveringskode<input name="code" autoComplete="off" required /></label>}
      <label>Passord<input name="password" type="password" autoComplete={activate ? 'new-password' : 'current-password'} minLength={activate ? 12 : undefined} maxLength={72} required /></label>
      {activate && <p className="muted">Bruk minst 12 tegn. Du får aktiveringskode fra Eilif eller Sigurd.</p>}
      {error && <p className="error" role="alert">{error}</p>}{notice && <p className="success" role="status">{notice}</p>}
      <button className="primary" disabled={busy}>{busy ? 'Et øyeblikk…' : activate ? 'Lagre passord' : 'Logg inn'}</button>
      <button className="text-button" type="button" disabled={busy} onClick={() => {setActivate(!activate); setError(''); setNotice('');}}>{activate ? 'Tilbake til innlogging' : 'Ny bruker eller glemt passord?'}</button>
    </form>
  </main>;
}

function SmsSettings() {
  const [settings, setSettings] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => { request('/admin/sms').then(setSettings).catch(err => setError(err.message)); }, []);
  async function save(event, username) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    const phone = new FormData(event.currentTarget).get('phone');
    try {
      await request(`/admin/sms/${username}`, json('PUT', { phone }));
      setSettings(await request('/admin/sms')); setNotice('Mobilnummeret er lagret.');
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  const statuses = { ACCEPTED:'Mottatt av SMS-tjenesten', CLAIMED:'Uavklart – sjekk SMS-tjenesten', UNCERTAIN:'Feil eller uavklart – sjekk SMS-tjenesten', SKIPPED:'Utelatt' };
  return <section className="panel sms-section"><h2>SMS-påminnelser</h2>
    <p className="muted">Søndag kl. 14 og mandag kl. 08, norsk tid. Bare oppgaver som fortsatt ikke er bekreftet utløser SMS.</p>
    {error && <p role="alert" className="error">{error}</p>}{notice && <p role="status" className="success">{notice}</p>}
    {!settings && !error && <p role="status">Henter SMS-oppsett…</p>}
    {settings && <><p className={settings.ready ? 'success' : 'notice'}>{settings.ready ? 'SMS er aktivert.' : 'SMS er ikke aktivert. Mobilnumrene kan legges inn nå; ingen meldinger sendes før tjenesten er koblet til.'}</p>
    {settings.contacts.map(contact => <form className="sms-contact" key={contact.username} onSubmit={event => save(event,contact.username)}><label>{contact.name}<input type="tel" name="phone" autoComplete="off" defaultValue={contact.phone} placeholder="+47 …" aria-label={`Mobilnummer til ${contact.name}`} /></label><button className="secondary" disabled={busy}>Lagre mobilnummer</button></form>)}
    {settings.attempts.length > 0 && <><h3>Siste påminnelser</h3>{settings.attempts.map(attempt => <div className="sms-log" key={`${attempt.start}-${attempt.taskId}-${attempt.kind}`}><strong>{attempt.name} · {attempt.kind === 'SUNDAY' ? 'Søndagspåminnelse' : 'Mandagspurring'}</strong><small>{statuses[attempt.status]} · {when(attempt.attemptedAt)}</small></div>)}<p className="muted">Mottatt av SMS-tjenesten betyr ikke at SMS-en er levert til mobilen. Uavklarte forsøk sendes ikke automatisk på nytt.</p></>}
    </>}
  </section>;
}

function Admin({ weeks, refresh }) {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => { request('/admin/users').then(setUsers).catch(err => setError(err.message)); }, []);
  async function run(action) {
    setBusy(true); setError(''); setNotice('');
    try { await action(); } catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  return <><p className="eyebrow">For Eilif og Sigurd</p><h1>Administrasjon</h1><p className="intro">Administrer tilgang og ukens ansvar.</p>
    {error && <p role="alert" className="error">{error}</p>}{notice && <p role="status" className="notice">{notice}</p>}
    <section className="panel"><h2>Beboere</h2><p className="muted">Lag en engangskode når noen trenger å sette et passord. Koden gjelder i 24 timer.</p>
      {users.map(user => <div className="user-row" key={user.username}><div><strong>{user.name}</strong><small>{user.admin ? 'Administrator' : 'Beboer'} · {user.active ? 'Konto aktiv' : 'Venter på aktivering'}</small></div><button className="secondary" disabled={busy} onClick={() => run(async () => { const result = await request(`/admin/users/${user.username}/invite`, {method:'POST'}); setNotice(`Aktiveringskode for ${user.name}: ${result.code}. Del den privat med ${user.name}.`); })}>Lag kode</button></div>)}
    </section>
    <SmsSettings/>
    {weeks.map(week => <section className="panel admin-week" key={week.start}><h2>{week.label} · uke {week.week}</h2>{week.assignments.map(task => <div className="assignment-edit" key={task.id}><label>{task.task}<select aria-label={`Ansvarlig for ${task.task}, ${week.label}`} value={task.username} disabled={busy || !!task.completedAt} onChange={event => run(async () => {await request(`/admin/weeks/${week.start}/tasks/${task.id}`, json('PUT', {username:event.target.value})); await refresh();})}>{people.map(([id,name]) => <option key={id} value={id}>{name}</option>)}</select></label>{task.completedAt && <button className="text-button" disabled={busy} onClick={() => { if (window.confirm(`Fjerne bekreftelsen for ${task.task}? Oppgaven vises da som ikke utført.`)) run(async () => {await request(`/admin/weeks/${week.start}/tasks/${task.id}/completion`, {method:'DELETE'}); await refresh();}); }}>Angre registrering</button>}</div>)}</section>)}
  </>;
}

export default function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [weeks, setWeeks] = useState([]);
  const [overdue, setOverdue] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [admin, setAdmin] = useState(false);
  async function refresh() { const data = await request('/dashboard'); setWeeks(data.weeks); setOverdue(data.overdue); }
  useEffect(() => {
    function expired() { setUser(null); setWeeks([]); setOverdue([]); setAdmin(false); csrf = null; }
    window.addEventListener('nb69-session-ended', expired);
    request('/me').then(setUser).catch(err => { if (!err.message.includes('Logg inn igjen')) setError(err.message); }).finally(() => setLoading(false));
    return () => window.removeEventListener('nb69-session-ended', expired);
  }, []);
  useEffect(() => {
    if (!user) return;
    let active = true;
    const load = () => request('/dashboard').then(data => { if (active) {setWeeks(data.weeks); setOverdue(data.overdue); setError('');} }).catch(err => {if (active) setError(err.message);});
    load(); const interval = setInterval(load, 60000);
    return () => {active = false; clearInterval(interval);};
  }, [user]);
  const current = weeks[0];
  const own = current?.assignments.filter(task => task.username === user?.username) || [];
  const count = current?.assignments.filter(task => task.completedAt).length || 0;
  async function complete(task, start = current.start) {
    setBusy(true); setError('');
    try {await request(`/weeks/${start}/tasks/${task.id}/completion`, {method:'POST'}); await refresh();}
    catch (err) {setError(err.message);} finally {setBusy(false);}
  }
  return <div className="app-shell"><header className="app-header"><a href="/" className="brand" aria-label="NB69 hjem">NB<span>69</span><em>.</em></a>{user && <div className="account"><div>{user.name}<small>{user.admin ? 'Administrator' : 'Beboer'}</small></div><span className="avatar" aria-hidden="true">{user.name[0]}</span></div>}</header>
    {loading ? <main><p role="status">Laster…</p></main> : !user ? <><Login onLogin={value => {setUser(value); setError('');}}/>{error && <p role="alert" className="error">{error}</p>}</> : <>
      <nav className="toolbar">{user.admin && <button className="text-button" onClick={() => setAdmin(!admin)}>{admin ? <ArrowLeft size={17}/> : <Settings size={17}/>} {admin ? 'Tilbake til oppgavene' : 'Administrasjon'}</button>}<button className="text-button logout" onClick={async () => {try {await request('/logout', {method:'POST'}); csrf=null; setUser(null); setWeeks([]); setOverdue([]); setAdmin(false);} catch(err) {setError(err.message);}}}><LogOut size={16}/> Logg ut</button></nav>
      <main>{error && <div className="error" role="alert">{error}<button className="text-button" onClick={() => refresh().then(() => setError('')).catch(err => setError(err.message))}>Prøv igjen</button></div>}
      {admin ? <Admin weeks={weeks} refresh={refresh}/> : <><p className="eyebrow">Hjemme på Bakklandet{current ? ` · uke ${current.week}` : ''}</p><h1>Hei, {user.name}.</h1><p className="intro">Litt fra hver. Et bedre sted å bo.</p>
      {!current && !error && <p role="status">Henter ukens oppgaver…</p>}
      {overdue.length > 0 && <section className="overdue-alert" aria-label="Oppgaver som har passert fristen"><div className="alert-heading"><TriangleAlert size={24}/><h2>Du har {overdue.length === 1 ? 'en oppgave' : `${overdue.length} oppgaver`} som haster</h2></div><p>Fristen er passert. Gjør ferdig oppgavene så snart som mulig og bekreft her.</p>{overdue.map(task => <article className="overdue-task" key={`${task.start}-${task.id}`}><h3>{task.task}</h3><p>Uke {task.week} · frist {when(new Date(new Date(task.dueAt).getTime()-1))}</p><button className="primary" disabled={busy} onClick={() => complete(task,task.start)}><Check size={20}/>{busy ? 'Lagrer…' : 'Bekreft utført'}</button></article>)}</section>}
      {current?.reminderDue && own.some(task => !task.completedAt) && <div className="deadline-reminder" role="status"><Bell size={21}/><div><strong>Husk oppgaven din i dag</strong><p>Ukens ansvar må være fullført før søndag er over.</p></div></div>}
      <div className="dashboard"><section className="my-tasks" aria-label="Dine ansvarsområder">
      {own.map(task => <article className="panel own-task" key={`${current.start}-${task.id}`}><div className="task-top"><span className="task-icon"><Recycle size={23}/></span><span>Ditt ansvar denne uka</span></div><h2>{task.task}</h2><p className="muted">Frist: søndag {when(new Date(new Date(current.dueAt).getTime()-1))}. Bekreft når du er ferdig.</p><button className={`primary ${task.completedAt ? 'completed' : ''}`} disabled={busy || !!task.completedAt} onClick={() => complete(task)}><Check size={20}/>{task.completedAt ? 'Utført' : busy ? 'Lagrer…' : 'Bekreft utført'}</button>{task.completedAt && <p className="receipt" role="status">Bekreftet {when(task.completedAt)}</p>}</article>)}
      {current && own.length === 0 && <div className="panel"><h2>Ingen oppgaver denne uka</h2><p className="muted">Du har ikke fått tildelt et ansvarsområde.</p></div>}
      </section>{current && <section className="household"><div className="section-heading"><h2>Hele leiligheten</h2><span aria-live="polite">{count} av {current.assignments.length} utført</span></div><progress aria-label="Utførte oppgaver" value={count} max={current.assignments.length || 1}/><ul className="task-list">{current.assignments.map(task => <li key={task.id}><div><strong>{task.task}</strong><small>{task.person}{task.username === user.username ? ' · deg' : ''}</small>{task.completedAt && <small>{when(task.completedAt)}</small>}</div><span className={`status ${task.completedAt ? 'done' : ''}`}>{task.completedAt ? <CircleCheck size={17}/> : <Circle size={17}/>} {task.completedAt ? 'Utført' : 'Gjenstår'}</span></li>)}</ul></section>}</div>
      {weeks[1] && <details className="next-week"><summary>Neste uke <span>Uke {weeks[1].week}</span></summary><ul className="task-list">{weeks[1].assignments.map(task => <li key={task.id}><strong>{task.task}</strong><span>{task.person}</span></li>)}</ul></details>}
      </>}
      </main></>}
      <footer>Nedre Bakklandet 69</footer>
    </div>;
}
