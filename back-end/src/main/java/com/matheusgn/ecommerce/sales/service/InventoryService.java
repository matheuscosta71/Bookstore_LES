package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BookRepository bookRepository;
    private final InventoryBalanceService inventoryBalanceService;

    /** Quantidade ainda disponível para reserva no carrinho (RN0031/RN0044). */
    public int getSellableQuantity(UUID bookId) {
        return inventoryBalanceService.getSellableQuantity(bookId);
    }

    public void assertAvailableStock(UUID bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        if (!book.isActive()) {
            throw new IllegalArgumentException("Livro inativo não pode ser vendido");
        }
        int sellable = inventoryBalanceService.getSellableQuantity(bookId);
        if (sellable < quantity) {
            throw new IllegalArgumentException("Estoque insuficiente para o livro: " + book.getTitle());
        }
    }

    /** Estoque físico suficiente para faturar a linha (após reserva no carrinho). */
    public void assertInventoryAvailableForFulfillment(UUID bookId, int quantity) {
        inventoryBalanceService.assertCanFulfillOrderLine(bookId, quantity);
    }

    /** Ajusta reserva em relação à alteração de quantidade no carrinho (RN0044). */
    public void adjustReservationForCart(UUID bookId, int delta) {
        inventoryBalanceService.adjustReservation(bookId, delta);
    }
}
