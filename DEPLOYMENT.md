# NB69: test og publisering

Denne grenen erstatter prikkevisningen med personlige kontoer og ukentlig bekreftelse. Gamle prikketabeller og data slettes ikke. Gamle prikke-API-er er blokkert, også for admin.

## Lokal test

Krever Node 22+ og Java 21. Bruk to terminaler:

```sh
cd backend/NB69
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

```sh
cd frontend
npm ci
npm run dev
```

Åpne adressen Vite skriver ut. Første oppstart lager fem kontoer og private, tilfeldige engangskoder i `backend/NB69/.local/activation-codes.md`. Velg «Ny bruker eller glemt passord?» og sett et personlig passord (minst 12 tegn, maks 72 UTF-8 byte). Aktiveringskoder utløper etter 24 timer. Start serveren på nytt for nye koder til kontoer som ennå ikke er aktivert. Admin kan deretter lage nye koder for alle fem.

Lokal H2-database ligger i `.local`, og oppgaver og passord overlever omstart. Innloggingsøkter ligger i serverminnet; alle må logge inn igjen etter omstart. Nettleseren husker ellers økten i opptil 30 dager, med 30 dagers inaktivitet som servergrense. Lokale data og koder skal aldri legges i Git.

## Før Sigurd publiserer

1. Gå gjennom denne grenen og test med minst én beboer og én administrator. Ta backup av Neon før databaseendringer. Kontroller de faktiske tjenestene og eksisterende miljøvariabler.
2. Sett Render-variablene `DATABASE_URL` (JDBC-format: `jdbc:postgresql://HOST/DATABASE?sslmode=require`), `DATABASE_USERNAME` og `DATABASE_PASSWORD`. Ikke bruk lokalprofilen i produksjon. `PORT` støttes fortsatt. Java 21 kreves.
3. Sett `NB69_BOOTSTRAP_CODE` til en tilfeldig kode på minst 24 tegn før første oppstart. Denne aktiverer bare Eilif. Etter at Eilif har satt passordet sitt, fjern variabelen og start på nytt. Eilif oppretter private aktiveringskoder for de fire andre. Sigurd får automatisk adminrettigheter.
4. Oppstart kjører `schema.sql`, som bare oppretter de nye tabellene `nb69_users`, `nb69_assignments` og `nb69_audit` hvis de mangler. Den eksisterende `prikker`-tabellen berøres ikke. Det legges ikke inn testpassord i produksjon.
5. Kopier `frontend/vercel.example.json` til `frontend/vercel.json`, og erstatt `REPLACE-WITH-RENDER-HOST` med den verifiserte Render-adressen. Vercel må bruke `frontend` som rot, `npm run build` som byggkommando og `dist` som publiseringsmappe. Frontend bruker alltid `/api`; den gamle `VITE_API_BASE` brukes ikke av den nye appen.
6. Publiser serveren og frontend koordinert. API-proxyen må bevare `Set-Cookie` og `Cookie`. All trafikk fra nettleseren skal gå gjennom nettsidens eget domene. Innlogging bruker en HttpOnly, Secure, SameSite=Lax-øktcookie i produksjon, og CSRF-token for alle endringer. Ikke åpne opp CORS som en erstatning for proxyen.
7. Test på den faktiske HTTPS-adressen: aktivering, login, logout, oppdatering av side, avkryssing, vanlig brukers manglende admintilgang og to separate brukere. Kontroller også at prikke-API-et gir 403, og at `/api/me` gir 401 uten innlogging. Lokal test bekrefter ikke Vercel/Render-oppsettet.

## Omfang og drift

- Navn og roller: Eilif og Sigurd er administratorer; Andreas, Jørgen og Erlend er beboere. Endring av disse faste rollene krever en separat kode-/databaseendring.
- Hver uke får egne oppgaver og egne bekreftelser. Rotasjonen viderefører eksisterende ISO-ukenummerlogikk, også ved årsskifte. Tidssone er Europe/Oslo. Neste uke kan ses, men ikke bekreftes på forhånd. Eldre uferdige oppgaver kan bekreftes etter fristen og lagres som forsinket.
- Admin kan tildele oppgaver denne og neste uke, angre feilregistreringer og lage aktiverings-/passordresetkoder. Tildeling lagres for den valgte uka, ikke som en varig endring av rotasjonen. Utførte oppgaver må angres før omfordeling. Endringer loggføres i `nb69_audit`.
- Bekreftelse er idempotent og kontrolleres på serveren mot innlogget bruker. Neste ukes oppgaver viser aldri denne ukas bekreftelser.
- Fristen er utgangen av søndag, med norsk tid og korrekt sommer-/vintertid. Egne uferdige oppgaver fra tidligere uker vises som et tydelig varsel helt til de blir bekreftet. Ingen bilder eller egen historikkside; historiske bekreftelser beholdes i databasen.
- Passord lagres med BCrypt. Aktiveringskoder lagres som SHA-256-hash, er engangsbruk og utløper etter 24 timer. Passordendring avslutter gamle økter på samme server.
- Kjør én Render-instans i denne første versjonen. Økter og begrensning av innloggingsforsøk ligger i minnet. Flere instanser krever delt øktlagring og delt begrensning av innloggingsforsøk. Det er 15 innloggingsforsøk per brukernavn og 60 login/aktiveringsforsøk per direkte nettverksadresse per 15 minutter. Bak proxy kan adressegrensen være delt mellom beboerne.
- Oppbevar databasebackup i Neon. En utrulling av gammel kode ruller ikke tilbake tabellene, men de er separate og kan stå urørt.

