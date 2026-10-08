package org.com.dev.carvel;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.repository.Repository;
import org.com.dev.carvel.user.User;

import java.sql.Connection;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        DatabaseConnection databaseConnection =
                new DatabaseConnection();

        Connection connection =
                databaseConnection.connection(
                        "jdbc:h2:mem:cdatra",
                        "sa",
                        ""
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
                        new Address(
                                "Some street",
                                67,
                                "Some city"
                        ),
                        "temporary"
                );

        User user2 =
                new User(
                        1,
                        "João",
                        "joao@gmail.com",
                        new Address(
                                "Another street",
                                20,
                                "Another city"
                        ),
                        "temporary2"
                );

        repository.save(user1);
        repository.save(user2);

        System.out.println("Before update:");

        List<User> usersBeforeUpdate =
                repository.findAll();

        for (User user : usersBeforeUpdate) {

            System.out.println(
                    user.getId()
                            + " | "
                            + user.getName()
                            + " | "
                            + user.getEmail()
            );
        }

        User updatedUser =
                new User(
                        1,
                        "João Atualizado",
                        "joao.novo@gmail.com",
                        new Address(
                                "Updated street",
                                50,
                                "Updated city"
                        ),
                        "temporary updated"
                );

        repository.update(updatedUser);

        System.out.println();
        System.out.println("After update:");

        List<User> usersAfterUpdate =
                repository.findAll();

        for (User user : usersAfterUpdate) {

            System.out.println(
                    user.getId()
                            + " | "
                            + user.getName()
                            + " | "
                            + user.getEmail()
            );
        }

        repository.delete(user2);

        List<User> usersAfterDelete = repository.findAll();

        System.out.println("After delete:");

        for (User user : usersAfterDelete) {

            System.out.println(
                    user.getId()
                            + " | "
                            + user.getName()
                            + " | "
                            + user.getEmail()
            );
        }

        connection.close();
    }
}