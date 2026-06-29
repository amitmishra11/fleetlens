import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../client';
import type { ConfigChange, ConfigMatrix, ConfigSnapshot, RegisterServiceRequest, ServiceDefinition } from '../types';

export function useConfigServices() {
  return useQuery({
    queryKey: ['config-services'],
    queryFn: () => api.get<ServiceDefinition[]>('/config/services').then((r) => r.data),
    refetchInterval: 60_000,
  });
}

export function useRegisterService() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: RegisterServiceRequest) =>
      api.post<ServiceDefinition>('/config/services', request).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['config-services'] });
      queryClient.invalidateQueries({ queryKey: ['config-matrix'] });
    },
  });
}

export function useUnregisterService() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (serviceId: string) => api.delete(`/config/services/${serviceId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['config-services'] });
      queryClient.invalidateQueries({ queryKey: ['config-matrix'] });
    },
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
