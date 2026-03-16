package com.example.demo4.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo4.entity.Product;
import com.example.demo4.repository.ProductRepository;
import com.example.demo4.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        productRepository.deleteAllByIdInBatch(ids);
    }

    @Override
    @Transactional
    public void batchUpdateStock(Map<Long, Integer> stockMap) {
        List<Product> products = productRepository.findAllById(stockMap.keySet());
        for (Product product : products) {
            product.setStock(stockMap.get(product.getId()));
        }
        productRepository.saveAll(products);
    }
}
