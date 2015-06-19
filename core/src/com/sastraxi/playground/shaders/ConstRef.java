package com.sastraxi.playground.shaders;

/**
 * A reference to a static field in a class.
 */
public class ConstRef {

    private final Class k;
    private final String name;

    public ConstRef(Class k, String name) {
        this.k = k;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getKlass() {
        return k;
    }
}