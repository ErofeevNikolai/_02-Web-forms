package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private String method;
    private String path;
    private String version;
    private Map<String, NameValuePair> parameters;

    Request(String requestLine) {
        requestPars(requestLine);
        if (path.contains("?")) {
            this.parameters = new HashMap<>();
            requestParam();
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    // парсинг строки
    private void requestPars(String requestLine) {
        final var parts = requestLine.split(" ");
        this.method = parts[0];
        this.path = parts[1];
        this.version = parts[2];
    }

    private void requestParam() {
        String[] pathPram = path.split("\\?", 2);
        path = pathPram[0];
        List<NameValuePair> list = URLEncodedUtils.parse(pathPram[1], Charset.forName("UTF-8"));
        for (NameValuePair param : list) {
            parameters.put(param.getName(), param);
        }
    }

    public NameValuePair getQueryParam(String name){
        return parameters.get(name);
    }

    public Collection<NameValuePair> getQueryParams(){
        return parameters.values();
    }
}

