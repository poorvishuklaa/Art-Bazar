const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })
const date = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' })

export const formatPrice = (amount) => inr.format(Number(amount)).replace(/\.00$/, '')
export const formatDate = (iso) => date.format(new Date(iso))

const inrCompact = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', notation: 'compact', maximumFractionDigits: 1 })
const monthShort = new Intl.DateTimeFormat('en-IN', { month: 'short' })
const monthLong = new Intl.DateTimeFormat('en-IN', { month: 'long', year: 'numeric' })

/** ₹4.2K, ₹1.3L: for axis ticks and tiles where space is tight. */
export const formatPriceCompact = (amount) => inrCompact.format(Number(amount))

/** "2026-11" -> "Nov" / "November 2026" */
const monthDate = (ym) => new Date(`${ym}-01T00:00:00`)
export const formatMonthShort = (ym) => monthShort.format(monthDate(ym))
export const formatMonthLong = (ym) => monthLong.format(monthDate(ym))
