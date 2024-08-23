package org.bayaweaver.codeartifactproxy;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.CodeartifactClientBuilder;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class AuthorizationTokenProvider {
    private final String domain;
    private final String domainOwner;
    private final CodeartifactClient codeartifactClient;
    private final AuthorizationTokenCache tokenCache;

    AuthorizationTokenProvider(
            String domain,
            String domainOwner,
            String region) {

        this(domain, domainOwner, region, null, null);
    }

    AuthorizationTokenProvider(
            String domain,
            String domainOwner,
            String region,
            String accessKeyId,
            String secretAccessKey) {

        this.domain = domain;
        this.domainOwner = domainOwner;
        CodeartifactClientBuilder codeartifactClientBuilder = CodeartifactClient.builder();
        codeartifactClientBuilder.region(Region.of(region));
        if (accessKeyId != null && !accessKeyId.isEmpty()) {
            AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
            codeartifactClientBuilder.credentialsProvider(credentialsProvider);
        }
        this.codeartifactClient = codeartifactClientBuilder.build();
        this.tokenCache = AuthorizationTokenCache.instance();
    }

    String fetchToken() throws TokenNotFetchedException {
        try {
            String token = this.tokenCache.get();
            if (token == null) {
                GetAuthorizationTokenRequest req = GetAuthorizationTokenRequest.builder()
                        .domain(domain)
                        .domainOwner(domainOwner)
                        .build();
                GetAuthorizationTokenResponse res;
                res = codeartifactClient.getAuthorizationToken(req);
                token = res.authorizationToken();
                Instant expiration = res.expiration().minus(1, ChronoUnit.MINUTES);
                this.tokenCache.put(token, expiration);
            }
            return token;
        } catch (Throwable e) {
            this.tokenCache.evict();
            throw new TokenNotFetchedException(e);
        }
    }
}
