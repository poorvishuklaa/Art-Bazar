import { useState } from 'react'
import { formatMonthLong, formatMonthShort, formatPrice, formatPriceCompact } from '../../utils/format'

const TARGET_TICKS = 4
const PLOT_HEIGHT = 200

/**
 * Picks a round gridline step (1, 2 or 5 × 10ⁿ) first, then the axis top as a whole number of
 * steps, so every label reads cleanly: ₹0 / ₹2K / ₹4K / ₹6K rather than ₹1.3K / ₹3.8K.
 */
function niceScale(peak) {
  const raw = Math.max(peak, 1000) / TARGET_TICKS
  const magnitude = 10 ** Math.floor(Math.log10(raw))
  const step = [1, 2, 5, 10].map((m) => m * magnitude).find((v) => v >= raw)
  const count = Math.max(2, Math.ceil(peak / step))
  return { max: step * count, ticks: Array.from({ length: count + 1 }, (_, i) => step * i) }
}

/**
 * Monthly revenue: one series, so no legend (the heading names it). Thin bars with a 4px rounded
 * top on a single baseline, hairline gridlines, a hover/focus tooltip on every month, only the
 * latest month labelled directly, and a table view for screen readers and exact values.
 */
const RevenueChart = ({ months }) => {
  const [active, setActive] = useState(null)
  const [showTable, setShowTable] = useState(false)
  const values = months.map((m) => Number(m.revenue))
  const { max, ticks } = niceScale(Math.max(...values))
  const last = months.length - 1

  return (
    <figure>
      <div className="flex gap-3">
        {/* y-axis labels */}
        <div className="relative w-12 shrink-0 text-right text-xs text-gray-500" style={{ height: PLOT_HEIGHT }} aria-hidden="true">
          {ticks.map((t) => (
            <span key={t} className="absolute right-0 -translate-y-1/2 tabular-nums" style={{ bottom: `${(t / max) * 100}%` }}>
              {formatPriceCompact(t)}
            </span>
          ))}
        </div>

        <div className="min-w-0 flex-1">
          <div className="relative" style={{ height: PLOT_HEIGHT }} onMouseLeave={() => setActive(null)}>
            {/* gridlines: hairline, solid, recessive */}
            {ticks.map((t) => (
              <div key={t} className="absolute inset-x-0 h-px bg-gray-100" style={{ bottom: `${(t / max) * 100}%` }} aria-hidden="true" />
            ))}
            <div className="absolute inset-x-0 bottom-0 h-px bg-gray-300" aria-hidden="true" />

            <div className="absolute inset-0 flex">
              {months.map((m, i) => {
                const pct = (values[i] / max) * 100
                const isActive = active === i
                return (
                  <div
                    key={m.month}
                    tabIndex={0}
                    role="img"
                    aria-label={`${formatMonthLong(m.month)}: ${formatPrice(values[i])}`}
                    onMouseEnter={() => setActive(i)}
                    onFocus={() => setActive(i)}
                    onBlur={() => setActive(null)}
                    className="group relative flex flex-1 cursor-default items-end justify-center outline-none"
                  >
                    {/* the whole column is the hit target, wider than the bar */}
                    <div
                      className={`w-6 max-w-[60%] rounded-t transition-opacity ${active !== null && !isActive ? 'opacity-40' : ''} group-focus-visible:ring-2 group-focus-visible:ring-sky-brand`}
                      style={{ height: values[i] > 0 ? `max(${pct}%, 4px)` : 0, backgroundColor: 'var(--color-brand-500)' }}
                    />
                    {i === last && values[i] > 0 && !isActive && (
                      <span className="absolute text-xs font-medium text-ink tabular-nums" style={{ bottom: `calc(${pct}% + 6px)` }}>
                        {formatPriceCompact(values[i])}
                      </span>
                    )}
                    {isActive && (
                      <div
                        role="tooltip"
                        className="pointer-events-none absolute z-10 rounded-lg bg-ink px-3 py-2 text-xs whitespace-nowrap text-white shadow-lg"
                        style={{ bottom: `calc(${Math.min(pct, 75)}% + 10px)` }}
                      >
                        <p className="text-gray-300">{formatMonthLong(m.month)}</p>
                        <p className="font-semibold tabular-nums">{formatPrice(values[i])}</p>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>

          {/* x-axis */}
          <div className="mt-2 flex text-xs text-gray-500" aria-hidden="true">
            {months.map((m, i) => (
              <span key={m.month} className={`flex-1 text-center ${i === last ? 'font-medium text-ink' : ''}`}>
                {formatMonthShort(m.month)}
              </span>
            ))}
          </div>
        </div>
      </div>

      <button
        type="button"
        onClick={() => setShowTable((v) => !v)}
        aria-expanded={showTable}
        className="mt-4 text-sm text-brand-500 hover:underline"
      >
        {showTable ? 'Hide table' : 'Show as table'}
      </button>
      {showTable && (
        <table className="mt-3 w-full max-w-sm text-sm">
          <thead>
            <tr className="border-b border-gray-200 text-left text-gray-500">
              <th className="py-1.5 font-medium">Month</th>
              <th className="py-1.5 text-right font-medium">Revenue</th>
            </tr>
          </thead>
          <tbody>
            {months.map((m, i) => (
              <tr key={m.month} className="border-b border-gray-100">
                <td className="py-1.5">{formatMonthLong(m.month)}</td>
                <td className="py-1.5 text-right tabular-nums">{formatPrice(values[i])}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </figure>
  )
}

export default RevenueChart
