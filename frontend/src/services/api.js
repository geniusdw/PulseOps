import axios from 'axios';

// Base URL is empty by default: requests go to the same origin and are proxied
// (Vite in dev, nginx in the container). Override with VITE_API_BASE if needed.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000,
});

function unwrap(promise) {
  return promise.then((r) => r.data);
}

export const api = {
  // Dashboard
  overview: () => unwrap(client.get('/api/dashboard/overview')),

  // Incidents
  listIncidents: (params) => unwrap(client.get('/api/incidents', { params })),
  incident: (id) => unwrap(client.get(`/api/incidents/${id}`)),
  incidentTimeline: (id) => unwrap(client.get(`/api/incidents/${id}/timeline`)),
  incidentRootCause: (id) => unwrap(client.get(`/api/incidents/${id}/root-cause`)),
  acknowledgeIncident: (id) => unwrap(client.post(`/api/incidents/${id}/acknowledge`)),
  resolveIncident: (id) => unwrap(client.post(`/api/incidents/${id}/resolve`)),
  explainIncident: (id) => unwrap(client.post(`/api/incidents/${id}/explain`)),

  // Events
  listEvents: (params) => unwrap(client.get('/api/events', { params })),
  event: (id) => unwrap(client.get(`/api/events/${id}`)),
  postEvent: (body) => unwrap(client.post('/api/events', body)),

  // Topology
  services: () => unwrap(client.get('/api/services')),

  // Simulator
  scenarios: () => unwrap(client.get('/api/simulator/scenarios')),
  runScenario: (slug) => unwrap(client.post(`/api/simulator/scenarios/${slug}`)),

  // Benchmark
  runBenchmark: (params) => unwrap(client.post('/api/benchmark', null, { params })),
};

export const EVENT_TYPES = [
  'HTTP_500', 'HTTP_503', 'HIGH_LATENCY', 'CPU_SPIKE', 'MEMORY_SPIKE',
  'DB_CONNECTION_EXHAUSTION', 'NETWORK_ERROR', 'DEPLOYMENT', 'SERVICE_RESTART',
  'PAYMENT_FAILURE', 'QUEUE_BACKLOG',
];

export const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const STATUSES = ['OPEN', 'INVESTIGATING', 'RESOLVED'];
