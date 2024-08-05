package org.bayaweaver.codeartifactproxy;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

class GeneralHttpHandler implements HttpHandler {
    private final URL codeArtifactDomainUrl;
    private final AuthorizationTokenProvider tokenProvider;

    GeneralHttpHandler(
            String codeArtifactDomain,
            String codeArtifactDomainOwner,
            String codeArtifactRegion,
            String accessKeyId,
            String secretAccessKey) {

        try {
            // "https://bnc-obs-435280699592.d.codeartifact.us-east-2.amazonaws.com";
            this.codeArtifactDomainUrl = URI
                    .create("https://" + codeArtifactDomain + "-" + codeArtifactDomainOwner
                            + ".d.codeartifact." + codeArtifactRegion + ".amazonaws.com")
                    .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("'aws.codeartifact.domain', 'aws.codeartifact.domain-owner'"
                    + "or 'aws.codeartifact.region' has URL-improper character.");
        }
        this.tokenProvider = new AuthorizationTokenProvider(
                codeArtifactDomain,
                codeArtifactDomainOwner,
                codeArtifactRegion,
                accessKeyId,
                secretAccessKey);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final String token;
        try {
            token = tokenProvider.fetchToken();
        } catch (TokenNotProvidedException e) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage().length());
            try (OutputStream initiatorResponseBody = exchange.getResponseBody()) {
                new ByteArrayInputStream(e.getMessage().getBytes()).transferTo(initiatorResponseBody);
            }
            return;
        }
        final URL artifactUrl = URI.create(codeArtifactDomainUrl.toString() + exchange.getRequestURI()).toURL();
        HttpURLConnection connectionToCodeArtifact = (HttpURLConnection) artifactUrl.openConnection();
        connectionToCodeArtifact.setRequestMethod(exchange.getRequestMethod());
        connectionToCodeArtifact.setRequestProperty("Authorization", "Bearer " + token);
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null && Integer.parseInt(contentLength) > 0) {
            connectionToCodeArtifact.setDoOutput(true);
            try (InputStream initiatorRequestBody = exchange.getRequestBody();
                 OutputStream codeArtifactRequestBody = connectionToCodeArtifact.getOutputStream()) {

                initiatorRequestBody.transferTo(codeArtifactRequestBody);
            }
        }
        int codeArtifactResponseCode = connectionToCodeArtifact.getResponseCode();
        exchange.sendResponseHeaders(codeArtifactResponseCode, connectionToCodeArtifact.getContentLengthLong());
        InputStream codeArtifactResponseBody = connectionToCodeArtifact.getInputStream();
        try (OutputStream initiatorResponseBody = exchange.getResponseBody()) {
            codeArtifactResponseBody.transferTo(initiatorResponseBody);
        }
    }
}
