package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.config.CartItemProperties;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.dto.CartItemResponse;
import com.matheusgn.ecommerce.sales.dto.CartResponse;
import com.matheusgn.ecommerce.sales.dto.CartUpsertItemRequest;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartItem;
import com.matheusgn.ecommerce.sales.entity.CartStatus;
import com.matheusgn.ecommerce.sales.repository.CartItemRepository;
import com.matheusgn.ecommerce.sales.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;
    private final InventoryService inventoryService;
    private final CartExpirationService cartExpirationService;
    private final CartItemProperties cartItemProperties;

    @Transactional
    public CartResponse getCart(UUID customerId) {
        Cart cart = getOrCreateOpenCartEntity(customerId);
        backfillCartItemTimestamps(cart);
        releaseExpiredReservationsSoft(cart);
        List<String> stockMessages = reconcileCartWithStock(cart);
        cartRepository.save(cart);
        return toResponse(cart, stockMessages, List.of());
    }

    /**
     * RN0032 / RN0044: antes de finalizar, alinha carrinho com estoque e expiração (mesma lógica do GET).
     */
    @Transactional
    public void prepareCartForCheckout(UUID customerId) {
        Cart cart = getOpenCartOrThrow(customerId);
        backfillCartItemTimestamps(cart);
        releaseExpiredReservationsSoft(cart);
        reconcileCartWithStock(cart);
        cartRepository.save(cart);
    }

    @Transactional
    public CartResponse addItem(UUID customerId, CartUpsertItemRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        Cart cart = getOrCreateOpenCartEntity(customerId);
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));

        inventoryService.assertAvailableStock(book.getId(), request.getQuantity());

        CartItem existing = cart.getItems().stream()
                .filter(ci -> ci.getBook().getId().equals(book.getId()))
                .findFirst()
                .orElse(null);

        BigDecimal unit = book.getSalePrice();
        Instant now = Instant.now();
        if (existing != null) {
            int newQty = existing.getQuantity() + request.getQuantity();
            inventoryService.assertAvailableStock(book.getId(), newQty);
            existing.setQuantity(newQty);
            existing.setUnitPrice(unit);
            existing.setTotalPrice(unit.multiply(BigDecimal.valueOf(newQty)).setScale(2, RoundingMode.HALF_UP));
            touchActivity(existing, now);
            boolean released = existing.isReservationReleased();
            if (released) {
                existing.setReservationReleased(false);
                inventoryService.adjustReservationForCart(book.getId(), newQty);
            } else {
                inventoryService.adjustReservationForCart(book.getId(), request.getQuantity());
            }
        } else {
            CartItem line = CartItem.builder()
                    .cart(cart)
                    .book(book)
                    .quantity(request.getQuantity())
                    .unitPrice(unit)
                    .totalPrice(unit.multiply(BigDecimal.valueOf(request.getQuantity())).setScale(2, RoundingMode.HALF_UP))
                    .createdAt(now)
                    .lastActivityAt(now)
                    .build();
            cart.getItems().add(line);
            inventoryService.adjustReservationForCart(book.getId(), request.getQuantity());
        }
        recalculate(cart);
        return toResponse(cartRepository.save(cart), List.of(), List.of());
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID customerId, UUID itemId, CartUpsertItemRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        Cart cart = getOpenCartOrThrow(customerId);
        CartItem item = cartItemRepository.findByIdAndCart_Id(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item do carrinho não encontrado"));

        if (cartExpirationService.isExpired(item, cartItemProperties.getExpirationMinutes())) {
            throw new IllegalArgumentException(
                    "Item expirado; adicione novamente ao carrinho ou remova a linha para continuar.");
        }

        inventoryService.assertAvailableStock(item.getBook().getId(), request.getQuantity());

        int delta = request.getQuantity() - item.getQuantity();
        inventoryService.adjustReservationForCart(item.getBook().getId(), delta);

        item.setQuantity(request.getQuantity());
        item.setUnitPrice(item.getBook().getSalePrice());
        item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP));
        touchActivity(item, Instant.now());
        recalculate(cart);
        return toResponse(cartRepository.save(cart), List.of(), List.of());
    }

    @Transactional
    public CartResponse removeItem(UUID customerId, UUID itemId) {
        Cart cart = getOpenCartOrThrow(customerId);
        CartItem item = cartItemRepository.findByIdAndCart_Id(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item do carrinho não encontrado"));
        if (!item.isReservationReleased()) {
            inventoryService.adjustReservationForCart(item.getBook().getId(), -item.getQuantity());
        }
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        recalculate(cart);
        return toResponse(cartRepository.save(cart), List.of(), List.of());
    }

    public Cart getOpenCartOrThrow(UUID customerId) {
        return cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("Carrinho aberto não encontrado"));
    }

    private Cart getOrCreateOpenCartEntity(UUID customerId) {
        return cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)
                .orElseGet(() -> {
                    Customer c = customerRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
                    Cart cart = Cart.builder()
                            .customer(c)
                            .status(CartStatus.OPEN)
                            .totalAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                            .build();
                    return cartRepository.save(cart);
                });
    }

    private void backfillCartItemTimestamps(Cart cart) {
        boolean changed = false;
        Instant now = Instant.now();
        for (CartItem ci : cart.getItems()) {
            if (ci.getLastActivityAt() == null) {
                ci.setCreatedAt(now);
                ci.setLastActivityAt(now);
                changed = true;
            }
        }
        if (changed) {
            cartRepository.save(cart);
        }
    }

    /**
     * Expiração soft: mantém linhas no banco; libera reserva de estoque uma vez por item expirado.
     */
    private void releaseExpiredReservationsSoft(Cart cart) {
        int minutes = cartItemProperties.getExpirationMinutes();
        for (CartItem ci : cart.getItems()) {
            if (cartExpirationService.isExpired(ci, minutes)) {
                if (!ci.isReservationReleased()) {
                    inventoryService.adjustReservationForCart(ci.getBook().getId(), -ci.getQuantity());
                    ci.setReservationReleased(true);
                }
            } else if (ci.isReservationReleased()) {
                ci.setReservationReleased(false);
            }
        }
    }

    /** RN0032: ajusta quantidades e remove itens conforme estoque vendável. */
    private List<String> reconcileCartWithStock(Cart cart) {
        int expirationMinutes = cartItemProperties.getExpirationMinutes();
        List<String> messages = new ArrayList<>();
        List<CartItem> toRemove = new ArrayList<>();
        for (CartItem ci : new ArrayList<>(cart.getItems())) {
            Book book = ci.getBook();
            UUID bookId = book.getId();
            boolean lineExpired = cartExpirationService.isExpired(ci, expirationMinutes);
            if (lineExpired) {
                if (!book.isActive()) {
                    messages.add(String.format(
                            "O livro \"%s\" não está mais disponível e foi removido do carrinho.", book.getTitle()));
                    if (!ci.isReservationReleased()) {
                        inventoryService.adjustReservationForCart(bookId, -ci.getQuantity());
                    }
                    toRemove.add(ci);
                }
                continue;
            }
            if (!book.isActive()) {
                messages.add(String.format(
                        "O livro \"%s\" não está mais disponível e foi removido do carrinho.", book.getTitle()));
                inventoryService.adjustReservationForCart(bookId, -ci.getQuantity());
                toRemove.add(ci);
                continue;
            }
            int sellable = inventoryService.getSellableQuantity(bookId);
            int maxForLine = sellable + ci.getQuantity();
            if (maxForLine <= 0) {
                messages.add(String.format(
                        "O livro \"%s\" está sem estoque e foi removido do carrinho.", book.getTitle()));
                inventoryService.adjustReservationForCart(bookId, -ci.getQuantity());
                toRemove.add(ci);
                continue;
            }
            if (ci.getQuantity() > maxForLine) {
                int newQty = maxForLine;
                int delta = newQty - ci.getQuantity();
                messages.add(String.format(
                        "A quantidade do livro \"%s\" foi ajustada de %d para %d devido ao estoque disponível.",
                        book.getTitle(), ci.getQuantity(), newQty));
                inventoryService.adjustReservationForCart(bookId, delta);
                ci.setQuantity(newQty);
                ci.setUnitPrice(book.getSalePrice());
                ci.setTotalPrice(book.getSalePrice()
                        .multiply(BigDecimal.valueOf(newQty))
                        .setScale(2, RoundingMode.HALF_UP));
                touchActivity(ci, Instant.now());
            }
        }
        for (CartItem ci : toRemove) {
            cart.getItems().remove(ci);
            cartItemRepository.delete(ci);
        }
        recalculate(cart);
        return messages;
    }

    private void touchActivity(CartItem item, Instant now) {
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(now);
        }
        item.setLastActivityAt(now);
    }

    private void recalculate(Cart cart) {
        int minutes = cartItemProperties.getExpirationMinutes();
        BigDecimal sum = cart.getItems().stream()
                .filter(ci -> !cartExpirationService.isExpired(ci, minutes))
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        cart.setTotalAmount(sum);
    }

    private CartResponse toResponse(
            Cart cart, List<String> stockAdjustmentMessages, List<String> reservationExpiredMessages) {
        int minutes = cartItemProperties.getExpirationMinutes();
        int warnMin = cartItemProperties.getWarningBeforeExpirationMinutes();
        Instant now = Instant.now();
        var items = cart.getItems().stream()
                .map(ci -> {
                    boolean expired = cartExpirationService.isExpired(ci, minutes);
                    boolean expiringSoon = computeExpiringSoon(ci, minutes, warnMin, expired, now);
                    return toItemResponse(ci, expired, expiringSoon, minutes);
                })
                .toList();
        boolean hasExpired = items.stream().anyMatch(CartItemResponse::isExpired);
        List<String> expiredMsgs = reservationExpiredMessages != null ? reservationExpiredMessages : List.of();
        return CartResponse.builder()
                .id(cart.getId())
                .status(cart.getStatus())
                .totalAmount(cart.getTotalAmount())
                .freightAmount(cart.getFreightAmount())
                .deliveryAddressId(cart.getDeliveryAddress() != null ? cart.getDeliveryAddress().getId() : null)
                .items(items)
                .itemExpirationMinutes(minutes)
                .expirationWarningMinutes(warnMin)
                .hasExpiredItems(hasExpired)
                .checkoutAllowed(!hasExpired)
                .stockAdjustmentMessages(stockAdjustmentMessages != null ? stockAdjustmentMessages : List.of())
                .reservationExpiredMessages(expiredMsgs)
                .build();
    }

    private static boolean computeExpiringSoon(
            CartItem ci, int expirationMinutes, int warningMinutes, boolean expired, Instant now) {
        if (expired || warningMinutes <= 0) {
            return false;
        }
        Instant ref = ci.getLastActivityAt() != null ? ci.getLastActivityAt() : ci.getCreatedAt();
        if (ref == null) {
            return false;
        }
        Instant deadline = ref.plus(expirationMinutes, ChronoUnit.MINUTES);
        Instant warnFrom = deadline.minus(warningMinutes, ChronoUnit.MINUTES);
        return !now.isBefore(warnFrom) && now.isBefore(deadline);
    }

    private CartItemResponse toItemResponse(
            CartItem ci, boolean expired, boolean expiringSoon, int expirationMinutes) {
        Instant ref = ci.getLastActivityAt() != null ? ci.getLastActivityAt() : ci.getCreatedAt();
        Instant expiresAt = ref != null ? ref.plus(expirationMinutes, ChronoUnit.MINUTES) : null;
        return CartItemResponse.builder()
                .id(ci.getId())
                .bookId(ci.getBook().getId())
                .title(ci.getBook().getTitle())
                .quantity(ci.getQuantity())
                .unitPrice(ci.getUnitPrice())
                .totalPrice(ci.getTotalPrice())
                .expired(expired)
                .purchaseDisabled(expired)
                .expiringSoon(expiringSoon)
                .expiresAt(expiresAt)
                .build();
    }
}
