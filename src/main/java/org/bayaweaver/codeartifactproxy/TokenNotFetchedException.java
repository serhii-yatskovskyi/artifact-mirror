package org.bayaweaver.codeartifactproxy;

public class TokenNotFetchedException extends Exception {

    TokenNotFetchedException(Throwable cause) {
        super("Token was not fetched", cause);
    }
}
