import { createElement, useEffect, useRef } from 'react';
import { Menu, X, House, User, ArrowLeftRight, ShoppingBasket, History, Settings, LogOut } from 'lucide-react';
export default function Navigation({ page, admin, incoming, onNavigate, onLogout }) {
  const disclosure = useRef(null);
  useEffect(() => {
    function close(event) {
      const node = disclosure.current;
      if (!node?.open) return;
      if (event.type === 'keydown' && event.key === 'Escape') {
        node.open = false; node.querySelector('summary').focus();
      } else if (event.type === 'pointerdown' && !node.contains(event.target)) node.open = false;
    }
    document.addEventListener('keydown',close); document.addEventListener('pointerdown',close);
    return () => {document.removeEventListener('keydown',close); document.removeEventListener('pointerdown',close);};
  }, []);
  const links = [['home','Ukens oppgaver',House],['profile','Min profil',User],['swaps','Bytte oppgave / borte',ArrowLeftRight],['shopping','Handleliste',ShoppingBasket],['history','Historikk',History],...(admin ? [['admin','Administrasjon',Settings]] : [])];
  return <details ref={disclosure} className="navigation"><summary aria-label="Hovedmeny"><Menu className="menu-open" size={23}/><X className="menu-close" size={23}/>{incoming>0&&<span className="menu-dot" aria-label={`${incoming} bytteforespørsler`}/>}</summary><nav aria-label="Hovedmeny">{links.map(([id,label,Icon])=><button key={id} aria-current={page===id?'page':undefined} onClick={()=>{disclosure.current.open=false;onNavigate(id);}}>{createElement(Icon,{size:19})}<span>{label}</span>{id==='swaps'&&incoming>0&&<span className="menu-count">{incoming}</span>}</button>)}<button className="menu-logout" onClick={()=>{disclosure.current.open=false;onLogout();}}><LogOut size={19}/>Logg ut</button></nav></details>;
}
