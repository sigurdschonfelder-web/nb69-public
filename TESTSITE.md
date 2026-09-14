# Felles testside – til Sigurd

Dette er en klargjort oppskrift, ikke en publisert tjeneste. Bruk branchen `codex/eilif-mobilversjon`. Kun Sigurd har hostingtilgang. Den eksisterende nettsiden skal fortsatt bruke sitt nåværende oppsett.

## 1. Separat database og Render-testserver

Opprett en egen tom Postgres-database for test. Ikke bruk den faktiske leilighetens database eller en kopi med ekte passord og persondata.

Opprett en separat Render Web Service fra samme GitHub-repo, med:

- Branch: `codex/eilif-mobilversjon`
- Runtime: Docker
- Root Directory: `backend/NB69`
- Dockerfile: `./Dockerfile` (relativt til valgt rot)
- Én instans. Velg kapasitet/pris selv i Render; dette er ikke opprettet eller bestilt av Codex.

Sett disse verdiene direkte i Render:

| Variabel | Verdi |
| --- | --- |
| `DATABASE_URL` | JDBC-adressen til testdatabasen, med `sslmode=require` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Testdatabasens tilgang |
| `NB69_BOOTSTRAP_CODE` | Tilfeldig kode med minst 24 tegn, for første aktivering av Eilif |
| `NB69_EMAIL_ENABLED` | `false` fram til korrekt testadresse er satt nedenfor |
| `NB69_SITE_URL` | Endelig HTTPS-adresse til testfrontend når den er opprettet |

Ikke aktiver Spring-profilen `local` på Render. Den bruker database på lokal disk og slår av e-post.

## 2. Separat Vercel-testprosjekt

Hent siste kode. Lag proxykonfigurasjonen med Render-testserverens faktiske adresse:

```sh
node scripts/prepare-test-frontend.mjs https://DIN-TESTSERVER.onrender.com
```

Skriptet lager `frontend/vercel.json` og nekter å overskrive en eksisterende fil. Kontroller at målet er testserveren. Denne konfigurasjonen inneholder ingen hemmeligheter, men må ikke brukes for den faktiske nettsiden. Behold den i en egen utrullingsbranch eller publiser fra den lokale arbeidskopien til et separat Vercel-prosjekt.

Vercel-prosjektet skal bruke `frontend` som rot, Vite, `npm run build` og output `dist`. Bruk testprosjektets stabile HTTPS-adresse, ikke en adresse som endres for hvert bygg. Proxyen holder API og innloggingscookies på samme domene. Testkonfigurasjonen ber søkemotorer om ikke å indeksere siden; dette erstatter ikke innlogging.

## 3. Aktivering og e-post

Sett `NB69_SITE_URL` i Render til testfrontendens HTTPS-adresse og start serveren på nytt. Aktiver Eilif med bootstrap-koden på denne siden; fjern deretter `NB69_BOOTSTRAP_CODE` og start på nytt. Eilif lager koder for de andre. Kodene fra den lokale Mac-databasen kan ikke brukes her.

Når dere er klare til å sende test-e-post, sett `RESEND_API_KEY`, `NB69_EMAIL_FROM` og `NB69_EMAIL_ENABLED=true` i testserveren. Sigurds eksisterende Resend-oppsett kan brukes hvis nøkkelen og avsenderen har riktig tilgang. Ikke lim nøkkelen inn i Git eller chatten. Kun adresser dere selv registrerer og bekrefter i testdatabasen får e-post; ukentlige påminnelser vil også kunne sendes når e-post er slått på.

## 4. Prøv sammen

- Åpne den samme HTTPS-lenken på hver deres telefon, og aktiver hver deres konto.
- Bekreft e-post under Min profil. Test «Glemt passord?» og at ny lenke åpner testsiden.
- Prøv menyen, oppgaver/fullføring/angre, bytte med godkjenning og handlelisten fra to kontoer.
- Prøv et bilde fra hver av telefonenes bildebibliotek. JPG, PNG og WebP støttes; HEIC må konverteres.
- Oppdater siden etter innlogging og etter serveromstart. Etter omstart må man logge inn igjen.
- Kontroller at vanlig beboer ikke får administrasjonstilgang.

Send testfrontendens lenke tilbake til Eilif når den er klar. Ingen endring av det faktiske domenet eller hovedbranch er nødvendig for denne testen.

Offisielle referanser: [Render Web Services](https://render.com/docs/web-services), [Docker på Render](https://render.com/docs/docker), [Vercel proxy/rewrites](https://vercel.com/docs/routing/rewrites).
