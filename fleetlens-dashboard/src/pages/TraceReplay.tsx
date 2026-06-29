import { useState } from 'react';
import clsx from 'clsx';
import { toast } from 'sonner';
import { Plus, Replace } from 'lucide-react';
import {
  useCaptureDemoTrace,
  useDeleteTrace,
  useReplayTrace,
  useTraceDiff,
  useTraces,
} from '../api/hooks/useTraces';
import { Button } from '../components/shared/Button';
import { Card, CardBody, CardHeader, CardTitle } from '../components/shared/Card';
import { EmptyState } from '../components/shared/EmptyState';
import { PageHeader } from '../components/shared/PageHeader';
import { TableSkeleton } from '../components/shared/Skeleton';

const CHANGE_TYPE_CLASSES: Record<string, string> = {
  ADDED: 'bg-emerald-500/5 text-emerald-300 ring-emerald-500/20',
  REMOVED: 'bg-red-500/5 text-red-300 ring-red-500/20',
  CHANGED: 'bg-amber-500/5 text-amber-300 ring-amber-500/20',
};

export function TraceReplay() {
  const { data: traces, isLoading, isError } = useTraces();
  const [selectedReplayId, setSelectedReplayId] = useState<string | undefined>(undefined);
  const { data: diff } = useTraceDiff(selectedReplayId);
  const captureMutation = useCaptureDemoTrace();
  const replayMutation = useReplayTrace();
  const deleteMutation = useDeleteTrace();

  function handleCapture() {
    captureMutation.mutate('order-service', {
      onSuccess: () => toast.success('Captured a demo trace for order-service'),
      onError: () => toast.error('Failed to capture trace'),
    });
  }

  function handleReplay(replayId: string) {
    setSelectedReplayId(replayId);
    replayMutation.mutate(
      { replayId },
      {
        onSuccess: () => toast.success('Replay complete — diff updated below'),
        onError: () => toast.error('Replay failed'),
      },
    );
  }

  function handleDelete(replayId: string) {
    deleteMutation.mutate(replayId, {
      onSuccess: () => {
        toast.success('Bundle deleted');
        if (selectedReplayId === replayId) setSelectedReplayId(undefined);
      },
      onError: () => toast.error('Failed to delete bundle'),
    });
  }

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        title="Trace Replay"
        subtitle="Capture a request trace once, replay it against any target with downstream calls mocked."
        actions={
          <Button variant="primary" onClick={handleCapture} loading={captureMutation.isPending}>
            <Plus className="size-4" />
            Capture demo trace
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Replay Bundles</CardTitle>
        </CardHeader>
        {isLoading ? (
          <TableSkeleton />
        ) : isError ? (
          <CardBody>
            <EmptyState tone="error" message="Failed to load replay bundles." />
          </CardBody>
        ) : (traces ?? []).length === 0 ? (
          <CardBody>
            <EmptyState
              icon={Replace}
              message="No trace replay bundles yet. Capture a demo trace, or embed FleetLensSpanExporter in a live service to capture real ones."
            />
          </CardBody>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead>
                <tr className="border-b border-border text-xs uppercase tracking-wide text-slate-500">
                  <th className="whitespace-nowrap px-5 py-3 font-medium">Service</th>
                  <th className="whitespace-nowrap px-5 py-3 font-medium">Trace ID</th>
                  <th className="whitespace-nowrap px-5 py-3 font-medium">Recorded</th>
                  <th className="px-5 py-3 font-medium" />
                </tr>
              </thead>
              <tbody>
                {(traces ?? []).map((trace) => (
                  <tr
                    key={trace.replayId}
                    className={clsx(
                      'border-b border-border/60 last:border-0 hover:bg-surface-hover/50',
                      selectedReplayId === trace.replayId && 'bg-brand-500/5',
                    )}
                  >
                    <td className="whitespace-nowrap px-5 py-3 text-slate-200">{trace.serviceId}</td>
                    <td className="whitespace-nowrap px-5 py-3 font-mono text-xs text-slate-400">{trace.traceId.slice(0, 12)}…</td>
                    <td className="whitespace-nowrap px-5 py-3 text-slate-500">{new Date(trace.recordedAt).toLocaleString()}</td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap justify-end gap-2">
                        <Button size="sm" onClick={() => setSelectedReplayId(trace.replayId)}>
                          View diff
                        </Button>
                        <Button
                          size="sm"
                          variant="primary"
                          loading={replayMutation.isPending && selectedReplayId === trace.replayId}
                          onClick={() => handleReplay(trace.replayId)}
                        >
                          Replay
                        </Button>
                        <Button size="sm" variant="danger" onClick={() => handleDelete(trace.replayId)}>
                          Delete
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {selectedReplayId && (
        <Card>
          <CardHeader>
            <CardTitle>Diff Viewer</CardTitle>
          </CardHeader>
          <CardBody>
            {!diff ? (
              <EmptyState message="No diff yet for this bundle. Click Replay to issue the captured request and compare." />
            ) : diff.fieldDiffs.length === 0 ? (
              <EmptyState message="No field-level differences — the replayed response matched exactly." />
            ) : (
              <ul className="flex flex-col gap-2">
                {diff.fieldDiffs.map((field) => (
                  <li
                    key={field.path}
                    className={clsx(
                      'flex flex-col gap-1 rounded-lg px-4 py-3 text-sm ring-1 ring-inset',
                      CHANGE_TYPE_CLASSES[field.changeType],
                    )}
                  >
                    <span className="font-mono text-xs">{field.path}</span>
                    <div className="flex flex-wrap gap-4 text-xs">
                      <span>expected: <code className="text-mono">{JSON.stringify(field.oldValue)}</code></span>
                      <span>actual: <code className="text-mono">{JSON.stringify(field.newValue)}</code></span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </CardBody>
        </Card>
      )}
    </div>
  );
}
