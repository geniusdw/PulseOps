export function SeverityBadge({ value }) {
  return <span className={`badge sev-${value}`}>{value}</span>;
}

export function StatusBadge({ value }) {
  return <span className={`badge st-${value}`}>{value}</span>;
}
