package com.strongloop.android.remoting.adapters;

import java.io.InputStream;

/**
 * A request parameter that is a (binary) stream.
 */
public class StreamParam {
    private final InputStream stream;
    private final String fileName;
    private final String contentType;

    public StreamParam(InputStream stream, String fileName) {
        this(stream, fileName, null);
    }

    public StreamParam(InputStream stream, String fileName, String contentType) {
        this.stream = stream;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    // TODO: Find alternative way to implement this
    /*
    public void putTo(RequestParams params, String key) {
        params.put(key, stream, fileName, contentType);
    }
    */
}
