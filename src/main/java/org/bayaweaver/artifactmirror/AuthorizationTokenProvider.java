package org.bayaweaver.artifactmirror;

public interface AuthorizationTokenProvider {

    String fetchToken() throws TokenNotFetchedException;
}
