package com.matheusgn.ecommerce.sales.support;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartItem;
import com.matheusgn.ecommerce.sales.entity.CartStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

public final class SalesTestFixtures {

    public static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID BOOK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private SalesTestFixtures() {
    }

    public static Customer customer() {
        return Customer.builder().id(CUSTOMER_ID).build();
    }

    public static Book book(BigDecimal price) {
        return Book.builder()
                .id(BOOK_ID)
                .title("Fixture Book")
                .isbn("9780000000999")
                .salePrice(price)
                .stockQuantity(50)
                .active(true)
                .build();
    }

    public static Cart openCartWithOneItem(Customer c, Book b, BigDecimal lineTotal) {
        Cart cart = Cart.builder()
                .id(UUID.randomUUID())
                .customer(c)
                .status(CartStatus.OPEN)
                .totalAmount(lineTotal)
                .items(new ArrayList<>())
                .paymentLines(new ArrayList<>())
                .build();
        cart.getItems().add(CartItem.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .book(b)
                .quantity(1)
                .unitPrice(b.getSalePrice())
                .totalPrice(lineTotal)
                .build());
        return cart;
    }
}
