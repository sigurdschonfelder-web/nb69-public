import { useEffect, useState } from "react";
import { fetchPrikker } from "../components/prikker.api";
import AdminActions from "../components/AdminActions";

export default function Prikker({ isLoggedIn }) {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const fetchData = async () => {
    setLoading(true)
    console.log("IIFE START");
    try {
      const data = await fetchPrikker();
      console.log("fetchPrikker RETURNED:", data);
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      console.log("CATCH:", e);
      console.error(e);
      setErr(String(e?.message ?? e));
    } finally {
      console.log("FINALLY");
      setLoading(false);
    }
  };

  useEffect(() => {
    console.log("useEffect START");
    fetchData();
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
                      <tr key={i}><td>{r.navn}</td><td>{r.prikker}</td>
                      <AdminActions isLoggedIn={isLoggedIn} navn={r.navn} refresh={fetchData}/>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <p>{isLoggedIn ? "Du er logget inn som admin" : ""}</p> 
          </div>
        </div>
      </div>
    </div>
  );
}
