import { Link } from 'react-router-dom';
import { api } from '../services/api.js';
import { usePolling } from '../hooks/usePolling.js';
import { StatCard } from '../components/StatCard.jsx';
import { SeverityBadge, StatusBadge } from '../components/Badges.jsx';
import { Loading, ErrorBox } from '../components/States.jsx';
import { EventVolumeChart } from '../charts/EventVolumeChart.jsx';
import { SeverityDistributionChart } from '../charts/SeverityDistributionChart.jsx';
import { timeAgo } from '../utils/format.js';

export default function DashboardPage() {
  const { data, error, loading } = usePolling(api.overview, 4000);

  if (loading && !data) return <Loading />;
  if (error && !data) return <ErrorBox error={error} />;

  const o = data;
  const p = o.pipeline;

  return (
    <>
      <div className="page-header">
        <h1>Dashboard</h1>
        <span className="subtitle">auto-refreshing every 4s</span>
      </div>

      <div className="grid cols-4" style={{ marginBottom: 16 }}>
        <StatCard label="Active incidents" value={o.activeIncidents}
          tone={o.activeIncidents > 0 ? 'high' : undefined} />
        <StatCard label="Critical incidents" value={o.criticalIncidents}
          tone={o.criticalIncidents > 0 ? 'critical' : undefined} />
        <StatCard label="Events / min" value={o.eventsPerMinute}
          hint={`${o.eventsLast15Minutes} in last 15 min`} />
        <StatCard label="Services affected" value={o.affectedServices.length}
          hint={o.affectedServices.join(', ') || 'none'} />
      </div>

      <div className="grid cols-2" style={{ marginBottom: 16 }}>
        <div className="card">
          <h2>Event volume (last 30 min)</h2>
          <EventVolumeChart buckets={o.eventVolume} />
        </div>
        <div className="card">
          <h2>Open incident severity</h2>
          <SeverityDistributionChart distribution={o.severityDistribution} />
        </div>
      </div>

      <div className="grid cols-2">
        <div className="card">
          <h2>Recent incidents</h2>
          {o.recentIncidents.length === 0 && <p style={{ color: 'var(--text-faint)' }}>No incidents yet — try the Simulator.</p>}
          {o.recentIncidents.length > 0 && (
            <table>
              <thead>
                <tr><th>ID</th><th>Title</th><th>Sev</th><th>Status</th><th>Updated</th></tr>
              </thead>
              <tbody>
                {o.recentIncidents.map((i) => (
                  <tr key={i.incidentId}>
                    <td className="mono"><Link to={`/incidents/${i.incidentId}`}>{i.incidentId}</Link></td>
                    <td>{i.title}</td>
                    <td><SeverityBadge value={i.severity} /></td>
                    <td><StatusBadge value={i.status} /></td>
                    <td style={{ color: 'var(--text-faint)' }}>{timeAgo(i.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <h2>Ingest pipeline</h2>
          <table>
            <tbody>
              <tr><td>Queue depth</td><td className="mono">{p.queueDepth} / {p.queueCapacity}</td></tr>
              <tr><td>Peak queue depth</td><td className="mono">{p.peakQueueDepth}</td></tr>
              <tr><td>Workers</td><td className="mono">{p.workerCount}</td></tr>
              <tr><td>Enqueued</td><td className="mono">{p.enqueued}</td></tr>
              <tr><td>Processed</td><td className="mono">{p.processed}</td></tr>
              <tr><td>Rejected (backpressure)</td><td className="mono">{p.rejected}</td></tr>
              <tr><td>Failed</td><td className="mono">{p.failed}</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
