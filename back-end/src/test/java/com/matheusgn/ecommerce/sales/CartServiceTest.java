package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.config.CartItemProperties;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.dto.CartUpsertItemRequest;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartItem;
import com.matheusgn.ecommerce.sales.entity.CartStatus;
import com.matheusgn.ecommerce.sales.repository.CartItemRepository;
import com.matheusgn.ecommerce.sales.repository.CartRepository;
import com.matheusgn.ecommerce.sales.service.CartExpirationService;
import com.matheusgn.ecommerce.sales.service.CartService;
import com.matheusgn.ecommerce.sales.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link com.matheusgn.ecommerce.sales.service.CartService}.
 * <p>
 * Transições de pedido, pagamento, cupom e troca não estão neste serviço — ver {@code CheckoutService},
 * {@code CouponService}, {@code ExchangeService}, etc.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private CartExpirationService cartExpirationService;
    @Mock
    private CartItemProperties cartItemProperties;

    @InjectMocks
    private CartService cartService;

    private UUID customerId;
    private UUID bookId;
    private UUID otherBookId;
    private Customer customer;
    private Book book;

    @BeforeEach
    void setUp() {
        customerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        bookId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        otherBookId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        customer = Customer.builder().id(customerId).build();
        book = Book.builder()
                .id(bookId)
                .title("Livro")
                .salePrice(new BigDecimal("10.00"))
                .isbn("9780000000000")
                .stockQuantity(5)
                .active(true)
                .build();
        org.mockito.Mockito.lenient().when(cartItemProperties.getExpirationMinutes()).thenReturn(30);
        org.mockito.Mockito.lenient().when(cartItemProperties.getWarningBeforeExpirationMinutes()).thenReturn(5);
        org.mockito.Mockito.lenient().when(cartExpirationService.isExpired(any(CartItem.class), anyInt()))
                .thenReturn(false);
        org.mockito.Mockito.lenient().when(inventoryService.getSellableQuantity(any())).thenReturn(100);
    }

    private Cart openCart() {
        return Cart.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .status(CartStatus.OPEN)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("RF0031 — Gerenciar carrinho")
    class Rf0031 {

        @Test
        @DisplayName("givenNoOpenCart_whenAddItem_thenCreatesCartAndSavesTwice")
        void givenNoOpenCart_whenAddItem_thenCreatesCartAndSavesTwice() {
            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.empty());
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartUpsertItemRequest req = CartUpsertItemRequest.builder().bookId(bookId).quantity(2).build();
            cartService.addItem(customerId, req);

            verify(cartRepository, times(2)).save(any(Cart.class));
        }

        @Test
        @DisplayName("givenOpenCartExists_whenAddItem_thenDoesNotLoadCustomerAgainForNewCart")
        void givenOpenCartExists_whenAddItem_thenDoesNotLoadCustomerAgainForNewCart() {
            Cart cart = openCart();
            Book book2 = Book.builder()
                    .id(otherBookId)
                    .title("Outro")
                    .salePrice(new BigDecimal("20.00"))
                    .isbn("9780000000003")
                    .stockQuantity(10)
                    .active(true)
                    .build();
            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.of(cart));
            when(bookRepository.findById(otherBookId)).thenReturn(Optional.of(book2));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            cartService.addItem(customerId, CartUpsertItemRequest.builder().bookId(otherBookId).quantity(1).build());

            verify(customerRepository, never()).findById(any());
        }

        @Test
        @DisplayName("givenUnknownBook_whenAddItem_thenThrows")
        void givenUnknownBook_whenAddItem_thenThrows() {
            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.of(openCart()));
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addItem(customerId, CartUpsertItemRequest.builder().bookId(bookId).quantity(1).build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("givenInsufficientStock_whenAddItem_thenThrows")
        void givenInsufficientStock_whenAddItem_thenThrows() {
            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.of(openCart()));
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            doThrow(new IllegalArgumentException("Estoque insuficiente"))
                    .when(inventoryService).assertAvailableStock(eq(bookId), eq(100));

            assertThatThrownBy(() -> cartService.addItem(customerId, CartUpsertItemRequest.builder().bookId(bookId).quantity(100).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Estoque");
        }

        @Test
        @DisplayName("givenItemInCart_whenAddItem_thenRecalculatesTotal")
        void givenItemInCart_whenAddItem_thenRecalculatesTotal() {
            Cart cart = openCart();
            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.of(cart));
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = cartService.addItem(customerId, CartUpsertItemRequest.builder().bookId(bookId).quantity(2).build());

            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(response.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("givenItem_whenRemoveItem_thenRecalculatesCartTotal")
        void givenItem_whenRemoveItem_thenRecalculatesCartTotal() {
            Cart cart = openCart();
            CartItem line = CartItem.builder()
                    .id(UUID.randomUUID())
                    .cart(cart)
                    .book(book)
                    .quantity(1)
                    .unitPrice(book.getSalePrice())
                    .totalPrice(new BigDecimal("10.00"))
                    .build();
            cart.getItems().add(line);
            cart.setTotalAmount(new BigDecimal("10.00"));

            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByIdAndCart_Id(line.getId(), cart.getId())).thenReturn(Optional.of(line));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = cartService.removeItem(customerId, line.getId());

            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
            verify(cartItemRepository).delete(line);
        }

        @Test
        @DisplayName("givenCartWithItems_whenGetCart_thenReturnsItems")
        void givenCartWithItems_whenGetCart_thenReturnsItems() {
            Cart cart = openCart();
            cart.getItems().add(CartItem.builder()
                    .id(UUID.randomUUID())
                    .cart(cart)
                    .book(book)
                    .quantity(1)
                    .unitPrice(book.getSalePrice())
                    .totalPrice(new BigDecimal("10.00"))
                    .build());
            cart.setTotalAmount(new BigDecimal("10.00"));
            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.of(cart));

            var response = cartService.getCart(customerId);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getTitle()).isEqualTo("Livro");
        }
    }

    @Nested
    @DisplayName("RF0032 — Quantidade")
    class Rf0032 {

        @Test
        @DisplayName("givenZeroQuantity_whenAddItem_thenThrows")
        void givenZeroQuantity_whenAddItem_thenThrows() {
            assertThatThrownBy(() -> cartService.addItem(customerId, CartUpsertItemRequest.builder().bookId(bookId).quantity(0).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantidade");

            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("givenNegativeQuantity_whenUpdateItem_thenThrows")
        void givenNegativeQuantity_whenUpdateItem_thenThrows() {
            assertThatThrownBy(() -> cartService.updateItemQuantity(customerId, UUID.randomUUID(),
                    CartUpsertItemRequest.builder().bookId(bookId).quantity(-1).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantidade");
            verify(cartRepository, never()).findByCustomer_IdAndStatus(any(), any());
        }

        @Test
        @DisplayName("givenInsufficientStock_whenUpdateItemQuantity_thenThrows")
        void givenInsufficientStock_whenUpdateItemQuantity_thenThrows() {
            Cart cart = openCart();
            UUID itemId = UUID.randomUUID();
            CartItem line = CartItem.builder()
                    .id(itemId)
                    .cart(cart)
                    .book(book)
                    .quantity(1)
                    .unitPrice(book.getSalePrice())
                    .totalPrice(new BigDecimal("10.00"))
                    .build();
            cart.getItems().add(line);
            cart.setTotalAmount(new BigDecimal("10.00"));

            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByIdAndCart_Id(itemId, cart.getId())).thenReturn(Optional.of(line));
            doThrow(new IllegalArgumentException("Estoque insuficiente para o livro: Livro"))
                    .when(inventoryService).assertAvailableStock(eq(bookId), eq(50));

            assertThatThrownBy(() -> cartService.updateItemQuantity(customerId, itemId,
                    CartUpsertItemRequest.builder().bookId(bookId).quantity(50).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Estoque");
            verify(inventoryService).assertAvailableStock(eq(bookId), eq(50));
        }

        @Test
        @DisplayName("givenLine_whenUpdateQuantity_thenRecalculatesLineAndCartTotals")
        void givenLine_whenUpdateQuantity_thenRecalculatesLineAndCartTotals() {
            Cart cart = openCart();
            UUID itemId = UUID.randomUUID();
            CartItem line = CartItem.builder()
                    .id(itemId)
                    .cart(cart)
                    .book(book)
                    .quantity(1)
                    .unitPrice(book.getSalePrice())
                    .totalPrice(new BigDecimal("10.00"))
                    .build();
            cart.getItems().add(line);
            cart.setTotalAmount(new BigDecimal("10.00"));

            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByIdAndCart_Id(itemId, cart.getId())).thenReturn(Optional.of(line));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = cartService.updateItemQuantity(customerId, itemId,
                    CartUpsertItemRequest.builder().bookId(bookId).quantity(3).build());

            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
            assertThat(response.getItems().get(0).getTotalPrice()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        }
    }

    @Nested
    @DisplayName("RF0033 — Carrinho aberto")
    class Rf0033 {

        @Test
        @DisplayName("givenNoOpenCart_whenGetOpenCartOrThrow_thenThrows")
        void givenNoOpenCart_whenGetOpenCartOrThrow_thenThrows() {
            when(cartRepository.findByCustomer_IdAndStatus(customerId, CartStatus.OPEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getOpenCartOrThrow(customerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
