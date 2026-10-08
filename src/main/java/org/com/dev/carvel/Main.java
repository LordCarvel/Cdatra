package org.com.dev.carvel;

import org.com.dev.carvel.repository.Repository;
import org.com.dev.carvel.user.User;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

    public static void main(String[] args) throws Exception {

        Connection connection =
                DriverManager.getConnection(
                        "jdbc:h2:mem:testdb"
                );

        Repository<User> repository =
                new Repository<>(
                        User.class,
                        connection
                );

        repository.createTable();

        User user =
                new User(
                        0,
                        "Carvel",
                        "carvel@gmail.com",
                        null,
                        null
                );

        System.out.println(
                "ID before save: " + user.getId()
        );

        repository.save(user);

        System.out.println(
                "ID after save: " + user.getId()
        );

        User userFromDatabase =
                repository.findById(
                        user.getId()
                );

        System.out.println(
                "Database user:"
        );

        System.out.println(
                "ID: " + userFromDatabase.getId()
                        + " | Name: " + userFromDatabase.getName()
                        + " | Email: " + userFromDatabase.getEmail()
        );

        connection.close();
    }
}