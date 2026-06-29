import type { ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';
import { Inbox, TriangleAlert } from 'lucide-react';

interface EmptyStateProps {
  message: string;
  icon?: LucideIcon;
  tone?: 'empty' | 'error';
  action?: ReactNode;
}

export function EmptyState({ message, icon, tone = 'empty', action }: EmptyStateProps) {
  const Icon = icon ?? (tone === 'error' ? TriangleAlert : Inbox);
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border bg-canvas/40 px-6 py-12 text-center">
      <Icon className={tone === 'error' ? 'size-7 text-red-500/70' : 'size-7 text-slate-600'} />
      <p className="max-w-sm text-sm text-slate-500">{message}</p>
      {action}
    </div>
  );
}
