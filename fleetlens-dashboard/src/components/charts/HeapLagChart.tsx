import { format } from 'date-fns';
import {
  Area,
  ComposedChart,
  Legend,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useMemoryCorrelations, useMemoryTimeline } from '../../api/hooks/useMemoryTimeline';
import { EmptyState } from '../shared/EmptyState';
import { Skeleton } from '../shared/Skeleton';

export function HeapLagChart({ serviceId }: { serviceId: string | undefined }) {
  const { data, isLoading, isError } = useMemoryTimeline(serviceId);
  const { data: correlations } = useMemoryCorrelations();

  if (!serviceId) {
    return <EmptyState message="Select a service above to view its heap and Kafka lag timeline." />;
  }

  if (isLoading) {
    return <Skeleton className="h-[300px] w-full" />;
  }

  if (isError) {
    return <EmptyState tone="error" message="Failed to load the memory timeline." />;
  }

  const points = data?.points ?? [];
  const serviceCorrelations = (correlations ?? []).filter((c) => c.serviceId === serviceId);

  if (points.length === 0) {
    return <EmptyState message="No heap or lag samples yet. The JMX and Kafka pollers run every 30s once a service is registered." />;
  }

  return (
    <ResponsiveContainer width="100%" height={320}>
      <ComposedChart data={points} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
        <XAxis
          dataKey="sampledAt"
          tickFormatter={(t) => format(new Date(t), 'HH:mm:ss')}
          stroke="#475467"
          tick={{ fill: '#64748b', fontSize: 11 }}
        />
        <YAxis
          yAxisId="heap"
          orientation="left"
          stroke="#475467"
          tick={{ fill: '#64748b', fontSize: 11 }}
          label={{ value: 'Heap (MB)', angle: -90, position: 'insideLeft', fill: '#64748b', fontSize: 11 }}
        />
        <YAxis
          yAxisId="lag"
          orientation="right"
          stroke="#475467"
          tick={{ fill: '#64748b', fontSize: 11 }}
          label={{ value: 'Consumer lag', angle: 90, position: 'insideRight', fill: '#64748b', fontSize: 11 }}
        />
        <Tooltip
          contentStyle={{ background: '#161b24', border: '1px solid #232a36', borderRadius: 8, fontSize: 12 }}
          labelFormatter={(t) => format(new Date(t as string), 'MMM d, HH:mm:ss')}
        />
        <Legend wrapperStyle={{ fontSize: 12, color: '#9aa6b8' }} />
        <Area
          yAxisId="heap"
          dataKey="heapUsedMb"
          fill="#6366f1"
          fillOpacity={0.15}
          stroke="#818cf8"
          strokeWidth={2}
          name="Heap used (MB)"
        />
        <Line yAxisId="lag" dataKey="kafkaLag" stroke="#fb923c" strokeWidth={2} dot={false} name="Consumer lag" />
        {serviceCorrelations.map((c) => (
          <ReferenceLine
            key={c.id}
            yAxisId="heap"
            x={c.occurredAt}
            stroke="#f59e0b"
            strokeDasharray="4 3"
            label={{ value: '⚠', position: 'top', fill: '#f59e0b' }}
          />
        ))}
      </ComposedChart>
    </ResponsiveContainer>
  );
}
