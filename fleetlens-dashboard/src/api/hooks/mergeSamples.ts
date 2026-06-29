import type { MemorySnapshot, MergedMemoryPoint } from '../types';

export function mergeSamples(heapSamples: MemorySnapshot[], lagSamples: MemorySnapshot[]): MergedMemoryPoint[] {
  const byTime = new Map<string, MergedMemoryPoint>();

  for (const s of heapSamples) {
    byTime.set(s.sampledAt, {
      sampledAt: s.sampledAt,
      heapUsedMb: s.heapUsedMb,
      heapMaxMb: s.heapMaxMb,
      kafkaLag: null,
    });
  }
  for (const s of lagSamples) {
    const existing = byTime.get(s.sampledAt);
    if (existing) {
      existing.kafkaLag = s.kafkaLag;
    } else {
      byTime.set(s.sampledAt, {
        sampledAt: s.sampledAt,
        heapUsedMb: null,
        heapMaxMb: null,
        kafkaLag: s.kafkaLag,
      });
    }
  }

  const points = Array.from(byTime.values()).sort(
    (a, b) => new Date(a.sampledAt).getTime() - new Date(b.sampledAt).getTime(),
  );

  // Carry the last known value forward so the lines stay continuous even though
  // heap and lag are sampled by two independent pollers on slightly offset clocks.
  let lastHeapUsed: number | null = null;
  let lastHeapMax: number | null = null;
  let lastLag: number | null = null;
  for (const p of points) {
    if (p.heapUsedMb !== null) lastHeapUsed = p.heapUsedMb;
    else p.heapUsedMb = lastHeapUsed;

    if (p.heapMaxMb !== null) lastHeapMax = p.heapMaxMb;
    else p.heapMaxMb = lastHeapMax;

    if (p.kafkaLag !== null) lastLag = p.kafkaLag;
    else p.kafkaLag = lastLag;
  }

  return points;
}
