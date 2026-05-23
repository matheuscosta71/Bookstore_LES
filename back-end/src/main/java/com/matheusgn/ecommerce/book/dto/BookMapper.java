package com.matheusgn.ecommerce.book.dto;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.domain.entity.Category;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class BookMapper {

    public static BookResponse toResponse(Book book) {
        List<UUID> categoryIds = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();
        if (book.getCategories() != null) {
            List<Category> sorted = book.getCategories().stream()
                    .sorted(Comparator.comparing(Category::getName))
                    .collect(Collectors.toList());
            for (Category c : sorted) {
                categoryIds.add(c.getId());
                categoryNames.add(c.getName());
            }
        }
        return BookResponse.builder()
                .id(book.getId())
                .code(book.getCode())
                .title(book.getTitle())
                .author(book.getAuthor())
                .category(book.getCategory())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .price(book.getSalePrice())
                .costPrice(book.getCostPrice())
                .pricingGroupId(book.getPricingGroup() != null ? book.getPricingGroup().getId() : null)
                .isbn(book.getIsbn())
                .maxSaleValue(book.getMaxSaleValue())
                .stockQuantity(book.getStockQuantity() != null ? book.getStockQuantity() : 0)
                .active(book.isActive())
                .publicationYear(book.getPublicationYear())
                .edition(book.getEdition())
                .pageCount(book.getPageCount())
                .synopsis(book.getSynopsis())
                .heightCm(book.getHeightCm())
                .widthCm(book.getWidthCm())
                .depthCm(book.getDepthCm())
                .weightKg(book.getWeightKg())
                .barcode(book.getBarcode())
                .authorId(book.getAuthorRef() != null ? book.getAuthorRef().getId() : null)
                .publisherId(book.getPublisher() != null ? book.getPublisher().getId() : null)
                .supplierId(book.getSupplier() != null ? book.getSupplier().getId() : null)
                .lastLifecycleReason(book.getLastLifecycleReason())
                .lastLifecycleJustification(book.getLastLifecycleJustification())
                .build();
    }

    public static Book toEntity(BookCreateRequest request) {
        boolean active = request.getActive() != null ? request.getActive() : true;
        BigDecimal sale = request.getPrice();
        BigDecimal cost = request.getCostPrice() != null ? request.getCostPrice() : sale;
        return Book.builder()
                .title(request.getTitle())
                .salePrice(sale)
                .costPrice(cost)
                .isbn(request.getIsbn().trim())
                .maxSaleValue(request.getMaxSaleValue())
                .stockQuantity(request.getStockQuantity())
                .active(active)
                .publicationYear(request.getPublicationYear())
                .edition(request.getEdition())
                .pageCount(request.getPageCount())
                .synopsis(request.getSynopsis())
                .heightCm(request.getHeightCm())
                .widthCm(request.getWidthCm())
                .depthCm(request.getDepthCm())
                .weightKg(request.getWeightKg())
                .barcode(request.getBarcode().trim())
                .build();
    }

    public static void applyUpdate(Book book, BookUpdateRequest request) {
        book.setTitle(request.getTitle());
        book.setSalePrice(request.getPrice());
        if (request.getCostPrice() != null) {
            book.setCostPrice(request.getCostPrice());
        }
        book.setIsbn(request.getIsbn().trim());
        book.setMaxSaleValue(request.getMaxSaleValue());
        book.setStockQuantity(request.getStockQuantity());
        book.setActive(Boolean.TRUE.equals(request.getActive()));
        book.setPublicationYear(request.getPublicationYear());
        book.setEdition(request.getEdition());
        book.setPageCount(request.getPageCount());
        book.setSynopsis(request.getSynopsis());
        book.setHeightCm(request.getHeightCm());
        book.setWidthCm(request.getWidthCm());
        book.setDepthCm(request.getDepthCm());
        book.setWeightKg(request.getWeightKg());
        book.setBarcode(request.getBarcode().trim());
    }
}
