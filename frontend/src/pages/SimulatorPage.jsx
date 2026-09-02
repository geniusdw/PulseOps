import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../services/api.js';
import { ErrorBox } from '../components/States.jsx';

export default function SimulatorPage() {
  const [scenarios, setScenarios] = useState([]);
  const [running, setRunning] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => { api.scenarios().then(setScenarios).catch((e) => setError(e.message)); }, []);

  async function run(slug) {
    setRunning(slug); setError(null); setResult(null);
    try { setResult(await api.runScenario(slug)); }
    catch (e) { setError(e?.response?.data?.message || e.message); }
    finally { setRunning(null); }
  }

  return (
    <>
      <div className="page-header">
        <h1>Simulator</h1>
        <span className="subtitle">inject synthetic cloud failures through the real pipeline</span>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <h2>Scenarios</h2>
        <div className="grid cols-2" style={{ marginTop: 8 }}>
          {scenarios.map((s) => (
            <div key={s.slug} style={{ border: '1px solid var(--border)', borderRadius: 8, padding: 12 }}>
              <strong>{s.name.replaceAll('_', ' ')}</strong>
              <p style={{ color: 'var(--text-dim)', fontSize: 13 }}>{s.description}</p>
              <button className="primary" disabled={running} onClick={() => run(s.slug)}>
                {running === s.slug ? 'Injecting…' : 'Run scenario'}
              </button>
            </div>
          ))}
        </div>
      </div>

      {error && <ErrorBox error={error} />}

      {result && (
        <div className="card" style={{ marginBottom: 16 }}>
          <h2>Last run: {result.scenario}</h2>
          <p style={{ color: 'var(--text-dim)' }}>{result.note}</p>
          <table>
            <tbody>
              <tr><td>Events created</td><td className="mono">{result.eventsCreated}</td></tr>
              <tr><td>Event IDs</td><td className="mono">{result.eventIds.join(', ')}</td></tr>
              {result.deploymentId && <tr><td>Deployment</td><td className="mono">{result.deploymentId}</td></tr>}
            </tbody>
          </table>
          <p style={{ marginTop: 10 }}>
            <Link to="/incidents">→ See the incident</Link> &nbsp;·&nbsp;
            <Link to="/">→ Dashboard</Link>
          </p>
        </div>
      )}

      <BenchmarkPanel />
    </>
  );
}

function BenchmarkPanel() {
  const [count, setCount] = useState(10000);
  const [workers, setWorkers] = useState(4);
  const [windowSize, setWindowSize] = useState(500);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function run() {
    setBusy(true); setError(null);
    try { setResult(await api.runBenchmark({ count, workers, windowSize })); }
    catch (e) { setError(e?.response?.data?.message || e.message); }
    finally { setBusy(false); }
  }

  return (
    <div className="card">
      <h2>Correlation benchmark</h2>
      <p style={{ color: 'var(--text-faint)', fontSize: 13, marginTop: 0 }}>
        Measures correlation throughput in memory on this machine right now (no database writes).
      </p>
      <div className="filters">
        <label>events <input type="number" value={count} min={1} max={200000}
          onChange={(e) => setCount(+e.target.value)} style={{ width: 100 }} /></label>
        <label>workers <input type="number" value={workers} min={1} max={64}
          onChange={(e) => setWorkers(+e.target.value)} style={{ width: 70 }} /></label>
        <label>window <input type="number" value={windowSize} min={10} max={5000}
          onChange={(e) => setWindowSize(+e.target.value)} style={{ width: 80 }} /></label>
        <button className="primary" disabled={busy} onClick={run}>{busy ? 'Running…' : 'Run benchmark'}</button>
      </div>
      {error && <ErrorBox error={error} />}
      {result && (
        <table>
          <tbody>
            <tr><td>Events</td><td className="mono">{result.eventCount}</td></tr>
            <tr><td>Workers</td><td className="mono">{result.workerCount}</td></tr>
            <tr><td>Window size</td><td className="mono">{result.windowSize}</td></tr>
            <tr><td>Windows processed</td><td className="mono">{result.windowsProcessed}</td></tr>
            <tr><td>Clusters found</td><td className="mono">{result.clustersFound}</td></tr>
            <tr><td>Elapsed</td><td className="mono">{result.elapsedMs} ms</td></tr>
            <tr><td>Throughput</td><td className="mono">{result.throughputPerSecond.toLocaleString()} events/s</td></tr>
          </tbody>
        </table>
      )}
      {result && <p style={{ color: 'var(--text-faint)', fontSize: 12 }}>{result.note}</p>}
    </div>
  );
}
