package org.com.dev.carvel;

import org.com.dev.carvel.query.Operator;
import org.com.dev.carvel.repository.Repository;
import org.com.dev.carvel.user.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Main {

    public static void main (String[] args) throws Exception {

        Connection connection = DriverManager.getConnection("jdbc:h2:mem:testdb");
        Repository<User> repository = new Repository<>(User.class, connection);

        repository.createTable();

        User user1 = new User(0, "Carvel", "carvel@gmail.com", null, null);
        User user2 = new User(0, "Joao", "joao@gmail.com", null, null);
        User user3 = new User(0, "Carvel", "carvel2@gmail.com", null, null);

        repository.save(user1);
        repository.save(user2);
        repository.save(user3);

        System.out.println("IDs generated:");
        System.out.println(user1.getId());
        System.out.println(user2.getId());
        System.out.println(user3.getId());

        System.out.println();
        System.out.println("ID > 1:");

        List<User> greaterThan = repository.findBy("id", 1, Operator.GREATER_THAN);

        for (User user : greaterThan) {

            System.out.println("ID: " + user.getId() + " | Name: " + user.getName() + " | Email: " + user.getEmail());
        }

        System.out.println();
        System.out.println("ID <= 2:");

        List<User> lessThanOrEqual = repository.findBy("id", 2, Operator.LESS_THAN_OR_EQUAL);

        for (User user : lessThanOrEqual) {

            System.out.println("ID: " + user.getId() + " | Name: " + user.getName() + " | Email: " + user.getEmail());
        }

        connection.close();
    }
}
