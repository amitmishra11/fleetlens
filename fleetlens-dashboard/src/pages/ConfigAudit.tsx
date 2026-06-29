import clsx from 'clsx';
import { History } from 'lucide-react';
import { useConfigDiff, useConfigMatrix } from '../api/hooks/useConfigMatrix';
import { Card, CardBody, CardHeader, CardTitle } from '../components/shared/Card';
import { EmptyState } from '../components/shared/EmptyState';
import { PageHeader } from '../components/shared/PageHeader';
import { ServiceSelector } from '../components/shared/ServiceSelector';
import { TableSkeleton } from '../components/shared/Skeleton';
import { useAppStore } from '../store/useAppStore';

const CHANGE_TYPE_CLASSES: Record<string, string> = {
  ADDED: 'bg-emerald-500/5 text-emerald-300 ring-emerald-500/20',
  REMOVED: 'bg-red-500/5 text-red-300 ring-red-500/20',
  MODIFIED: 'bg-amber-500/5 text-amber-300 ring-amber-500/20',
};

export function ConfigAudit() {
  const selectedServiceId = useAppStore((s) => s.selectedServiceId);
  const { data, isLoading, isError } = useConfigMatrix();
  const { data: diff } = useConfigDiff(selectedServiceId ?? undefined);

  const envs = data?.environments ?? [];
  const rows = data?.keys ?? [];

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        title="Config Audit"
        subtitle="Live /actuator/env snapshots, diffed across environments and over time."
        actions={<ServiceSelector />}
      />

      <Card>
        <CardHeader>
          <CardTitle>Key × Environment Matrix</CardTitle>
        </CardHeader>
        {isLoading ? (
          <TableSkeleton />
        ) : isError ? (
          <CardBody>
            <EmptyState tone="error" message="Failed to load the config matrix." />
          </CardBody>
        ) : rows.length === 0 ? (
          <CardBody>
            <EmptyState message="No config data yet. The auditor polls every registered service's /actuator/env every 60s." />
          </CardBody>
        ) : (
          <div className="max-h-[480px] overflow-auto">
            <table className="w-full text-left text-sm">
              <thead className="sticky top-0 bg-surface">
                <tr className="border-b border-border text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-5 py-3 font-medium">Key</th>
                  {envs.map((env) => (
                    <th key={env} className="px-5 py-3 text-center font-medium">
                      {env}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.key} className={clsx('border-b border-border/60 last:border-0', row.hasDrift && 'bg-amber-500/5')}>
                    <td className="px-5 py-3 font-mono text-xs text-slate-300">{row.key}</td>
                    {envs.map((env) => (
                      <td key={env} className="px-5 py-3 text-center font-mono text-xs text-slate-400">
                        {row.values[env] !== undefined && row.values[env] !== null ? (
                          String(row.values[env])
                        ) : (
                          <span className="text-slate-600">—</span>
                        )}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Recent Changes{selectedServiceId ? ` — ${selectedServiceId}` : ''}</CardTitle>
        </CardHeader>
        <CardBody>
          {!selectedServiceId ? (
            <EmptyState icon={History} message="Select a service above to see its most recent config change." />
          ) : !diff || diff.length === 0 ? (
            <EmptyState message="No drift detected yet between this service's last two snapshots." />
          ) : (
            <ul className="flex flex-col gap-2">
              {diff.map((change) => (
                <li
                  key={change.key}
                  className={clsx('flex flex-col gap-1 rounded-lg px-4 py-3 text-sm ring-1 ring-inset', CHANGE_TYPE_CLASSES[change.changeType])}
                >
                  <span className="font-mono text-xs">{change.key}</span>
                  <div className="flex flex-wrap gap-4 text-xs">
                    <span>before: <code className="text-mono">{JSON.stringify(change.oldValue)}</code></span>
                    <span>after: <code className="text-mono">{JSON.stringify(change.newValue)}</code></span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
