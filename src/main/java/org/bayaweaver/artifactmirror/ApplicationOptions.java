package org.bayaweaver.artifactmirror;

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
            for (Option o : Option.values()) {
                if (arg.startsWith("--" + o.name + "=")) {
                    values.put(o, arg.substring(arg.indexOf('=') + 1));
                }
            }
        }
        for (Option o : Option.values()) {
            if (!values.containsKey(o)) {
                if (o.required) {
                    throw new IllegalArgumentException("Missing required option '" + o.name + "'");
                }
                values.put(o, o.defaultValue);
            }
        }
        return new ApplicationOptions(values);
    }

    String value(Option o) {
        return values.get(o);
    }

    enum Option {
        HTTP_SERVER_PORT("server.port", "80"),
        CODEARTIFACT_DOMAIN("aws.codeartifact.domain", true),
        CODEARTIFACT_DOMAIN_OWNER("aws.codeartifact.domainOwner", true),
        CODEARTIFACT_REGION("aws.codeartifact.region", true),
        AWS_ACCESS_KEY_ID("aws.accessKeyId", false),
        AWS_SECRET_ACCESS_KEY("aws.secretKey", false);

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
