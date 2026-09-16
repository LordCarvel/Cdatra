package org.com.dev.carvel.user;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.annotations.Column;
import org.com.dev.carvel.annotations.Entity;

@Entity(tableNaame = "users")
public class User {

    private @Column(columName = "id") int id;
    protected @Column(columName = "name") String name;
    protected @Column(columName = "user_email") String email;
    private Address address;
    protected String temporary;

    public User (int id, String name, String email, Address address, String temporary) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.temporary = temporary;
    }
}