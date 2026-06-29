import { ChevronDown } from 'lucide-react';
import { useConfigServices } from '../../api/hooks/useConfigMatrix';
import { useAppStore } from '../../store/useAppStore';

export function ServiceSelector() {
  const { data: services } = useConfigServices();
  const selectedServiceId = useAppStore((s) => s.selectedServiceId);
  const setSelectedServiceId = useAppStore((s) => s.setSelectedServiceId);

  const options = services ?? [];

  return (
    <div className="relative">
      <select
        className="appearance-none rounded-lg border border-border bg-surface-raised px-3 py-2 pr-8 text-sm text-slate-200 outline-none transition-colors hover:border-slate-600 focus:border-brand-500"
        value={selectedServiceId ?? ''}
        onChange={(e) => setSelectedServiceId(e.target.value || null)}
      >
        <option value="">All services</option>
        {options.map((svc) => (
          <option key={svc.id} value={svc.id}>
            {svc.id}
          </option>
        ))}
      </select>
      <ChevronDown className="pointer-events-none absolute right-2.5 top-1/2 size-3.5 -translate-y-1/2 text-slate-500" />
    </div>
  );
}
