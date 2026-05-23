package com.matheusgn.ecommerce.inventory.entity;

import com.matheusgn.ecommerce.book.entity.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    private Book book;

    @Column(nullable = false)
    private int quantityAvailable;

    /**
     * Quantidade reservada por carrinhos abertos (RN0044). Disponível para novas reservas =
     * {@code quantityAvailable - quantityReserved}.
     */
    @Column(nullable = false)
    @Builder.Default
    private int quantityReserved = 0;

    @Column(nullable = false)
    private Instant lastUpdatedAt;
}
