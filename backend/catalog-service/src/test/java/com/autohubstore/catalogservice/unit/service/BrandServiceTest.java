package com.autohubstore.catalogservice.unit.service;

import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;
import com.autohubstore.catalogservice.domain.entity.Brand;
import com.autohubstore.catalogservice.domain.mapper.BrandMapper;
import com.autohubstore.catalogservice.exception.BrandNotFoundException;
import com.autohubstore.catalogservice.repository.BrandRepository;
import com.autohubstore.catalogservice.service.BrandService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    private static final UUID FIRST_BRAND_ID = UUID.randomUUID();
    private static final UUID SECOND_BRAND_ID = UUID.randomUUID();

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @InjectMocks
    private BrandService brandService;

    @Test
    @DisplayName("Deve retornar as marcas na mesma ordem devolvida pelo repositorio")
    void shouldReturnBrandsInRepositoryOrder() {
        Brand first = Brand.builder().id(FIRST_BRAND_ID).name("Brembo").slug("brembo").build();
        Brand second = Brand.builder().id(SECOND_BRAND_ID).name("NGK").slug("ngk").build();
        BrandResponse firstResponse = new BrandResponse(FIRST_BRAND_ID, "Brembo", "brembo");
        BrandResponse secondResponse = new BrandResponse(SECOND_BRAND_ID, "NGK", "ngk");

        when(brandRepository.findAllByOrderByNameAsc()).thenReturn(List.of(first, second));
        when(brandMapper.toResponse(first)).thenReturn(firstResponse);
        when(brandMapper.toResponse(second)).thenReturn(secondResponse);

        List<BrandResponse> result = brandService.findBrands();

        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    @Test
    @DisplayName("Deve retornar a marca quando o id existir")
    void shouldReturnBrandWhenIdExists() {
        Brand brand = Brand.builder().id(FIRST_BRAND_ID).name("Brembo").slug("brembo").build();
        when(brandRepository.findById(FIRST_BRAND_ID)).thenReturn(Optional.of(brand));

        Brand result = brandService.findEntityOrThrow(FIRST_BRAND_ID);

        assertThat(result).isEqualTo(brand);
    }

    @Test
    @DisplayName("Deve lancar BrandNotFoundException quando a marca nao existir")
    void shouldThrowBrandNotFoundExceptionWhenBrandDoesNotExist() {
        when(brandRepository.findById(FIRST_BRAND_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.findEntityOrThrow(FIRST_BRAND_ID))
                .isInstanceOf(BrandNotFoundException.class);
    }

}
