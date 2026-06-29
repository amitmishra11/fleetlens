import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { AlertOctagon, CheckCircle2, ListTree, Server } from 'lucide-react';
import { useIncidents, useResolveIncident } from '../api/hooks/useIncidents';
import { useGlobalTimeline } from '../api/hooks/useTimeline';
import { useConfigServices } from '../api/hooks/useConfigMatrix';
import { IncidentTimeline } from '../components/timeline/IncidentTimeline';
import { Card, CardBody, CardHeader, CardTitle } from '../components/shared/Card';
import { EmptyState } from '../components/shared/EmptyState';
import { PageHeader } from '../components/shared/PageHeader';
import { SeverityBadge } from '../components/shared/SeverityBadge';
import { StatCard } from '../components/shared/StatCard';
import { TableSkeleton } from '../components/shared/Skeleton';
import { TimeRangePicker } from '../components/shared/TimeRangePicker';
import { Button } from '../components/shared/Button';
import { useAppStore } from '../store/useAppStore';

export function Dashboard() {
  const timeRange = useAppStore((s) => s.timeRange);
  const { data: timeline, isLoading: timelineLoading, isError: timelineError } = useGlobalTimeline({
    from: timeRange.from.toISOString(),
    to: timeRange.to.toISOString(),
  });
  const { data: incidentPage, isLoading: incidentsLoading, isError: incidentsError } = useIncidents({
    page: 0,
    size: 10,
  });
  const { data: services } = useConfigServices();
  const resolveIncident = useResolveIncident();

  const incidents = incidentPage?.content ?? [];
  const openIncidents = incidents.filter((i) => !i.resolvedAt);
  const criticalOpen = openIncidents.filter((i) => i.severity === 'CRITICAL');

  function handleResolve(id: string) {
    resolveIncident.mutate(id, {
      onSuccess: () => toast.success('Incident resolved'),
      onError: () => toast.error('Failed to resolve incident'),
    });
  }

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        title="Overview"
        subtitle="Cross-service signals correlated into a single incident timeline."
        actions={<TimeRangePicker />}
      />

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard label="Open incidents" value={openIncidents.length} icon={AlertOctagon} tone={openIncidents.length > 0 ? 'warn' : 'good'} />
        <StatCard label="Critical" value={criticalOpen.length} icon={AlertOctagon} tone={criticalOpen.length > 0 ? 'critical' : 'good'} />
        <StatCard label="Services monitored" value={services?.length ?? 0} icon={Server} />
        <StatCard label="Resolved (page)" value={incidents.length - openIncidents.length} icon={CheckCircle2} tone="good" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Global Incident Timeline</CardTitle>
        </CardHeader>
        <CardBody>
          {timelineLoading ? (
            <TableSkeleton rows={4} cols={1} />
          ) : timelineError ? (
            <EmptyState tone="error" message="Failed to load the global timeline." />
          ) : (
            <IncidentTimeline entries={timeline} showService />
          )}
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Recent Incidents</CardTitle>
        </CardHeader>
        {incidentsLoading ? (
          <TableSkeleton />
        ) : incidentsError ? (
          <CardBody>
            <EmptyState tone="error" message="Failed to load incidents." />
          </CardBody>
        ) : incidents.length === 0 ? (
          <CardBody>
            <EmptyState icon={ListTree} message="No incidents recorded yet. Once two or more module signals correlate within the time window, they'll show up here." />
          </CardBody>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead>
                <tr className="border-b border-border text-xs uppercase tracking-wide text-slate-500">
                  <th className="whitespace-nowrap px-5 py-3 font-medium">Service</th>
                  <th className="px-5 py-3 font-medium">Title</th>
                  <th className="whitespace-nowrap px-5 py-3 font-medium">Severity</th>
                  <th className="whitespace-nowrap px-5 py-3 font-medium">Signals</th>
                  <th className="whitespace-nowrap px-5 py-3 font-medium">Opened</th>
                  <th className="whitespace-nowrap px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium" />
                </tr>
              </thead>
              <tbody>
                {incidents.map((incident) => (
                  <tr key={incident.id} className="border-b border-border/60 last:border-0 hover:bg-surface-hover/50">
                    <td className="whitespace-nowrap px-5 py-3">
                      <Link to={`/services/${incident.serviceId}`} className="font-medium text-brand-400 hover:underline">
                        {incident.serviceId}
                      </Link>
                    </td>
                    <td className="max-w-xs truncate px-5 py-3 text-slate-300" title={incident.title}>{incident.title}</td>
                    <td className="whitespace-nowrap px-5 py-3">
                      <SeverityBadge severity={incident.severity} />
                    </td>
                    <td className="whitespace-nowrap px-5 py-3 text-slate-400">{incident.eventCount}</td>
                    <td className="whitespace-nowrap px-5 py-3 text-slate-500">{new Date(incident.openedAt).toLocaleString()}</td>
                    <td className="whitespace-nowrap px-5 py-3">
                      {incident.resolvedAt ? (
                        <span className="text-xs font-medium text-emerald-400">Resolved</span>
                      ) : (
                        <span className="text-xs font-medium text-amber-400">Open</span>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-5 py-3 text-right">
                      {!incident.resolvedAt && (
                        <Button size="sm" onClick={() => handleResolve(incident.id)} loading={resolveIncident.isPending}>
                          Resolve
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
