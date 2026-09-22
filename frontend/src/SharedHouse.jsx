import { useEffect, useState } from 'react';
const json = (method, body) => ({method,headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
import { weekLabel, weekRange } from './dates';
const statuses = {ACCEPTED:'Byttet er avtalt',DECLINED:'Avslått',CANCELLED:'Trukket tilbake',EXPIRED:'Ikke lenger mulig å gjennomføre'};
export default function SharedHouse({ user, request, refresh, revision, mode }) {
  const [data,setData]=useState(null);
  const [error,setError]=useState('');
  const [notice,setNotice]=useState('');
  const [busy,setBusy]=useState(false);
  const [awayWeek,setAwayWeek]=useState('');
  const [awayKey,setAwayKey]=useState('');
  const [person,setPerson]=useState('');
  const [returnWeek,setReturnWeek]=useState('');
  const [returnKey,setReturnKey]=useState('');
  const [name,setName]=useState('');
  const [retry,setRetry]=useState(0);
  async function load(){const [weeks,swaps,items]=await Promise.all([request('/shared/weeks'),request('/shared/swaps'),request('/shared/shopping')]);return {weeks,swaps,items};}
  useEffect(()=>{
    let active=true;
    Promise.all([request('/shared/weeks'),request('/shared/swaps'),request('/shared/shopping')]).then(([weeks,swaps,items])=>{if(active){setData({weeks,swaps,items});setError('');}}).catch(err=>{if(active)setError(err.message);});
    return ()=>{active=false;};
  },[request,revision,retry]);
  async function run(action,message){
    setBusy(true);setError('');setNotice('');
    try{const result=await action();setData(await load());await refresh();setNotice(result?.status==='EXPIRED'?'Byttet kunne ikke gjennomføres fordi en oppgave er endret, utført eller fristen er passert.':message);return true;}
    catch(err){setError(err.message);return false;}finally{setBusy(false);}
  }
  const options=data?.weeks.flatMap(week=>week.assignments.filter(task=>!task.completedAt).map(task=>({...task,start:week.start,week:week.week,key:`${week.start}/${task.id}`}))) || [];
  const own=options.filter(task=>task.username===user.username);
  const ownWeeks=data?.weeks.filter(week=>own.some(task=>task.start===week.start)) || [];
  const awayTasks=own.filter(task=>task.start===awayWeek);
  const away=awayTasks.length===1 ? awayTasks[0] : awayTasks.find(task=>task.key===awayKey);
  const returns=options.filter(task=>away && task.start>away.start && task.username!==user.username);
  const people=[...new Map(returns.map(task=>[task.username,task.person])).entries()];
  const returnWeeks=data?.weeks.filter(week=>returns.some(task=>task.start===week.start && task.username===person)) || [];
  const returnTasks=returns.filter(task=>task.start===returnWeek && task.username===person);
  const back=returnTasks.length===1 ? returnTasks[0] : returnTasks.find(task=>task.key===returnKey);
  const remaining=data?.items.filter(item=>!item.boughtBy).length || 0;
  const incoming=data?.swaps.filter(swap=>swap.status==='PENDING' && swap.recipient===user.username).length || 0;
  return <section className="shared-house" aria-label="Felles i leiligheten">
    <h1>{mode==='swaps' ? 'Bytte oppgave / borte' : 'Handleliste'}</h1>
    {error&&<div className="error" role="alert">{error}<button className="text-button" onClick={()=>setRetry(retry+1)}>Prøv igjen</button></div>}
    {notice&&<p className="success" role="status">{notice}</p>}
    {!data&&!error&&<p role="status">Henter bytter og handleliste…</p>}
    {mode==='swaps'&&!!incoming&&<p className="notice" role="status">Du har {incoming} bytteforespørsel{incoming===1?'':'er'} å svare på.</p>}
    {data&&<>{mode==='swaps'&&<section>
      <p className="muted">Be noen ta oppgaven din, og ta deres en senere uke. Ansvaret ditt gjelder til den andre godtar. Dere kan planlegge åtte uker framover.</p>
      <form className="shared-form" onSubmit={async event=>{event.preventDefault();if(!away||!back||!returns.some(task=>task.key===back.key))return;if(await run(()=>request('/shared/swaps',json('POST',{awayWeek:away.start,awayTask:away.id,returnWeek:back.start,returnTask:back.id})),'Forespørselen er sendt. Ansvaret endres når den andre godtar.')){setAwayWeek('');setAwayKey('');setPerson('');setReturnWeek('');setReturnKey('');}}}>
        <fieldset className="swap-step" disabled={busy}><legend>1. Når trenger du hjelp?</legend>
          <label>Uka jeg er borte<select required value={awayTasks.length ? awayWeek : ''} onChange={event=>{setAwayWeek(event.target.value);setAwayKey('');setPerson('');setReturnWeek('');setReturnKey('');}}><option value="">Velg uke</option>{ownWeeks.map(week=><option key={week.start} value={week.start}>{weekLabel(week)}</option>)}</select></label>
          {awayTasks.length>1&&<label>Oppgaven jeg bytter bort<select required value={away?.key || ''} onChange={event=>{setAwayKey(event.target.value);setPerson('');setReturnWeek('');setReturnKey('');}}><option value="">Velg oppgave</option>{awayTasks.map(task=><option key={task.key} value={task.key}>{task.task}</option>)}</select></label>}
          {away&&<p className="selected-task"><span>Ditt ansvar</span><strong>{away.task}</strong><small>{weekRange(away.start)}</small></p>}
        </fieldset>
        <fieldset className="swap-step" disabled={busy||!away}><legend>2. Hvem vil du bytte med?</legend>
          <label>Beboer<select required value={people.some(([id])=>id===person) ? person : ''} onChange={event=>{setPerson(event.target.value);setReturnWeek('');setReturnKey('');}}><option value="">Velg person</option>{people.map(([id,name])=><option key={id} value={id}>{name}</option>)}</select></label>
        </fieldset>
        <fieldset className="swap-step" disabled={busy||!away||!people.some(([id])=>id===person)}><legend>3. Når tar du en oppgave tilbake?</legend>
          <label>Uka jeg tar tilbake<select required value={returnTasks.length ? returnWeek : ''} onChange={event=>{setReturnWeek(event.target.value);setReturnKey('');}}><option value="">Velg en senere uke</option>{returnWeeks.map(week=><option key={week.start} value={week.start}>{weekLabel(week)}</option>)}</select></label>
          {returnTasks.length>1&&<label>Oppgaven jeg tar tilbake<select required value={back?.key || ''} onChange={event=>setReturnKey(event.target.value)}><option value="">Velg oppgave</option>{returnTasks.map(task=><option key={task.key} value={task.key}>{task.task}</option>)}</select></label>}
          {back&&<p className="selected-task"><span>Du tar over</span><strong>{back.task}</strong><small>{weekRange(back.start)}</small></p>}
        </fieldset>
        {away&&!returns.length&&<p className="muted">Ingen senere oppgaver tilgjengelig i perioden. Velg en tidligere uke.</p>}
        {away&&back&&<p className="swap-summary">{back.person} tar {away.task.toLowerCase()} for deg i uke {away.week}. Du tar {back.task.toLowerCase()} for {back.person} i uke {back.week}.</p>}
        <button className="primary" disabled={busy||!away||!back||!returns.some(task=>task.key===back.key)}>Send bytteforespørsel</button>
      </form>
      <h3>Dine bytteforespørsler</h3>
      {!data.swaps.length&&<p className="muted">Ingen forespørsler ennå.</p>}
      {data.swaps.map(swap=><article className="swap-card" key={swap.id}><strong>{swap.requesterName} ↔ {swap.recipientName}</strong><p>{swap.recipientName} tar {swap.awayTask} · {weekRange(swap.awayWeek)}.</p><p>{swap.requesterName} tar {swap.returnTask} · {weekRange(swap.returnWeek)}.</p>
        <small>{swap.status==='PENDING'?`Venter på ${swap.recipientName}`:statuses[swap.status]}</small>
        {swap.status==='PENDING'&&<div className="shared-actions">{swap.recipient===user.username?<><button className="secondary" disabled={busy} onClick={()=>run(()=>request(`/shared/swaps/${swap.id}/decision`,json('POST',{action:'ACCEPT'})),'Byttet er godtatt og oppgavene er oppdatert.')}>Godta byttet</button><button className="text-button" disabled={busy} onClick={()=>run(()=>request(`/shared/swaps/${swap.id}/decision`,json('POST',{action:'DECLINE'})),'Forespørselen er avslått.')}>Avslå</button></>:<button className="text-button" disabled={busy} onClick={()=>run(()=>request(`/shared/swaps/${swap.id}/decision`,json('POST',{action:'CANCEL'})),'Forespørselen er trukket tilbake.')}>Trekk tilbake</button>}</div>}
      </article>)}
    </section>}
    {mode==='shopping'&&<section><p className="intro">{remaining} {remaining===1?'vare':'varer'} gjenstår</p>
      <form className="shared-form" onSubmit={async event=>{event.preventDefault();if(await run(()=>request('/shared/shopping',json('POST',{name})),'Varen er lagt til.'))setName('');}}><label>Hva mangler vi?<input value={name} onChange={event=>setName(event.target.value)} maxLength={120} required disabled={busy} placeholder="For eksempel dopapir"/></label><button className="secondary" disabled={busy||!name.trim()}>Legg til</button></form>
      {!data.items.some(item=>!item.boughtBy)&&<p className="muted">Alt på listen er kjøpt. Legg til det dere mangler.</p>}
      <ul className="shopping-list">{data.items.filter(item=>!item.boughtBy).map(item=><li key={item.id}><label><input type="checkbox" disabled={busy} checked={false} onChange={()=>run(()=>request(`/shared/shopping/${item.id}`,json('PUT',{bought:true})),'Markert som kjøpt.')}/><span>{item.name}<small>Lagt til av {item.addedName}</small></span></label></li>)}</ul>
      {data.items.some(item=>item.boughtBy)&&<details><summary className="bought-summary">Kjøpte varer</summary><ul className="shopping-list">{data.items.filter(item=>item.boughtBy).map(item=><li key={item.id}><label><input type="checkbox" disabled={busy} checked onChange={()=>run(()=>request(`/shared/shopping/${item.id}`,json('PUT',{bought:false})),'Varen er tilbake på handlelisten.')}/><span><s>{item.name}</s><small>Kjøpt av {item.boughtName} · fjern krysset for å angre</small></span></label></li>)}</ul></details>}
    </section>}</>}
  </section>;
}
