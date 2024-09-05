package org.bayaweaver.artifactgateway;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bayaweaver.artifactgateway.codeartifact.CodeartifactAuthorizationTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

class ArtifactRepositoryRequestHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactRepositoryRequestHandler.class);

    private final String artifactRepositoryUrl;
    private final AuthorizationTokenProvider tokenProvider;

    private ArtifactRepositoryRequestHandler(String artifactRepositoryUrl, AuthorizationTokenProvider tokenProvider) {
        this.artifactRepositoryUrl = artifactRepositoryUrl;
        this.tokenProvider = tokenProvider;
    }

    public static HttpHandler codeartifact(
            String codeartifactDomain,
            String codeartifactDomainOwner,
            String codeartifactRegion,
            String accessKeyId,
            String secretAccessKey) {

        URL artifactRepositoryUrl;
        try {
            // "https://bnc-obs-435280699592.d.codeartifact.us-east-2.amazonaws.com";
            artifactRepositoryUrl = URI
                    .create("https://" + codeartifactDomain + "-" + codeartifactDomainOwner
                            + ".d.codeartifact." + codeartifactRegion + ".amazonaws.com")
                    .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("'aws.codeartifact.domain', 'aws.codeartifact.domain-owner'"
                    + "or 'aws.codeartifact.region' has URL-improper character.");
        }
        AuthorizationTokenProvider tokenProvider = new CodeartifactAuthorizationTokenProvider(
                codeartifactDomain,
                codeartifactDomainOwner,
                codeartifactRegion,
                accessKeyId,
                secretAccessKey);
        return new ArtifactRepositoryRequestHandler(artifactRepositoryUrl.toString(), tokenProvider);
    }

    @Override
    public void handle (HttpExchange exchange) {
        try (exchange) {
            final String token;
            try {
                token = tokenProvider.fetchToken();
            } catch (TokenNotFetchedException e) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage().length());
                try (OutputStream initiatorResponseBody = exchange.getResponseBody()) {
                    new ByteArrayInputStream(e.getMessage().getBytes()).transferTo(initiatorResponseBody);
                }
                return;
            }
            final URL artifactUrl = URI.create(artifactRepositoryUrl + exchange.getRequestURI()).toURL();
            HttpURLConnection artifactConnection = (HttpURLConnection) artifactUrl.openConnection();
            artifactConnection.setRequestMethod(exchange.getRequestMethod());
            exchange.getRequestHeaders().forEach((headerName, headerValues) -> {
                if (!headerName.equalsIgnoreCase("Host")) {
                    for (String headerValue : headerValues) {
                        artifactConnection.addRequestProperty(headerName, headerValue);
                    }
                }
            });
            artifactConnection.setRequestProperty("Authorization", "Bearer " + token);
            String contentLengthStr = exchange.getRequestHeaders().getFirst("Content-Length");
            long contentLength = (contentLengthStr != null) ? Long.parseLong(contentLengthStr) : 0;
            if (contentLength > 0) {
                artifactConnection.setDoOutput(true);
                exchange.getRequestBody().transferTo(artifactConnection.getOutputStream());
            }
            long artifactContentLength = artifactConnection.getContentLengthLong();
            artifactContentLength = (artifactContentLength == -1) ? 0 : artifactContentLength;
            exchange.sendResponseHeaders(
                    artifactConnection.getResponseCode(),
                    artifactContentLength < 0 ? 0 : artifactContentLength);
            if (artifactContentLength > 0) {
                artifactConnection.getInputStream().transferTo(exchange.getResponseBody());
            }
        } catch (IOException e) {
            logger.error("{} {}: {}", exchange.getRequestMethod(), exchange.getRequestURI().toString(), e.getMessage());
        } catch (Exception e) {
            logger.atError()
                    .setCause(e)
                    .log("{} {}: Unexpected error.", exchange.getRequestMethod(), exchange.getRequestURI().toString());
        }
    }
}