## Kontroller

```sh
cd backend/NB69
./mvnw test
```

```sh
cd frontend
npm run build
npm run lint
```

## Frister og e-post

- Frist er mandag kl. 00.00 (altså innen hele søndagen er over), Europe/Oslo. Bekreftelser før dette tidspunktet er i tide. Senere bekreftelser får `late=true`, uten tilbakedatering.
- Søndag fra kl. 14 vises påminnelse på siden. Fra mandag kl. 00 vises tidligere uferdige oppgaver som et personlig hastevarsel med egen «Bekreft utført»-knapp. Bare eieren kan bekrefte dem.
- Manglende ukeplaner fylles fra første lagrede uke når systemet brukes igjen. Ingen restanser opprettes før første bruk.
- E-postpåminnelse søndag kl. 14 og purring mandag kl. 08 for forrige uke, bare hvis oppgaven fortsatt er uferdig. Serverjobben sjekker hvert minutt og kan ta igjen utsending senere samme søndag/mandag. Ingen gamle søndagspåminnelser sendes på mandag. Bekreftelse før utsending avlyser påminnelsen.

### Koble til Resend

1. Opprett en Resend-konto og legg til et avsenderdomene, gjerne et eget underdomene som `varsler.nb69.no`. Legg inn DNS-postene Resend oppgir hos domenets DNS-leverandør og vent på verifisering. Ikke erstatt eksisterende e-postoppsett for hoveddomenet.
2. Opprett en API-nøkkel for sending og legg `RESEND_API_KEY` direkte i Render. Ikke legg nøkkelen i Git eller frontend.
3. Sett `NB69_EMAIL_FROM` til en adresse på det verifiserte domenet, for eksempel `NB69 <paaminnelse@varsler.nb69.no>`. Eksemplet er ikke en adresse vi allerede har opprettet. Domenet må verifiseres før sending til beboerne.
4. Legg inn beboernes e-postadresser under Administrasjon → E-postpåminnelser på den publiserte nettsiden. Tomt felt fjerner adressen. Adressene er bare tilgjengelige for administratorer og brukes ikke som innloggingsnavn.
5. Etter en avtalt leverandørtest og kontroll av adresser: sett `NB69_EMAIL_ENABLED=true` og publiser/start serveren på nytt. Kontroller mottak og søppelpostfilter på den faktiske mottakerkontoen. Resend-aksept betyr ikke garantert levering til innboksen.

Resend-dokumentasjon: [domener](https://resend.com/docs/dashboard/domains/introduction), [sending](https://resend.com/docs/api-reference/emails/send-email), [gratisplan og priser](https://resend.com/pricing).

### Drift og overgang fra SMS

- E-post er avslått som standard. Lokalprofilen tvinger `nb69.email.enabled=false` selv om produksjonsvariabler finnes. Ingen reelle e-poster er sendt under utvikling; testene bruker falsk sender/mocket HTTP-klient.
- Render-serveren må være våken søndag kl. 14 og mandag kl. 08. Ingen egen Codex-automatisering er nødvendig.
- SMS-koden og SMS-jobben er fjernet. Fjern eventuelle `TWILIO_*` og `NB69_SMS_ENABLED`-variabler fra Render. De brukes ikke lenger. Tidligere SMS-tabeller og eventuelle mobilnumre slettes ikke automatisk; eksisterende data blir stående urørt, mens nye installasjoner bare oppretter e-posttabellene.
- Nye tabeller: `nb69_email_contacts` og `nb69_email_reminders`. Ta databasebackup før oppstart. Oppgaver, passord, roller og bekreftelser beholdes.
- Én varig utsendingsreservasjon per uke, oppgave og påminnelsestype hindrer gjentatte utsendinger etter omstart. Resend får også en stabil `Idempotency-Key`. Resend beholder slike nøkler i 24 timer; databasens reservasjon varer videre.
- `ACCEPTED` betyr mottatt av Resend. `UNCERTAIN` / `CLAIMED` krever manuell kontroll i Resend ved feil/krasj og prøves ikke automatisk igjen fordi levering kan ha skjedd. Siste 20 forsøk vises for admin. Manglende adresse bruker ikke opp påminnelsen.
- Oppgaven sjekkes igjen under lås før utsending. En samtidig bekreftelse kan vente inntil nettverkstidsavbruddet på ti sekunder.
