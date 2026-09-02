import { NavLink, Outlet } from 'react-router-dom';

const links = [
  { to: '/', label: 'Dashboard', end: true, icon: '▦' },
  { to: '/incidents', label: 'Incidents', icon: '⚠' },
  { to: '/events', label: 'Event Explorer', icon: '≡' },
  { to: '/services', label: 'Service Map', icon: '⬡' },
  { to: '/simulator', label: 'Simulator', icon: '⚡' },
];

export default function Layout() {
  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">Pulse<span>Ops</span></div>
        <nav>
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end}
              className={({ isActive }) => (isActive ? 'active' : '')}>
              <span aria-hidden>{l.icon}</span>
              <span>{l.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
