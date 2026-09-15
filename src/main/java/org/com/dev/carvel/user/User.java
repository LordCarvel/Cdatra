package org.com.dev.carvel.user;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.annotations.Entity;

@Entity(tableNaame = "users")
public class User {
    private int id;
    protected String name;
    protected String email;
    private Address address;

    public User (int id, String name, String email, Address address) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
    }
}