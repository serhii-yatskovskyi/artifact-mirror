package org.bayaweaver.artifactmirror;

public final class TokenNotFetchedException extends Exception {

    public TokenNotFetchedException(Throwable cause) {
        super("Token was not fetched. " + cause.getMessage());
    }
}
