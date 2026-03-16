package com.example.demo4.service;

import java.util.List;
import java.util.Map;

import com.example.demo4.entity.Product;

public interface ProductService {
    Product addProduct(Product product);

    Product getProductById(Long id);

    List<Product> getAllProducts();

    Product updateProduct(Product product);

    void deleteProduct(Long id);

    void batchDelete(List<Long> ids);

    void batchUpdateStock(Map<Long, Integer> stockMap);
}
