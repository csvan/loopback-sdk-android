// Copyright (c) 2013 StrongLoop. All rights reserved.

package com.strongloop.android.remoting.adapters;

import com.ning.http.client.*;
import com.ning.http.client.multipart.FilePart;
import com.strongloop.android.remoting.JsonUtil;
import com.strongloop.android.util.Log;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A specific {@link Adapter} implementation for RESTful servers.
 * <p/>
 * In addition to implementing the {@link Adapter} interface,
 * <code>RestAdapter</code> contains a single {@link RestContract} to map
 * remote methods to custom HTTP routes. This is only required if the HTTP
 * settings have been customized on the server. When in doubt, try without.
 *
 * @see RestContract
 */
public class RestAdapter extends Adapter {
    private static final String TAG = "remoting.RestAdapter";

    private HttpClient client;
    private RestContract contract;

    public RestAdapter(String url) {
        super(url);
        this.contract = new RestContract();
    }

    /**
     * Gets this adapter's {@link RestContract}, a custom contract for
     * fine-grained route configuration.
     *
     * @return the contract.
     */
    public RestContract getContract() {
        return contract;
    }

    /**
     * Get the underlying HTTP client. This allows subclasses to add
     * custom headers like Authorization.
     *
     * @return the client.
     */
    protected HttpClient getClient() {
        return client;
    }

    /**
     * Sets this adapter's {@link RestContract}, a custom contract for
     * fine-grained route configuration.
     *
     * @param contract The contract.
     */
    public void setContract(RestContract contract) {
        this.contract = contract;
    }

    @Override
    public void connect(String url) {
        if (url == null) {
            client = null;
        } else {
            client = new HttpClient(url);

            // TODO: Find way to set these headers globally
            //client.addHeader("Accept", "application/json");
        }
    }

