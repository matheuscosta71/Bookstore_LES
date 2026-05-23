package com.matheusgn.ecommerce.book.entity;

import com.matheusgn.ecommerce.domain.entity.Author;
import com.matheusgn.ecommerce.domain.entity.Category;
import com.matheusgn.ecommerce.domain.entity.Publisher;
import com.matheusgn.ecommerce.domain.entity.Supplier;
import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "books",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_books_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_books_isbn", columnNames = "isbn"),
                @UniqueConstraint(name = "uk_books_barcode", columnNames = "barcode")
        },
        indexes = {
                @Index(name = "idx_books_code", columnList = "code"),
                @Index(name = "idx_books_isbn", columnList = "isbn"),
                @Index(name = "idx_books_title", columnList = "title")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** RN0018: formato LIVR-XXXXXX-CC; imutável após criação. */
    @Column(length = 32, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    /** Texto legado / denormalizado do autor. */
    private String author;

    /** Primeira categoria (legado / filtros). */
    private String category;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal costPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_group_id")
    private PricingGroup pricingGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_ref_id")
    private Author authorRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_ref_id")
    private Category categoryRef;

    /** RN0012: múltiplas categorias. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_category_links",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @Column(nullable = false)
    private String isbn;

    @Column(precision = 19, scale = 2)
    private BigDecimal maxSaleValue;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private boolean active;

    /** RN0011; nullable para linhas legadas antes da regra. */
    private Integer publicationYear;

    @Column(length = 80)
    private String edition;

    private Integer pageCount;

    @Column(length = 4000)
    private String synopsis;

    @Column(precision = 10, scale = 3)
    private BigDecimal heightCm;

    @Column(precision = 10, scale = 3)
    private BigDecimal widthCm;

    @Column(precision = 10, scale = 3)
    private BigDecimal depthCm;

    @Column(precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(length = 32)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private BookLifecycleReason lastLifecycleReason;

    @Column(length = 2000)
    private String lastLifecycleJustification;
}
