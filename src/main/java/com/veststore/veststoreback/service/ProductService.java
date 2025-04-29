package com.veststore.veststoreback.service;

import com.veststore.veststoreback.dto.ProductDto;
import com.veststore.veststoreback.exception.ResourceNotFoundException;
import com.veststore.veststoreback.model.Category;
import com.veststore.veststoreback.model.Product;
import com.veststore.veststoreback.model.ProductSize;
import com.veststore.veststoreback.repository.CategoryRepository;
import com.veststore.veststoreback.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        return productRepository.findByCategory(category);
    }

    public List<Product> getProductsWithLowStock() {
        return productRepository.findAll().stream()
                .filter(Product::hasLowStock)
                .collect(Collectors.toList());
    }

    public List<Product> filterProducts(Long categoryId, ProductSize size, String color,
                                        BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findWithFilters(categoryId, size, color, minPrice, maxPrice);
    }

    @Transactional
    public Product createProduct(ProductDto productDto) {
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDto.getCategoryId()));

        Product product = new Product();
        mapDtoToProduct(productDto, product, category);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductDto productDto) {
        Product product = getProductById(id);
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDto.getCategoryId()));

        mapDtoToProduct(productDto, product, category);

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    @Transactional
    public void updateStock(Long productId, int quantity) {
        Product product = getProductById(productId);
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }

    private void mapDtoToProduct(ProductDto dto, Product product, Category category) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSize(dto.getSize());
        product.setColor(dto.getColor());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(category);
        product.setImageUrl(dto.getImageUrl());

        if (dto.getLowStockThreshold() != null) {
            product.setLowStockThreshold(dto.getLowStockThreshold());
        }
    }
}