package com.strongloop.android.loopback;

/**
 * Provides a unified API for working with a given Loopback model.
 * <p/>
 * Created by christopher on 30/01/15.
 */
public class ModelInterface<T extends Model> {

    private final ModelRepository<T> repository;

    private ModelInterface(ModelRepository<T> repository) {
        this.repository = repository;
    }
}
