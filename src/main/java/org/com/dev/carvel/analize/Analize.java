package org.com.dev.carvel.analize;

import org.com.dev.carvel.annotations.Entity;

import java.lang.reflect.Field;

public class Analize {

    public void analize (Class value) {
        Field[] fields = value.getDeclaredFields();

        for (Field field : fields) {
            System.out.println("Field name: " + field.getName());
            System.out.println("Field type: " + field.getType());


        }
        if (value.isAnnotationPresent(Entity.class)) {
            System.out.println("Contains @Entity");
        }
    }
}
