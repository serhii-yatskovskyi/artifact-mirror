package org.bayaweaver.artifactmirror;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bayaweaver.artifactmirror.codeartifact.CodeartifactAuthorizationTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
            // "https://my-domain-111122223333.d.codeartifact.us-east-1.amazonaws.com";
            artifactRepositoryUrl = URI
                    .create("https://" + codeartifactDomain + "-" + codeartifactDomainOwner
                            + ".d.codeartifact." + codeartifactRegion + ".amazonaws.com")
                    .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("URL-improper symbol in 'aws.codeartifact.domain',"
                    + " 'aws.codeartifact.domain-owner' or 'aws.codeartifact.region'");
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
            final URL artifactUrl = URI.create(artifactRepositoryUrl + exchange.getRequestURI()).toURL();
            HttpURLConnection artifactConnection = (HttpURLConnection) artifactUrl.openConnection();
            copyRequest(exchange, artifactConnection);
            artifactConnection.setRequestProperty("Authorization", "Bearer " + token);
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
            if (!headerName.equalsIgnoreCase("Host")) {
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
        InputStream artifactResponseBody;
        artifactResponseBody = artifactConnection.getErrorStream();
        if (artifactResponseBody == null) {
            artifactResponseBody = artifactConnection.getInputStream();
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
