package com.example.demo4.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo4.entity.Product;
import com.example.demo4.service.ProductService;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadProduct(
            @RequestParam("name") String name,
            @RequestParam("price") BigDecimal price,
            @RequestParam("description") String description,
            @RequestParam("image") MultipartFile image) {

        Map<String, Object> result = new HashMap<>();

        if (image.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择图片");
            return result;
        }

        // 验证文件类型
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            result.put("success", false);
            result.put("message", "只能上传图片文件");
            return result;
        }

        try {
            // 创建上传目录
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = image.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            image.transferTo(filePath);

            // 保存商品信息
            Product product = new Product();
            product.setName(name);
            product.setPrice(price);
            product.setDescription(description);
            product.setImagePath("/uploads/" + filename);

            productService.addProduct(product);

            result.put("success", true);
            result.put("message", "商品上传成功");
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/list")
    public List<Product> listProducts() {
        return productService.getAllProducts();
    }

    @PutMapping("/update")
    public Map<String, Object> updateProduct(
            @RequestParam("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("price") BigDecimal price,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        Map<String, Object> result = new HashMap<>();
        Product product = productService.getProductById(id);
        if (product == null) {
            result.put("success", false);
            result.put("message", "商品不存在");
            return result;
        }

        product.setName(name);
        product.setPrice(price);
        product.setDescription(description);

        if (image != null && !image.isEmpty()) {
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("success", false);
                result.put("message", "只能上传图片文件");
                return result;
            }
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String originalFilename = image.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String filename = UUID.randomUUID().toString() + extension;
                Path filePath = uploadPath.resolve(filename);
                image.transferTo(filePath);
                product.setImagePath("/uploads/" + filename);
            } catch (IOException e) {
                result.put("success", false);
                result.put("message", "图片上传失败: " + e.getMessage());
                return result;
            }
        }

        productService.updateProduct(product);
        result.put("success", true);
        result.put("message", "商品更新成功");
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteProduct(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Product product = productService.getProductById(id);
        if (product == null) {
            result.put("success", false);
            result.put("message", "商品不存在");
            return result;
        }
        productService.deleteProduct(id);
        result.put("success", true);
        result.put("message", "商品删除成功");
        return result;
    }
}
