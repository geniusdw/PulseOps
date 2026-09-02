import { useState } from 'react';
import { api, EVENT_TYPES, SEVERITIES } from '../services/api.js';
import { usePolling } from '../hooks/usePolling.js';
import { SeverityBadge } from '../components/Badges.jsx';
import { Loading, ErrorBox, Empty } from '../components/States.jsx';
import { clock } from '../utils/format.js';

const SERVICES = ['api-gateway', 'payment-api', 'transaction-service', 'user-service', 'notification-service', 'database'];

export default function EventExplorerPage() {
  const [service, setService] = useState('');
  const [eventType, setEventType] = useState('');
  const [severity, setSeverity] = useState('');

  const { data, error, loading } = usePolling(
    () => api.listEvents({
      service: service || undefined,
      eventType: eventType || undefined,
      severity: severity || undefined,
      size: 100,
    }),
    4000,
    [service, eventType, severity],
  );

  return (
    <>
      <div className="page-header">
        <h1>Event Explorer</h1>
        <span className="subtitle">{data ? `${data.totalElements} events total` : ''}</span>
      </div>

      <div className="filters">
        <select value={service} onChange={(e) => setService(e.target.value)}>
          <option value="">All services</option>
          {SERVICES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select value={eventType} onChange={(e) => setEventType(e.target.value)}>
          <option value="">All event types</option>
          {EVENT_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
        <select value={severity} onChange={(e) => setSeverity(e.target.value)}>
          <option value="">All severities</option>
          {SEVERITIES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {loading && !data && <Loading />}
      {error && !data && <ErrorBox error={error} />}
      {data && data.content.length === 0 && <Empty label="No events match these filters." />}

      {data && data.content.length > 0 && (
        <div className="card" style={{ padding: 0 }}>
          <table>
            <thead>
              <tr><th>Event ID</th><th>Time</th><th>Service</th><th>Type</th><th>Severity</th><th>Message</th></tr>
            </thead>
            <tbody>
              {data.content.map((e) => (
                <tr key={e.eventId}>
                  <td className="mono">{e.eventId}</td>
                  <td className="mono">{clock(e.timestamp)}</td>
                  <td>{e.service}</td>
                  <td className="mono">{e.eventType}</td>
                  <td><SeverityBadge value={e.severity} /></td>
                  <td style={{ color: 'var(--text-dim)' }}>{e.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
