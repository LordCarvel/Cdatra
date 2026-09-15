package org.com.dev.carvel.user;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.annotations.Colum;
import org.com.dev.carvel.annotations.Entity;

@Entity(tableNaame = "users")
public class User {

    private @Colum(columName = "id") int id;
    protected @Colum(columName = "name") String name;
    protected @Colum(columName = "email") String email;
    private @Colum(columName = "address ") Address address;
    protected String temporary;

    public User (int id, String name, String email, Address address, String temporary) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.temporary = temporary;
    }
}