import type { HTMLAttributes, ReactNode } from 'react';
import clsx from 'clsx';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Card({ children, className, ...rest }: CardProps) {
  return (
    <div
      className={clsx(
        'rounded-xl border border-border bg-surface shadow-card',
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

export function CardHeader({ children, className, ...rest }: CardProps) {
  return (
    <div className={clsx('flex items-center justify-between gap-3 border-b border-border px-5 py-4', className)} {...rest}>
      {children}
    </div>
  );
}

export function CardTitle({ children, className }: { children: ReactNode; className?: string }) {
  return <h2 className={clsx('text-sm font-semibold text-slate-100', className)}>{children}</h2>;
}

export function CardBody({ children, className, ...rest }: CardProps) {
  return (
    <div className={clsx('p-5', className)} {...rest}>
      {children}
    </div>
  );
}
