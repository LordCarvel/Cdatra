package org.com.dev.carvel.analize;

import java.lang.reflect.Field;

public class Analize {

    public void analize (Class value) {
        Field[] fields = value.getDeclaredFields();

        for (Field field : fields) {
            System.out.println("Field name: " + field.getName());
            System.out.println("Field type: " + field.getType().getSimpleName());
        }
    }
}