    @Override
    public boolean isConnected() {
        return client != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the contract is not set
     *                               (see {@link #setContract(RestContract)})
     *                               or the adapter is not connected.
     */
    @Override
    public void invokeStaticMethod(String method,
                                   Map<String, ? extends Object> parameters,
                                   final Callback callback) {
        AsyncCompletionHandler<Response> httpHandler = new CallbackHandler(callback);
        invokeStaticMethod(method, parameters, httpHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the contract is not set
     *                               (see {@link #setContract(RestContract)})
     *                               or the adapter is not connected.
     */
    @Override
    public void invokeStaticMethod(String method,
                                   Map<String, ? extends Object> parameters,
                                   final BinaryCallback callback) {
        AsyncCompletionHandler<Response> httpHandler = new BinaryHandler(callback);
        invokeStaticMethod(method, parameters, httpHandler);
    }

    private void invokeStaticMethod(String method,
                                    Map<String, ? extends Object> parameters,
                                    AsyncCompletionHandler<Response> httpHandler) {
        if (contract == null) {
            throw new IllegalStateException("Invalid contract");
        }

        String verb = contract.getVerbForMethod(method);
        String path = contract.getUrlForMethod(method, parameters);
        ParameterEncoding parameterEncoding = contract.getParameterEncodingForMethod(method);

        request(path, verb, parameters, parameterEncoding, httpHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the contract is not set
     *                               (see {@link #setContract(RestContract)})
     *                               or the adapter is not connected.
     */
    @Override
    public void invokeInstanceMethod(String method,
                                     Map<String, ? extends Object> constructorParameters,
                                     Map<String, ? extends Object> parameters,
                                     final Callback callback) {
        AsyncCompletionHandler<Response> httpHandler = new CallbackHandler(callback);
        invokeInstanceMethod(method, constructorParameters, parameters, httpHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the contract is not set
     *                               (see {@link #setContract(RestContract)})
     *                               or the adapter is not connected.
     */
    @Override
    public void invokeInstanceMethod(String method,
                                     Map<String, ? extends Object> constructorParameters,
                                     Map<String, ? extends Object> parameters,
                                     final BinaryCallback callback) {
        AsyncCompletionHandler<Response> httpHandler = new BinaryHandler(callback);
        invokeInstanceMethod(method, constructorParameters, parameters, httpHandler);
    }

    ;

    private void invokeInstanceMethod(String method,
                                      Map<String, ? extends Object> constructorParameters,
                                      Map<String, ? extends Object> parameters,
                                      AsyncCompletionHandler<Response> httpHandler) {
        if (contract == null) {
            throw new IllegalStateException("Invalid contract");
        }

        Map<String, Object> combinedParameters = new HashMap<String, Object>();
        if (constructorParameters != null) {
            combinedParameters.putAll(constructorParameters);
        }
        if (parameters != null) {
            combinedParameters.putAll(parameters);
        }

        String verb = contract.getVerbForMethod(method);
        String path = contract.getUrlForMethod(method, combinedParameters);
        ParameterEncoding parameterEncoding = contract.getParameterEncodingForMethod(method);

        request(path, verb, combinedParameters, parameterEncoding, httpHandler);
    }

    private void request(String path,
                         String verb,
                         Map<String, ? extends Object> parameters,
                         ParameterEncoding parameterEncoding,
                         AsyncCompletionHandler<Response> responseHandler) {

        if (!isConnected()) {
            throw new IllegalStateException("Adapter not connected");
        }

        client.request(verb, path, parameters, parameterEncoding, responseHandler);
    }

    class AsyncCompletionHandlerWithStatusCorrection extends AsyncCompletionHandlerBase {

        @Override
        public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
            if (status.getStatusCode() == 500) {
                return STATE.ABORT;
            } else {
                return super.onStatusReceived(status);
            }
        }
    }

    class CallbackHandler extends AsyncCompletionHandlerWithStatusCorrection {
        private final Callback callback;

        public CallbackHandler(Callback callback) {
            this.callback = callback;
        }

        @Override
        public Response onCompleted(Response response) throws Exception {
            try {
                String responseBody = response.getResponseBody();
                Log.getLogger().info("Success (string): " + response);
                callback.onSuccess(responseBody);
            } catch (Throwable t) {
                callback.onError(t);
            }

            return super.onCompleted(response);
        }

        @Override
        public void onThrowable(Throwable t) {
            Log.getLogger().severe("HTTP request (string) failed: " + t.getMessage());
            callback.onError(t);

            super.onThrowable(t);
        }
    }

    class BinaryHandler extends AsyncCompletionHandlerWithStatusCorrection {
        private final BinaryCallback callback;

        public BinaryHandler(BinaryCallback callback) {
            //super(new String[]{".*"});
            this.callback = callback;
        }

        @Override
        public Response onCompleted(Response response) throws Exception {

            byte[] binaryData = response.getResponseBodyAsBytes();

            Log.getLogger().info("Success (binary): " + binaryData.length + " bytes");

            try {
                String contentType = response.getContentType();
                callback.onSuccess(binaryData, contentType);
            } catch (Throwable t) {
                callback.onError(t);
            }

            return super.onCompleted(response);
        }

        @Override
        public void onThrowable(Throwable t) {
            Log.getLogger().severe("HTTP request (binary) failed: " + t.getMessage());
            callback.onError(t);

            super.onThrowable(t);
        }
    }

    //
    // Mimic AFNetworking as much as possible.
    //
    // Internally, it's using "Android Asynchronous Http Client".
    // http://loopj.com/android-async-http/
    // The benefit is connection pools, persistent cookies,
    // an asynchronous API, Android bug workarounds, etc.
    // The drawback is it doesn't support HEAD or OPTION.
    //

    enum ParameterEncoding {
        FORM_URL,
        JSON,
        FORM_MULTIPART
    }

    public static class HttpClient extends AsyncHttpClient {

        private Map<String, String> headers = new HashMap<>();

        private static String getVersionName() {
            String appVersion = null;
            //TODO: Find reasonable way to set this
            return (appVersion != null) ? appVersion : "";
        }

        private String baseUrl;

        public HttpClient(String baseUrl) {
            if (baseUrl == null) {
                throw new IllegalArgumentException(
                        "The baseUrl cannot be null");
            }

            this.baseUrl = baseUrl;

            // Make sure base url ends with a trailing slash.
            if (!this.baseUrl.endsWith("/")) {
                this.baseUrl += "/";
            }

            // More useful User-Agent, similar to AFNetworing.
            String appName = "StongLoopRemoting App";

            String userAgent = appName + " (" + "Placeholder" + ")";

            setUserAgent(userAgent);
            addHeader("Accept", "application/json");
        }

        private void setUserAgent(String userAgent) {
            addHeader("User-Agent", userAgent);
        }

        public void request(String method,
                            String path,
                            Map<String, ? extends Object> parameters,
                            ParameterEncoding parameterEncoding,
                            final AsyncCompletionHandler<Response> httpCallback) {

            //TODO: Test extensively to make sure this URI works in tandem with the Android one
            if (path != null) {
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
            }

            BoundRequestBuilder request = prepareRequest(method, baseUrl + path);

            String contentType = null;
            String charset = "utf-8";

            if (parameters != null) {
                // Encodes a GET, HEAD or DELETE request
                if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                    for (Map.Entry<String, ? extends Object> entry : flattenParameters(parameters).entrySet()) {
                        request.addQueryParam(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
                // Encodes A POST or PUT request
                else if (parameterEncoding == ParameterEncoding.FORM_URL) {

                    // NOTE: Code for "x-www-form-urlencoded" is not used
                    // and is untested.
                    contentType = "application/x-www-form-urlencoded; charset=" + charset;
                    // TODO: Set body param properly
                    for (Map.Entry<String, ? extends Object> entry : parameters.entrySet()) {
                        request.addFormParam(entry.getKey(), String.valueOf(entry.getValue()));
                    }

                } else if (parameterEncoding == ParameterEncoding.FORM_MULTIPART) {

                    if (!"POST".equalsIgnoreCase(method)) {
                        throw new UnsupportedOperationException(
                                "RestAdapter does not support multipart PUT requests");
                    }

                    for (Map.Entry<String, ? extends Object> entry : parameters.entrySet()) {
                        Object value = entry.getValue();
                        if (value != null) {
                            if (value instanceof java.io.File) {
                                request.addBodyPart(new FilePart(entry.getKey(), (File) value));
                            } else if (value instanceof StreamParam) {
                                try {
                                    ((StreamParam) value).putTo(request, entry.getKey());
                                } catch (IOException e) {
                                    // TODO: Handle this in a better way. Should not continue.
                                    e.printStackTrace();
                                }
                            } else if (value instanceof String) {
                                request.addFormParam(entry.getKey(), (String) entry.getValue());
                            } else {
                                throw new IllegalArgumentException(
                                        "Unknown param type for RequestParams: "
                                                + value.getClass().getName());
                            }
                        }
                    }
                } else if (parameterEncoding == ParameterEncoding.JSON) {
                    contentType = "application/json; charset=" + charset;
                    String s = "";
                    try {
                        s = String.valueOf(JsonUtil.toJson(parameters));
                    } catch (JSONException e) {
                        Log.getLogger().severe("Couldn't convert parameters to JSON: " + e.getMessage());
                    }

                    // TODO: Make sure that encoding works
                    request.setBody(s);
                    //body = new StringEntity(s, charset);
                }
            }

            //String url = uri.build().toString();
            //logRequest(method, url, body, requestParams);

            // Set the headers
            for (String header : headers.keySet()) {
                request.addHeader(header, getHeader(header));
            }

            if ("GET".equalsIgnoreCase(method)) {
                request.execute(httpCallback);
                //get(context, url, headers, null, httpCallback);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                request.execute(httpCallback);
                //delete(context, url, headers, httpCallback);
            } else if ("POST".equalsIgnoreCase(method)) {
                request.addHeader("Content-Type", contentType);
                request.execute(httpCallback);
            } else if ("PUT".equalsIgnoreCase(method)) {
                request.addHeader("Content-Type", contentType);
                request.execute(httpCallback);
            } else {
                throw new IllegalArgumentException("Illegal method: " + method + ". Only GET, POST, PUT, DELETE supported.");
            }
        }

        private BoundRequestBuilder prepareRequest(String method, String url) {
            if ("GET".equalsIgnoreCase(method)) {
                return prepareGet(url);
            } else if ("HEAD".equalsIgnoreCase(method)) {
                return prepareHead(url);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                return prepareDelete(url);
            } else if ("POST".equalsIgnoreCase(method)) {
                return preparePost(url);
            } else if ("PUT".equalsIgnoreCase(method)) {
                return preparePut(url);
            } else if ("OPTIONS".equalsIgnoreCase(method)) {
                return prepareOptions(url);
            } else {
                return null;
            }
        }

        /*
        private void logRequest(String method, String url, HttpEntity body, RequestParams requestParams) {
            Log.getLogger().info(method + " " + url);
            if (requestParams != null)
                Log.getLogger().info(requestParams.toString());
            else if (body != null && body.isRepeatable()) {
                try {
                    // Convert body stream to string
                    // Based on http://stackoverflow.com/a/5445161/69868
                    Scanner s = new Scanner(body.getContent()).useDelimiter("\\A");
                    if (s.hasNext())
                        Log.getLogger().info(s.next());
                } catch (IOException e) {
                }
            }
        }
        */

        private Map<String, Object> flattenParameters(
                final Map<String, ? extends Object> parameters) {
            return flattenParameters(null, parameters);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> flattenParameters(
                final String keyPrefix,
                final Map<String, ? extends Object> parameters) {

            // This method converts nested maps into a flat list
            //   Input:  { "here": { "lat": 10, "lng": 20 }
            //   Output: { "here[lat]": 10, "here[lng]": 20 }

            Map<String, Object> result = new HashMap<String, Object>();

            for (Map.Entry<String, ? extends Object> entry
                    : parameters.entrySet()) {

                String key = keyPrefix != null
                        ? keyPrefix + "[" + entry.getKey() + "]"
                        : entry.getKey();

                Object value = entry.getValue();

                if (value instanceof Map) {
                    result.putAll(flattenParameters(key, (Map) value));
                } else {
                    result.put(key, value);
                }
            }

            return result;
        }

        public void addHeader(String key, String value) {
            headers.put(key, value);
        }

        public String getHeader(String key) {
            return headers.get(key);
        }

        public void removeHeader(String key) {
            headers.remove(key);
        }
    }
}
