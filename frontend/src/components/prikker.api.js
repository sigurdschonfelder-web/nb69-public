// frontend/src/components/prikker.api.js
// Basen hentes fra .env (VITE_API_BASE=http://localhost:8080)
// Husk å restarte `npm run dev` etter endring i .env
const API = import.meta.env.VITE_API_BASE ?? "";

export async function fetchPrikker() {
  // Bygg full URL mot backend
  const url = `${API}/api/prikker`;

  try {
    const r = await fetch(url, { headers: { Accept: "application/json" } });
    if (!r.ok) {
      // Hjelper ved feilsøking: se status + svartekst i konsollen
      const text = await r.text().catch(() => "");
      console.log("response from backend: ", text)
      throw new Error(`HTTP ${r.status} – ${text}`);
    }

    const data = await r.json();
    console.log("GET /api/prikker data:", data, "isArray:", Array.isArray(data));
    return data;
  } 
  catch (e) {
    console.error("fetchPrikker feilet:", e);
    throw e;
  }
}

/*
  Alternativ (hvis du bruker Vite-proxy i vite.config.js):
  export async function fetchPrikker() {
    const r = await fetch("/api/prikker");
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return r.json();
  }
*/
