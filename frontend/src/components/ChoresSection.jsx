import React, { useEffect, useMemo, useState } from "react";
import ChoresTable from "./ChoresTable";
import { fetchCurrentChores } from "./chores.api.js";

export default function ChoresSection() {
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [chores, setChores] = useState([]);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const data = await fetchCurrentChores();
        if (!alive) return;
        setChores(Array.isArray(data.assignments) ? data.assignments : []);
      } catch (e) {
        if (alive) setErr(String(e?.message ?? e));
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => { alive = false; };
  }, []);

  // Eksempeldata hvis backend ikke svarer, så du ser designet
  const choresOrMock = useMemo(() => {
    if (loading) return [];
    if (!err && chores.length) return chores;
    return [
      { task: "Kjøkken",           person: "Andreas" },
      { task: "Stue & Støvsuging", person: "Jørgen"  },
      { task: "Bad",               person: "Erlend"  },
      { task: "Papp & Glass",      person: "Eilif"   },
      { task: "Pant",              person: "Sigurd"  },
    ];
  }, [loading, err, chores]);

  const nextWeek = useMemo(() => {
    if (!Array.isArray(choresOrMock) || choresOrMock.length === 0) return [];
    const people  = choresOrMock.map(c => c.person);
    const rotated = people.map((_, i) => people[(i + 1) % people.length]);
    return choresOrMock.map((c, i) => ({ task: c.task, person: rotated[i] }));
  }, [choresOrMock]);

  return (
    <>
      {/* DENNE UKEN */}
      <div className="card">
        <div className="card__header">
          <h3 className="card__title">Denne uken</h3>
        </div>
        <div className="card__body">
          {loading && <div>Laster…</div>}
          {!loading && <ChoresTable chores={choresOrMock} />}
          {!!err && !loading && (
            <p className="note">Backend ikke tilgjengelig – viser eksempeldata.</p>
          )}
        </div>
      </div>

      {/* NESTE UKE */}
      <div className="card">
        <div className="card__header">
          <h3 className="card__title">Neste uke</h3>
        </div>
        <div className="card__body">
          {loading && <div>Laster…</div>}
          {!loading && <ChoresTable chores={nextWeek} />}
        </div>
      </div>
    </>
  );
}
