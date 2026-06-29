import type { TimelineEntry } from '../../api/types';
import { EmptyState } from '../shared/EmptyState';
import { TimelineEvent } from './TimelineEvent';

export function IncidentTimeline({
  entries,
  showService = false,
}: {
  entries: TimelineEntry[] | undefined;
  showService?: boolean;
}) {
  const items = entries ?? [];

  if (items.length === 0) {
    return <EmptyState message="No timeline events in this range yet. They'll appear here as soon as a module detects something." />;
  }

  return (
    <ul className="flex flex-col gap-5 border-l border-border pl-3">
      {[...items].reverse().map((entry, idx) => (
        <TimelineEvent key={`${entry.occurredAt}-${idx}`} entry={entry} showService={showService} />
      ))}
    </ul>
  );
}
