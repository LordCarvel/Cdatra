package io.github.lordcarvel.cdatra.mapping;

import java.lang.reflect.Field;

public class Container<T> {

    private T value;

    public Container (T value) {

        this.value = value;
    }

    public T getValue () {

        return value;
    }

    public void setValue (T value) {

        this.value = value;
    }
}
