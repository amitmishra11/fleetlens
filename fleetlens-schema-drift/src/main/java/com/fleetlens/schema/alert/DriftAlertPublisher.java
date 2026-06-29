package com.fleetlens.schema.alert;

import com.fleetlens.common.event.SchemaDriftEvent;
import com.fleetlens.schema.detector.DriftReport;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around the Spring application event bus so callers don't need to know how to
 * construct a SchemaDriftEvent from a DriftReport.
 */
@Component
public class DriftAlertPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public DriftAlertPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishIfBreaking(String topic, DriftReport report) {
        if (report.hasBreaking()) {
            eventPublisher.publishEvent(new SchemaDriftEvent(topic, topic, report.findings().size(), true));
        }
    }
}
