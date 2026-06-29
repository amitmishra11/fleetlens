import { useQuery } from '@tanstack/react-query';
import { api } from '../client';
import type { ConfigChange, ConfigMatrix, ConfigSnapshot, ServiceDefinition } from '../types';

export function useConfigServices() {
  return useQuery({
    queryKey: ['config-services'],
    queryFn: () => api.get<ServiceDefinition[]>('/config/services').then((r) => r.data),
    refetchInterval: 60_000,
  });
}

export function useConfigLatest(serviceId: string | undefined) {
  return useQuery({
    queryKey: ['config-latest', serviceId],
    queryFn: () => api.get<ConfigSnapshot>(`/config/services/${serviceId}/latest`).then((r) => r.data),
    enabled: !!serviceId,
    refetchInterval: 60_000,
  });
}

export function useConfigDiff(serviceId: string | undefined) {
  return useQuery({
    queryKey: ['config-diff', serviceId],
    queryFn: () => api.get<ConfigChange[]>(`/config/services/${serviceId}/diff`).then((r) => r.data),
    enabled: !!serviceId,
    refetchInterval: 60_000,
  });
}

export function useConfigMatrix() {
  return useQuery({
    queryKey: ['config-matrix'],
    queryFn: () => api.get<ConfigMatrix>('/config/matrix').then((r) => r.data),
    refetchInterval: 60_000,
  });
}
