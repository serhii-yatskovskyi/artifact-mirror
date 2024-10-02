package org.bayaweaver.artifactmirror;

import java.util.Map;
import java.util.function.Predicate;

public enum Option {
    SERVER_PORT("server.port", "80"),
    CODEARTIFACT_ENDPOINT(
            "aws.codeartifact.vpc-endpoint",
            opts -> false,
            v -> v.startsWith("https://") || v.startsWith("http://")),
    CODEARTIFACT_API_ENDPOINT(
            "aws.codeartifact.api.vpc-endpoint",
            opts -> {
                String value = opts.get(Option.CODEARTIFACT_ENDPOINT);
                if (value == null) {
                    return false;
                }
                return !value.isEmpty();
            },
            v -> v.startsWith("https://") || v.startsWith("http://")),
    CODEARTIFACT_DOMAIN("aws.codeartifact.domain", opts -> true, v -> v.matches("[a-z0-9.-]+")),
    CODEARTIFACT_DOMAIN_OWNER("aws.codeartifact.domainOwner", opts -> true, v -> v.matches("^\\d{12}$")),
    CODEARTIFACT_REGION("aws.codeartifact.region", opts -> true),
    AWS_ACCESS_KEY_ID("aws.accessKeyId", opts -> {
        String value = opts.get(Option.valueOf("AWS_SECRET_ACCESS_KEY"));
        if (value == null) {
            return false;
        }
        return !value.isEmpty();
    }),
    AWS_SECRET_ACCESS_KEY("aws.secretKey", opts -> {
        String value = opts.get(Option.AWS_ACCESS_KEY_ID);
        if (value == null) {
            return false;
        }
        return !value.isEmpty();
    });

    private final String name;
    private final String defaultValue;
    private final Predicate<Map<Option, String>> required;
    private final Predicate<String> valid;

    Option(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.required = opts -> false;
        this.valid = value -> true;
    }

    Option(String name, Predicate<Map<Option, String>> required) {
        this(name, required, value -> true);
    }

    Option(String name, Predicate<Map<Option, String>> required, Predicate<String> valid) {
        this.name = name;
        this.defaultValue = null;
        this.required = required;
        this.valid = valid;
    }

    String defaultValue() {
        return defaultValue;
    }

    Predicate<Map<Option, String>> required() {
        return required;
    }

    Predicate<String> valid() {
        return valid;
    }

    @Override
    public String toString() {
        return name;
    }
}
