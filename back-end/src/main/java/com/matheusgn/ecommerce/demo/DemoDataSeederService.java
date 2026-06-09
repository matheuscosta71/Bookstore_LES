package com.matheusgn.ecommerce.demo;

import com.matheusgn.ecommerce.book.dto.BookCreateRequest;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.book.service.BookService;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.config.InventoryDataMigration;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
import com.matheusgn.ecommerce.config.AdminProperties;
import com.matheusgn.ecommerce.customer.dto.AddressCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CreditCardCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CustomerCreateRequest;
import com.matheusgn.ecommerce.customer.entity.AddressType;
import com.matheusgn.ecommerce.customer.service.CustomerAddressService;
import com.matheusgn.ecommerce.customer.service.CustomerCreditCardService;
import com.matheusgn.ecommerce.customer.service.CustomerService;
import com.matheusgn.ecommerce.sales.dto.CartUpsertItemRequest;
import com.matheusgn.ecommerce.sales.dto.CheckoutAddressRequest;
import com.matheusgn.ecommerce.sales.dto.CheckoutPaymentRequest;
import com.matheusgn.ecommerce.sales.dto.CreateExchangeRequest;
import com.matheusgn.ecommerce.sales.dto.ExchangeReceiveRequest;
import com.matheusgn.ecommerce.sales.dto.ExchangeRequestResponse;
import com.matheusgn.ecommerce.sales.dto.FreightRequest;
import com.matheusgn.ecommerce.sales.dto.FreightResponse;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.dto.PaymentLineRequest;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import com.matheusgn.ecommerce.sales.service.CartService;
import com.matheusgn.ecommerce.sales.service.CheckoutService;
import com.matheusgn.ecommerce.sales.service.ExchangeService;
import com.matheusgn.ecommerce.sales.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoDataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeederService.class);

    private final DemoSeedProperties demoSeedProperties;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final PricingGroupRepository pricingGroupRepository;
    private final CustomerService customerService;
    private final CustomerAddressService customerAddressService;
    private final CustomerCreditCardService customerCreditCardService;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final AdminOrderService adminOrderService;
    private final AdminProperties adminProperties;
    private final ExchangeService exchangeService;
    private final InventoryService inventoryService;
    private final InventoryBalanceService inventoryBalanceService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void seedIfNeeded() {
        if (!demoSeedProperties.isEnabled()) {
            return;
        }
        String marker = demoSeedProperties.getMarkerIsbn();
        if (marker == null || marker.isBlank()) {
            log.warn("Demo seed skipped: marker ISBN vazio");
            return;
        }
        if (bookRepository.existsByIsbn(marker.trim())) {
            log.info("Demo seed skipped: livro marcador já existe (ISBN {})", marker);
            return;
        }

        log.info("Demo seed: criando catálogo, clientes, pedidos e uma troca de amostra…");
        List<UUID> bookIds = createDemoBooks(marker.trim());
        List<CustomerSeed> customers = createDemoCustomers();

        int span = Math.max(7, demoSeedProperties.getDaysBack());
        Random rnd = new Random(424242L);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (int i = 0; i < 28; i++) {
            CustomerSeed c = customers.get(i % customers.size());
            int monthOffset = i / 2;
            int daysAgo = monthOffset * 30 + (i % 2) * 10 + 2;
            LocalTime t = LocalTime.of(9 + rnd.nextInt(10), rnd.nextInt(60));
            Instant when = today.minusDays(daysAgo).atTime(t).atZone(ZoneOffset.UTC).toInstant();
            UUID bookId = bookIds.get(rnd.nextInt(bookIds.size()));
            int qty = 1 + rnd.nextInt(3);
            int sellable = inventoryService.getSellableQuantity(bookId);
            if (sellable < 1) {
                int attempts = 0;
                while (sellable < 1 && attempts < bookIds.size() * 8) {
                    bookId = bookIds.get(rnd.nextInt(bookIds.size()));
                    sellable = inventoryService.getSellableQuantity(bookId);
                    attempts++;
                }
            }
            if (sellable < 1) {
                log.warn("Demo seed: estoque esgotado para pedidos simulados na iteração {}; encerrando loop.", i);
                break;
            }
            qty = Math.min(qty, sellable);
            OrderResponse order = placeDeliveredOrder(c, bookId, qty);
            backdateOrder(order.getId(), when);
        }

        CustomerSeed ex = customers.get(0);
        UUID exBookId = bookIds.get(0);
        if (inventoryService.getSellableQuantity(exBookId) < 1) {
            for (UUID bid : bookIds) {
                if (inventoryService.getSellableQuantity(bid) >= 1) {
                    exBookId = bid;
                    break;
                }
            }
        }
        if (inventoryService.getSellableQuantity(exBookId) < 1) {
            throw new IllegalStateException("Demo seed: sem estoque mínimo para pedido de troca de exemplo.");
        }
        OrderResponse exchangeOrder = placeDeliveredOrder(ex, exBookId, 1);
        Instant exWhen = today.minusDays(2).atTime(15, 30).atZone(ZoneOffset.UTC).toInstant();
        backdateOrder(exchangeOrder.getId(), exWhen);

        UUID orderItemId = exchangeOrder.getItems().get(0).getId();
        ExchangeRequestResponse created = exchangeService.requestExchange(
                ex.customerId(),
                exchangeOrder.getId(),
                CreateExchangeRequest.builder().orderItemId(orderItemId).build());
        String adminKey = adminProperties.getKey();
        exchangeService.authorize(created.getId(), adminKey);
        exchangeService.receive(
                created.getId(),
                ExchangeReceiveRequest.builder().returnToStock(true).build(),
                adminKey);

        Random restockRnd = new Random(131313L);
        for (UUID bid : bookIds) {
            int q = 1 + restockRnd.nextInt(9);
            inventoryBalanceService.setPhysicalStockForDemoPresentation(bid, q);
        }

        log.info("Demo seed concluído ({} livros, {} clientes, troca de exemplo completada).", bookIds.size(), customers.size());
    }

    private List<UUID> createDemoBooks(String markerIsbn) {
        UUID publisherId = publisherRepository
                .findByNameIgnoreCase("Pearson")
                .orElseThrow(() -> new IllegalStateException("Editora Pearson não encontrada (domínio)"))
                .getId();
        UUID supplierId = supplierRepository
                .findByNameIgnoreCase("Distribuidora Alpha")
                .orElseThrow(() -> new IllegalStateException("Fornecedor Distribuidora Alpha não encontrado"))
                .getId();
        UUID pricingGroupId = pricingGroupRepository
                .findByName(InventoryDataMigration.DEFAULT_GROUP_NAME)
                .orElseThrow(() -> new IllegalStateException("Grupo de precificação Padrão não encontrado"))
                .getId();

        // ISBN-13 de edições US/UK com capas reais na Open Library.
        List<DemoBookDef> defs = new ArrayList<>(List.of(
                new DemoBookDef(
                        "The Great Gatsby",
                        "F. Scott Fitzgerald",
                        "Literatura",
                        markerIsbn,
                        new BigDecimal("49.90"),
                        5,
                        1925,
                        "1ª",
                        180,
                        "Nos anos loucos de Nova York, Nick Carraway conhece o misterioso milionário Jay Gatsby, obcecado "
                                + "por reencontrar Daisy Buchanan. Um retrato da ambição, do amor e da ilusão americana."),
                new DemoBookDef(
                        "1984 / Nineteen Eighty-Four",
                        "George Orwell",
                        "Ficção",
                        "9780451524935",
                        new BigDecimal("45.00"),
                        6,
                        1950,
                        "1ª",
                        328,
                        "Num estado totalitário que reescreve a história e vigia todos, Winston Smith questiona o Partido "
                                + "e arrisca tudo por um gesto de humanidade. Clássico sobre vigilância e liberdade."),
                new DemoBookDef(
                        "1984",
                        "George Orwell",
                        "Ficção",
                        "9780141036144",
                        new BigDecimal("42.00"),
                        7,
                        1949,
                        "1ª",
                        328,
                        "Winston Smith trabalha no Ministério da Verdade, alterando registros para servir ao Grande Irmão. "
                                + "Sua busca pela verdade desafia uma sociedade moldada pelo medo e pela propaganda."),
                new DemoBookDef(
                        "Nineteen Eighty-Four",
                        "George Orwell",
                        "Ficção",
                        "9780141187761",
                        new BigDecimal("44.00"),
                        8,
                        1949,
                        "1ª",
                        326,
                        "Em Oceania, pensamentos são crime e o amor é traição. Winston e Julia tentam resistir a um "
                                + "regime que controla até a linguagem — distopia fundamental da literatura moderna."),
                new DemoBookDef(
                        "To Kill a Mockingbird",
                        "Harper Lee",
                        "Literatura",
                        "9780061120084",
                        new BigDecimal("52.00"),
                        9,
                        1960,
                        "1ª",
                        376,
                        "Vista pelos olhos da menina Scout Finch, a história do advogado Atticus na defesa de Tom Robinson "
                                + "expõe preconceito racial e injustiça no sul dos Estados Unidos dos anos 1930."),
                new DemoBookDef(
                        "The Catcher in the Rye",
                        "J. D. Salinger",
                        "Literatura",
                        "9780316769488",
                        new BigDecimal("48.00"),
                        9,
                        1951,
                        "1ª",
                        277,
                        "Holden Caulfield vagueia por Nova York após ser expulso da escola, num monólogo íntimo sobre "
                                + "fingimento, pertencimento e a transição à vida adulta."),
                new DemoBookDef(
                        "Harry Potter Paperback Box Set (Books 1–7)",
                        "J. K. Rowling",
                        "Infantil",
                        "9780545162074",
                        new BigDecimal("299.00"),
                        7,
                        2007,
                        "Box",
                        3400,
                        "Coleção com os sete volumes da saga: de um menino que descobre ser bruxo até o confronto final "
                                + "com as forças que ameaçam o mundo mágico. Aventura, amizade e crescimento."),
                new DemoBookDef(
                        "A Game of Thrones",
                        "George R. R. Martin",
                        "Ficção",
                        "9780553386790",
                        new BigDecimal("59.99"),
                        8,
                        1996,
                        "1ª",
                        694,
                        "Famílias nobres disputam o Trono de Ferro enquanto uma ameaça antiga desperta além da Muralha. "
                                + "Primeiro volume de uma saga épica de intriga, honra e sobrevivência em Westeros.")));

        List<UUID> ids = new ArrayList<>();
        for (DemoBookDef d : defs) {
            UUID authorId = authorRepository
                    .findByNameIgnoreCase(d.authorName())
                    .orElseThrow(() -> new IllegalStateException("Autor não encontrado: " + d.authorName()))
                    .getId();
            UUID categoryId = categoryRepository
                    .findByNameIgnoreCase(d.categoryName())
                    .orElseThrow(() -> new IllegalStateException("Categoria não encontrada: " + d.categoryName()))
                    .getId();
            String barcode = "D" + onlyDigits(d.isbn());
            BigDecimal cost = d.price().multiply(new BigDecimal("0.60")).setScale(2, java.math.RoundingMode.HALF_UP);
            var req = BookCreateRequest.builder()
                    .title(d.title())
                    .authorId(authorId)
                    .publisherId(publisherId)
                    .supplierId(supplierId)
                    .categoryIds(List.of(categoryId))
                    .publicationYear(d.publicationYear())
                    .edition(d.edition())
                    .pageCount(d.pageCount())
                    .synopsis(d.synopsis())
                    .heightCm(new BigDecimal("23.0"))
                    .widthCm(new BigDecimal("15.2"))
                    .depthCm(new BigDecimal("2.5"))
                    .weightKg(new BigDecimal("0.450"))
                    .barcode(barcode)
                    .price(d.price())
                    .costPrice(cost)
                    .pricingGroupId(pricingGroupId)
                    .isbn(d.isbn().trim())
                    .stockQuantity(d.stock())
                    .active(true)
                    .build();
            ids.add(bookService.create(req).getId());
        }
        return ids;
    }

    private static String onlyDigits(String isbn) {
        return isbn.replaceAll("\\D", "");
    }

    private List<CustomerSeed> createDemoCustomers() {
        String pwd = "DemoSeed9!";
        LocalDate birthDemo = LocalDate.of(1991, 6, 15);
        List<CustomerSeed> out = new ArrayList<>();
        out.add(seedCustomer(
                "Machado Souza",
                "machado.souza@example.com",
                "05801457490",
                "51988776655",
                "Senh@Forte",
                LocalDate.of(1990, 4, 22)));
        out.add(seedCustomer(
                "Ana Demo",
                "demo.seed.ana@example.com",
                "52998224725",
                "31988881111",
                pwd,
                birthDemo));
        out.add(seedCustomer(
                "Bruno Demo",
                "demo.seed.bruno@example.com",
                "39053344705",
                "31988882222",
                pwd,
                birthDemo));
        out.add(seedCustomer(
                "Carla Demo",
                "demo.seed.carla@example.com",
                "11144477735",
                "31988883333",
                pwd,
                birthDemo));
        out.add(seedCustomer(
                "Diego Demo",
                "demo.seed.diego@example.com",
                "86834405099",
                "31988884444",
                pwd,
                birthDemo));
        out.add(seedCustomer(
                "Elisa Demo",
                "demo.seed.elisa@example.com",
                "19100000000",
                "31988885555",
                pwd,
                birthDemo));
        return out;
    }

    private CustomerSeed seedCustomer(
            String name, String email, String cpf, String phone, String password, LocalDate birthDate) {
        var created = customerService.create(CustomerCreateRequest.builder()
                .fullName(name)
                .email(email)
                .cpf(cpf)
                .phone(phone)
                .birthDate(birthDate)
                .password(password)
                .confirmPassword(password)
                .active(true)
                .build());
        UUID id = created.getId();

        var addrReq = AddressCreateRequest.builder()
                .nickname("Casa")
                .street("Rua das Sementes")
                .number("200")
                .neighborhood("Centro")
                .city("Belo Horizonte")
                .state("MG")
                .zipCode("30130100")
                .type(AddressType.DELIVERY)
                .build();
        var address = customerAddressService.addAddress(id, addrReq);

        var billingReq = AddressCreateRequest.builder()
                .nickname("Cobrança")
                .street("Rua das Sementes")
                .number("200")
                .neighborhood("Centro")
                .city("Belo Horizonte")
                .state("MG")
                .zipCode("30130100")
                .type(AddressType.BILLING)
                .build();
        customerAddressService.addAddress(id, billingReq);

        var cardReq = CreditCardCreateRequest.builder()
                .cardholderName(name)
                .cardNumber("4111111111111111")
                .brand("VISA")
                .expirationMonth(12)
                .expirationYear(2030)
                .preferred(true)
                .build();
        var card = customerCreditCardService.addCard(id, cardReq);

        return new CustomerSeed(id, address.getId(), card.getId());
    }

    private OrderResponse placeDeliveredOrder(CustomerSeed customer, UUID bookId, int quantity) {
        UUID cid = customer.customerId();
        cartService.addItem(cid, CartUpsertItemRequest.builder().bookId(bookId).quantity(quantity).build());

        FreightResponse freight = checkoutService.calculateFreight(
                cid, FreightRequest.builder().addressId(customer.addressId()).build());

        checkoutService.applyDeliveryAddress(
                cid,
                CheckoutAddressRequest.builder().addressId(customer.addressId()).build());

        checkoutService.applyPayment(
                cid,
                CheckoutPaymentRequest.builder()
                        .lines(List.of(PaymentLineRequest.builder()
                                .paymentType(PaymentType.CREDIT_CARD)
                                .amount(freight.getGrandTotal())
                                .creditCardId(customer.cardId())
                                .build()))
                        .build());

        OrderResponse order = checkoutService.finalizePurchase(cid);
        String key = adminProperties.getKey();
        adminOrderService.approvePayment(order.getId(), key);
        adminOrderService.dispatch(order.getId(), key);
        adminOrderService.markDelivered(order.getId(), key);
        return order;
    }

    private void backdateOrder(UUID orderId, Instant when) {
        Timestamp ts = Timestamp.from(when);
        jdbcTemplate.update("UPDATE sales_orders SET created_at = ?, updated_at = ? WHERE id = ?", ts, ts, orderId);
        jdbcTemplate.update(
                "UPDATE customer_transactions SET transaction_date = ? WHERE description = ?",
                ts,
                "Compra — pedido " + orderId);
    }

    private record CustomerSeed(UUID customerId, UUID addressId, UUID cardId) {}

    private record DemoBookDef(
            String title,
            String authorName,
            String categoryName,
            String isbn,
            BigDecimal price,
            int stock,
            int publicationYear,
            String edition,
            int pageCount,
            String synopsis) {}
}
