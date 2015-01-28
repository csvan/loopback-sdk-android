package com.strongloop.android.util;

import java.util.logging.Logger;

/**
 * Created by christopher on 27/01/15.
 * <p/>
 * Temporary solution to migrate from the previous Android logging framework.
 */
public class Log {
    private static Logger logger = Logger.getAnonymousLogger();

    public static Logger getLogger() {
        return logger;
    }
}
