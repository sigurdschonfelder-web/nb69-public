const API = import.meta.env.VITE_API_BASE;

export async function fetchCurrentChores() {
  const r = await fetch(`${API}/api/chores/current`, {
    headers: { "Accept": "application/json" },
  });
  if (!r.ok) throw new Error(`HTTP ${r.status}`);
  return r.json();
}


