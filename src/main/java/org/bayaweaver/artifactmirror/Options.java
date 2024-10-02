package org.bayaweaver.artifactmirror;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private final Map<Option, String> values;

    private Options(Map<Option, String> values) {
        this.values = values;
    }

    static Options parse(String[] args) {
        Map<Option, String> values = new HashMap<>();
        for (String arg : args) {
            for (Option o : Option.values()) {
                if (arg.startsWith("--" + o + "=")) {
                    values.put(o, arg.substring(arg.indexOf('=') + 1));
                }
            }
        }
        for (Option o : Option.values()) {
            if (!values.containsKey(o)) {
                if (o.required().test(values)) {
                    throw new IllegalArgumentException("Missing required option '" + o + "'");
                }
                if (o.defaultValue() != null) {
                    values.put(o, o.defaultValue());
                }
            }
        }
        for (Map.Entry<Option, String> o : values.entrySet()) {
            if (!o.getKey().valid().test(o.getValue())) {
                throw new IllegalArgumentException("Option '" + o + "' is not valid");
            }
        }
        return new Options(values);
    }

    String value(Option o) {
        return values.get(o);
    }

    boolean contains(Option o) {
        return values.containsKey(o);
    }
}
