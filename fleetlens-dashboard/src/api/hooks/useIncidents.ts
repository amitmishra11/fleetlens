import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../client';
import type { Incident, IncidentPage, IncidentQueryParams } from '../types';

export function useIncidents(params?: IncidentQueryParams) {
  return useQuery({
    queryKey: ['incidents', params],
    queryFn: () => api.get<IncidentPage>('/incidents', { params }).then((r) => r.data),
    refetchInterval: 15_000,
  });
}

export function useIncident(incidentId: string | undefined) {
  return useQuery({
    queryKey: ['incident', incidentId],
    queryFn: () => api.get<Incident>(`/incidents/${incidentId}`).then((r) => r.data),
    enabled: !!incidentId,
    refetchInterval: 15_000,
  });
}

export function useResolveIncident() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (incidentId: string) => api.patch(`/incidents/${incidentId}/resolve`).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] });
    },
  });
}
