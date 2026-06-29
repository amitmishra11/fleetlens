import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../client';
import type { ReplayBundle, ReplayResult, TraceBundleSummary, TraceDiff } from '../types';

export function useTraces() {
  return useQuery({
    queryKey: ['traces'],
    queryFn: () => api.get<TraceBundleSummary[]>('/traces').then((r) => r.data),
    refetchInterval: 15_000,
  });
}

export function useTrace(replayId: string | undefined) {
  return useQuery({
    queryKey: ['trace', replayId],
    queryFn: () => api.get<ReplayBundle>(`/traces/${replayId}`).then((r) => r.data),
    enabled: !!replayId,
  });
}

export function useTraceDiff(replayId: string | undefined) {
  return useQuery({
    queryKey: ['trace-diff', replayId],
    queryFn: () =>
      api
        .get<TraceDiff>(`/traces/${replayId}/diff`, { validateStatus: (s) => s === 200 || s === 204 })
        .then((r) => (r.status === 204 ? null : r.data)),
    enabled: !!replayId,
    refetchInterval: 15_000,
  });
}

/** Generates a realistic synthetic order-service trace bundle for demoing replay/diff without a live OTel agent. */
function buildDemoBundle(serviceId: string): ReplayBundle {
  const traceId = crypto.randomUUID();
  const orderId = crypto.randomUUID();
  return {
    replayId: crypto.randomUUID(),
    traceId,
    serviceId,
    rootSpan: {
      spanId: crypto.randomUUID(),
      name: 'POST /orders',
      httpMethod: 'POST',
      httpPath: '/orders',
      requestBody: { customerId: 'cust-12', amount: 42.5 },
      responseBody: { orderId, status: 'CREATED', amount: 42.5 },
      responseStatus: 201,
    },
    downstreamCalls: [
      {
        spanId: crypto.randomUUID(),
        calledService: 'inventory-service',
        httpMethod: 'GET',
        httpPath: `/inventory/${orderId}`,
        requestCapture: {},
        responseCapture: { available: true, warehouse: 'EU-1' },
      },
    ],
    recordedAt: new Date().toISOString(),
  };
}

export function useCaptureDemoTrace() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (serviceId: string) =>
      api.post<TraceBundleSummary>(`/traces/capture/${serviceId}`, buildDemoBundle(serviceId)).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['traces'] });
    },
  });
}

const DEFAULT_REPLAY_TARGET = 'http://localhost:8090';

export function useReplayTrace() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ replayId, targetBaseUrl }: { replayId: string; targetBaseUrl?: string }) =>
      api.post<ReplayResult>(`/traces/${replayId}/replay`, undefined, {
        params: { targetBaseUrl: targetBaseUrl ?? DEFAULT_REPLAY_TARGET },
      }).then((r) => r.data),
    onSuccess: (_data, { replayId }) => {
      queryClient.invalidateQueries({ queryKey: ['trace-diff', replayId] });
    },
  });
}

export function useDeleteTrace() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (replayId: string) => api.delete(`/traces/${replayId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['traces'] });
    },
  });
}
