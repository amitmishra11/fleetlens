import { describe, expect, it } from 'vitest';
import type { MemorySnapshot } from '../types';
import { mergeSamples } from './mergeSamples';

function heapSample(sampledAt: string, heapUsedMb: number, heapMaxMb = 1024): MemorySnapshot {
  return {
    id: `heap-${sampledAt}`,
    serviceId: 'order-service',
    heapUsedMb,
    heapMaxMb,
    gcPauseMs: null,
    kafkaLag: null,
    consumerGroup: null,
    sampledAt,
  };
}

function lagSample(sampledAt: string, kafkaLag: number): MemorySnapshot {
  return {
    id: `lag-${sampledAt}`,
    serviceId: 'order-service',
    heapUsedMb: null,
    heapMaxMb: null,
    gcPauseMs: null,
    kafkaLag,
    consumerGroup: 'order-service-group',
    sampledAt,
  };
}

describe('mergeSamples', () => {
  it('merges a heap sample and a lag sample that share a timestamp into one point', () => {
    const points = mergeSamples(
      [heapSample('2026-01-01T00:00:00Z', 30)],
      [lagSample('2026-01-01T00:00:00Z', 100)],
    );

    expect(points).toEqual([
      { sampledAt: '2026-01-01T00:00:00Z', heapUsedMb: 30, heapMaxMb: 1024, kafkaLag: 100 },
    ]);
  });

  it('sorts merged points chronologically regardless of input order', () => {
    const points = mergeSamples(
      [heapSample('2026-01-01T00:00:10Z', 40), heapSample('2026-01-01T00:00:00Z', 30)],
      [],
    );

    expect(points.map((p) => p.sampledAt)).toEqual([
      '2026-01-01T00:00:00Z',
      '2026-01-01T00:00:10Z',
    ]);
  });

  it('carries the last known value forward across independently-sampled series', () => {
    const points = mergeSamples(
      [heapSample('2026-01-01T00:00:00Z', 30), heapSample('2026-01-01T00:00:20Z', 35)],
      [lagSample('2026-01-01T00:00:10Z', 50)],
    );

    expect(points).toEqual([
      { sampledAt: '2026-01-01T00:00:00Z', heapUsedMb: 30, heapMaxMb: 1024, kafkaLag: null },
      { sampledAt: '2026-01-01T00:00:10Z', heapUsedMb: 30, heapMaxMb: 1024, kafkaLag: 50 },
      { sampledAt: '2026-01-01T00:00:20Z', heapUsedMb: 35, heapMaxMb: 1024, kafkaLag: 50 },
    ]);
  });

  it('leaves leading points null when no sample has arrived yet for that series', () => {
    const points = mergeSamples([], [lagSample('2026-01-01T00:00:00Z', 10)]);

    expect(points[0]).toEqual({
      sampledAt: '2026-01-01T00:00:00Z',
      heapUsedMb: null,
      heapMaxMb: null,
      kafkaLag: 10,
    });
  });

  it('returns an empty array when both series are empty', () => {
    expect(mergeSamples([], [])).toEqual([]);
  });
});
