package com.strongloop.android.loopback;

import com.strongloop.android.loopback.callbacks.VoidCallback;
import com.strongloop.android.remoting.JsonUtil;
import com.strongloop.android.remoting.adapters.Adapter;
import com.strongloop.android.remoting.adapters.RestContract;
import com.strongloop.android.remoting.adapters.RestContractItem;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A base class implementing {@link ModelRepository} for the built-in User type.
 * <p>
 * <pre>{@code
 * UserRepository<MyUser> userRepo = new UserRepository<MyUser>("user", MyUser.class);
 * }</pre>
 * <p>
 * Most application are extending the built-in User model and adds new properties
 * like address, etc. You should create your own Repository class
 * by extending this base class in such case.
 * <p>
 * <pre>{@code
 * public class Customer extends User {
 *   // your custom properties and prototype (instance) methods
 * }
 *
 * public class CustomerRepository extends UserRepository<Customer> {
 *     public interface LoginCallback extends LoginCallback<Customer> {
 *     }
 *
 *     public CustomerRepository() {
 *         super("customer", null, Customer.class);
 *     }
 *
 *     // your custom static methods
 * }
 * }</pre>
 */
public class UserRepository<U extends User> extends ModelRepository<U> {

    private AccessTokenRepository accessTokenRepository;

    private RestAdapter getRestAdapter() {
        return (RestAdapter) getAdapter();
    }

    private AccessTokenRepository getAccessTokenRepository() {
        if (accessTokenRepository == null) {
            accessTokenRepository = getRestAdapter()
                    .createRepository(AccessTokenRepository.class);
        }
        return accessTokenRepository;
    }

    /**
     * Creates a new UserRepository, associating it with
     * the static {@code U} user class and the user class name.
     * @param className The remote class name.
     * @param userClass The User (sub)class. It must have a public no-argument constructor.
     */
    public UserRepository(String className, Class<U> userClass) {
        super(className, null, userClass);
    }

    /**
     * Creates a new UserRepository, associating it with
     * the static {@code U} user class and the user class name.
     * @param className The remote class name.
     * @param nameForRestUrl The pluralized class name to use in REST transport.
     *                       Use {@code null} for the default value, which is the plural
     *                       form of className.
     * @param userClass The User (sub)class. It must have a public no-argument constructor.
     */
    public UserRepository(String className, String nameForRestUrl, Class<U> userClass) {
        super(className, nameForRestUrl, userClass);
    }

    /**
     * Callback passed to loginUser to receive success and newly created
     * {@code U} user instance or thrown error.
     */
    public interface LoginCallback<U> {
        public void onSuccess(AccessToken token, U currentUser);
        public void onError(Throwable t);
    }

    /**
     * Creates a {@code U} user instance given an email and a password.
     * @param email
     * @param password
     * @return A {@code U} user instance.
     */
    public U createUser(String email, String password)     {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("email", email);
        map.put("password", password);
        U user = createObject(map);
        return user;
    }

     /**
     * Creates a {@link com.strongloop.android.remoting.adapters.RestContract} representing the user type's custom
     * routes. Used to extend an {@link com.strongloop.android.remoting.adapters.Adapter} to support user. Calls
     * super {@link ModelRepository} createContract first.
     *
     * @return A {@link com.strongloop.android.remoting.adapters.RestContract} for this model type.
     */

    public RestContract createContract() {
        RestContract contract = super.createContract();

        String className = getClassName();

        contract.addItem(new RestContractItem("/" + getNameForRestUrl() + "/login?include=user", "POST"),
                className + ".login");
        contract.addItem(new RestContractItem("/" + getNameForRestUrl() + "/logout", "GET"),
                className + ".logout");
        return contract;
    }

    /**
     * Creates a new {@code U} user given the email, password and optional parameters.
     * @param email - user email
     * @param password - user password
     * @param parameters - optional parameters
     * @return A new {@code U} user instance.
     */
    public U createUser(String email, String password,
            Map<String, ? extends Object> parameters) {

        HashMap<String, Object> allParams = new HashMap<String, Object>();
        allParams.putAll(parameters);
        allParams.put("email", email);
        allParams.put("password", password);
        U user = createObject(allParams);

        return user;
    }

    /**
     * Login a user given an email and password.
     * Creates a {@link AccessToken} and {@code U} user models if successful.
     * @param email - user email
     * @param password - user password
     * @param callback - success/error callback
     */
    public void loginUser(String email, String password,
            final LoginCallback<U> callback) {

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("email",  email);
        params.put("password",  password);

        invokeStaticMethod("login", params,
                new Adapter.JsonObjectCallback() {

                    @Override
                    public void onError(Throwable t) {
                        callback.onError(t);
                    }

                    @Override
                    public void onSuccess(JSONObject response) {
                        AccessToken token = getAccessTokenRepository()
                                .createObject(JsonUtil.fromJson(response));
                        getRestAdapter().setAccessToken(token.getId().toString());

                        JSONObject userJson = response.optJSONObject("user");
                        U user = userJson != null
                                ? createObject(JsonUtil.fromJson(userJson))
                                : null;

                        callback.onSuccess(token, user);
                    }
                });
    }

    /**
     * Logs the current user out of the server and removes the access
     * token from the system.
     * <p>
     * @param callback The callback to be executed when finished.
     */

    public void logout(final VoidCallback callback) {

        invokeStaticMethod("logout", null,
                new Adapter.Callback() {

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }

            @Override
            public void onSuccess(String response) {
                RestAdapter radapter = (RestAdapter)getAdapter();
                radapter.clearAccessToken();
                callback.onSuccess();
            }
        });
    }

}