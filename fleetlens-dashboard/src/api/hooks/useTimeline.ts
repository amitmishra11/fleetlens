import { useQuery } from '@tanstack/react-query';
import { api } from '../client';
import type { ServiceTimeline, TimelineEntry } from '../types';

interface TimelineRange {
  from?: string;
  to?: string;
}

export function useServiceTimeline(serviceId: string | undefined, range?: TimelineRange) {
  return useQuery({
    queryKey: ['timeline', serviceId, range],
    queryFn: () =>
      api.get<ServiceTimeline>(`/timeline/${serviceId}`, { params: range }).then((r) => r.data),
    enabled: !!serviceId,
    refetchInterval: 30_000,
  });
}

export function useGlobalTimeline(range?: TimelineRange) {
  return useQuery({
    queryKey: ['timeline-global', range],
    queryFn: () => api.get<TimelineEntry[]>('/timeline/global', { params: range }).then((r) => r.data),
    refetchInterval: 30_000,
  });
}
