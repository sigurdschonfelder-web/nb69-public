import HeaderBar from "./components/HeaderBar";
import React from "react";
import { Phone, Mail, MapPin, ArrowRight, CheckCircle2, Hammer, BadgeCheck, Users } from "lucide-react";

const Section = ({ id, className = "", children }) => (
  <section id={id} className={`py-16 sm:py-24 ${className}`}>{children}</section>
);

const Container = ({ children, className = "" }) => (
  <div className={`max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 ${className}`}>{children}</div>
);

const Pill = ({ children }) => (
  <span className="inline-flex items-center rounded-2xl bg-sky-500/10 px-3 py-1 text-xs font-medium text-sky-700 ring-1 ring-inset ring-sky-500/20">{children}</span>
);

const Card = ({ children }) => (
  <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">{children}</div>
);

const FeatureItem = ({ icon: Icon, title, children }) => (
  <Card>
    <div className="flex items-start gap-3">
      <div className="mt-1"><Icon className="h-5 w-5" aria-hidden /></div>
      <div>
        <h3 className="font-semibold">{title}</h3>
        <p className="mt-2 text-sm text-slate-600">{children}</p>
      </div>
    </div>
  </Card>
);

function Header() {
  return (
    <header className="sticky top-0 z-40 bg-white/70 backdrop-blur border-b border-slate-200/70">
      <Container>
        <div className="flex items-center justify-between h-16">
          <a href="#" className="flex items-center gap-2" aria-label="NB69 hjem">
            <div className="h-9 w-9 grid place-content-center rounded-2xl bg-sky-500 text-white font-bold">NB</div>
            <span className="font-semibold tracking-tight">NB69</span>
          </a>
          <nav aria-label="Hovedmeny" className="hidden md:block">
            <ul className="flex items-center gap-6">
              <li><a href="#om" className="hover:text-sky-600">Om</a></li>
              <li><a href="#tjenester" className="hover:text-sky-600">Tjenester</a></li>
              <li><a href="#aktuelt" className="hover:text-sky-600">Aktuelt</a></li>
              <li><a href="#kontakt" className="hover:text-sky-600">Kontakt</a></li>
            </ul>
          </nav>
          <a href="#kontakt" className="hidden md:inline-flex items-center gap-2 rounded-2xl border border-sky-500/30 bg-sky-500/10 px-3 py-2 text-sm font-medium hover:bg-sky-500/20 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:ring-offset-2">Ta kontakt</a>
        </div>
      </Container>
    </header>
  );
}

function Hero() {
  return (
    <Section>
      <div className="absolute inset-0 -z-10 opacity-60" aria-hidden>
        <svg className="w-full h-full" viewBox="0 0 1440 320" preserveAspectRatio="none">
          <defs>
            <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor="#7dd3fc" />
              <stop offset="100%" stopColor="#bae6fd" />
            </linearGradient>
          </defs>
          <path fill="url(#g)" d="M0,64L48,69.3C96,75,192,85,288,122.7C384,160,480,224,576,218.7C672,213,768,139,864,117.3C960,96,1056,128,1152,165.3C1248,203,1344,245,1392,266.7L1440,288L1440,0L1392,0C1344,0,1248,0,1152,0C1056,0,960,0,864,0C768,0,672,0,576,0C480,0,384,0,288,0C192,0,96,0,48,0L0,0Z"></path>
        </svg>
      </div>
      <Container>
        <div className="grid md:grid-cols-2 gap-12 items-center">
          <div>
            <Pill>Visuell oppussing pågår</Pill>
            <h1 className="mt-3 text-4xl sm:text-5xl font-bold tracking-tight text-slate-900">Velkommen til <span className="text-sky-600">NB69</span></h1>
            <p className="mt-4 text-lg text-slate-700">Bytt denne teksten med en kort pitch: hvem dere er, hva dere gjør, og hvorfor besøkende skal velge dere.</p>
            <div className="mt-6 flex flex-wrap gap-3">
              <a href="#tjenester" className="rounded-2xl bg-sky-600 text-white px-5 py-3 font-medium inline-flex items-center gap-2 hover:bg-sky-700 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:ring-offset-2">Se tjenester <ArrowRight className="h-4 w-4"/></a>
              <a href="#kontakt" className="rounded-2xl border border-slate-300 px-5 py-3 font-medium hover:bg-white/60 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:ring-offset-2">Kontakt oss</a>
            </div>
            <ul className="mt-6 grid sm:grid-cols-2 gap-3 text-sm text-slate-700">
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4"/>10+ år erfaring</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4"/>Rask responstid</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4"/>Forutsigbare priser</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4"/>Fornøyde kunder</li>
            </ul>
          </div>
          <div className="relative">
            <div className="aspect-[4/3] rounded-2xl bg-white shadow-xl ring-1 ring-slate-200 overflow-hidden">
              <img src="https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?q=80&w=1600&auto=format&fit=crop" alt="Illustrasjonsbilde" className="w-full h-full object-cover" />
            </div>
            <div className="absolute -bottom-6 -right-6 hidden sm:block bg-white/70 backdrop-blur rounded-2xl shadow p-4">
              <p className="text-sm">Legg til nøkkeltall – f.eks. "500+ leveranser"</p>
            </div>
          </div>
        </div>
      </Container>
    </Section>
  );
}

