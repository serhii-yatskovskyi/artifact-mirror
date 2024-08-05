package org.bayaweaver.codeartifactproxy;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class AuthorizationTokenProvider {
    private final String domain;
    private final String domainOwner;
    private final CodeartifactClient codeartifactClient;
    private String token;
    private Instant tokenExpirationTime;

    AuthorizationTokenProvider(
            String domain,
            String domainOwner,
            String region,
            String accessKeyId,
            String secretAccessKey) {

        this.domain = domain;
        this.domainOwner = domainOwner;
        AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        this.codeartifactClient = CodeartifactClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                .build();
    }

    String fetchToken() throws TokenNotProvidedException {
        if (token == null || tokenExpirationTime.isBefore(Instant.now())) {
            GetAuthorizationTokenRequest req = GetAuthorizationTokenRequest.builder()
                    .domain(domain)
                    .domainOwner(domainOwner)
                    .build();
            GetAuthorizationTokenResponse res;
            try {
                res = codeartifactClient.getAuthorizationToken(req);
            } catch (Exception e) {
                token = null;
                throw new TokenNotProvidedException(e.getMessage());
            }
            token = res.authorizationToken();
            tokenExpirationTime = res.expiration().minus(1, ChronoUnit.MINUTES);
        }
        return token;
    }
}
