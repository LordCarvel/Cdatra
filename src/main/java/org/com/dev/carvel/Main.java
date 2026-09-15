package org.com.dev.carvel;

import org.com.dev.carvel.container.Container;
import org.com.dev.carvel.user.User;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        Container<Integer> id = new Container<>(1);
        Container<String> name = new Container<>("Carvel");
        Container<String> email = new Container<>("carveltest1@gmail.com");

        User user = new User(id.getValue(), name.getValue(), email.getValue());

        Field[] fields = User.class.getDeclaredFields();

        for (Field field : fields) {
            System.out.println("Field name: " + field.getName());
        }
    }
}