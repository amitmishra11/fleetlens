package com.fleetlens.schema.api;

import com.fleetlens.common.util.TimeUtils;
import com.fleetlens.schema.detector.DriftAnalyser;
import com.fleetlens.schema.detector.DriftReport;
import com.fleetlens.schema.store.SchemaVersion;
import com.fleetlens.schema.store.SchemaVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/schema")
public class SchemaDriftController {

    private final SchemaVersionRepository versionRepo;
    private final DriftAnalyser analyser;

    public SchemaDriftController(SchemaVersionRepository versionRepo, DriftAnalyser analyser) {
        this.versionRepo = versionRepo;
        this.analyser = analyser;
    }

    @GetMapping("/topics")
    public List<TopicSummary> listTopics() {
        return versionRepo.findDistinctTopics().stream()
                .map(topic -> {
                    List<SchemaVersion> versions = versionRepo.findByTopicOrderByVersionAsc(topic);
                    int latestVersion = versions.isEmpty() ? 0 : versions.get(versions.size() - 1).getVersion();
                    boolean hasBreakingHistory = versions.stream().anyMatch(SchemaVersion::isBreaking);
                    return new TopicSummary(topic, latestVersion, hasBreakingHistory);
                })
                .toList();
    }

    @GetMapping("/topics/{topic}/versions")
    public List<SchemaVersion> versionHistory(@PathVariable String topic) {
        return versionRepo.findByTopicOrderByVersionAsc(topic);
    }

    @GetMapping("/topics/{topic}/diff")
    public ResponseEntity<DriftReportResponse> diff(@PathVariable String topic,
                                                      @RequestParam int from,
                                                      @RequestParam int to) {
        SchemaVersion fromVersion = versionRepo.findByTopicAndVersion(topic, from)
            .orElseThrow(() -> new NoSuchElementException("No version " + from + " for topic " + topic));
        SchemaVersion toVersionEntity = versionRepo.findByTopicAndVersion(topic, to)
            .orElseThrow(() -> new NoSuchElementException("No version " + to + " for topic " + topic));

        DriftReport report = analyser.analyse(fromVersion.toInferred(), toVersionEntity.toInferred());
        return ResponseEntity.ok(DriftReportResponse.from(report));
    }

    @GetMapping("/breaking")
    public List<SchemaVersion> breakingSince(@RequestParam String since) {
        Instant sinceInstant = TimeUtils.parseIsoOrNow(since);
        return versionRepo.findByBreakingTrueAndDetectedAtAfter(sinceInstant);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
