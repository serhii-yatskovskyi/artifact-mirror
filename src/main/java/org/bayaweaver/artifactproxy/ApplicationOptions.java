package org.bayaweaver.artifactproxy;

import java.util.HashMap;
import java.util.Map;

public class ApplicationOptions {
    private final Map<Option, String> values;

    private ApplicationOptions(Map<Option, String> values) {
        this.values = values;
    }

    static ApplicationOptions parse(String[] args) {
        Map<Option, String> values = new HashMap<>();
        for (String arg : args) {
            for (Option option : Option.values()) {
                if (arg.startsWith("--" + option.name + "=")) {
                    values.put(option, arg.substring(arg.indexOf('=') + 1));
                }
            }
        }
        for (Option option : Option.values()) {
            if (!values.containsKey(option)) {
                if (option.required) {
                    throw new IllegalArgumentException("Missing required option '" + option.name + "'.");
                }
                values.put(option, option.defaultValue);
            }
        }
        return new ApplicationOptions(values);
    }

    String value(Option propertyOption) {
        return values.get(propertyOption);
    }

    enum Option {
        HTTP_SERVER_PORT("server.http.port", "80"),
        HTTPS_SERVER_PORT("server.https.port", "443"),
        SSL_CERTIFICATE_PATH("server.ssl.certificate", "certificate.crt"),
        SSL_CA_BUNDLE_PATH("server.ssl.ca-bundle", false),
        SSL_PRIVATE_KEY_PATH("server.ssl.private-key", "private.key"),
        CODEARTIFACT_DOMAIN("aws.codeartifact.domain", true),
        CODEARTIFACT_DOMAIN_OWNER("aws.codeartifact.domain-owner", true),
        CODEARTIFACT_REGION("aws.codeartifact.region", true),
        AWS_ACCESS_KEY_ID("aws.access-key-id", false),
        AWS_SECRET_ACCESS_KEY("aws.secret-access-key", false);

        private final String name;
        private final String defaultValue;
        private final boolean required;

        Option(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.required = false;
        }

        Option(String name, boolean required) {
            this.name = name;
            this.defaultValue = null;
            this.required = required;
        }
    }
}
