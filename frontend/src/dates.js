const month = new Intl.DateTimeFormat('nb-NO',{month:'long',timeZone:'UTC'});
export function weekRange(start) {
  const from = new Date(`${start}T12:00:00Z`);
  const to = new Date(from);to.setUTCDate(to.getUTCDate()+6);
  if(Number.isNaN(from.getTime())) return '';
  if(from.getUTCFullYear()!==to.getUTCFullYear())return `${from.getUTCDate()}. ${month.format(from)} ${from.getUTCFullYear()}–${to.getUTCDate()}. ${month.format(to)} ${to.getUTCFullYear()}`;
  const range=from.getUTCMonth()===to.getUTCMonth()?`${from.getUTCDate()}.–${to.getUTCDate()}. ${month.format(to)}`:`${from.getUTCDate()}. ${month.format(from)}–${to.getUTCDate()}. ${month.format(to)}`;
  return `${range} ${from.getUTCFullYear()}`;
}
export const weekLabel = week => `Uke ${week.week} · ${weekRange(week.start)}`;
