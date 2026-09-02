import {
  Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { clock } from '../utils/format.js';

export function EventVolumeChart({ buckets }) {
  const data = (buckets || []).map((b) => ({ t: clock(b.minute), count: b.count }));
  return (
    <ResponsiveContainer width="100%" height={200}>
      <AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -20 }}>
        <defs>
          <linearGradient id="vol" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#3b82f6" stopOpacity={0.5} />
            <stop offset="100%" stopColor="#3b82f6" stopOpacity={0} />
          </linearGradient>
        </defs>
        <XAxis dataKey="t" tick={{ fill: '#6b7684', fontSize: 11 }} interval="preserveStartEnd" />
        <YAxis tick={{ fill: '#6b7684', fontSize: 11 }} allowDecimals={false} />
        <Tooltip contentStyle={{ background: '#161b22', border: '1px solid #2a313c', borderRadius: 8 }} />
        <Area type="monotone" dataKey="count" stroke="#3b82f6" fill="url(#vol)" strokeWidth={2} />
      </AreaChart>
    </ResponsiveContainer>
  );
}
