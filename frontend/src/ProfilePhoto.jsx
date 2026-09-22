import { useEffect, useState } from 'react';
import Avatar from './Avatar';
export default function ProfilePhoto({ user, request, onChanged }) {
  const [preview,setPreview]=useState(null);
  const [blob,setBlob]=useState(null);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState('');
  const [notice,setNotice]=useState('');
  useEffect(()=>()=>{if(preview) URL.revokeObjectURL(preview);},[preview]);
  async function choose(event){
    const file=event.target.files?.[0];event.target.value='';if(!file)return;
    setError('');setNotice('');setBlob(null);setPreview(null);setBusy(true);
    try{
      if(file.size>12*1024*1024) throw new Error('Velg et bilde under 12 MB.');
      if(!['image/jpeg','image/png','image/webp'].includes(file.type)) throw new Error('Velg et JPG-, PNG- eller WebP-bilde. HEIC må først lagres som JPG.');
      const bitmap=await createImageBitmap(file,{imageOrientation:'from-image'});
      const canvas=document.createElement('canvas');canvas.width=256;canvas.height=256;
      const context=canvas.getContext('2d');context.fillStyle='#fff';context.fillRect(0,0,256,256);
      const side=Math.min(bitmap.width,bitmap.height);
      context.drawImage(bitmap,(bitmap.width-side)/2,(bitmap.height-side)/2,side,side,0,0,256,256);bitmap.close();
      const result=await new Promise(resolve=>canvas.toBlob(resolve,'image/jpeg',0.85));
      if(!result)throw new Error('Kunne ikke behandle bildet. Prøv et annet bilde.');
      setBlob(result);setPreview(URL.createObjectURL(result));
    }catch(err){setError(err.message || 'Kunne ikke lese bildet.');}finally{setBusy(false);}
  }
  async function save(remove=false){
    setBusy(true);setError('');setNotice('');
    try{
      if(remove)await request('/profile/avatar',{method:'DELETE'});
      else{const form=new FormData();form.append('image',blob,'profile.jpg');await request('/profile/avatar',{method:'POST',body:form});}
      await onChanged();setBlob(null);setPreview(null);setNotice(remove?'Profilbildet er fjernet.':'Profilbildet er lagret.');
    }catch(err){setError(err.message);}finally{setBusy(false);}
  }
  return <section className="panel photo-panel"><h2>Profilbildet ditt</h2><div className="photo-preview">{preview?<img src={preview} alt="Forhåndsvisning av nytt profilbilde"/>:<Avatar username={user.username} name={user.name} version={user.avatarVersion} large/>}</div>
    <p className="muted">Bildet vises ved navnet og oppgavene dine. Vi bruker midten av bildet.</p>
    {error&&<p className="error" role="alert">{error}</p>}{notice&&<p className="success" role="status">{notice}</p>}
    <label>Velg bilde<input type="file" accept="image/jpeg,image/png,image/webp" disabled={busy} onChange={choose}/></label>
    {blob&&<button className="primary" disabled={busy} onClick={()=>save(false)}>{busy?'Lagrer…':'Lagre profilbilde'}</button>}
    {user.avatarVersion&&<button className="text-button" disabled={busy} onClick={()=>save(true)}>Fjern profilbilde</button>}
  </section>;
}
