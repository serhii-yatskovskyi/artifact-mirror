package org.bayaweaver.artifactproxy;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bayaweaver.artifactproxy.codeartifact.CodeartifactAuthorizationTokenProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

class ArtifactRepositoryAccess implements HttpHandler {
    private final URL artifactRepositoryUrl;
    private final AuthorizationTokenProvider tokenProvider;

    private ArtifactRepositoryAccess(URL artifactRepositoryUrl, AuthorizationTokenProvider tokenProvider) {
        this.artifactRepositoryUrl = artifactRepositoryUrl;
        this.tokenProvider = tokenProvider;
    }

    public static ArtifactRepositoryAccess codeartifact(
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
        return new ArtifactRepositoryAccess(artifactRepositoryUrl, tokenProvider);
    }

    @Override
    public void handle (HttpExchange exchange) throws IOException {
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
        final URL artifactUrl = URI.create(artifactRepositoryUrl.toString() + exchange.getRequestURI()).toURL();
        HttpURLConnection connectionToCodeArtifact = (HttpURLConnection) artifactUrl.openConnection();
        connectionToCodeArtifact.setRequestMethod(exchange.getRequestMethod());
        connectionToCodeArtifact.setRequestProperty("Authorization", "Bearer " + token);
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null && Integer.parseInt(contentLength) > 0) {
            connectionToCodeArtifact.setDoOutput(true);
            try (InputStream initiatorRequestBody = exchange.getRequestBody();
                 OutputStream codeArtifactRequestBody = connectionToCodeArtifact.getOutputStream()) {

                initiatorRequestBody.transferTo(codeArtifactRequestBody);
            } catch (IOException ignored) {
            }
        }
        long codeArtifactContentLength = connectionToCodeArtifact.getContentLengthLong();
        try (InputStream codeArtifactResponseBody = connectionToCodeArtifact.getInputStream();
             OutputStream initiatorResponseBody = exchange.getResponseBody()) {

            codeArtifactResponseBody.transferTo(initiatorResponseBody);
        } catch (IOException e) {
            codeArtifactContentLength = -1;
        }
        int codeArtifactResponseCode = connectionToCodeArtifact.getResponseCode();
        exchange.sendResponseHeaders(codeArtifactResponseCode, codeArtifactContentLength);
    }
}
