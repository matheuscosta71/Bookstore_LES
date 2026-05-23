package com.matheusgn.ecommerce.inventory.service;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.Inventory;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovement;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import com.matheusgn.ecommerce.inventory.repository.InventoryMovementRepository;
import com.matheusgn.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryBalanceService {

    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional(readOnly = true)
    public int getQuantityAvailable(UUID bookId) {
        Inventory inv = inventoryRepository.findByBook_Id(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o livro"));
        return inv.getQuantityAvailable();
    }

    /** Unidades ainda disponíveis para novas reservas (RN0044). */
    @Transactional(readOnly = true)
    public int getSellableQuantity(UUID bookId) {
        Inventory inv = inventoryRepository.findByBook_Id(bookId).orElse(null);
        if (inv == null) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
            return Math.max(0, book.getStockQuantity());
        }
        return Math.max(0, inv.getQuantityAvailable() - inv.getQuantityReserved());
    }

    /**
     * Valida quantidade física em estoque para concluir o pedido (checkout).
     * <p>
     * Diferente de {@link #getSellableQuantity}: itens já reservados no carrinho não contam como
     * "vendáveis" para novas reservas, mas ainda existem fisicamente — a finalização deve comparar com
     * {@code quantityAvailable}, não com (disponível − reservado).
     */
    @Transactional(readOnly = true)
    public void assertCanFulfillOrderLine(UUID bookId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        if (!book.isActive()) {
            throw new IllegalArgumentException("Livro inativo não pode ser vendido");
        }
        Inventory inv = inventoryRepository.findByBook_Id(bookId).orElse(null);
        if (inv == null) {
            if (book.getStockQuantity() < quantity) {
                throw new IllegalArgumentException("Estoque insuficiente para o livro: " + book.getTitle());
            }
            return;
        }
        if (inv.getQuantityAvailable() < quantity) {
            throw new IllegalArgumentException("Estoque insuficiente para o livro: " + book.getTitle());
        }
    }

    /**
     * Ajusta reserva de estoque para o carrinho (delta &gt; 0 reserva; delta &lt; 0 libera).
     */
    @Transactional
    public void adjustReservation(UUID bookId, int delta) {
        if (delta == 0) {
            return;
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        if (!book.isActive()) {
            throw new IllegalArgumentException("Livro inativo não pode ser vendido");
        }
        Inventory inv = inventoryRepository.findByBook_Id(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o livro"));
        if (delta > 0) {
            int sellable = inv.getQuantityAvailable() - inv.getQuantityReserved();
            if (sellable < delta) {
                throw new IllegalArgumentException("Estoque insuficiente para o livro: " + book.getTitle());
            }
            inv.setQuantityReserved(inv.getQuantityReserved() + delta);
        } else {
            int release = -delta;
            int newReserved = inv.getQuantityReserved() - release;
            if (newReserved < 0) {
                newReserved = 0;
            }
            inv.setQuantityReserved(newReserved);
        }
        inv.setLastUpdatedAt(Instant.now());
        inventoryRepository.save(inv);
    }

    @Transactional
    public void initializeInventoryForBook(UUID bookId, int initialQuantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        if (inventoryRepository.existsByBook_Id(bookId)) {
            return;
        }
        Instant now = Instant.now();
        Inventory inv = Inventory.builder()
                .book(book)
                .quantityAvailable(initialQuantity)
                .quantityReserved(0)
                .lastUpdatedAt(now)
                .build();
        inventoryRepository.save(inv);
        book.setStockQuantity(initialQuantity);
        bookRepository.save(book);
    }

    @Transactional
    public void syncQuantityFromBook(UUID bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        Inventory inv = inventoryRepository.findByBook_Id(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o livro"));
        inv.setQuantityAvailable(quantity);
        inv.setLastUpdatedAt(Instant.now());
        book.setStockQuantity(quantity);
        inventoryRepository.save(inv);
        bookRepository.save(book);
    }

    /**
     * Ajusta estoque físico após a simulação de pedidos no seed de demo: valores entre 1 e 9 para vitrine,
     * com reservas zeradas (pedidos demo já foram concluídos).
     */
    @Transactional
    public void setPhysicalStockForDemoPresentation(UUID bookId, int quantity) {
        if (quantity < 1 || quantity > 9) {
            throw new IllegalArgumentException("Estoque de demonstração deve estar entre 1 e 9 unidades.");
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        Inventory inv = inventoryRepository.findByBook_Id(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o livro"));
        inv.setQuantityAvailable(quantity);
        inv.setQuantityReserved(0);
        inv.setLastUpdatedAt(Instant.now());
        book.setStockQuantity(quantity);
        inventoryRepository.save(inv);
        bookRepository.save(book);
    }

    @Transactional
    public void increaseStock(UUID bookId, int quantity, InventoryMovementType movementType,
                              InventoryReferenceType referenceType, UUID referenceId, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        Inventory inv = inventoryRepository.findByBook_Id(bookId).orElse(null);
        Instant now = Instant.now();
        if (inv == null) {
            inv = Inventory.builder()
                    .book(book)
                    .quantityAvailable(quantity)
                    .quantityReserved(0)
                    .lastUpdatedAt(now)
                    .build();
        } else {
            inv.setQuantityAvailable(inv.getQuantityAvailable() + quantity);
            inv.setLastUpdatedAt(now);
        }
        inventoryRepository.save(inv);
        book.setStockQuantity(inv.getQuantityAvailable());
        bookRepository.save(book);
        inventoryMovementRepository.save(InventoryMovement.builder()
                .book(book)
                .movementType(movementType)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .quantity(quantity)
                .notes(notes)
                .build());
    }

    @Transactional
    public void decreaseStock(UUID bookId, int quantity, InventoryMovementType movementType,
                              InventoryReferenceType referenceType, UUID referenceId, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        Inventory inv = inventoryRepository.findByBook_Id(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o livro"));
        if (!book.isActive()) {
            throw new IllegalArgumentException("Livro inativo não pode ser vendido");
        }
        int reservedRelease = Math.min(quantity, inv.getQuantityReserved());
        inv.setQuantityReserved(inv.getQuantityReserved() - reservedRelease);
        if (inv.getQuantityAvailable() < quantity) {
            throw new IllegalArgumentException("Estoque insuficiente para o livro: " + book.getTitle());
        }
        inv.setQuantityAvailable(inv.getQuantityAvailable() - quantity);
        inv.setLastUpdatedAt(Instant.now());
        inventoryRepository.save(inv);
        book.setStockQuantity(inv.getQuantityAvailable());
        bookRepository.save(book);
        inventoryMovementRepository.save(InventoryMovement.builder()
                .book(book)
                .movementType(movementType)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .quantity(quantity)
                .notes(notes)
                .build());
    }
}
