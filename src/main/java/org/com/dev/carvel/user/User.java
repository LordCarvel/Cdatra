package org.com.dev.carvel.user;

public class User {
    private int id;
    protected String name;
    protected String email;

    public User (int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
}