import React from "react";

function isoWeek(date = new Date()) {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = (d.getUTCDay() + 6) % 7;
  d.setUTCDate(d.getUTCDate() - dayNum + 3);
  const firstThu = new Date(Date.UTC(d.getUTCFullYear(), 0, 4));
  return 1 + Math.round(((d - firstThu) / 86400000 - 3 + ((firstThu.getUTCDay() + 6) % 7)) / 7);
}

export default function HeaderBar() {
  const uke = isoWeek();
  const year = new Date().getFullYear();

  return (
    <header className="sticky top-0 z-40 border-b border-slate-800 bg-slate-950/90 backdrop-blur">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-14 sm:h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="h-8 w-8 sm:h-9 sm:w-9 grid place-content-center rounded-xl bg-sky-500 text-white font-bold">
            NB
          </div>
          <div className="leading-tight">
            <div className="font-semibold text-slate-100 text-sm sm:text-base">NB69</div>
            {/* Skjul adressen på helt smal bredde */}
            <div className="hidden xs:block text-[11px] sm:text-xs text-slate-400">
              Nedre Bakklandet 69
            </div>
          </div>
        </div>

        {/* Uke-pill */}
        <div className="rounded-xl border border-sky-500/20 bg-sky-500/10 px-2.5 py-1 text-xs sm:text-sm text-sky-200">
          Uke {uke} – {year}
        </div>
      </div>
    </header>
  );
}
