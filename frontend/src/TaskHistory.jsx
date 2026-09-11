import { useEffect, useState } from 'react';
import Avatar from './Avatar';
const when = value => new Intl.DateTimeFormat('nb-NO', {day:'numeric',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit',timeZone:'Europe/Oslo'}).format(new Date(value));
export default function TaskHistory({ request, revision }) {
  const [open, setOpen] = useState(false);
  const [weeks, setWeeks] = useState(null);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    if (!open) return;
    let active = true;
    request('/history').then(data => {if(active){setWeeks(data);setError('');}}).catch(err => {if(active)setError(err.message);});
    return () => {active=false;};
  }, [open, request, revision, retry]);
  return <details className="next-week history" onToggle={event => setOpen(event.currentTarget.open)}><summary>Historikk <span>Siste 12 uker</span></summary>
    <p className="muted">Ansvar, tidspunkt og kommentarer for hele leiligheten. Uker vises fra dere begynte å bruke siden.</p>
    {error && <div role="alert" className="error">{error}<button className="text-button" onClick={() => setRetry(retry+1)}>Prøv igjen</button></div>}
    {!weeks && !error && <p role="status">Henter historikk…</p>}
    {weeks?.length === 0 && <p>Ingen oppgaver registrert ennå.</p>}
    {weeks?.map(week => <details className="history-week" key={week.start}><summary>Uke {week.week} · {week.start}<span>{week.assignments.filter(task=>task.completedAt).length} av {week.assignments.length} utført</span></summary>
      <ul className="task-list">{week.assignments.map(task => <li key={task.id}><div className="history-entry"><strong>{task.task}</strong><div className="resident-label"><Avatar username={task.username} name={task.person} version={task.avatarVersion}/>{task.person}</div><p className={task.completedAt ? 'done' : 'muted'}>{task.completedAt ? `Utført ${when(task.completedAt)}${task.late ? ' · Etter fristen' : ' · Innen fristen'}` : new Date(week.dueAt) <= new Date() ? 'Ikke utført · Fristen er passert' : 'Gjenstår'}</p>{task.comment && <p className="task-comment">{task.comment}</p>}</div></li>)}</ul>
    </details>)}
  </details>;
}
