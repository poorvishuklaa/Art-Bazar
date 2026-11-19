const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })
const date = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' })

export const formatPrice = (amount) => inr.format(Number(amount)).replace(/\.00$/, '')
export const formatDate = (iso) => date.format(new Date(iso))
