// src/ChoreTest.jsx
import { useEffect, useState } from "react";
import "../App.css";

export default function ChoreTest() {
    const [data, setData] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        fetch("/api/chores/current")
            .then((r) => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(setData)
            .catch((e) => {
                console.error("Fetch-feil:", e);
                setError(String(e));
            });
    }, []);

    if (error) return <p style={{ color: "red" }}>Feil: {error}</p>;
    if (!data) return <p>Laster…</p>;

    return (
        <div>
            <h2>Uke {data.week} – {data.weekYear}</h2>
            <ul>
                {data.assignments?.map((a, i) => (
                    <li key={i}><strong>{a.task}:</strong> {a.person}</li>
                ))}
            </ul>
        </div>
    );
}
