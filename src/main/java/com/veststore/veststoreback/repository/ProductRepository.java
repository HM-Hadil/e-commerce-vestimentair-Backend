package com.veststore.veststoreback.repository;

import com.veststore.veststoreback.model.Category;
import com.veststore.veststoreback.model.Product;
import com.veststore.veststoreback.model.ProductSize;
import com.veststore.veststoreback.model.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);

    List<Product> findBySize(Size size);

    List<Product> findByColor(String color);

    List<Product> findByStockLessThanEqual(Integer threshold);

    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT p FROM Product p WHERE " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:size IS NULL OR p.size = :size) AND " +
            "(:color IS NULL OR p.color = :color) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> findWithFilters(Long categoryId, ProductSize size, String color,
                                  BigDecimal minPrice, BigDecimal maxPrice);
}