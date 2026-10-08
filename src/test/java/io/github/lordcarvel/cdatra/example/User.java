package io.github.lordcarvel.cdatra.example;

import io.github.lordcarvel.cdatra.annotation.Column;
import io.github.lordcarvel.cdatra.annotation.Entity;
import io.github.lordcarvel.cdatra.annotation.GeneratedValue;
import io.github.lordcarvel.cdatra.annotation.Id;

@Entity(tableNaame = "users")
public class User {

    @Id
    @GeneratedValue
    private @Column(columName = "id") int id;
    protected @Column(columName = "name") String name;
    protected @Column(columName = "user_email") String email;
    private Address address;
    protected String temporary;

    public User () {

    }

    public User (int id, String name, String email, Address address, String temporary) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.temporary = temporary;
    }

    public int getId () {

        return id;
    }

    public String getName () {

        return name;
    }

    public String getEmail () {

        return email;
    }

    public Address getAddress () {

        return address;
    }

    public String getTemporary () {

        return temporary;
    }
}
