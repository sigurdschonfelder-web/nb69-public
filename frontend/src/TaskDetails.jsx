import { useState } from 'react';

const checklists = [
  ['Rydd kjøkkenbenken og vasken', 'Vask benker, komfyr og vask', 'Tørk av bordet og ta ut søpla'],
  ['Rydd fellesarealene', 'Tørk støv på tilgjengelige flater', 'Støvsug stue og felles gulv'],
  ['Vask toalett, servant og speil', 'Vask dusjen og gulvet', 'Tøm søpla på badet'],
  ['Samle papp og glass', 'Brett pappen og sorter emballasjen', 'Lever på riktig returpunkt'],
  ['Samle flasker og bokser med pant', 'Ta med panten og lever den', 'Rydd plassen der panten står'],
];

export default function TaskDetails({ task, busy, onComplete }) {
  const [comment, setComment] = useState('');
  return <>
    <details className="task-details"><summary>Hva innebærer oppgaven?</summary>
      <p className="muted">Forslag til sjekkliste. Kryssene er bare en huskeliste og lagres ikke.</p>
      <ul className="checklist">{(checklists[task.id] || []).map(item => <li key={item}><label><input type="checkbox"/>{item}</label></li>)}</ul>
    </details>
    {!task.completedAt && <form className="completion-form" onSubmit={async event => {event.preventDefault(); if (await onComplete(comment)) setComment('');}}>
      <details className="task-details"><summary>Legg igjen en kommentar</summary><label>Kommentar (valgfritt)<textarea value={comment} onChange={event => setComment(event.target.value)} maxLength={500} rows={3} disabled={busy} placeholder="For eksempel: Vi er tomme for oppvaskmiddel."/></label><small>Synlig for alle i leiligheten · {comment.length}/500 tegn</small></details>
      <button className="primary" disabled={busy}>{busy ? 'Lagrer…' : 'Bekreft utført'}</button>
    </form>}
    {task.completedAt && <><div className="primary completed">✓ Utført</div>{task.comment && <p className="task-comment">{task.comment}</p>}</>}
  </>;
}
