package org.com.dev.carvel;

import org.com.dev.carvel.address.Address;
import org.com.dev.carvel.analize.Analize;
import org.com.dev.carvel.product.Product;
import org.com.dev.carvel.user.User;

public class Main {
    public static void main(String[] args) {

        User user = new User(1, "carvel", "carveltest1@gmail.com", new Address("rua josé perine", 0,"quatro barras"), "sla");
        Product product = new Product(1, "coffe");

        Analize analize = new Analize();

        analize.analize(user.getClass());
        analize.analize(product.getClass());
    }
}