package org.bayaweaver.artifactgateway;

public interface AuthorizationTokenProvider {

    String fetchToken() throws TokenNotFetchedException;
}
