import type { InputHTMLAttributes, ReactNode } from 'react';
import clsx from 'clsx';

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-medium text-slate-400">{label}</span>
      {children}
      {hint && <span className="text-xs text-slate-600">{hint}</span>}
    </label>
  );
}

export function Input({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={clsx(
        'rounded-lg border border-border bg-surface-raised px-3 py-2 text-sm text-slate-200 outline-none transition-colors placeholder:text-slate-600 hover:border-slate-600 focus:border-brand-500',
        className,
      )}
      {...rest}
    />
  );
}
