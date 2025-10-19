import React from "react";

export default function ChoresTable({ chores }) {
  return (
    <div>
      <table className="table">
        <thead>
          <tr>
            <th>Oppgave</th>
            <th>Ansvarlig</th>
          </tr>
        </thead>
        <tbody>
          {chores.map((row, i) => (
            <tr key={i}>
              <td>{row.task}</td>
              <td>{row.person}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
