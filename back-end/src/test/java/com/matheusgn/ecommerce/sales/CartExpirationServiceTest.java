package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.config.CartItemProperties;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartItem;
import com.matheusgn.ecommerce.sales.entity.CartStatus;
import com.matheusgn.ecommerce.sales.service.CartExpirationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartExpirationServiceTest {

    @Test
    void marksExpiredBasedOnLastActivity() {
        CartItemProperties props = new CartItemProperties();
        props.setExpirationMinutes(30);
        CartExpirationService svc = new CartExpirationService(props);

        Book book = Book.builder()
                .id(UUID.randomUUID())
                .title("B")
                .isbn("9780000000001")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(1)
                .active(true)
                .build();
        Cart cart = Cart.builder()
                .id(UUID.randomUUID())
                .customer(Customer.builder().id(UUID.randomUUID()).build())
                .status(CartStatus.OPEN)
                .totalAmount(BigDecimal.TEN)
                .build();
        CartItem fresh = CartItem.builder()
                .cart(cart)
                .book(book)
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .totalPrice(BigDecimal.TEN)
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .build();
        CartItem stale = CartItem.builder()
                .cart(cart)
                .book(book)
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .totalPrice(BigDecimal.TEN)
                .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .lastActivityAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build();
        cart.setItems(List.of(fresh, stale));

        assertThat(svc.isExpired(fresh)).isFalse();
        assertThat(svc.isExpired(stale)).isTrue();
        assertThat(svc.hasBlockingExpiredItems(cart)).isTrue();
    }
}
