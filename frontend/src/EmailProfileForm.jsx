import { useEffect, useState } from 'react';
export default function EmailProfileForm({ request, confirmation, onConfirmed }) {
  const [profile,setProfile]=useState(null);
  const [notice,setNotice]=useState('');
  const [error,setError]=useState('');
  const [preview,setPreview]=useState(null);
  const [busy,setBusy]=useState(false);
  useEffect(()=>{request('/profile/email').then(setProfile).catch(err=>setError(err.message));},[request]);
  const json=(method,body)=>({method,headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  async function save(event){
    event.preventDefault();setBusy(true);setError('');setNotice('');setPreview(null);
    try{const result=await request('/profile/email',json('PUT',{email:new FormData(event.currentTarget).get('email')}));setProfile(result.profile);setNotice(result.message);setPreview(result.previewLink);}
    catch(err){setError(err.message);}finally{setBusy(false);}
  }
  async function confirm(){
    setBusy(true);setError('');
    try{setProfile(await request('/profile/email/confirm',json('POST',{token:confirmation})));setNotice('E-postadressen er bekreftet.');setPreview(null);onConfirmed();}
    catch(err){setError(err.message);}finally{setBusy(false);}
  }
  return <><p className="eyebrow">Din konto</p><h1>Min profil</h1><p className="intro">Motta påminnelser om oppgavene dine.</p>
    {error&&<p className="error" role="alert">{error}</p>}{notice&&<p className="notice" role="status">{notice}</p>}
    {confirmation&&<section className="panel"><h2>Bekreft e-postadressen</h2><p>Bekreft at du vil bruke adressen du registrerte på denne kontoen.</p><button className="primary" disabled={busy} onClick={confirm}>Bekreft e-postadresse</button></section>}
    {!profile&&!error&&<p role="status">Henter profilen din…</p>}
    {profile&&<form className="panel login-form" onSubmit={save}><h2>E-postpåminnelser</h2><label>E-postadressen din<input type="email" name="email" autoComplete="email" required maxLength={254} defaultValue={profile.email}/></label>
      <p className="muted">{profile.verified?'Adressen er bekreftet. Hvis du endrer den, må den nye adressen bekreftes før du får flere påminnelser.':'Bekreft adressen via lenken i e-posten før du kan motta påminnelser.'}</p>
      {!profile.sendingEnabled&&<p className="notice">E-postutsending er ikke aktivert ennå.</p>}
      <button className="primary" disabled={busy}>{busy?'Lagrer…':profile.verified?'Lagre e-postadresse':'Lagre og be om bekreftelseslenke'}</button>
      {preview&&<a href={preview} className="text-button">Åpne lokal testlenke – ingen e-post er sendt</a>}
    </form>}
  </>;
}
