import { format } from 'date-fns';
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { SchemaVersion } from '../../api/types';
import { EmptyState } from '../shared/EmptyState';

export function SchemaVersionChart({ versions }: { versions: SchemaVersion[] | undefined }) {
  const items = versions ?? [];

  if (items.length === 0) {
    return <EmptyState message="No schema version history available yet." />;
  }

  const chartData = items.map((v) => ({
    version: `v${v.version}`,
    detectedAt: v.detectedAt,
    breaking: v.breaking,
    value: 1,
  }));

  return (
    <ResponsiveContainer width="100%" height={180}>
      <BarChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#1f2733" vertical={false} />
        <XAxis dataKey="version" stroke="#475467" tick={{ fill: '#64748b', fontSize: 11 }} />
        <YAxis hide />
        <Tooltip
          contentStyle={{ background: '#161b24', border: '1px solid #232a36', borderRadius: 8, fontSize: 12 }}
          formatter={(_value, _name, entry) => [
            entry.payload.breaking ? 'Breaking change' : 'Non-breaking',
            format(new Date(entry.payload.detectedAt), 'MMM d, HH:mm'),
          ]}
        />
        <Bar dataKey="value" radius={[4, 4, 0, 0]}>
          {chartData.map((d) => (
            <Cell key={d.version} fill={d.breaking ? '#f87171' : '#34d399'} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
