package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.entity.AddressType;
import com.matheusgn.ecommerce.customer.entity.CreditCard;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.AddressRepository;
import com.matheusgn.ecommerce.customer.repository.CreditCardRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.service.CustomerAddressService;
import com.matheusgn.ecommerce.customer.service.CustomerCreditCardService;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.dto.CheckoutAddressRequest;
import com.matheusgn.ecommerce.sales.dto.CheckoutPaymentRequest;
import com.matheusgn.ecommerce.sales.dto.FreightRequest;
import com.matheusgn.ecommerce.sales.dto.FreightResponse;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.dto.PaymentLineRequest;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartItem;
import com.matheusgn.ecommerce.sales.entity.CartPaymentLine;
import com.matheusgn.ecommerce.sales.entity.CartStatus;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.entity.Payment;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.CartRepository;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final BigDecimal MIN_CREDIT_CARD_LINE_BRL = new BigDecimal("10.00");

    private final CartRepository cartRepository;
    private final CartService cartService;
    private final FreightService freightService;
    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressService customerAddressService;
    private final CreditCardRepository creditCardRepository;
    private final CustomerCreditCardService customerCreditCardService;
    private final CouponService couponService;
    private final SalesOrderRepository salesOrderRepository;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final CartExpirationService cartExpirationService;
    private final AuditLogService auditLogService;

    @Transactional
    public FreightResponse calculateFreight(UUID customerId, FreightRequest request) {
        cartService.prepareCartForCheckout(customerId);
        Cart cart = cartService.getOpenCartOrThrow(customerId);
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Carrinho vazio");
        }
        if (cart.getTotalAmount() == null
                || cart.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Não há itens ativos no carrinho para calcular frete");
        }
        BigDecimal freight = freightService.calculate(cart, request.getAddressId(), customerId);
        cart.setFreightAmount(freight);
        cartRepository.save(cart);

        BigDecimal itemsSubtotal = cart.getTotalAmount();
        BigDecimal grandTotal = itemsSubtotal.add(freight).setScale(2, RoundingMode.HALF_UP);
        return FreightResponse.builder()
                .freightAmount(freight)
                .itemsSubtotal(itemsSubtotal)
                .grandTotal(grandTotal)
                .build();
    }

    @Transactional
    public void applyDeliveryAddress(UUID customerId, CheckoutAddressRequest request) {
        Cart cart = cartService.getOpenCartOrThrow(customerId);
        if (request.getAddressId() != null && request.getNewAddress() != null) {
            throw new IllegalArgumentException("Informe apenas addressId ou newAddress");
        }
        Address addr;
        if (request.getAddressId() != null) {
            addr = addressRepository.findByIdAndCustomer_Id(request.getAddressId(), customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));
        } else if (request.getNewAddress() != null) {
            if (request.getSaveToProfile() == null) {
                throw new IllegalArgumentException("Informe saveToProfile ao cadastrar novo endereço");
            }
            if (Boolean.TRUE.equals(request.getSaveToProfile())) {
                var resp = customerAddressService.addAddress(customerId, request.getNewAddress());
                addr = addressRepository.findById(resp.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));
            } else {
                addr = createAddressForCheckoutOnly(customerId, request);
            }
        } else {
            throw new IllegalArgumentException("Informe addressId ou newAddress");
        }
        cart.setDeliveryAddress(addr);
        cartRepository.save(cart);
    }

    private Address createAddressForCheckoutOnly(UUID customerId, CheckoutAddressRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        var r = request.getNewAddress();
        Address address = Address.builder()
                .customer(customer)
                .nickname(r.getNickname().trim())
                .street(r.getStreet().trim())
                .number(r.getNumber().trim())
                .complement(r.getComplement() != null ? r.getComplement().trim() : null)
                .neighborhood(r.getNeighborhood().trim())
                .city(r.getCity().trim())
                .state(r.getState().trim().toUpperCase())
                .zipCode(r.getZipCode().replaceAll("\\D", ""))
                .type(r.getType())
                .active(true)
                .build();
        return addressRepository.save(address);
    }

    @Transactional
    public void applyPayment(UUID customerId, CheckoutPaymentRequest request) {
        Cart cart = cartService.getOpenCartOrThrow(customerId);
        if (request.getNewCreditCard() != null && request.getSaveNewCardToProfile() == null) {
            throw new IllegalArgumentException("Informe saveNewCardToProfile ao usar novo cartão");
        }

        cart.getPaymentLines().clear();
        cart.setEphemeralCreditCardId(null);

        UUID newCardId = null;
        if (request.getNewCreditCard() != null) {
            var resp = customerCreditCardService.addCard(customerId, request.getNewCreditCard());
            newCardId = resp.getId();
            if (!Boolean.TRUE.equals(request.getSaveNewCardToProfile())) {
                cart.setEphemeralCreditCardId(newCardId);
            }
        }

        Set<String> couponCodesSeen = new HashSet<>();
        for (PaymentLineRequest line : request.getLines()) {
            if (line.getCouponCode() != null && !couponCodesSeen.add(line.getCouponCode().trim().toLowerCase())) {
                throw new IllegalArgumentException("Cupom repetido nas linhas de pagamento");
            }
            CartPaymentLine pl = CartPaymentLine.builder()
                    .cart(cart)
                    .paymentType(line.getPaymentType())
                    .amount(line.getAmount().setScale(2, RoundingMode.HALF_UP))
                    .build();

            switch (line.getPaymentType()) {
                case CREDIT_CARD -> {
                    UUID cardId = line.getCreditCardId();
                    if (cardId == null) {
                        if (newCardId == null) {
                            throw new IllegalArgumentException("Informe creditCardId ou newCreditCard para pagamento com cartão");
                        }
                        cardId = newCardId;
                    }
                    creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));
                    pl.setCreditCardId(cardId);
                }
                case EXCHANGE_COUPON, PROMOTIONAL_COUPON -> {
                    if (line.getCouponCode() == null || line.getCouponCode().isBlank()) {
                        throw new IllegalArgumentException("Informe couponCode para pagamento com cupom");
                    }
                    couponService.loadAndValidate(
                            line.getCouponCode().trim(),
                            customerId,
                            line.getAmount(),
                            line.getPaymentType());
                    pl.setCouponCode(line.getCouponCode().trim());
                }
            }
            cart.getPaymentLines().add(pl);
        }
        BigDecimal itemsSubtotal = cart.getTotalAmount() != null ? cart.getTotalAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal freight = cart.getFreightAmount() != null ? cart.getFreightAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal grandTotal = itemsSubtotal.add(freight).setScale(2, RoundingMode.HALF_UP);
        validateCardAndPromoRulesFromPaymentRequest(request, grandTotal);
        cartRepository.save(cart);
    }

    /** RN0033–RN0035: no máximo um cupom promocional; mínimo R$ 10 por linha de cartão (com exceções). */
    private void validateCardAndPromoRulesFromPaymentRequest(CheckoutPaymentRequest request, BigDecimal grandTotal) {
        List<PaymentLineRequest> lines = request.getLines();

        // RN0036: Validação de cupons desnecessários
        List<PaymentLineRequest> couponLines = lines.stream()
                .filter(l -> l.getPaymentType() == PaymentType.EXCHANGE_COUPON
                        || l.getPaymentType() == PaymentType.PROMOTIONAL_COUPON)
                .toList();
        BigDecimal totalCouponAmount = couponLines.stream()
                .map(PaymentLineRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (PaymentLineRequest line : couponLines) {
            BigDecimal remainingCouponAmount = totalCouponAmount.subtract(line.getAmount());
            if (remainingCouponAmount.compareTo(grandTotal) >= 0) {
                throw new IllegalArgumentException(
                        "O cupom '" + line.getCouponCode() + "' é desnecessário pois os outros cupons já cobrem o valor total da compra.");
            }
        }

        long cardLineCount = lines.stream()
                .filter(l -> l.getPaymentType() == PaymentType.CREDIT_CARD)
                .count();
        boolean hasCouponLine = lines.stream()
                .anyMatch(l -> l.getPaymentType() == PaymentType.EXCHANGE_COUPON
                        || l.getPaymentType() == PaymentType.PROMOTIONAL_COUPON);
        for (PaymentLineRequest line : lines) {
            if (line.getPaymentType() != PaymentType.CREDIT_CARD) {
                continue;
            }
            BigDecimal amt = line.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (hasCouponLine && cardLineCount == 1) {
                continue;
            }
            if (amt.compareTo(MIN_CREDIT_CARD_LINE_BRL) < 0) {
                throw new IllegalArgumentException(
                        "Cada linha de cartão deve ser de no mínimo R$ 10,00 (exceto o restante com cupom em linha única de cartão).");
            }
        }
    }

    private void validateCardAndPromoRulesFromCart(Cart cart, BigDecimal grandTotal) {
        List<CartPaymentLine> lines = cart.getPaymentLines();

        // RN0036: Validação de cupons desnecessários
        List<CartPaymentLine> couponLines = lines.stream()
                .filter(l -> l.getPaymentType() == PaymentType.EXCHANGE_COUPON
                        || l.getPaymentType() == PaymentType.PROMOTIONAL_COUPON)
                .toList();
        BigDecimal totalCouponAmount = couponLines.stream()
                .map(CartPaymentLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (CartPaymentLine line : couponLines) {
            BigDecimal remainingCouponAmount = totalCouponAmount.subtract(line.getAmount());
            if (remainingCouponAmount.compareTo(grandTotal) >= 0) {
                throw new IllegalArgumentException(
                        "O cupom '" + line.getCouponCode() + "' é desnecessário pois os outros cupons já cobrem o valor total da compra.");
            }
        }

        long cardLineCount = lines.stream()
                .filter(l -> l.getPaymentType() == PaymentType.CREDIT_CARD)
                .count();
        boolean hasCouponLine = lines.stream()
                .anyMatch(l -> l.getPaymentType() == PaymentType.EXCHANGE_COUPON
                        || l.getPaymentType() == PaymentType.PROMOTIONAL_COUPON);
        for (CartPaymentLine line : lines) {
            if (line.getPaymentType() != PaymentType.CREDIT_CARD) {
                continue;
            }
            BigDecimal amt = line.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (hasCouponLine && cardLineCount == 1) {
                continue;
            }
            if (amt.compareTo(MIN_CREDIT_CARD_LINE_BRL) < 0) {
                throw new IllegalArgumentException(
                        "Cada linha de cartão deve ser de no mínimo R$ 10,00 (exceto o restante com cupom em linha única de cartão).");
            }
        }
    }

    @Transactional
    public OrderResponse finalizePurchase(UUID customerId) {
        cartService.prepareCartForCheckout(customerId);
        Cart cart = cartService.getOpenCartOrThrow(customerId);
        log.info("[CheckoutService][finalizePurchase] Início customerId={} cartId={} itens={}",
                customerId, cart.getId(), cart.getItems().size());
        if (cartExpirationService.hasBlockingExpiredItems(cart)) {
            throw new IllegalArgumentException(
                    "Existem itens expirados no carrinho; adicione novamente ou atualize as quantidades antes de finalizar.");
        }
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Carrinho vazio");
        }
        if (cart.getDeliveryAddress() == null) {
            throw new IllegalArgumentException("Endereço de entrega não definido");
        }
        if (cart.getFreightAmount() == null) {
            throw new IllegalArgumentException("Frete não calculado");
        }
        if (cart.getPaymentLines().isEmpty()) {
            throw new IllegalArgumentException("Forma de pagamento não definida");
        }

        BigDecimal itemsSubtotal = cart.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal freight = cart.getFreightAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = itemsSubtotal.add(freight).setScale(2, RoundingMode.HALF_UP);

        validateCardAndPromoRulesFromCart(cart, grandTotal);

        BigDecimal paid = cart.getPaymentLines().stream()
                .map(CartPaymentLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        boolean hasCard = cart.getPaymentLines().stream()
                .anyMatch(pl -> pl.getPaymentType() == PaymentType.CREDIT_CARD);

        if (hasCard && paid.compareTo(grandTotal) != 0) {
            throw new IllegalArgumentException("Total dos pagamentos deve cobrir o valor da compra (incluindo frete)");
        }
        if (!hasCard) {
            if (paid.compareTo(grandTotal) < 0) {
                throw new IllegalArgumentException("Total dos pagamentos deve cobrir o valor da compra (incluindo frete)");
            }
            if (paid.compareTo(grandTotal) > 0) {
                BigDecimal troco = paid.subtract(grandTotal).setScale(2, RoundingMode.HALF_UP);
                couponService.issueTrocoExchangeCoupon(customerId, troco);
                log.info("[CheckoutService][finalizePurchase] RN0036 troco emitido customerId={} valor={}", customerId, troco);
            }
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        if (addressRepository.countByCustomer_IdAndTypeAndActiveTrue(customerId, AddressType.BILLING) < 1) {
            throw new IllegalArgumentException(
                    "Cadastre ao menos um endereço de cobrança ativo no perfil (RN0021).");
        }
        if (addressRepository.countByCustomer_IdAndTypeAndActiveTrue(customerId, AddressType.DELIVERY) < 1) {
            throw new IllegalArgumentException(
                    "Cadastre ao menos um endereço de entrega ativo no perfil (RN0022).");
        }

        for (CartItem ci : cart.getItems()) {
            inventoryService.assertInventoryAvailableForFulfillment(ci.getBook().getId(), ci.getQuantity());
        }

        SalesOrder order = SalesOrder.builder()
                .customer(customer)
                .cart(cart)
                .deliveryAddress(cart.getDeliveryAddress())
                .freightAmount(freight)
                .itemsSubtotal(itemsSubtotal)
                .totalAmount(grandTotal)
                .status(OrderStatus.EM_PROCESSAMENTO)
                .build();

        for (CartItem ci : cart.getItems()) {
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .book(ci.getBook())
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getUnitPrice())
                    .totalPrice(ci.getTotalPrice())
                    .exchangeRequested(false)
                    .build();
            order.getItems().add(oi);
        }

        for (CartPaymentLine pl : cart.getPaymentLines()) {
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentType(pl.getPaymentType())
                    .amount(pl.getAmount())
                    .couponCode(pl.getCouponCode())
                    .build();
            if (pl.getPaymentType() == PaymentType.CREDIT_CARD && pl.getCreditCardId() != null) {
                CreditCard card = creditCardRepository.findByIdAndCustomer_Id(pl.getCreditCardId(), customerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));
                String cn = card.getCardNumber();
                payment.setCardLastDigits(cn.length() >= 4 ? cn.substring(cn.length() - 4) : cn);
            }
            order.getPayments().add(payment);
            // Cupons: resgate em AdminOrderService.approvePayment (RN0028 — só após pagamento efetivado).
        }

        salesOrderRepository.save(order);
        Map<String, Object> orderAudit = new LinkedHashMap<>();
        orderAudit.put("customerId", customerId);
        orderAudit.put("totalAmount", grandTotal);
        orderAudit.put("itemsSubtotal", itemsSubtotal);
        orderAudit.put("freightAmount", freight);
        orderAudit.put("itemCount", order.getItems().size());
        orderAudit.put("status", order.getStatus().name());
        auditLogService.logCreate("SalesOrder", order.getId(), orderAudit);
        log.info("[CheckoutService][finalizePurchase] Pedido criado orderId={} customerId={} total={} status={}",
                order.getId(), customerId, grandTotal, order.getStatus());
        // RN0028: baixa de estoque e extrato apenas após aprovação do pagamento (AdminOrderService.approvePayment).

        if (cart.getEphemeralCreditCardId() != null) {
            customerCreditCardService.deactivateCard(customerId, cart.getEphemeralCreditCardId());
            cart.setEphemeralCreditCardId(null);
        }

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        return orderService.toResponse(order);
    }
}
