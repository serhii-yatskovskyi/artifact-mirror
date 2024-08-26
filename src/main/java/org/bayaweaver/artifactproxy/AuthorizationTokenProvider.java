package org.bayaweaver.artifactproxy;

public interface AuthorizationTokenProvider {

    String fetchToken() throws TokenNotFetchedException;
}
