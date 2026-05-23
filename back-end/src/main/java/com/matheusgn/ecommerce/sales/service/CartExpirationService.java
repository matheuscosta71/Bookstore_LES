package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.config.CartItemProperties;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class CartExpirationService {

    private final CartItemProperties cartItemProperties;

    public boolean isExpired(CartItem item) {
        return isExpired(item, cartItemProperties.getExpirationMinutes());
    }

    public boolean isExpired(CartItem item, int expirationMinutes) {
        Instant ref = item.getLastActivityAt() != null ? item.getLastActivityAt() : item.getCreatedAt();
        if (ref == null) {
            return false;
        }
        Instant deadline = ref.plus(expirationMinutes, ChronoUnit.MINUTES);
        return deadline.isBefore(Instant.now());
    }

    public boolean hasBlockingExpiredItems(Cart cart) {
        int minutes = cartItemProperties.getExpirationMinutes();
        return cart.getItems().stream().anyMatch(ci -> isExpired(ci, minutes));
    }
}
