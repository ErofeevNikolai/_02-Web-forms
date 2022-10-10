package ru.netology;

import org.apache.http.NameValuePair;

public class RequestParam implements NameValuePair {
    private String name;
    private String value;

    RequestParam(String name, String value){
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }
}
