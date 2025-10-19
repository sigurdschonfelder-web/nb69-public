import { useEffect, useState } from "react";
import { fetchPrikker } from "../components/prikker.api";

export default function Prikker() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const data = await fetchPrikker();
        if (!alive) return;
        setRows(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error(e);
        if (alive) setErr(String(e?.message ?? e));
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => { alive = false; };
  }, []);

  return (
    <div style={{ minHeight: "100vh", background: "#fff" }}>
      <div style={{ height: 56 }} /> {/* spacer for fast navbar */}
      <div className="overlay-area" style={{ marginTop: 32 }}>
        <div className="cards" style={{ maxWidth: 720 }}>
          <div className="card">
            <div className="card__header">
              <h3 className="card__title">Prikker</h3>
            </div>
            <div className="card__body">
              {loading && <div>Laster…</div>}
              {err && <div className="note" style={{ color: "#b91c1c" }}>Feil: {err}</div>}

              {!loading && !err && (
                <table className="table">
                  <thead>
                    <tr><th>Navn</th><th>Antall prikker</th></tr>
                  </thead>
                  <tbody>
                    {rows.map((r, i) => (
                      <tr key={i}><td>{r.navn}</td><td>{r.prikker}</td></tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
