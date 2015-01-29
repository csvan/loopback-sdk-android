package com.strongloop.android.loopback.test;

import com.strongloop.android.loopback.RestAdapter;

public class RestAdapterTest extends AsyncTestCase {
    private RestAdapter adapter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        //testContext.clearSharedPreferences(RestAdapter.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        adapter = createRestAdapter();
    }
}
