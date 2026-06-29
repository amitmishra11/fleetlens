import type { LucideIcon } from 'lucide-react';
import clsx from 'clsx';

interface StatCardProps {
  label: string;
  value: string | number;
  icon: LucideIcon;
  tone?: 'default' | 'critical' | 'warn' | 'good';
}

const TONE_CLASSES: Record<NonNullable<StatCardProps['tone']>, string> = {
  default: 'text-brand-400 bg-brand-500/10',
  critical: 'text-red-400 bg-red-500/10',
  warn: 'text-amber-400 bg-amber-500/10',
  good: 'text-emerald-400 bg-emerald-500/10',
};

export function StatCard({ label, value, icon: Icon, tone = 'default' }: StatCardProps) {
  return (
    <div className="flex items-center gap-4 rounded-xl border border-border bg-surface p-4 shadow-card">
      <div className={clsx('flex size-10 shrink-0 items-center justify-center rounded-lg', TONE_CLASSES[tone])}>
        <Icon className="size-5" />
      </div>
      <div>
        <div className="text-2xl font-semibold leading-none text-slate-50">{value}</div>
        <div className="mt-1 text-xs font-medium text-slate-500">{label}</div>
      </div>
    </div>
  );
}
