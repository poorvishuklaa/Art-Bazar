const QuantityStepper = ({ value, min = 1, max, onChange, disabled, label = 'Quantity' }) => (
  <div className="inline-flex items-center rounded-full ring-1 ring-gray-300" role="group" aria-label={label}>
    <button
      type="button"
      aria-label="Decrease"
      disabled={disabled || value <= min}
      onClick={() => onChange(value - 1)}
      className="h-9 w-9 rounded-l-full text-lg hover:bg-gray-50 disabled:opacity-30"
    >
      −
    </button>
    <span className="w-8 text-center text-sm tabular-nums" aria-live="polite">
      {value}
    </span>
    <button
      type="button"
      aria-label="Increase"
      disabled={disabled || (max != null && value >= max)}
      onClick={() => onChange(value + 1)}
      className="h-9 w-9 rounded-r-full text-lg hover:bg-gray-50 disabled:opacity-30"
    >
      +
    </button>
  </div>
)

export default QuantityStepper
