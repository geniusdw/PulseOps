import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import IncidentsPage from './pages/IncidentsPage.jsx';
import IncidentDetailPage from './pages/IncidentDetailPage.jsx';
import EventExplorerPage from './pages/EventExplorerPage.jsx';
import ServiceMapPage from './pages/ServiceMapPage.jsx';
import SimulatorPage from './pages/SimulatorPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="incidents" element={<IncidentsPage />} />
        <Route path="incidents/:id" element={<IncidentDetailPage />} />
        <Route path="events" element={<EventExplorerPage />} />
        <Route path="services" element={<ServiceMapPage />} />
        <Route path="simulator" element={<SimulatorPage />} />
        <Route path="*" element={<div className="state-msg">Page not found.</div>} />
      </Route>
    </Routes>
  );
}