function About() {
  return (
    <Section id="om">
      <Container>
        <div className="max-w-3xl">
          <h2 className="text-3xl font-bold tracking-tight">Om NB69</h2>
          <p className="mt-4 text-slate-700">Skriv 3–5 setninger om hvem dere er. Inkluder sted, spisskompetanse og hva som gjør dere unike.</p>
        </div>
        <div className="mt-10 grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <FeatureItem icon={Hammer} title="Kjerneområde">Kort beskrivelse av hva dere gjør mest av.</FeatureItem>
          <FeatureItem icon={BadgeCheck} title="Hvorfor oss?">Kvalitet, pålitelighet eller pris – hva skiller dere ut?</FeatureItem>
          <FeatureItem icon={Users} title="Sertifiseringer/medlemskap">Nevn relevante godkjenninger.</FeatureItem>
        </div>
      </Container>
    </Section>
  );
}

function Services() {
  const items = [
    { title: "Tjeneste 1", desc: "Kort beskrivelse av tjenesten." },
    { title: "Tjeneste 2", desc: "Kort beskrivelse av tjenesten." },
    { title: "Tjeneste 3", desc: "Kort beskrivelse av tjenesten." },
  ];
  return (
    <Section id="tjenester" className="bg-slate-50">
      <Container>
        <div className="max-w-3xl">
          <h2 className="text-3xl font-bold tracking-tight">Tjenester</h2>
          <p className="mt-4 text-slate-700">Kort og tydelig beskrivelse av hva som tilbys. Legg inn en enkel handling per kort.</p>
        </div>
        <div className="mt-10 grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {items.map((i) => (
            <Card key={i.title}>
              <h3 className="font-semibold">{i.title}</h3>
              <p className="mt-2 text-sm text-slate-600">{i.desc}</p>
              <a href="#kontakt" className="mt-4 inline-block text-sm font-medium text-sky-700 hover:underline">Få et tilbud →</a>
            </Card>
          ))}
        </div>
      </Container>
    </Section>
  );
}

function News() {
  return (
    <Section id="aktuelt">
      <Container>
        <div className="max-w-3xl">
          <h2 className="text-3xl font-bold tracking-tight">Aktuelt</h2>
          <p className="mt-4 text-slate-700">Del oppdateringer, prosjekter og nyheter – eller skjul seksjonen hvis dere ikke trenger den.</p>
        </div>
        <div className="mt-10 grid md:grid-cols-3 gap-6">
          {[1,2,3].map((n) => (
            <Card key={n}>
              <h3 className="font-semibold">Nyhetstittel {n}</h3>
              <p className="mt-2 text-sm text-slate-600">2–3 setninger om saken. <time dateTime="2025-10-19">19. oktober 2025</time></p>
              <a href="#" className="mt-4 inline-block text-sm font-medium text-sky-700 hover:underline">Les mer →</a>
            </Card>
          ))}
        </div>
      </Container>
    </Section>
  );
}

function Contact() {
  return (
    <Section id="kontakt" className="bg-slate-50">
      <Container>
        <div className="grid md:grid-cols-2 gap-10">
          <div>
            <h2 className="text-3xl font-bold tracking-tight">Kontakt oss</h2>
            <p className="mt-4 text-slate-700">Skriv hvordan folk enklest får tak i dere. Legg inn telefon, e-post og åpningstider.</p>
            <ul className="mt-6 space-y-3 text-slate-700">
              <li className="flex items-center gap-2"><Phone className="h-4 w-4"/> <strong>Telefon:</strong> <a className="hover:underline" href="tel:+47XXXXXXXX">+47 XX XX XX XX</a></li>
              <li className="flex items-center gap-2"><Mail className="h-4 w-4"/> <strong>E-post:</strong> <a className="hover:underline" href="mailto:post@nb69.no">post@nb69.no</a></li>
              <li className="flex items-center gap-2"><MapPin className="h-4 w-4"/> <strong>Adresse:</strong> Gateadresse, 7010 Trondheim</li>
            </ul>
          </div>
          <Card>
            <form action="mailto:post@nb69.no" method="post" encType="text/plain" className="grid gap-4">
              <div>
                <label className="block text-sm font-medium" htmlFor="navn">Navn</label>
                <input id="navn" className="mt-1 w-full rounded-xl border border-slate-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-sky-500" placeholder="Ola Nordmann" />
              </div>
              <div>
                <label className="block text-sm font-medium" htmlFor="epost">E-post</label>
                <input id="epost" type="email" className="mt-1 w-full rounded-xl border border-slate-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-sky-500" placeholder="ola@norge.no" />
              </div>
              <div>
                <label className="block text-sm font-medium" htmlFor="melding">Melding</label>
                <textarea id="melding" rows={5} className="mt-1 w-full rounded-xl border border-slate-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-sky-500" placeholder="Hva kan vi hjelpe med?" />
              </div>
              <button className="rounded-2xl bg-sky-600 text-white px-5 py-3 font-medium inline-flex items-center gap-2 hover:bg-sky-700 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:ring-offset-2" type="submit">
                Send melding <ArrowRight className="h-4 w-4"/>
              </button>
            </form>
          </Card>
        </div>
      </Container>
    </Section>
  );
}

function Footer() {
  return (
    <footer className="py-10 border-t border-slate-200">
      <Container>
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-sm text-slate-600">© {new Date().getFullYear()} NB69. Alle rettigheter forbeholdt.</p>
          <div className="text-sm text-slate-600">Bygges med React & Tailwind</div>
        </div>
      </Container>
    </footer>
  );
}

export default function NB69Landing() {
  return (
    <div className="antialiased text-slate-800 bg-gradient-to-b from-sky-50 to-white min-h-screen relative">
      <a href="#innhold" className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 bg-white px-3 py-2 rounded">Hopp til innhold</a>
      <Header />
      <main id="innhold" className="relative">
        <Hero />
        <About />
        <Services />
        <News />
        <Contact />
      </main>
      <Footer />
    </div>
  );
}
