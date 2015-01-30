package com.strongloop.android.loopback;

/**
 * The main entry point for interacting with a Loopback server instance,
 * this class provides the funtionality needed to login as a remote user
 * (if necessary), work with the remote models, and other utilities in
 * order to make working with Loopback a breeze.
 * <p/>
 * Created by christopher on 30/01/15.
 */
public class LoopbackInterface {

    private final String url;

    public static LoopbackInterface getDefault(String url) {
        return new LoopbackInterface(url);
    }

    private LoopbackInterface(String url) {
        this.url = url;
    }
}
