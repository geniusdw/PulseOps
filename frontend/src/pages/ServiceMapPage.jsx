import { api } from '../services/api.js';
import { usePolling } from '../hooks/usePolling.js';
import { Loading, ErrorBox } from '../components/States.jsx';

const TIER_ROW = { EDGE: 0, APPLICATION: 1, DATA: 2 };
const WIDTH = 900;
const ROW_HEIGHT = 150;
const NODE_W = 150;
const NODE_H = 46;

function layout(services) {
  const byTier = { EDGE: [], APPLICATION: [], DATA: [] };
  services.forEach((s) => (byTier[s.tier] || byTier.APPLICATION).push(s));
  const pos = {};
  Object.entries(byTier).forEach(([tier, nodes]) => {
    const row = TIER_ROW[tier] ?? 1;
    nodes.forEach((n, i) => {
      const gap = WIDTH / (nodes.length + 1);
      pos[n.name] = { x: gap * (i + 1), y: row * ROW_HEIGHT + 40 };
    });
  });
  return pos;
}

export default function ServiceMapPage() {
  const { data, error, loading } = usePolling(
    () => Promise.all([api.services(), api.listIncidents({ size: 100 })]),
    5000,
  );

  if (loading && !data) return <Loading />;
  if (error && !data) return <ErrorBox error={error} />;

  const [graph, incidents] = data;
  const affected = new Set(
    incidents.content
      .filter((i) => i.status !== 'RESOLVED')
      .flatMap((i) => i.affectedServices),
  );
  const pos = layout(graph.services);
  const height = 3 * ROW_HEIGHT;

  return (
    <>
      <div className="page-header">
        <h1>Service Map</h1>
        <span className="subtitle">services in an active incident are outlined in red</span>
      </div>

      <div className="card" style={{ overflowX: 'auto' }}>
        <svg viewBox={`0 0 ${WIDTH} ${height}`} width="100%" style={{ minWidth: 640 }}>
          <defs>
            <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" fill="#6b7684" />
            </marker>
          </defs>

          {graph.edges.map((e, i) => {
            const a = pos[e.source]; const b = pos[e.target];
            if (!a || !b) return null;
            return <line key={i} className="svc-edge" x1={a.x} y1={a.y + NODE_H / 2} x2={b.x} y2={b.y - NODE_H / 2} />;
          })}

          {graph.services.map((s) => {
            const p = pos[s.name];
            const isAffected = affected.has(s.name);
            return (
              <g key={s.name}>
                <rect className={`svc-node ${isAffected ? 'affected' : ''}`}
                  x={p.x - NODE_W / 2} y={p.y - NODE_H / 2} width={NODE_W} height={NODE_H} rx="8" />
                <text className="svc-label" x={p.x} y={p.y + 4} textAnchor="middle">{s.displayName}</text>
              </g>
            );
          })}
        </svg>
      </div>

      <div className="grid cols-3" style={{ marginTop: 16 }}>
        {graph.services.map((s) => (
          <div className="card" key={s.name}>
            <h2>{s.displayName} {affected.has(s.name) && <span className="badge sev-HIGH">in incident</span>}</h2>
            <p style={{ color: 'var(--text-dim)', fontSize: 13, marginTop: 0 }}>{s.description}</p>
            <div style={{ fontSize: 12 }}>
              <div><span style={{ color: 'var(--text-faint)' }}>depends on: </span>
                {s.dependsOn.join(', ') || '—'}</div>
              <div><span style={{ color: 'var(--text-faint)' }}>used by: </span>
                {s.dependedOnBy.join(', ') || '—'}</div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
