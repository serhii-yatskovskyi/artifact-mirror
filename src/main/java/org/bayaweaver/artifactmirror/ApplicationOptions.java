package org.bayaweaver.artifactmirror;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ApplicationOptions {
    private final Map<Option, String> values;

    private ApplicationOptions(Map<Option, String> values) {
        this.values = values;
    }

    static ApplicationOptions parse(String[] args) {
        Map<Option, String> values = new HashMap<>();
        for (String arg : args) {
            for (Option o : Option.values()) {
                if (arg.startsWith("--" + o.name + "=")) {
                    values.put(o, arg.substring(arg.indexOf('=') + 1));
                }
            }
        }
        for (Option o : Option.values()) {
            if (!values.containsKey(o)) {
                if (o.required.test(values)) {
                    throw new IllegalArgumentException("Missing required option '" + o.name + "'");
                }
                if (o.defaultValue != null) {
                    values.put(o, o.defaultValue);
                }
            }
        }
        return new ApplicationOptions(values);
    }

    String value(Option o) {
        return values.get(o);
    }

    boolean isPresent(Option o) {
        return values.containsKey(o);
    }

    enum Option {
        HTTP_SERVER_PORT("server.port", "80"),
        CODEARTIFACT_ENDPOINT("aws.codeartifact.vpc-endpoint", opts -> false),
        CODEARTIFACT_API_ENDPOINT("aws.codeartifact.api.vpc-endpoint", opts -> {
            String value = opts.get(Option.CODEARTIFACT_ENDPOINT);
            if (value == null) {
                return false;
            }
            return !value.isEmpty();
        }),
        CODEARTIFACT_DOMAIN("aws.codeartifact.domain", opts -> true),
        CODEARTIFACT_DOMAIN_OWNER("aws.codeartifact.domainOwner", opts -> true),
        CODEARTIFACT_REGION("aws.codeartifact.region", opts -> true),
        AWS_ACCESS_KEY_ID("aws.accessKeyId", opts -> false),
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

        Option(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.required = opts -> false;
        }

        Option(String name, Predicate<Map<Option, String>> required) {
            this.name = name;
            this.defaultValue = null;
            this.required = required;
        }
    }
}
