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

## Frister og SMS

- Frist er mandag kl. 00.00 (altså innen hele søndagen er over), Europe/Oslo. Bekreftelser før dette tidspunktet er i tide. Senere bekreftelser får `late=true` i uke-API-et, uten at tidspunktet tilbakedateres.
- Søndag fra kl. 14 vises påminnelse på siden. Fra mandag kl. 00 vises tidligere uferdige oppgaver som et personlig hastevarsel med egen «Bekreft utført»-knapp. Bare eieren kan bekrefte disse, også når en admin er innlogget.
- Når siden har vært ubrukt i flere uker, fylles manglende ukeplaner fra første lagrede uke. Det opprettes ikke restanser for uker før første bruk. Den eksisterende rotasjonen beholdes.
- SMS sender en påminnelse søndag kl. 14 og en purring mandag kl. 08 for forrige uke, bare hvis oppgaven fortsatt er uferdig. En serverjobb sjekker hvert minutt. Ved nedetid kan den ta igjen påminnelsen senere samme søndag/mandag, men ikke sende en gammel søndagspåminnelse på mandag. En bekreftelse før mandagens utsending avlyser purringen.
- Koble til [Twilio Messaging](https://www.twilio.com/docs/messaging/api/message-resource) med `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` og `TWILIO_FROM` i Render. Avsenderen må være konfigurert for SMS til Norge. Sett deretter `NB69_SMS_ENABLED=true`. SMS har en kostnad hos leverandøren; ingen konto er opprettet og ingen abonnement er bestilt av denne endringen. Prøvekontoer krever verifiserte mottakernumre hos Twilio.
- Administratorene legger inn beboernes mobilnumre under Administrasjon → SMS-påminnelser, med landskode (for eksempel +47). Tomt felt fjerner nummeret. Ingen nummer sendes til SMS-tjenesten før utsending er slått på. Bare admin har tilgang til mobilnumrene.
- Lokalprofilen tvinger SMS av, selv om miljøvariabler for produksjon finnes. Testene bruker en falsk sender og sender ingen faktiske meldinger. Ingen ekte leverandørtest er gjennomført.
- Render-serveren må være våken på de aktuelle tidene. En sovende gratistjeneste kan ikke garantere planlagte SMS-er. Bruk en alltid kjørende server før aktivering. Det trengs ingen separat Codex-automatisering.
- `nb69_sms_reminders` lagrer én utsendingsreservasjon per uke, oppgave og påminnelsestype før nettverkskallet. Dette hindrer dobbeltutsending ved gjentatte sjekker og omstart. Oppgaven sjekkes på nytt under lås før sending. En låst oppgave kan bruke inntil nettverkstidsavbruddet på å la en samtidig bekreftelse slippe til.
- `ACCEPTED` betyr mottatt av leverandøren, ikke bekreftet levert til telefonen. `UNCERTAIN` / `CLAIMED` krever manuell kontroll i Twilio ved feil eller krasj; de sendes ikke automatisk på nytt fordi leveransen kan ha skjedd. Siste 20 forsøk vises for admin. Et manglende mobilnummer bruker ikke opp påminnelsen; et nummer som legges til senere samme utsendingsdag kan derfor fortsatt få melding.
- Før produksjon: ta databasebackup, kjør testene, legg inn riktige numre, kontroller avsenderoppsettet og utfør en avtalt leverandørtest. Klokkegrenser, feiltilfeller og deduplisering er testet lokalt, men faktisk SMS-leveranse og drift på Render må bekreftes ved tilkobling.
