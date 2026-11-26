export type ISODateString = `${number}-${number}-${number}`

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

export function toIsoDate(date: Date): ISODateString {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` as ISODateString
}

export function todayIso(now = new Date()): ISODateString {
  return toIsoDate(now)
}

export function shiftDay(isoDate: string, days: number): ISODateString {
  const [year, month, day] = isoDate.split('-').map(Number)
  return toIsoDate(new Date(year, month - 1, day + days))
}
