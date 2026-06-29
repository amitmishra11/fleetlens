import clsx from 'clsx';
import type { TimeRangePreset } from '../../api/types';
import { useAppStore } from '../../store/useAppStore';

const PRESETS: TimeRangePreset[] = ['1h', '6h', '24h'];

export function TimeRangePicker() {
  const preset = useAppStore((s) => s.timeRange.preset);
  const setTimeRangePreset = useAppStore((s) => s.setTimeRangePreset);

  return (
    <div className="flex gap-0.5 rounded-lg border border-border bg-surface-raised p-1">
      {PRESETS.map((p) => (
        <button
          key={p}
          type="button"
          onClick={() => setTimeRangePreset(p)}
          className={clsx(
            'rounded-md px-3 py-1 text-xs font-medium transition-colors',
            preset === p ? 'bg-brand-600 text-white' : 'text-slate-400 hover:text-slate-200',
          )}
        >
          {p}
        </button>
      ))}
    </div>
  );
}
