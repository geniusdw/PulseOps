import { useCallback, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../services/api.js';
import { usePolling } from '../hooks/usePolling.js';
import { SeverityBadge, StatusBadge } from '../components/Badges.jsx';
import { ConfidenceBar } from '../components/ConfidenceBar.jsx';
import { Loading, ErrorBox } from '../components/States.jsx';
import { clock, dateTime } from '../utils/format.js';

export default function IncidentDetailPage() {
  const { id } = useParams();
  const fetcher = useCallback(() => api.incident(id), [id]);
  const { data, error, loading, refresh } = usePolling(fetcher, 5000, [id]);

  const [busy, setBusy] = useState(false);
  const [explanation, setExplanation] = useState(null);
  const [explainError, setExplainError] = useState(null);

  async function act(fn) {
    setBusy(true);
    try { await fn(id); await refresh(); } finally { setBusy(false); }
  }

  async function explain() {
    setBusy(true); setExplainError(null);
    try { setExplanation(await api.explainIncident(id)); }
    catch (e) { setExplainError(e?.response?.data?.message || e.message); }
    finally { setBusy(false); }
  }

  if (loading && !data) return <Loading />;
  if (error && !data) return <ErrorBox error={error} />;
  const inc = data;

  return (
    <>
      <div className="page-header">
        <div>
          <Link to="/incidents" style={{ fontSize: 13 }}>← Incidents</Link>
          <h1 style={{ marginTop: 6 }}>{inc.incidentId} · {inc.title}</h1>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button disabled={busy || inc.status !== 'OPEN'} onClick={() => act(api.acknowledgeIncident)}>
            Acknowledge
          </button>
          <button className="danger" disabled={busy || inc.status === 'RESOLVED'}
            onClick={() => act(api.resolveIncident)}>Resolve</button>
        </div>
      </div>

      <div className="grid cols-4" style={{ marginBottom: 16 }}>
        <div className="card"><div className="label" style={{ color: 'var(--text-faint)', fontSize: 12 }}>Severity</div>
          <div style={{ marginTop: 8 }}><SeverityBadge value={inc.severity} /></div></div>
        <div className="card"><div className="label" style={{ color: 'var(--text-faint)', fontSize: 12 }}>Status</div>
          <div style={{ marginTop: 8 }}><StatusBadge value={inc.status} /></div></div>
        <div className="card"><div className="label" style={{ color: 'var(--text-faint)', fontSize: 12 }}>Grouping confidence</div>
          <div style={{ marginTop: 8 }}><ConfidenceBar value={inc.confidenceScore} /></div></div>
        <div className="card"><div className="label" style={{ color: 'var(--text-faint)', fontSize: 12 }}>Started</div>
          <div style={{ marginTop: 8, fontSize: 13 }}>{dateTime(inc.startedAt)}</div></div>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <h2>Affected services</h2>
        <div className="pill-row">{inc.affectedServices.map((s) => <span key={s} className="pill">{s}</span>)}</div>
      </div>

      <div className="grid cols-2" style={{ marginBottom: 16 }}>
        <div className="card">
          <h2>Why these events were grouped</h2>
          <div className="explanation">{inc.correlationSummary || 'No correlation summary.'}</div>
          {inc.correlation?.topSignals?.length > 0 && (
            <table style={{ marginTop: 12 }}>
              <thead><tr><th>Signal</th><th>Avg strength</th><th>Example</th></tr></thead>
              <tbody>
                {inc.correlation.topSignals.map((s) => (
                  <tr key={s.signal}>
                    <td>{s.signal}</td>
                    <td className="mono">{s.averageRawValue.toFixed(2)}</td>
                    <td style={{ color: 'var(--text-dim)', fontSize: 12 }}>{s.sampleDetails?.[0]}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <h2>Probable root cause</h2>
          <p style={{ color: 'var(--text-faint)', fontSize: 12, marginTop: 0 }}>
            Heuristic scores from rules over the correlated events — ranked hypotheses, not probabilities.
          </p>
          {inc.rootCauses.map((rc) => (
            <div key={rc.type} style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <strong>{rc.label}</strong>
                <span className="mono">{Math.round(rc.score * 100)}%</span>
              </div>
              <div className="confidence-bar" style={{ margin: '4px 0' }}>
                <div style={{ width: `${Math.round(rc.score * 100)}%` }} />
              </div>
              <ul className="evidence-list">
                {rc.evidence.map((e, idx) => <li key={idx}>{e}</li>)}
              </ul>
            </div>
          ))}
        </div>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2>AI explanation (optional)</h2>
          <button className="primary" disabled={busy} onClick={explain}>Generate explanation</button>
        </div>
        {explainError && <ErrorBox error={explainError} />}
        {explanation && (
          <div style={{ marginTop: 12 }}>
            <p>{explanation.summary}</p>
            <p><strong>Probable cause:</strong> {explanation.probableCause}
              <span className="pill" style={{ marginLeft: 8 }}>generated by {explanation.generatedBy}</span></p>
            <h3>Evidence</h3>
            <ul className="evidence-list">{explanation.evidence.map((e, i) => <li key={i}>{e}</li>)}</ul>
            <h3>Recommended checks</h3>
            <ul className="evidence-list">{explanation.recommendedChecks.map((c, i) => <li key={i}>{c}</li>)}</ul>
          </div>
        )}
        {!explanation && !explainError && (
          <p style={{ color: 'var(--text-faint)', fontSize: 13 }}>
            The deterministic engine above is the source of truth. This turns it into prose (mock explainer by default;
            swap in an LLM behind the same interface).
          </p>
        )}
      </div>

      <div className="grid cols-2">
        <div className="card">
          <h2>Timeline</h2>
          <ul className="timeline">
            {inc.timeline.map((t, i) => (
              <li key={i}>
                <span className={`dot ${t.kind}`} />
                <div className="t-time">{clock(t.at)} · {t.kind}</div>
                <div className="t-title">{t.title}</div>
                {t.detail && <div className="t-detail">{t.detail}</div>}
              </li>
            ))}
          </ul>
        </div>

        <div className="card">
          <h2>Correlated events ({inc.events.length})</h2>
          <table>
            <thead><tr><th>Time</th><th>Service</th><th>Type</th><th>Sev</th></tr></thead>
            <tbody>
              {inc.events.map((e) => (
                <tr key={e.eventId}>
                  <td className="mono">{clock(e.timestamp)}</td>
                  <td>{e.service}</td>
                  <td className="mono">{e.eventType}</td>
                  <td><SeverityBadge value={e.severity} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
