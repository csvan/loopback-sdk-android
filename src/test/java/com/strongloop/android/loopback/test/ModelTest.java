package com.strongloop.android.loopback.test;

import com.strongloop.android.loopback.Model;
import com.strongloop.android.loopback.ModelRepository;
import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.ObjectCallback;
import com.strongloop.android.loopback.callbacks.VoidCallback;
import com.strongloop.android.util.Log;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.strongloop.android.loopback.test.TestHelpers.assertPropertyNames;
import static org.junit.Assert.*;

public class ModelTest extends AsyncTestCase {

    private ModelRepository<Model> repository;
    private RestAdapter adapter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        adapter = createRestAdapter();
        repository = adapter.createRepository("widget");
    }

    @Test
    public void testCreateAndRemove() throws Throwable {
        final Object[] lastId = new Object[1];

        Map<String, Object> params = new HashMap<>();
        params.put("name", "Foobar");
        params.put("bars", 1);

        final Model model = repository.createModel(params);

        assertEquals("Foobar", model.get("name"));
        assertEquals(1, model.get("bars"));
        assertNull(model.getId());

        doAsyncTest(new AsyncTest() {

            @Override
            public void run() {
                model.save(new VoidTestCallback() {

                    @Override
                    public void onSuccess() {
                        lastId[0] = model.getId();
                        Log.getLogger().log(Level.FINE, "ModelTest", "id: " + model.getId());
                        assertNotNull(model.getId());
                        notifyFinished();
                    }
                });
            }
        });

        assertNotNull(lastId[0]);

        JSONObject remoteJson = fetchJsonObjectById(repository, lastId[0]);
        assertNotNull(remoteJson);
        assertPropertyNames(remoteJson, "id", "name", "bars");

        doAsyncTest(new AsyncTest() {

            @Override
            public void run() {
                repository.findById(lastId[0],
                        new ObjectTestCallback<Model>() {

                            @Override
                            public void onSuccess(Model model) {
                                model.destroy(new VoidTestCallback());
                            }

                        });
            }
        });
    }

    @Test
    public void testFind() throws Throwable {
        doAsyncTest(new AsyncTest() {

            @Override
            public void run() {
                repository.findById(2, new ObjectTestCallback<Model>() {

                    @Override
                    public void onSuccess(Model model) {
                        assertNotNull("No model found with id 2", model);
                        assertTrue("Invalid class", (model instanceof Model));
                        assertEquals("Invalid name", "Bar", model.get("name"));
                        assertEquals("Invalid bars", 1, model.get("bars"));
                        notifyFinished();
                    }
                });
            }
        });
    }

    @Test
    public void testFindAll() throws Throwable {
        doAsyncTest(new AsyncTest() {

            @Override
            public void run() {
                repository.findAll(new ListTestCallback<Model>() {

                    @Override
                    public void onSuccess(List<Model> list) {
                        assertNotNull("No models returned.", list);
                        assertTrue("Invalid # of models returned: " +
                                list.size(), list.size() >= 2);
                        assertTrue("Invalid class",
                                (list.get(0) instanceof Model));
                        assertEquals("Invalid name", "Foo",
                                list.get(0).get("name"));
                        assertEquals("Invalid bars", 0,
                                list.get(0).get("bars"));
                        assertEquals("Invalid name", "Bar",
                                list.get(1).get("name"));
                        assertEquals("Invalid bars", 1,
                                list.get(1).get("bars"));
                        notifyFinished();
                    }
                });
            }
        });
    }

    @Test
    public void testUpdate() throws Throwable {
        doAsyncTest(new AsyncTest() {

            ObjectCallback<Model> verify =
                    new ObjectTestCallback<Model>() {

                        @Override
                        public void onSuccess(Model model) {
                            assertNotNull("No model found with id 2", model);
                            assertTrue("Invalid class", (model instanceof Model));
                            assertEquals("Invalid name", "Barfoo", model.get("name"));
                            assertEquals("Invalid bars", 1, model.get("bars"));

                            model.put("name", "Bar");
                            model.save(new VoidTestCallback());
                        }
                    };

            VoidCallback findAgain = new VoidTestCallback() {

                @Override
                public void onSuccess() {
                    repository.findById(2, verify);
                }
            };

            ObjectCallback<Model> update =
                    new ObjectTestCallback<Model>() {

                        @Override
                        public void onSuccess(Model model) {
                            assertNotNull("No model found with ID 2", model);
                            model.put("name", "Barfoo");
                            model.save(findAgain);
                        }
                    };

            @Override
            public void run() {
                repository.findById(2, update);
            }
        });
    }

    @Test
    public void testRestContractUsesPluralizedNameInUrl() {
        adapter.createRepository("weapon");

        String methodUrl = adapter.getContract().getUrlForMethod("weapon.all", null);
        assertEquals("/weapons", methodUrl);
    }

    @Test
    public void testRestContractUsesCustomNameInUrl() {
        adapter.createRepository("ammo", "ammo");

        String methodUrl = adapter.getContract().getUrlForMethod("ammo.all", null);
        assertEquals("/ammo", methodUrl);
    }
}
