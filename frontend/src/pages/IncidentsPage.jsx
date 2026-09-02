import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, SEVERITIES, STATUSES } from '../services/api.js';
import { usePolling } from '../hooks/usePolling.js';
import { SeverityBadge, StatusBadge } from '../components/Badges.jsx';
import { ConfidenceBar } from '../components/ConfidenceBar.jsx';
import { Loading, ErrorBox, Empty } from '../components/States.jsx';
import { timeAgo } from '../utils/format.js';

export default function IncidentsPage() {
  const [status, setStatus] = useState('');
  const [severity, setSeverity] = useState('');

  const { data, error, loading } = usePolling(
    () => api.listIncidents({ status: status || undefined, severity: severity || undefined, size: 100 }),
    5000,
    [status, severity],
  );

  return (
    <>
      <div className="page-header"><h1>Incidents</h1></div>

      <div className="filters">
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select value={severity} onChange={(e) => setSeverity(e.target.value)}>
          <option value="">All severities</option>
          {SEVERITIES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {loading && !data && <Loading />}
      {error && !data && <ErrorBox error={error} />}
      {data && data.content.length === 0 && <Empty label="No incidents match these filters." />}

      {data && data.content.length > 0 && (
        <div className="card" style={{ padding: 0 }}>
          <table>
            <thead>
              <tr>
                <th>ID</th><th>Title</th><th>Severity</th><th>Status</th>
                <th>Affected services</th><th>Started</th><th>Confidence</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((i) => (
                <tr key={i.incidentId}>
                  <td className="mono"><Link to={`/incidents/${i.incidentId}`}>{i.incidentId}</Link></td>
                  <td>{i.title}</td>
                  <td><SeverityBadge value={i.severity} /></td>
                  <td><StatusBadge value={i.status} /></td>
                  <td>
                    <div className="pill-row">
                      {i.affectedServices.map((s) => <span key={s} className="pill">{s}</span>)}
                    </div>
                  </td>
                  <td style={{ color: 'var(--text-faint)' }}>{timeAgo(i.startedAt)}</td>
                  <td style={{ width: 90 }}><ConfidenceBar value={i.confidenceScore} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
