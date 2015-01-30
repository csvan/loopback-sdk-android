package com.strongloop.android.remoting.adapters;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.multipart.ByteArrayPart;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
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

    public void putTo(AsyncHttpClient.BoundRequestBuilder requestBuilder, String key) throws IOException {
        byte[] bytes = IOUtils.toByteArray(stream);
        ByteArrayPart arrayPart = new ByteArrayPart(key, bytes, contentType, null, fileName);
        requestBuilder.addBodyPart(arrayPart);
    }
}
