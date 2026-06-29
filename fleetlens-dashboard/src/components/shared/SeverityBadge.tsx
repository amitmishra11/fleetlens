import clsx from 'clsx';
import { CircleAlert, Info, OctagonAlert } from 'lucide-react';
import type { Severity } from '../../api/types';

const CONFIG: Record<Severity, { classes: string; icon: typeof Info }> = {
  INFO: { classes: 'bg-sky-500/10 text-sky-400 ring-sky-500/20', icon: Info },
  WARN: { classes: 'bg-amber-500/10 text-amber-400 ring-amber-500/20', icon: CircleAlert },
  CRITICAL: { classes: 'bg-red-500/10 text-red-400 ring-red-500/20', icon: OctagonAlert },
};

export function SeverityBadge({ severity }: { severity: Severity }) {
  const { classes, icon: Icon } = CONFIG[severity];
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset',
        classes,
      )}
    >
      <Icon className="size-3" />
      {severity}
    </span>
  );
}
