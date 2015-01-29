package com.strongloop.android.loopback.test;

import com.strongloop.android.loopback.RestAdapter;
import org.junit.Test;

import static org.junit.Assert.*;

public class RestAdapterTest extends AsyncTestCase {
    private RestAdapter adapter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        //testContext.clearSharedPreferences(RestAdapter.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        adapter = createRestAdapter();
    }

    @Test
    public void testAccessTokenIsStoredInSharedPreferences() {
        final String[] accessTokenRef = new String[1];
        adapter.setAccessToken("an-access-token");

        // android-async-http client does not allow inspection of request headers
        // the workaround is to override the setter method
        new RestAdapter(REST_SERVER_URL) {
            @Override
            public void setAccessToken(String value) {
                accessTokenRef[0] = value;
            }
        };

        assertEquals("an-access-token", accessTokenRef[0]);
    }
}
