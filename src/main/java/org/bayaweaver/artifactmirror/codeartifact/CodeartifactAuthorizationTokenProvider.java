package org.bayaweaver.artifactmirror.codeartifact;

import org.bayaweaver.artifactmirror.AuthorizationTokenProvider;
import org.bayaweaver.artifactmirror.TokenNotFetchedException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.CodeartifactClientBuilder;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CodeartifactAuthorizationTokenProvider implements AuthorizationTokenProvider {
    private final String domain;
    private final String domainOwner;
    private final CodeartifactClient codeartifactClient;
    private final AuthorizationTokenCache tokenCache;

    public CodeartifactAuthorizationTokenProvider(
            String domain,
            String domainOwner,
            String region,
            String accessKeyId,
            String secretAccessKey,
            URI codeartifactApiEndpoint) {

        this.domain = domain;
        this.domainOwner = domainOwner;
        CodeartifactClientBuilder codeartifactClientBuilder = CodeartifactClient.builder();
        if (codeartifactApiEndpoint != null) {
            codeartifactClientBuilder.endpointOverride(codeartifactApiEndpoint);
        }
        codeartifactClientBuilder.region(Region.of(region));
        if (accessKeyId != null && !accessKeyId.isEmpty()) {
            AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
            codeartifactClientBuilder.credentialsProvider(credentialsProvider);
        }
        this.codeartifactClient = codeartifactClientBuilder.build();
        this.tokenCache = AuthorizationTokenCache.instance();
    }

    @Override
    public String fetchToken() throws TokenNotFetchedException {
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
