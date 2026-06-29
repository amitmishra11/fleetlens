import { useQuery } from '@tanstack/react-query';
import { api } from '../client';
import type { SchemaDiff, SchemaTopic, SchemaVersion } from '../types';

export function useSchemaTopics() {
  return useQuery({
    queryKey: ['schema-topics'],
    queryFn: () => api.get<SchemaTopic[]>('/schema/topics').then((r) => r.data),
    refetchInterval: 30_000,
  });
}

export function useSchemaVersions(topic: string | undefined) {
  return useQuery({
    queryKey: ['schema-versions', topic],
    queryFn: () => api.get<SchemaVersion[]>(`/schema/topics/${topic}/versions`).then((r) => r.data),
    enabled: !!topic,
    refetchInterval: 30_000,
  });
}

export function useSchemaDiff(topic: string | undefined, from?: number, to?: number) {
  return useQuery({
    queryKey: ['schema-diff', topic, from, to],
    queryFn: () =>
      api
        .get<SchemaDiff>(`/schema/topics/${topic}/diff`, { params: { from, to } })
        .then((r) => r.data),
    enabled: !!topic && from !== undefined && to !== undefined && from !== to,
    refetchInterval: 30_000,
  });
}

export function useBreakingChanges(since?: string) {
  return useQuery({
    queryKey: ['schema-breaking', since],
    queryFn: () => api.get<SchemaVersion[]>('/schema/breaking', { params: { since } }).then((r) => r.data),
    refetchInterval: 30_000,
  });
}
