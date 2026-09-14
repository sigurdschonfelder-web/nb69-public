import { writeFileSync, existsSync } from 'node:fs';
const input=process.argv[2];
if(!input){console.error('Bruk: node scripts/prepare-test-frontend.mjs https://DIN-TESTSERVER.onrender.com');process.exit(1);}
const url=new URL(input);
if(url.protocol!=='https:' || url.username || url.password || url.search || url.hash || url.pathname!=='/' || !url.hostname.endsWith('.onrender.com'))throw new Error('Oppgi bare HTTPS-adressen til Render-testserveren.');
const target=new URL('../frontend/vercel.json',import.meta.url);
if(existsSync(target))throw new Error('frontend/vercel.json finnes allerede. Kontroller den manuelt, så eksisterende oppsett ikke overskrives.');
writeFileSync(target,JSON.stringify({rewrites:[{source:'/api/:path*',destination:`${url.origin}/api/:path*`},{source:'/((?!api/).*)',destination:'/index.html'}],headers:[{source:'/:path*',headers:[{key:'X-Robots-Tag',value:'noindex, nofollow'}]}]},null,2)+'\n');
console.log('frontend/vercel.json er klar for det separate testprosjektet.');
