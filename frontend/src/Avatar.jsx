import { useState } from 'react';
export default function Avatar({ username, name, version, large = false }) {
  const [failed, setFailed] = useState(null);
  const src = version ? `/api/avatars/${encodeURIComponent(username)}?v=${encodeURIComponent(version)}` : null;
  return <span className={`avatar ${large ? 'avatar-large' : ''}`} aria-hidden="true">{src && failed !== src ? <img src={src} alt="" onError={() => setFailed(src)} /> : name?.[0]}</span>;
}
