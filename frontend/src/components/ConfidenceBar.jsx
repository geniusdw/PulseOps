export function ConfidenceBar({ value }) {
  const pct = Math.round((value || 0) * 100);
  return (
    <div title={`${pct}%`}>
      <div className="confidence-bar">
        <div style={{ width: `${pct}%` }} />
      </div>
      <span style={{ fontSize: 11, color: 'var(--text-faint)' }}>{pct}%</span>
    </div>
  );
}
