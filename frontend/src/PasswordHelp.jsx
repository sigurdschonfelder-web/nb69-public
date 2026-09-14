import { useState } from 'react';
const json = body => ({method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
export default function PasswordHelp({ request, token, onBack, people, onReset }) {
  const [error,setError]=useState('');const [notice,setNotice]=useState('');const [busy,setBusy]=useState(false);
  async function submit(event){
    event.preventDefault();const form=new FormData(event.currentTarget);setError('');setNotice('');
    if(token && form.get('password')!==form.get('repeat')){setError('Passordene er ikke like.');return;}
    setBusy(true);
    try{
      if(token){await request('/password-reset/confirm',json({token,password:form.get('password')}));onReset();}
      else{const result=await request('/password-reset/request',json({username:form.get('username')}));setNotice(result.message);}
    }catch(err){setError(err.message);}finally{setBusy(false);}
  }
  return <main className="login"><h1>{token?'Sett nytt passord':'Glemt passord?'}</h1><p className="intro">{token?'Velg et nytt passord med minst 12 tegn.':'Vi sender en lenke til den bekreftede e-postadressen din.'}</p>
    <form className="panel login-form" onSubmit={submit}>
      {!token&&<label>Hvem er du?<select name="username" disabled={busy}>{people.map(([id,name])=><option value={id} key={id}>{name}</option>)}</select></label>}
      {token&&<><label>Nytt passord<input name="password" type="password" autoComplete="new-password" required minLength={12} maxLength={72} disabled={busy}/></label><label>Gjenta passordet<input name="repeat" type="password" autoComplete="new-password" required minLength={12} maxLength={72} disabled={busy}/></label></>}
      {error&&<p role="alert" className="error">{error}</p>}{notice&&<p role="status" className="success">{notice}</p>}
      <button className="primary" disabled={busy}>{busy?'Et øyeblikk…':token?'Lagre nytt passord':'Send passordlenke'}</button>
      {!token&&<p className="muted">Har du ikke bekreftet en e-postadresse, kan Eilif eller Sigurd fortsatt lage en aktiveringskode til deg. Den lokale testsiden sender ikke e-post.</p>}
      <button type="button" className="text-button" disabled={busy} onClick={onBack}>Tilbake til innlogging</button>
    </form></main>;
}
