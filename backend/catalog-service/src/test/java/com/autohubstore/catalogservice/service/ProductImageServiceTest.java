package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.exception.UnsupportedImageTypeException;
import com.autohubstore.catalogservice.repository.ProductRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ProductImageServiceTest {

    private static final String BUCKET = "catalog-images";

    private ProductRepository productRepository;
    private MinioClient minioClient;
    private ProductImageService service;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        minioClient = Mockito.mock(MinioClient.class);
        service = new ProductImageService(productRepository, minioClient, BUCKET);
    }

    @Test
    void rejectsUnsupportedContentType() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(Product.builder().id(productId).images(new java.util.ArrayList<>()).build()));

        MockMultipartFile file = new MockMultipartFile("files", "malware.exe",
                "application/octet-stream", "content".getBytes());

        assertThatThrownBy(() -> service.uploadImages(productId, List.of(file)))
                .isInstanceOf(UnsupportedImageTypeException.class);
    }

    @Test
    void throwsWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg",
                "image/jpeg", "content".getBytes());

        assertThatThrownBy(() -> service.uploadImages(productId, List.of(file)))
                .isInstanceOf(ProductNotFoundException.class);
    }

}
