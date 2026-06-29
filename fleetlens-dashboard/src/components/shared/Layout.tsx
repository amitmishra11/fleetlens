import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import {
  Activity,
  GitCompareArrows,
  LayoutDashboard,
  Radar,
  Replace,
  Server,
  SlidersHorizontal,
} from 'lucide-react';
import clsx from 'clsx';
import { ServiceManager } from './ServiceManager';

const LINKS = [
  { to: '/', label: 'Overview', icon: LayoutDashboard },
  { to: '/schema', label: 'Schema Drift', icon: GitCompareArrows },
  { to: '/traces', label: 'Trace Replay', icon: Replace },
  { to: '/memory', label: 'Memory Profiler', icon: Activity },
  { to: '/config', label: 'Config Audit', icon: SlidersHorizontal },
];

export function Layout() {
  const [managingServices, setManagingServices] = useState(false);

  return (
    <div className="flex min-h-screen bg-canvas">
      <aside className="flex w-60 flex-col border-r border-border bg-surface">
        <div className="flex items-center gap-2 px-5 py-5">
          <div className="flex size-8 items-center justify-center rounded-lg bg-brand-600">
            <Radar className="size-4 text-white" />
          </div>
          <span className="text-base font-semibold text-slate-50">FleetLens</span>
        </div>

        <nav className="flex flex-1 flex-col gap-1 px-3">
          {LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/'}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-brand-500/10 text-brand-300'
                    : 'text-slate-400 hover:bg-surface-hover hover:text-slate-200',
                )
              }
            >
              <link.icon className="size-4" />
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="flex flex-col gap-3 border-t border-border px-3 py-4">
          <button
            type="button"
            onClick={() => setManagingServices(true)}
            className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium text-slate-400 transition-colors hover:bg-surface-hover hover:text-slate-200"
          >
            <Server className="size-4" />
            Manage Services
          </button>
          <div className="flex items-center gap-1.5 px-3 text-xs text-slate-600">
            <span className="relative flex size-2">
              <span className="absolute inline-flex size-full animate-ping rounded-full bg-emerald-400 opacity-60" />
              <span className="relative inline-flex size-2 rounded-full bg-emerald-400" />
            </span>
            Live
          </div>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-7xl px-8 py-8">
          <Outlet />
        </div>
      </main>

      {managingServices && <ServiceManager onClose={() => setManagingServices(false)} />}
    </div>
  );
}
