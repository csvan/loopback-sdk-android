package com.strongloop.android.loopback;

import com.strongloop.android.loopback.callbacks.TypedCallback;
import org.atteo.evo.inflector.English;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    private final RestAdapter restAdapter;
    private UserRepository<User> userRepository;
    private User currentUser;

    public static LoopbackInterface getDefault(String url) {
        return new LoopbackInterface(url);
    }

    private LoopbackInterface(String url) {
        this.url = url;

        this.restAdapter = new RestAdapter(url);
    }

    /**
     * Asynchronously creates an authenticated session for the Loopback server handled by this interface.
     *
     * @param username username/email of the user on the server
     * @param password plainttext password for the user on the server
     * @param callback callback upon finishing
     */
    public void loginAsync(String username, String password, final TypedCallback<User> callback) {
        this.userRepository.loginUser(username, password, new UserRepository.LoginCallback<User>() {
            @Override
            public void onSuccess(AccessToken token, User currentUser) {
                setCurrentUser(currentUser);
                callback.onSuccess(currentUser);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    /**
     * Synchronously creates an authenticated session for the Loopback server handled by this interface.
     *
     * @param username username/email of the user on the server
     * @param password plainttext password for the user on the server
     * @return true if login succeeded, false otherwise
     */
    public boolean loginSynchronously(String username, String password) throws LoopbackAuthenticationException {
        if (this.userRepository == null) {
            this.userRepository = new UserRepository<User>("User", "users", User.class);
        }

        final CountDownLatch synchLatch = new CountDownLatch(1);

        this.userRepository.loginUser(username, password, new UserRepository.LoginCallback<User>() {
            @Override
            public void onSuccess(AccessToken token, User currentUser) {
                setCurrentUser(currentUser);
                synchLatch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                synchLatch.countDown();
            }
        });

        while (true) {
            try {
                synchLatch.await(10, TimeUnit.SECONDS);

                // We timed out!
                if (synchLatch.getCount() == 0 && getCurrentUser() == null) {
                    throw new LoopbackAuthenticationException("Login attempt timed out");
                }

                // Finished, but failed to log user in
                else if (getCurrentUser() == null) {
                    throw new LoopbackAuthenticationException("Login attempt failed, please check username and password");
                }

                //Succesfully logged the user in
                else {
                    return true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * @return the User currently logged in to the Loopback instance.
     */
    public User getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Gets a repository for the given model.
     *
     * @param modelType
     * @param <M>
     * @return a repository for the model of type M
     */
    public <M extends Model> ModelRepository<M> getRepositoryForModel(Class<M> modelType) {
        String modelName = modelType.getSimpleName();
        ModelRepository<M> repository = restAdapter.createRepository(modelName, English.plural(modelName), modelType);
        repository.setAdapter(restAdapter);
        return repository;
    }
}
