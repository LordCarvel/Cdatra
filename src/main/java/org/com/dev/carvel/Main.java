package org.com.dev.carvel;

import org.com.dev.carvel.repository.Repository;
import org.com.dev.carvel.user.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

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

        User user1 =
                new User(
                        0,
                        "Carvel",
                        "carvel@gmail.com",
                        null,
                        null
                );

        User user2 =
                new User(
                        0,
                        "Joao",
                        "joao@gmail.com",
                        null,
                        null
                );

        User user3 =
                new User(
                        0,
                        "Carvel",
                        "carvel2@gmail.com",
                        null,
                        null
                );

        repository.save(user1);
        repository.save(user2);
        repository.save(user3);

        List<User> users =
                repository.findBy(
                        "name",
                        "Carvel"
                );

        for (User user : users) {

            System.out.println(
                    "ID: " + user.getId()
                            + " | Name: " + user.getName()
                            + " | Email: " + user.getEmail()
            );
        }

        connection.close();
    }
}