import {
  Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';

const COLORS = {
  LOW: '#3fb950', MEDIUM: '#d29922', HIGH: '#f85149', CRITICAL: '#ff5c8a',
};

export function SeverityDistributionChart({ distribution }) {
  const data = Object.entries(distribution || {}).map(([severity, count]) => ({ severity, count }));
  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -20 }}>
        <XAxis dataKey="severity" tick={{ fill: '#6b7684', fontSize: 11 }} />
        <YAxis tick={{ fill: '#6b7684', fontSize: 11 }} allowDecimals={false} />
        <Tooltip cursor={{ fill: '#1c2230' }}
          contentStyle={{ background: '#161b22', border: '1px solid #2a313c', borderRadius: 8 }} />
        <Bar dataKey="count" radius={[4, 4, 0, 0]}>
          {data.map((d) => <Cell key={d.severity} fill={COLORS[d.severity] || '#3b82f6'} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
