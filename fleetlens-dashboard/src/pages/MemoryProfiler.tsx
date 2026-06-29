import { FileWarning, Zap } from 'lucide-react';
import { HeapLagChart } from '../components/charts/HeapLagChart';
import { useHeapDumps, useMemoryCorrelations } from '../api/hooks/useMemoryTimeline';
import { Card, CardBody, CardHeader, CardTitle } from '../components/shared/Card';
import { EmptyState } from '../components/shared/EmptyState';
import { PageHeader } from '../components/shared/PageHeader';
import { ServiceSelector } from '../components/shared/ServiceSelector';
import { useAppStore } from '../store/useAppStore';

export function MemoryProfiler() {
  const selectedServiceId = useAppStore((s) => s.selectedServiceId);
  const { data: correlations } = useMemoryCorrelations();
  const { data: heapDumps } = useHeapDumps();

  const filteredCorrelations = selectedServiceId
    ? (correlations ?? []).filter((c) => c.serviceId === selectedServiceId)
    : correlations ?? [];
  const filteredDumps = selectedServiceId
    ? (heapDumps ?? []).filter((d) => d.serviceId === selectedServiceId)
    : heapDumps ?? [];

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        title="Memory Profiler"
        subtitle="JVM heap usage correlated with Kafka consumer lag, sampled via JMX every 30s."
        actions={<ServiceSelector />}
      />

      <Card>
        <CardHeader>
          <CardTitle>Heap &amp; Lag Timeline</CardTitle>
        </CardHeader>
        <CardBody>
          <HeapLagChart serviceId={selectedServiceId ?? undefined} />
        </CardBody>
      </Card>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Heap / Lag Correlations</CardTitle>
          </CardHeader>
          <CardBody>
            {filteredCorrelations.length === 0 ? (
              <EmptyState
                icon={Zap}
                message="No correlations yet — these fire when heap usage and consumer lag rise together within the same 5-minute window."
              />
            ) : (
              <ul className="flex flex-col gap-3">
                {filteredCorrelations.map((c) => (
                  <li key={c.id} className="rounded-lg bg-amber-500/5 px-4 py-3 text-sm ring-1 ring-inset ring-amber-500/20">
                    <div className="flex items-center justify-between text-xs text-slate-500">
                      <span>{c.serviceId}</span>
                      <span>{new Date(c.occurredAt).toLocaleString()}</span>
                    </div>
                    <p className="mt-1 text-amber-200">{c.summary}</p>
                    <p className="mt-1 text-xs text-slate-500">
                      heap trend {c.heapTrend.toFixed(3)} · lag trend {c.lagTrend.toFixed(3)}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Triggered Heap Dumps</CardTitle>
          </CardHeader>
          <CardBody>
            {filteredDumps.length === 0 ? (
              <EmptyState
                icon={FileWarning}
                message="No heap dumps triggered — these fire automatically once heap usage crosses the configured threshold."
              />
            ) : (
              <ul className="flex flex-col gap-3">
                {filteredDumps.map((d) => (
                  <li key={d.filePath} className="rounded-lg bg-surface-raised px-4 py-3 text-sm ring-1 ring-inset ring-border">
                    <div className="flex items-center justify-between text-xs text-slate-500">
                      <span>{d.serviceId}</span>
                      <span>{new Date(d.triggeredAt).toLocaleString()}</span>
                    </div>
                    <p className="mt-1 break-all font-mono text-xs text-slate-400">{d.filePath}</p>
                  </li>
                ))}
              </ul>
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
