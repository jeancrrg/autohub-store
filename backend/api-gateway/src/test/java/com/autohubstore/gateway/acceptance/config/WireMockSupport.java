package com.autohubstore.gateway.acceptance.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public final class WireMockSupport {

    private static final WireMockServer SERVER = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        SERVER.start();
    }

    private WireMockSupport() {
    }

    public static WireMockServer server() {
        return SERVER;
    }

    public static int port() {
        return SERVER.port();
    }

}
