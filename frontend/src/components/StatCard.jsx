export function StatCard({ label, value, hint, tone }) {
  return (
    <div className="card stat-card">
      <div className="label">{label}</div>
      <div className="value" style={tone ? { color: `var(--sev-${tone})` } : undefined}>{value}</div>
      {hint && <div className="hint">{hint}</div>}
    </div>
  );
}
