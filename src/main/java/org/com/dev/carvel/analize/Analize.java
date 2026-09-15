package org.com.dev.carvel.analize;

import org.com.dev.carvel.annotations.Colum;
import org.com.dev.carvel.annotations.Entity;

import java.lang.reflect.Field;

public class Analize {

    public void analize (Class value) {
        Field[] fields = value.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Colum.class)) {
                Colum colum = field.getAnnotation(Colum.class);

                System.out.println("Field name: " + field.getName() + "|| Colum name: " + colum.columName());
            }
        }
        if (value.isAnnotationPresent(Entity.class)) {
            Entity entity = (Entity) value.getAnnotation(Entity.class);

            System.out.println("Contains @Entity, the table name is: " + entity.tableNaame());

        }
    }
}
