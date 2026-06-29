import { create } from 'zustand';
import { subHours } from 'date-fns';
import type { TimeRangePreset } from '../api/types';

interface TimeRange {
  preset: TimeRangePreset;
  from: Date;
  to: Date;
}

function rangeForPreset(preset: TimeRangePreset): TimeRange {
  const hours = preset === '1h' ? 1 : preset === '6h' ? 6 : 24;
  const to = new Date();
  return { preset, from: subHours(to, hours), to };
}

interface AppState {
  selectedServiceId: string | null;
  timeRange: TimeRange;
  setSelectedServiceId: (serviceId: string | null) => void;
  setTimeRangePreset: (preset: TimeRangePreset) => void;
}

export const useAppStore = create<AppState>((set) => ({
  selectedServiceId: null,
  timeRange: rangeForPreset('6h'),
  setSelectedServiceId: (serviceId) => set({ selectedServiceId: serviceId }),
  setTimeRangePreset: (preset) => set({ timeRange: rangeForPreset(preset) }),
}));
