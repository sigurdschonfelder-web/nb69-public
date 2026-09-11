import { useEffect, useState } from 'react';
import { Circle, CircleCheck, Recycle, LogOut, Settings, ArrowLeft, House, TriangleAlert, Bell } from 'lucide-react';
import './index.css';
import EmailProfileForm from './EmailProfileForm';
import Avatar from './Avatar';
import TaskDetails from './TaskDetails';
import TaskHistory from './TaskHistory';
import SharedHouse from './SharedHouse';

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

function EmailSettings() {
  const [settings, setSettings] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { request('/admin/email').then(setSettings).catch(err => setError(err.message)); }, []);
  const statuses = { ACCEPTED:'Mottatt av e-posttjenesten', CLAIMED:'Uavklart – sjekk e-posttjenesten', UNCERTAIN:'Feil eller uavklart – sjekk e-posttjenesten', SKIPPED:'Utelatt' };
  return <section className="panel email-section"><h2>E-postpåminnelser</h2>
    <p className="muted">Søndag kl. 14 og mandag kl. 08, norsk tid. Bare oppgaver som fortsatt ikke er bekreftet utløser e-post.</p>
    {error && <p role="alert" className="error">{error}</p>}
    {!settings && !error && <p role="status">Henter e-postoppsett…</p>}
    {settings && <><p className={settings.ready ? 'success' : 'notice'}>{settings.ready ? 'E-postutsending er aktivert.' : 'E-postutsending er ikke aktivert. E-postadressene kan registreres i Min profil nå; ingen meldinger sendes før tjenesten er koblet til.'}</p>
    <p className="muted">Hver beboer legger inn og bekrefter sin egen adresse i Min profil.</p>
    {settings.contacts.map(contact => <div className="email-contact" key={contact.username}><strong>{contact.name}</strong><span>{contact.email || 'Ingen adresse registrert'}</span><small>{contact.verified ? 'Bekreftet' : contact.email ? 'Venter på bekreftelse' : 'Ikke registrert'}</small></div>)}
    {settings.attempts.length > 0 && <><h3>Siste påminnelser</h3>{settings.attempts.map(attempt => <div className="email-log" key={`${attempt.start}-${attempt.taskId}-${attempt.kind}`}><strong>{attempt.name} · {attempt.kind === 'SUNDAY' ? 'Søndagspåminnelse' : 'Mandagspurring'}</strong><small>{statuses[attempt.status]} · {when(attempt.attemptedAt)}</small></div>)}<p className="muted">Mottatt av e-posttjenesten betyr ikke at e-posten er levert til innboksen. Uavklarte forsøk sendes ikke automatisk på nytt.</p></>}
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
    <EmailSettings/>
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
  const [profileOpen, setProfileOpen] = useState(false);
  const [confirmation, setConfirmation] = useState(() => new URLSearchParams(window.location.hash.slice(1)).get('confirm-email'));
  const [emailVerified, setEmailVerified] = useState(true);
  useEffect(() => {
    const readLink=() => {
      const token=new URLSearchParams(window.location.hash.slice(1)).get('confirm-email');
      if(token) {setConfirmation(token);setProfileOpen(true);window.history.replaceState(null,'',window.location.pathname+window.location.search);}
    };
    readLink();window.addEventListener('hashchange',readLink);
    return () => window.removeEventListener('hashchange',readLink);
  }, []);
  useEffect(() => {if(user) request('/profile/email').then(profile=>setEmailVerified(profile.verified)).catch(()=>{});}, [user, profileOpen]);
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
  async function undoOwn(task, start) {
    if (!window.confirm(`Angre registreringen av ${task.task}? Oppgaven blir stående som ikke utført, og kommentaren fjernes.`)) return;
    setBusy(true); setError('');
    try {await request(`/weeks/${start}/tasks/${task.id}/completion`, {method:'DELETE'}); await refresh();}
    catch (err) {setError(err.message);} finally {setBusy(false);}
  }
  async function complete(task, start = current.start, comment = '') {
    setBusy(true); setError('');
    try {await request(`/weeks/${start}/tasks/${task.id}/completion`, json('POST', {comment})); await refresh(); return true;}
    catch (err) {setError(err.message); return false;} finally {setBusy(false);}
  }
  return <div className="app-shell"><header className="app-header"><a href="/" className="brand" aria-label="NB69 hjem">NB<span>69</span><em>.</em></a>{user && <div className="account"><div>{user.name}<small>{user.admin ? 'Administrator' : 'Beboer'}</small></div><Avatar username={user.username} name={user.name} version={user.avatarVersion}/></div>}</header>
    {loading ? <main><p role="status">Laster…</p></main> : !user ? <><Login onLogin={value => {setUser(value); setError('');}}/>{error && <p role="alert" className="error">{error}</p>}</> : <>
      <nav className="toolbar"><button className="text-button" onClick={() => {setProfileOpen(!profileOpen);setAdmin(false);setConfirmation(null);}}>{profileOpen ? 'Tilbake til oppgavene' : 'Min profil'}</button>{user.admin && <button className="text-button" onClick={() => {setAdmin(!admin);setProfileOpen(false);}}>{admin ? <ArrowLeft size={17}/> : <Settings size={17}/>} {admin ? 'Tilbake til oppgavene' : 'Administrasjon'}</button>}<button className="text-button logout" onClick={async () => {try {await request('/logout', {method:'POST'}); csrf=null; setUser(null); setWeeks([]); setOverdue([]); setAdmin(false);} catch(err) {setError(err.message);}}}><LogOut size={16}/> Logg ut</button></nav>
      <main>{error && <div className="error" role="alert">{error}<button className="text-button" onClick={() => refresh().then(() => setError('')).catch(err => setError(err.message))}>Prøv igjen</button></div>}
      {profileOpen || confirmation ? <EmailProfileForm user={user} onPhotoChanged={async () => {setUser(await request('/me'));await refresh();}} request={request} confirmation={confirmation} onConfirmed={() => {setConfirmation(null);setEmailVerified(true);}}/> : admin ? <Admin weeks={weeks} refresh={refresh}/> : <><p className="eyebrow">Hjemme på Bakklandet{current ? ` · uke ${current.week}` : ''}</p><h1>Hei, {user.name}.</h1><p className="intro">Litt fra hver. Et bedre sted å bo.</p>
      {!emailVerified && <div className="notice"><strong>Legg inn og bekreft e-postadressen din</strong><p>Da kan du få påminnelser om oppgavene dine.</p><button className="text-button" onClick={() => setProfileOpen(true)}>Åpne Min profil</button></div>}
      {!current && !error && <p role="status">Henter ukens oppgaver…</p>}
      {overdue.length > 0 && <section className="overdue-alert" aria-label="Oppgaver som har passert fristen"><div className="alert-heading"><TriangleAlert size={24}/><h2>Du har {overdue.length === 1 ? 'en oppgave' : `${overdue.length} oppgaver`} som haster</h2></div><p>Fristen er passert. Gjør ferdig oppgavene så snart som mulig og bekreft her.</p>{overdue.map(task => <article className="overdue-task" key={`${task.start}-${task.id}`}><h3>{task.task}</h3><p>Uke {task.week} · frist {when(new Date(new Date(task.dueAt).getTime()-1))}</p><TaskDetails task={task} busy={busy} onComplete={comment => complete(task,task.start,comment)}/></article>)}</section>}
      {current?.reminderDue && own.some(task => !task.completedAt) && <div className="deadline-reminder" role="status"><Bell size={21}/><div><strong>Husk oppgaven din i dag</strong><p>Ukens ansvar må være fullført før søndag er over.</p></div></div>}
      <div className="dashboard"><section className="my-tasks" aria-label="Dine ansvarsområder">
      {own.map(task => <article className="panel own-task" key={`${current.start}-${task.id}`}><div className="task-top"><span className="task-icon"><Recycle size={23}/></span><span>Ditt ansvar denne uka</span></div><h2>{task.task}</h2><p className="muted">Frist: søndag {when(new Date(new Date(current.dueAt).getTime()-1))}. Bekreft når du er ferdig.</p><TaskDetails task={task} busy={busy} onComplete={comment => complete(task,current.start,comment)} onUndo={() => undoOwn(task,current.start)}/>{task.completedAt && <p className="receipt" role="status">Bekreftet {when(task.completedAt)}</p>}</article>)}
      {current && own.length === 0 && <div className="panel"><h2>Ingen oppgaver denne uka</h2><p className="muted">Du har ikke fått tildelt et ansvarsområde.</p></div>}
      </section>{current && <section className="household"><div className="section-heading"><h2>Hele leiligheten</h2><span aria-live="polite">{count} av {current.assignments.length} utført</span></div><progress aria-label="Utførte oppgaver" value={count} max={current.assignments.length || 1}/><ul className="task-list">{current.assignments.map(task => <li key={task.id}><div><strong>{task.task}</strong><div className="resident-label"><Avatar username={task.username} name={task.person} version={task.avatarVersion}/><small>{task.person}{task.username === user.username ? ' · deg' : ''}</small></div>{task.completedAt && <small>{when(task.completedAt)}</small>}{task.comment && <p className="task-comment">{task.comment}</p>}</div><span className={`status ${task.completedAt ? 'done' : ''}`}>{task.completedAt ? <CircleCheck size={17}/> : <Circle size={17}/>} {task.completedAt ? 'Utført' : 'Gjenstår'}</span></li>)}</ul></section>}</div>
      {weeks[1] && <details className="next-week"><summary>Neste uke <span>Uke {weeks[1].week}</span></summary><ul className="task-list">{weeks[1].assignments.map(task => <li key={task.id}><strong>{task.task}</strong><span>{task.person}</span></li>)}</ul></details>}
      <SharedHouse user={user} request={request} refresh={refresh} revision={weeks}/>
      <TaskHistory request={request} revision={weeks} username={user.username} busy={busy} onUndo={undoOwn}/>
      </>}
      </main></>}
      <footer>Nedre Bakklandet 69</footer>
    </div>;
}
