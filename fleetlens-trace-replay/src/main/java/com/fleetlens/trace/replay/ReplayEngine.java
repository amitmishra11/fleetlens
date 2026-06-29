package com.fleetlens.trace.replay;

import com.fleetlens.common.event.TraceDiffEvent;
import com.fleetlens.common.util.JsonUtils;
import com.fleetlens.trace.capture.ReplayBundle;
import com.fleetlens.trace.capture.RootSpanInfo;
import com.fleetlens.trace.diff.TraceDiff;
import com.fleetlens.trace.diff.TraceDiffEngine;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Re-issues a captured root request with all downstream dependencies mocked
 * via WireMock, then diffs the actual response against the captured one.
 */
@Component
public class ReplayEngine {

    private final TraceDiffEngine diffEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpClient httpClient;

    public ReplayEngine(TraceDiffEngine diffEngine, ApplicationEventPublisher eventPublisher) {
        this.diffEngine = diffEngine;
        this.eventPublisher = eventPublisher;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public ReplayResult replay(ReplayBundle bundle, ReplayOptions options) {
        MockDispatcher mockDispatcher = new MockDispatcher(options.mockPort());
        mockDispatcher.start(bundle);

        try {
            HttpResponse<String> actual = issueRequest(bundle.rootSpan(), options.targetBaseUrl());

            TraceDiff diff = diffEngine.diff(bundle.rootSpan().responseBody(), actual.body());

            eventPublisher.publishEvent(new TraceDiffEvent(bundle.serviceId(), bundle.traceId(), diff.diffCount()));

            return new ReplayResult(bundle.replayId(), diff, actual.statusCode());
        } catch (Exception e) {
            throw new IllegalStateException("Replay failed for bundle " + bundle.replayId(), e);
        } finally {
            mockDispatcher.stop();
        }
    }

    private HttpResponse<String> issueRequest(RootSpanInfo rootSpan, String targetBaseUrl) throws Exception {
        String method = rootSpan.httpMethod() == null ? "GET" : rootSpan.httpMethod().toUpperCase();
        String path = rootSpan.httpPath() == null ? "/" : rootSpan.httpPath();
        URI uri = URI.create(stripTrailingSlash(targetBaseUrl) + path);

        BodyPublisher bodyPublisher = rootSpan.requestBody() != null
                ? BodyPublishers.ofString(JsonUtils.toJson(rootSpan.requestBody()))
                : BodyPublishers.noBody();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .method(method, bodyPublisher)
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String stripTrailingSlash(String baseUrl) {
        return baseUrl != null && baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
