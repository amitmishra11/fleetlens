import { useState, type FormEvent } from 'react';
import { toast } from 'sonner';
import { Plus, Server, ShieldCheck, Trash2 } from 'lucide-react';
import { useConfigServices, useRegisterService, useUnregisterService } from '../../api/hooks/useConfigMatrix';
import { Button } from './Button';
import { EmptyState } from './EmptyState';
import { Field, Input } from './Input';
import { Modal } from './Modal';
import { TableSkeleton } from './Skeleton';

const EMPTY_FORM = {
  id: '',
  baseUrl: '',
  env: 'local',
  jmxHost: '',
  jmxPort: '',
  kafkaConsumerGroups: '',
};

export function ServiceManager({ onClose }: { onClose: () => void }) {
  const { data: services, isLoading } = useConfigServices();
  const registerService = useRegisterService();
  const unregisterService = useUnregisterService();
  const [form, setForm] = useState(EMPTY_FORM);

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form.id.trim() || !form.baseUrl.trim()) {
      toast.error('Service ID and base URL are required');
      return;
    }
    registerService.mutate(
      {
        id: form.id.trim(),
        baseUrl: form.baseUrl.trim(),
        env: form.env.trim() || undefined,
        jmxHost: form.jmxHost.trim() || undefined,
        jmxPort: form.jmxPort.trim() ? Number(form.jmxPort.trim()) : undefined,
        kafkaConsumerGroups: form.kafkaConsumerGroups.trim()
          ? form.kafkaConsumerGroups.split(',').map((g) => g.trim()).filter(Boolean)
          : undefined,
      },
      {
        onSuccess: () => {
          toast.success(`Now monitoring ${form.id.trim()}`);
          setForm(EMPTY_FORM);
        },
        onError: () => toast.error('Failed to register service — check the ID and base URL'),
      },
    );
  }

  function handleRemove(id: string) {
    unregisterService.mutate(id, {
      onSuccess: () => toast.success(`Stopped monitoring ${id}`),
      onError: () => toast.error('Failed to remove service'),
    });
  }

  return (
    <Modal title="Manage Monitored Services" onClose={onClose} width="max-w-xl">
      <div className="flex flex-col gap-6">
        <div>
          <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500">Currently Monitored</h3>
          {isLoading ? (
            <TableSkeleton rows={2} cols={1} />
          ) : (services ?? []).length === 0 ? (
            <EmptyState icon={Server} message="No services registered yet." />
          ) : (
            <ul className="flex flex-col gap-2">
              {(services ?? []).map((svc) => (
                <li
                  key={svc.id}
                  className="flex items-center justify-between rounded-lg border border-border bg-surface-raised px-3 py-2"
                >
                  <div className="flex items-center gap-2.5">
                    <Server className="size-3.5 text-slate-500" />
                    <div>
                      <div className="text-sm font-medium text-slate-200">{svc.id}</div>
                      <div className="text-xs text-slate-500">{svc.baseUrl} · {svc.env}</div>
                    </div>
                  </div>
                  {svc.managed ? (
                    <Button size="sm" variant="danger" onClick={() => handleRemove(svc.id)} loading={unregisterService.isPending}>
                      <Trash2 className="size-3.5" />
                    </Button>
                  ) : (
                    <span className="inline-flex items-center gap-1 px-2 text-xs text-slate-600">
                      <ShieldCheck className="size-3 text-emerald-500/70" />
                      static
                    </span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        <div>
          <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500">
            Add a Locally Running Service
          </h3>
          <form onSubmit={handleSubmit} className="flex flex-col gap-3">
            <div className="grid grid-cols-2 gap-3">
              <Field label="Service ID *">
                <Input
                  placeholder="my-local-service"
                  value={form.id}
                  onChange={(e) => update('id', e.target.value)}
                />
              </Field>
              <Field label="Environment">
                <Input placeholder="local" value={form.env} onChange={(e) => update('env', e.target.value)} />
              </Field>
            </div>
            <Field label="Base URL *" hint="Where its Spring Boot Actuator is reachable from this machine">
              <Input
                placeholder="http://localhost:8081"
                value={form.baseUrl}
                onChange={(e) => update('baseUrl', e.target.value)}
              />
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="JMX Host" hint="Optional — enables heap profiling">
                <Input placeholder="localhost" value={form.jmxHost} onChange={(e) => update('jmxHost', e.target.value)} />
              </Field>
              <Field label="JMX Port">
                <Input
                  placeholder="9010"
                  inputMode="numeric"
                  value={form.jmxPort}
                  onChange={(e) => update('jmxPort', e.target.value)}
                />
              </Field>
            </div>
            <Field label="Kafka Consumer Groups" hint="Optional, comma-separated — enables lag tracking">
              <Input
                placeholder="my-service-group, my-other-group"
                value={form.kafkaConsumerGroups}
                onChange={(e) => update('kafkaConsumerGroups', e.target.value)}
              />
            </Field>
            <Button type="submit" variant="primary" loading={registerService.isPending} className="mt-1 self-start">
              <Plus className="size-4" />
              Register Service
            </Button>
          </form>
        </div>
      </div>
    </Modal>
  );
}
