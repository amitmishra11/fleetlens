import { useNavigate, useParams } from 'react-router-dom';
import { Activity, GitCompareArrows, Replace, SlidersHorizontal } from 'lucide-react';
import { useServiceTimeline } from '../api/hooks/useTimeline';
import { IncidentTimeline } from '../components/timeline/IncidentTimeline';
import { Card, CardBody, CardHeader, CardTitle } from '../components/shared/Card';
import { EmptyState } from '../components/shared/EmptyState';
import { PageHeader } from '../components/shared/PageHeader';
import { TableSkeleton } from '../components/shared/Skeleton';
import { TimeRangePicker } from '../components/shared/TimeRangePicker';
import { Button } from '../components/shared/Button';
import { useAppStore } from '../store/useAppStore';

const MODULE_LINKS = [
  { to: '/schema', label: 'Schema Drift', icon: GitCompareArrows },
  { to: '/traces', label: 'Trace Replay', icon: Replace },
  { to: '/memory', label: 'Memory Profiler', icon: Activity },
  { to: '/config', label: 'Config Audit', icon: SlidersHorizontal },
];

export function ServiceDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const timeRange = useAppStore((s) => s.timeRange);
  const setSelectedServiceId = useAppStore((s) => s.setSelectedServiceId);
  const { data, isLoading, isError } = useServiceTimeline(id, {
    from: timeRange.from.toISOString(),
    to: timeRange.to.toISOString(),
  });

  if (!id) {
    return <EmptyState message="No service specified." />;
  }

  function goToModule(to: string) {
    setSelectedServiceId(id ?? null);
    navigate(to);
  }

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={id} subtitle="Per-service signal timeline across all modules." actions={<TimeRangePicker />} />

      <section className="flex flex-wrap gap-3">
        {MODULE_LINKS.map((link) => (
          <Button key={link.to} variant="secondary" onClick={() => goToModule(link.to)}>
            <link.icon className="size-4" />
            {link.label}
          </Button>
        ))}
      </section>

      <Card>
        <CardHeader>
          <CardTitle>Service Timeline</CardTitle>
        </CardHeader>
        <CardBody>
          {isLoading ? (
            <TableSkeleton rows={4} cols={1} />
          ) : isError ? (
            <EmptyState tone="error" message="Failed to load this service's timeline." />
          ) : (
            <IncidentTimeline entries={data?.entries} />
          )}
        </CardBody>
      </Card>
    </div>
  );
}
