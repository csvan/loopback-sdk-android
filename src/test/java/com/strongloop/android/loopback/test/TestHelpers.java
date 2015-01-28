package com.strongloop.android.loopback.test;

import com.google.common.collect.ImmutableSet;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

public class TestHelpers {
    public static void assertPropertyNames(JSONObject obj, String... names) {
        Set<String> actual = new HashSet<String>();
        Iterator iter = obj.keys();
        while (iter.hasNext())
            actual.add((String) iter.next());

        Set<String> expected = ImmutableSet.copyOf(names);

        assertEquals(expected, actual);
    }
}
