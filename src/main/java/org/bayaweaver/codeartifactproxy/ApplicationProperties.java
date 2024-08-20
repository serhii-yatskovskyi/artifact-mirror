package org.bayaweaver.codeartifactproxy;

import java.util.HashMap;
import java.util.Map;

public class ApplicationProperties {
    private final Map<Property, String> values;

    private ApplicationProperties(Map<Property, String> values) {
        this.values = values;
    }

    static ApplicationProperties parse(String[] args) {
        Map<Property, String> values = new HashMap<>();
        for (String arg : args) {
            for (Property property : Property.values()) {
                if (arg.startsWith("--" + property.name + "=")) {
                    values.put(property, arg.substring(arg.indexOf('=') + 1));
                }
            }
        }
        for (Property property : Property.values()) {
            if (!values.containsKey(property)) {
                if (property.required) {
                    throw new IllegalArgumentException("Missing required property '" + property.name + "'.");
                }
                values.put(property, property.defaultValue);
            }
        }
        return new ApplicationProperties(values);
    }

    String value(Property propertyProperty) {
        return values.get(propertyProperty);
    }

    enum Property {
        SERVER_PORT("server.port", "443"),
        SSL_CERTIFICATE_PATH("server.ssl.certificate", true),
        SSL_CA_BUNDLE_PATH("server.ssl.ca-bundle", false),
        SSL_PRIVATE_KEY_PATH("server.ssl.private-key", true),
        CODEARTIFACT_DOMAIN("aws.codeartifact.domain", true),
        CODEARTIFACT_DOMAIN_OWNNER("aws.codeartifact.domain-owner", true),
        CODEARTIFACT_REGION("aws.codeartifact.region", true),
        AWS_ACCESS_KEY_ID("aws.access-key-id", true),
        AWS_SECRET_ACCESS_KEY("aws.secret-access-key", true);

        private final String name;
        private final String defaultValue;
        private final boolean required;

        Property(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.required = false;
        }

        Property(String name, boolean required) {
            this.name = name;
            this.defaultValue = null;
            this.required = required;
        }
    }
}
