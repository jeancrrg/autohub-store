package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.entity.ProductImage;
import com.autohubstore.catalogservice.domain.dto.response.ProductImageResponse;
import com.autohubstore.catalogservice.domain.mapper.ProductImageMapper;
import com.autohubstore.catalogservice.exception.ImageStorageException;
import com.autohubstore.catalogservice.exception.ImageTooLargeException;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.exception.UnsupportedImageTypeException;
import com.autohubstore.catalogservice.repository.ProductImageRepository;
import com.autohubstore.catalogservice.repository.ProductRepository;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductImageMapper productImageMapper;
    private final MinioClient minioClient;

    @Value("${spring.minio.bucket}")
    private String bucket;

    @Transactional(readOnly = true)
    public List<ProductImageResponse> findImages(UUID productId) {
        return productImageMapper.toResponseList(productImageRepository.findByProductId(productId));
    }

    @Transactional
    public List<ProductImageResponse> uploadImages(UUID productId, List<MultipartFile> files) {
        verifyProductExists(productId);
        List<ProductImage> existingImages = productImageRepository.findByProductId(productId);
        boolean hasExistingPrimary = existingImages.stream().anyMatch(ProductImage::isPrimary);
        boolean noImagesYet = existingImages.isEmpty();

        List<ProductImageResponse> uploaded = new ArrayList<>();
        boolean firstInBatch = noImagesYet;
        for (MultipartFile file : files) {
            ProductImage image = uploadOne(productId, file, !hasExistingPrimary && firstInBatch);
            firstInBatch = false;
            uploaded.add(productImageMapper.toResponse(image));
        }
        return uploaded;
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        verifyProductExists(productId);
        ProductImage image = productImageRepository.findById(imageId)
                .filter(img -> img.getProductId().equals(productId))
                .orElseThrow(() -> new ProductNotFoundException(imageId.toString()));

        removeFromMinio(objectKey(image.getUrl()));
        productImageRepository.delete(image);
    }

    private ProductImage uploadOne(UUID productId, MultipartFile file, boolean primary) {
        validate(file);
        String objectKey = productId + "/" + UUID.randomUUID() + extensionOf(file);
        putInMinio(objectKey, file);

        ProductImage image = ProductImage.builder()
                .productId(productId)
                .url(publicUrl(objectKey))
                .primary(primary)
                .build();
        return productImageRepository.save(image);
    }

    private void validate(MultipartFile file) {
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new UnsupportedImageTypeException(file.getContentType());
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImageTooLargeException(file.getOriginalFilename());
        }
    }

    private void putInMinio(String objectKey, MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(input, file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
        }
        catch (MinioException | IOException e) {
            throw new ImageStorageException("falha ao enviar arquivo pro storage: " + e.getMessage(), e);
        }
    }

    private void removeFromMinio(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        }
        catch (MinioException e) {
            throw new ImageStorageException("falha ao remover arquivo do storage: " + e.getMessage(), e);
        }
    }

    private void verifyProductExists(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId.toString());
        }
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
