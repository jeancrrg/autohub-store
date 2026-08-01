package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.domain.entity.ProductImage;
import com.autohubstore.catalogservice.domain.dto.response.ProductImageResponse;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.exception.UnsupportedImageTypeException;
import com.autohubstore.catalogservice.repository.ProductRepository;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductImageService {

    private final ProductRepository productRepository;
    private final MinioClient minioClient;

    public ProductImageService(ProductRepository productRepository, MinioClient minioClient) {
        this.productRepository = productRepository;
        this.minioClient = minioClient;
    }

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    @Value("${spring.minio.bucket}")
    private String bucket;

    @Transactional
    public List<ProductImageResponse> uploadImages(UUID productId, List<MultipartFile> files) {
        Product product = findProductOrThrow(productId);
        boolean hasExistingPrimary = product.getImages().stream().anyMatch(ProductImage::isPrimary);

        List<ProductImageResponse> uploaded = files.stream()
                .map(file -> uploadOne(product, file, hasExistingPrimary))
                .collect(Collectors.toList());

        productRepository.save(product);
        return uploaded;
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        Product product = findProductOrThrow(productId);
        ProductImage image = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ProductNotFoundException(imageId.toString()));

        removeFromMinio(objectKey(image.getUrl()));
        product.getImages().remove(image);
        productRepository.save(product);
    }

    private ProductImageResponse uploadOne(Product product, MultipartFile file, boolean hasExistingPrimary) {
        validate(file);
        String objectKey = product.getId() + "/" + UUID.randomUUID() + extensionOf(file);
        putInMinio(objectKey, file);

        ProductImage image = ProductImage.builder()
                .product(product)
                .url(publicUrl(objectKey))
                .primary(!hasExistingPrimary && product.getImages().isEmpty())
                .build();
        product.getImages().add(image);
        return new ProductImageResponse(image.getId(), image.getUrl(), image.isPrimary());
    }

    private void validate(MultipartFile file) {
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new UnsupportedImageTypeException(file.getContentType());
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new UnsupportedImageTypeException("arquivo excede 5MB: " + file.getOriginalFilename());
        }
    }

    private void putInMinio(String objectKey, MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(input, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new UnsupportedImageTypeException("falha ao enviar arquivo pro storage: " + e.getMessage());
        }
    }

    private void removeFromMinio(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new UnsupportedImageTypeException("falha ao remover arquivo do storage: " + e.getMessage());
        }
    }

    private Product findProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));
    }

    private String extensionOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        int dotIndex = name == null ? -1 : name.lastIndexOf('.');
        return dotIndex >= 0 ? name.substring(dotIndex) : "";
    }

    private String publicUrl(String objectKey) {
        return "/" + bucket + "/" + objectKey;
    }

    private String objectKey(String url) {
        return url.replaceFirst("^/" + bucket + "/", "");
    }

}
