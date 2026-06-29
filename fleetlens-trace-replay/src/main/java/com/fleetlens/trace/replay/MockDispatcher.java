package com.fleetlens.trace.replay;

import com.fleetlens.common.util.JsonUtils;
import com.fleetlens.trace.capture.DownstreamCall;
import com.fleetlens.trace.capture.ReplayBundle;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Wraps a WireMock server lifecycle for replaying a bundle's downstream calls
 * as canned stubs.
 */
public class MockDispatcher {

    private final WireMockServer server;

    public MockDispatcher(int port) {
        this.server = new WireMockServer(WireMockConfiguration.options().port(port));
    }

    public void start(ReplayBundle bundle) {
        server.start();
        for (DownstreamCall call : bundle.downstreamCalls()) {
            stub(call);
        }
    }

    private void stub(DownstreamCall call) {
        String method = call.httpMethod() == null ? "GET" : call.httpMethod().toUpperCase();
        String body = call.responseCapture() == null ? "" : JsonUtils.toJson(call.responseCapture());

        server.stubFor(WireMock.request(method, WireMock.urlEqualTo(call.httpPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    public void stop() {
        if (server.isRunning()) {
            server.stop();
        }
    }

    public int port() {
        return server.port();
    }
}
