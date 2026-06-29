import { useQuery } from '@tanstack/react-query';
import { api } from '../client';
import type { CorrelationEvent, HeapDump, MemorySnapshot, MemoryTimelineResponse } from '../types';
import { mergeSamples } from './mergeSamples';

export function useMemoryTimeline(serviceId: string | undefined) {
  return useQuery({
    queryKey: ['memory-timeline', serviceId],
    queryFn: () =>
      api.get<MemoryTimelineResponse>(`/memory/services/${serviceId}/timeline`).then((r) => r.data),
    enabled: !!serviceId,
    refetchInterval: 30_000,
    select: (data) => ({
      ...data,
      points: mergeSamples(data.heapSamples, data.lagSamples),
    }),
  });
}

export function useMemoryLatest(serviceId: string | undefined) {
  return useQuery({
    queryKey: ['memory-latest', serviceId],
    queryFn: () =>
      api
        .get<{ snapshot: MemorySnapshot | Record<string, never> }>(`/memory/services/${serviceId}/latest`)
        .then((r) => r.data.snapshot as MemorySnapshot | null),
    enabled: !!serviceId,
    refetchInterval: 30_000,
  });
}

export function useMemoryCorrelations(since?: string) {
  return useQuery({
    queryKey: ['memory-correlations', since],
    queryFn: () =>
      api.get<CorrelationEvent[]>('/memory/correlations', { params: { since } }).then((r) => r.data),
    refetchInterval: 30_000,
  });
}

export function useHeapDumps() {
  return useQuery({
    queryKey: ['heap-dumps'],
    queryFn: () => api.get<HeapDump[]>('/memory/heapdumps').then((r) => r.data),
    refetchInterval: 30_000,
  });
}
