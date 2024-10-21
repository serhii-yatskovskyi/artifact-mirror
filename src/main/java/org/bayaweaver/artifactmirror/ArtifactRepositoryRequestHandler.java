package org.bayaweaver.artifactmirror;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class ArtifactRepositoryRequestHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactRepositoryRequestHandler.class);

    private final ArtifactRepositoryUrlFactory artifactRepositoryUrlFactory;
    private final AuthorizationTokenProvider tokenProvider;

    ArtifactRepositoryRequestHandler(
            ArtifactRepositoryUrlFactory artifactRepositoryUrlFactory,
            AuthorizationTokenProvider tokenProvider) {

        this.artifactRepositoryUrlFactory = artifactRepositoryUrlFactory;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void handle(HttpExchange exchange) {
        logger.debug("{}: redirection initiated", exchange.getRequestMethod() + " " + exchange.getRequestURI());
        try {
            final String token;
            try {
                token = tokenProvider.fetchToken();
            } catch (TokenNotFetchedException e) {
                logger.error("Artifact repository access token was not fetched", e);
                returnError(exchange, e.getMessage());
                return;
            }
            final URL artifactUrl = artifactRepositoryUrlFactory.create(exchange);
            HttpURLConnection artifactConnection = (HttpURLConnection) artifactUrl.openConnection();
            artifactConnection.setRequestProperty("Authorization", "Bearer " + token);
            copyRequest(exchange, artifactConnection);
            final int responseCode = artifactConnection.getResponseCode();
            exchange.sendResponseHeaders(responseCode, 0);
            copyResponse(artifactConnection, exchange);
        } catch (Exception e) {
            String errorMessage = e.toString();
            logger.error(exchange.getRequestMethod() + " " + exchange.getRequestURI() + ": " + errorMessage, e);
            returnError(exchange, errorMessage);
        } finally {
            exchange.close();
        }
    }

    private void copyRequest(HttpExchange exchange, HttpURLConnection artifactConnection) throws IOException {
        exchange.getRequestHeaders().forEach((headerName, headerValues) -> {
            if (!headerName.equalsIgnoreCase("Host") && !headerName.equalsIgnoreCase("Authorization")) {
                for (String headerValue : headerValues) {
                    artifactConnection.addRequestProperty(headerName, headerValue);
                }
            }
        });
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equals("POST") || requestMethod.equals("PUT")) {
            artifactConnection.setDoOutput(true); // Implicitly sets POST
            artifactConnection.setRequestMethod(requestMethod);
            try (InputStream originalRequestBody = exchange.getRequestBody();
                 OutputStream artifactRequestBody = artifactConnection.getOutputStream()) {

                originalRequestBody.transferTo(artifactRequestBody);
            }
        }
    }

    private void copyResponse(HttpURLConnection artifactConnection, HttpExchange exchange) throws IOException {
        for (Map.Entry<String, List<String>> header : artifactConnection.getHeaderFields().entrySet()) {
            String headerName = header.getKey();
            if (headerName != null
                    && !headerName.toLowerCase().startsWith("x-amzn-")
                    && !headerName.equalsIgnoreCase("Content-Length")) {

                for (String headerValue : header.getValue()) {
                    exchange.getResponseHeaders().add(headerName, headerValue);
                }
            }
        }
        InputStream artifactResponseBody;
        artifactResponseBody = artifactConnection.getErrorStream();
        if (artifactResponseBody == null) {
            artifactResponseBody = artifactConnection.getInputStream();
            final String encoding = artifactConnection.getContentEncoding();
            if ("GZIP".equalsIgnoreCase(encoding)) {
                artifactResponseBody = new GZIPInputStream(artifactResponseBody);
            } else if ("DEFLATE".equalsIgnoreCase(encoding)) {
                artifactResponseBody = new InflaterInputStream(artifactResponseBody);
            }
        }
        try {
            artifactResponseBody.transferTo(exchange.getResponseBody());
        } catch (IOException e) {
            artifactResponseBody.close();
        }
    }

    private void returnError(HttpExchange exchange, String errorMessage) {
        try {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMessage.length());
            exchange.getResponseBody().write(errorMessage.getBytes());
        } catch (IOException e1) {
            logger.error(e1.getMessage(), e1);
        }
    }
}
