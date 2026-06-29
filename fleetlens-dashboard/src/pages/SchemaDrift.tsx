import { useState } from 'react';
import clsx from 'clsx';
import { GitCompareArrows, ShieldAlert, ShieldCheck } from 'lucide-react';
import { useSchemaDiff, useSchemaTopics, useSchemaVersions } from '../api/hooks/useSchemaTopics';
import { SchemaVersionChart } from '../components/charts/SchemaVersionChart';
import { Card, CardBody, CardHeader, CardTitle } from '../components/shared/Card';
import { EmptyState } from '../components/shared/EmptyState';
import { PageHeader } from '../components/shared/PageHeader';
import { TableSkeleton } from '../components/shared/Skeleton';

const FINDING_LABEL: Record<string, string> = {
  FIELD_REMOVED: 'Field removed',
  FIELD_ADDED: 'Field added',
  TYPE_CHANGED: 'Type changed',
};

export function SchemaDrift() {
  const { data: topics, isLoading, isError } = useSchemaTopics();
  const [selectedTopic, setSelectedTopic] = useState<string | undefined>(undefined);
  const { data: versions } = useSchemaVersions(selectedTopic);
  const latestTwo = (versions ?? []).slice(-2);
  const fromVersion = latestTwo[0]?.version;
  const toVersion = latestTwo[1]?.version;
  const { data: diff } = useSchemaDiff(selectedTopic, fromVersion, toVersion);

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title="Schema Drift" subtitle="Inferred Kafka message schemas, versioned, with breaking-change detection." />

      <Card>
        <CardHeader>
          <CardTitle>Monitored Topics</CardTitle>
        </CardHeader>
        {isLoading ? (
          <TableSkeleton />
        ) : isError ? (
          <CardBody>
            <EmptyState tone="error" message="Failed to load schema topics." />
          </CardBody>
        ) : (topics ?? []).length === 0 ? (
          <CardBody>
            <EmptyState
              icon={GitCompareArrows}
              message="No topics monitored yet. The sampler scans Kafka every 2 minutes and versions any topic it finds."
            />
          </CardBody>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-5 py-3 font-medium">Topic</th>
                  <th className="px-5 py-3 font-medium">Latest version</th>
                  <th className="px-5 py-3 font-medium">Breaking history</th>
                </tr>
              </thead>
              <tbody>
                {(topics ?? []).map((topic) => (
                  <tr
                    key={topic.topic}
                    onClick={() => setSelectedTopic(topic.topic)}
                    className={clsx(
                      'cursor-pointer border-b border-border/60 last:border-0 hover:bg-surface-hover/50',
                      selectedTopic === topic.topic && 'bg-brand-500/5',
                    )}
                  >
                    <td className="whitespace-nowrap px-5 py-3 font-mono text-xs text-slate-200">{topic.topic}</td>
                    <td className="whitespace-nowrap px-5 py-3 text-slate-400">v{topic.latestVersion}</td>
                    <td className="whitespace-nowrap px-5 py-3">
                      {topic.hasBreakingHistory ? (
                        <span className="inline-flex items-center gap-1 text-red-400">
                          <ShieldAlert className="size-3.5" /> Yes
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-emerald-400">
                          <ShieldCheck className="size-3.5" /> No
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {selectedTopic && (
        <Card>
          <CardHeader>
            <CardTitle>Version History — {selectedTopic}</CardTitle>
          </CardHeader>
          <CardBody>
            <SchemaVersionChart versions={versions} />
          </CardBody>
        </Card>
      )}

      {selectedTopic && diff && (
        <Card>
          <CardHeader>
            <CardTitle>
              Diff v{diff.fromVersion} → v{diff.toVersion}
            </CardTitle>
          </CardHeader>
          <CardBody>
            {diff.findings.length === 0 ? (
              <EmptyState message="No differences between these two versions." />
            ) : (
              <ul className="flex flex-col gap-2">
                {diff.findings.map((finding) => (
                  <li
                    key={`${finding.field}-${finding.findingType}`}
                    className={clsx(
                      'flex items-center justify-between rounded-lg px-4 py-3 text-sm ring-1 ring-inset',
                      finding.isBreaking
                        ? 'bg-red-500/5 text-red-300 ring-red-500/20'
                        : 'bg-emerald-500/5 text-emerald-300 ring-emerald-500/20',
                    )}
                  >
                    <span className="font-mono text-xs">{finding.field}</span>
                    <span className="text-xs font-medium">
                      {FINDING_LABEL[finding.findingType] ?? finding.findingType}
                      {finding.isBreaking ? ' · breaking' : ' · non-breaking'}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </CardBody>
        </Card>
      )}
    </div>
  );
}
