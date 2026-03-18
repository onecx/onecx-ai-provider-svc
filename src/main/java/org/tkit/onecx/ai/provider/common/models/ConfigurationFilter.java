package org.tkit.onecx.ai.provider.common.models;

public class ConfigurationFilter {

    private Key key;

    private String value;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public enum Key {

        APP_ID;
    }

}
