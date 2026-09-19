package com.autohubstore.catalogservice.unit.service;

import com.autohubstore.catalogservice.domain.dto.response.ProductImageResponse;
import com.autohubstore.catalogservice.domain.entity.ProductImage;
import com.autohubstore.catalogservice.domain.mapper.ProductImageMapper;
import com.autohubstore.catalogservice.exception.ImageStorageException;
import com.autohubstore.catalogservice.exception.ImageTooLargeException;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.exception.UnsupportedImageTypeException;
import com.autohubstore.catalogservice.repository.ProductImageRepository;
import com.autohubstore.catalogservice.repository.ProductRepository;
import com.autohubstore.catalogservice.service.ProductImageService;

import io.minio.MinioClient;
import io.minio.errors.MinioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID IMAGE_ID = UUID.randomUUID();
    private static final String BUCKET_NAME = "catalog-images";
    private static final long OVERSIZED_LENGTH = 6L * 1024 * 1024;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private MinioClient minioClient;

    private ProductImageService productImageService;

    @BeforeEach
    void setUp() {
        productImageService = new ProductImageService(productRepository, productImageRepository,
                productImageMapper, minioClient);
        ReflectionTestUtils.setField(productImageService, "bucket", BUCKET_NAME);
    }

    @Test
    @DisplayName("Deve marcar a primeira imagem enviada como principal quando o produto nao tiver imagens")
    void shouldMarkFirstUploadedImageAsPrimaryWhenProductHasNoImages() {
        MultipartFile file = new MockMultipartFile("files", "foto.png", "image/png", "conteudo".getBytes());
        ProductImage savedImage = ProductImage.builder().id(IMAGE_ID).productId(PRODUCT_ID).primary(true).build();

        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(productImageRepository.findByProductId(PRODUCT_ID)).thenReturn(Collections.emptyList());
        when(productImageRepository.save(any())).thenReturn(savedImage);
        when(productImageMapper.toResponse(savedImage)).thenReturn(new ProductImageResponse(IMAGE_ID, "url", true));

        List<ProductImageResponse> result = productImageService.uploadImages(PRODUCT_ID, List.of(file));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).primary()).isTrue();
        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageRepository).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().isPrimary()).isTrue();
    }

    @Test
    @DisplayName("Nao deve marcar como principal quando o produto ja tiver uma imagem principal")
    void shouldNotMarkAsPrimaryWhenProductAlreadyHasAPrimaryImage() {
        MultipartFile file = new MockMultipartFile("files", "foto.png", "image/png", "conteudo".getBytes());
        ProductImage existingPrimary = ProductImage.builder().id(UUID.randomUUID()).productId(PRODUCT_ID)
                .primary(true).build();
        ProductImage savedImage = ProductImage.builder().id(IMAGE_ID).productId(PRODUCT_ID).primary(false).build();

        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(productImageRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of(existingPrimary));
        when(productImageRepository.save(any())).thenReturn(savedImage);
        when(productImageMapper.toResponse(savedImage)).thenReturn(new ProductImageResponse(IMAGE_ID, "url", false));

        productImageService.uploadImages(PRODUCT_ID, List.of(file));

        ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageRepository).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().isPrimary()).isFalse();
    }

    @Test
    @DisplayName("Deve lancar UnsupportedImageTypeException quando o tipo de arquivo nao for suportado")
    void shouldThrowUnsupportedImageTypeExceptionWhenContentTypeIsNotSupported() {
        MultipartFile file = new MockMultipartFile("files", "documento.pdf", "application/pdf", new byte[]{1});
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(productImageRepository.findByProductId(PRODUCT_ID)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> productImageService.uploadImages(PRODUCT_ID, List.of(file)))
                .isInstanceOf(UnsupportedImageTypeException.class);

        verify(productImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar ImageTooLargeException quando o arquivo exceder o tamanho maximo permitido")
    void shouldThrowImageTooLargeExceptionWhenFileExceedsMaxSize() {
        byte[] oversizedContent = new byte[(int) OVERSIZED_LENGTH];
        MultipartFile file = new MockMultipartFile("files", "foto-grande.png", "image/png", oversizedContent);
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(productImageRepository.findByProductId(PRODUCT_ID)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> productImageService.uploadImages(PRODUCT_ID, List.of(file)))
                .isInstanceOf(ImageTooLargeException.class);

        verify(productImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar ProductNotFoundException ao enviar imagem para produto inexistente")
    void shouldThrowProductNotFoundExceptionWhenUploadingImageForNonexistentProduct() {
        MultipartFile file = new MockMultipartFile("files", "foto.png", "image/png", new byte[]{1});
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> productImageService.uploadImages(PRODUCT_ID, List.of(file)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lancar ImageStorageException quando o storage falhar ao enviar o arquivo")
    void shouldThrowImageStorageExceptionWhenStorageFailsToReceiveFile() {
        MultipartFile file = new MockMultipartFile("files", "foto.png", "image/png", new byte[]{1});
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(productImageRepository.findByProductId(PRODUCT_ID)).thenReturn(Collections.emptyList());
        stubPutObjectToThrowMinioException();

        assertThatThrownBy(() -> productImageService.uploadImages(PRODUCT_ID, List.of(file)))
                .isInstanceOf(ImageStorageException.class);
    }

    private void stubPutObjectToThrowMinioException() {
        try {
            doThrow(new MinioException("falha de rede")).when(minioClient).putObject(any());
        }
        catch (MinioException e) {
            throw new IllegalStateException("Falha ao configurar mock do MinioClient", e);
        }
    }

    @Test
    @DisplayName("Deve remover a imagem quando ela pertencer ao produto informado")
    void shouldDeleteImageWhenItBelongsToTheInformedProduct() {
        ProductImage image = ProductImage.builder().id(IMAGE_ID).productId(PRODUCT_ID)
                .url("/" + BUCKET_NAME + "/" + PRODUCT_ID + "/arquivo.png").primary(true).build();
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(productImageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));

        productImageService.deleteImage(PRODUCT_ID, IMAGE_ID);

        verify(productImageRepository).delete(image);
    }

    @Test
    @DisplayName("Deve lancar ProductNotFoundException ao remover imagem que nao pertence ao produto informado")
    void shouldThrowProductNotFoundExceptionWhenImageDoesNotBelongToTheInformedProduct() {
        UUID otherProductId = UUID.randomUUID();
        ProductImage image = ProductImage.builder().id(IMAGE_ID).productId(otherProductId)
                .url("/" + BUCKET_NAME + "/" + otherProductId + "/arquivo.png").primary(true).build();
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(productImageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> productImageService.deleteImage(PRODUCT_ID, IMAGE_ID))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productImageRepository, never()).delete(any());
    }

}
