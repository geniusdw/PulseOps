export function Loading({ label = 'Loading…' }) {
  return <div className="state-msg">{label}</div>;
}

export function ErrorBox({ error }) {
  return <div className="state-msg error">⚠ {error}</div>;
}

export function Empty({ label = 'Nothing here yet.' }) {
  return <div className="state-msg">{label}</div>;
}
