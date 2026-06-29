import clsx from 'clsx';
import { Activity, GitCompareArrows, Replace, SlidersHorizontal } from 'lucide-react';
import type { ModuleType } from '../../api/types';

const CONFIG: Record<ModuleType, { label: string; icon: typeof Activity; classes: string }> = {
  SCHEMA_DRIFT: { label: 'Schema', icon: GitCompareArrows, classes: 'text-violet-400 bg-violet-500/10' },
  TRACE_REPLAY: { label: 'Trace', icon: Replace, classes: 'text-cyan-400 bg-cyan-500/10' },
  MEMORY: { label: 'Memory', icon: Activity, classes: 'text-orange-400 bg-orange-500/10' },
  CONFIG: { label: 'Config', icon: SlidersHorizontal, classes: 'text-emerald-400 bg-emerald-500/10' },
};

export function ModuleBadge({ module }: { module: ModuleType }) {
  const { label, icon: Icon, classes } = CONFIG[module];
  return (
    <span className={clsx('inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium', classes)}>
      <Icon className="size-3" />
      {label}
    </span>
  );
}
