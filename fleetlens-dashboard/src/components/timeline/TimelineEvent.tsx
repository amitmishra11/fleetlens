import { format } from 'date-fns';
import { Link } from 'react-router-dom';
import type { TimelineEntry } from '../../api/types';
import { ModuleBadge } from '../shared/ModuleBadge';
import { SeverityBadge } from '../shared/SeverityBadge';

const DOT_CLASSES: Record<TimelineEntry['severity'], string> = {
  INFO: 'bg-sky-400',
  WARN: 'bg-amber-400',
  CRITICAL: 'bg-red-400',
};

export function TimelineEvent({ entry, showService = false }: { entry: TimelineEntry; showService?: boolean }) {
  return (
    <li className="relative pl-6">
      <span className={`absolute left-0 top-1.5 size-2.5 rounded-full ring-4 ring-canvas ${DOT_CLASSES[entry.severity]}`} />
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs text-slate-500">{format(new Date(entry.occurredAt), 'MMM d, HH:mm:ss')}</span>
        <ModuleBadge module={entry.module} />
        <SeverityBadge severity={entry.severity} />
        {showService && (
          <Link to={`/services/${entry.serviceId}`} className="text-xs font-medium text-brand-400 hover:underline">
            {entry.serviceId}
          </Link>
        )}
      </div>
      <p className="mt-1.5 text-sm text-slate-300">{entry.summary}</p>
    </li>
  );
}
