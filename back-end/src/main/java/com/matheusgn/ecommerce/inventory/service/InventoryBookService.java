package com.matheusgn.ecommerce.inventory.service;

import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.dto.InventoryBookResponse;
import com.matheusgn.ecommerce.inventory.entity.Inventory;
import com.matheusgn.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryBookService {

    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventoryBookResponse getByBookId(UUID bookId) {
        var book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        Inventory inv = inventoryRepository.findByBook_Id(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o livro"));
        return InventoryBookResponse.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .category(book.getCategory())
                .quantityAvailable(inv.getQuantityAvailable())
                .lastUpdatedAt(inv.getLastUpdatedAt())
                .build();
    }
}
