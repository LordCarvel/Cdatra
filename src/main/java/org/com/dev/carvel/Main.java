package org.com.dev.carvel;

import org.com.dev.carvel.query.LogicalOperator;
import org.com.dev.carvel.query.Operator;
import org.com.dev.carvel.query.QueryCondition;
import org.com.dev.carvel.query.QueryFilter;
import org.com.dev.carvel.repository.Repository;
import org.com.dev.carvel.user.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main (String[] args) throws Exception {

        Connection connection = DriverManager.getConnection("jdbc:h2:mem:testdb");

        Repository<User> repository = new Repository<>(User.class, connection);

        repository.createTable();

        User user1 = new User(0, "Carvel", "carvel@gmail.com", null, null);
        User user2 = new User(0, "Joao", "joao@gmail.com", null, null);
        User user3 = new User(0, "Carvel", "carvel2@gmail.com", null, null);
        User user4 = new User(0, "Maria", "maria@gmail.com", null, null);

        repository.save(user1);
        repository.save(user2);
        repository.save(user3);
        repository.save(user4);

        List<QueryFilter> andFilters = new ArrayList<>();

        QueryCondition andCondition1 = new QueryCondition("id", 1, Operator.GREATER_THAN);
        QueryCondition andCondition2 = new QueryCondition("name", "Carvel", Operator.EQUAL);

        andFilters.add(new QueryFilter(andCondition1, null));
        andFilters.add(new QueryFilter(andCondition2, LogicalOperator.AND));

        List<User> andUsers = repository.findByFilters(andFilters);

        System.out.println("AND:");

        for (User user : andUsers) {

            System.out.println(
                    "ID: " + user.getId()
                            + " | Name: " + user.getName()
                            + " | Email: " + user.getEmail()
            );
        }

        List<QueryFilter> orFilters = new ArrayList<>();

        QueryCondition orCondition1 = new QueryCondition("id", 1, Operator.EQUAL);
        QueryCondition orCondition2 = new QueryCondition("name", "Maria", Operator.EQUAL);

        orFilters.add(new QueryFilter(orCondition1, null));
        orFilters.add(new QueryFilter(orCondition2, LogicalOperator.OR));

        List<User> orUsers = repository.findByFilters(orFilters);

        System.out.println();
        System.out.println("OR:");

        for (User user : orUsers) {

            System.out.println(
                    "ID: " + user.getId()
                            + " | Name: " + user.getName()
                            + " | Email: " + user.getEmail()
            );
        }

        connection.close();
    }
}