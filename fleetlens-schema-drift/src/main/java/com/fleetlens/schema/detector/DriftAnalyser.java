package com.fleetlens.schema.detector;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares two inferred schema versions of the same topic and reports drift findings.
 *
 * Breaking-change rule for TYPE_CHANGED: this build treats ANY observed type change as
 * breaking. The plan calls out "widening" changes (e.g. INT -> LONG) as potentially safe,
 * but reliably classifying widening vs narrowing across Jackson's JsonNodeType set (which
 * does not distinguish numeric sub-types) is out of scope here — we choose the conservative,
 * simpler rule and document it rather than risk silently missing a real break.
 */
@Component
public class DriftAnalyser {

    public DriftReport analyse(InferredSchema prev, InferredSchema next) {
        List<DriftFinding> findings = new ArrayList<>();

        Set<String> removed = new HashSet<>(prev.fieldNames());
        removed.removeAll(next.fieldNames());
        removed.forEach(f -> findings.add(new DriftFinding(f, FindingType.FIELD_REMOVED, true)));

        Set<String> added = new HashSet<>(next.fieldNames());
        added.removeAll(prev.fieldNames());
        added.forEach(f -> findings.add(new DriftFinding(f, FindingType.FIELD_ADDED, false)));

        prev.fieldNames().stream()
            .filter(next.fieldNames()::contains)
            .filter(f -> !prev.typeOf(f).equals(next.typeOf(f)))
            .forEach(f -> findings.add(new DriftFinding(f, FindingType.TYPE_CHANGED, true)));

        boolean hasBreaking = findings.stream().anyMatch(DriftFinding::breaking);
        return new DriftReport(prev.topic(), prev.version(), next.version(), findings, hasBreaking);
    }
}
