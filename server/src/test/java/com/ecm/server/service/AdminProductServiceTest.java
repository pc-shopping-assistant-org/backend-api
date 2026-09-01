package com.ecm.server.service;

import com.ecm.server.common.StatusCode;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.dto.response.ProductDetailResponse;
import com.ecm.server.mapper.ProductImageMapper;
import com.ecm.server.mapper.ProductMapper;
import com.ecm.server.mapper.ProductVariantMapper;
import com.ecm.server.mapper.SupplierMapper;
import com.ecm.server.model.Product;
import com.ecm.server.model.ProductVariant;
import com.ecm.server.repository.BrandRepository;
import com.ecm.server.repository.CategoryRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.FileRepository;
import com.ecm.server.repository.OrderItemRepository;
import com.ecm.server.repository.OptionRepository;
import com.ecm.server.repository.ProductImageRepository;
import com.ecm.server.repository.ProductRepository;
import com.ecm.server.repository.ProductSupplierRepository;
import com.ecm.server.repository.ProductVariantRepository;
import com.ecm.server.repository.SupplierRepository;
import com.ecm.server.repository.VariantOptionRepository;
import com.ecm.server.service.impl.AdminProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductSupplierRepository productSupplierRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private FileRepository fileRepository;
    @Mock private OptionRepository optionRepository;
    @Mock private VariantOptionRepository variantOptionRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ProductVariantMapper productVariantMapper;
    @Mock private ProductImageMapper productImageMapper;
    @Mock private SupplierMapper supplierMapper;

    @InjectMocks private AdminProductServiceImpl service;

    @Test
    void getAdminProductByIdIncludesInactiveVariantsForEditing() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder().id(productId).status("INACTIVE").build();
        ProductVariant variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .status("INACTIVE")
                .quantity(0)
                .build();
        ProductDetailResponse response = ProductDetailResponse.builder()
                .id(productId)
                .status("INACTIVE")
                .build();

        when(productRepository.findAdminDetailById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByProductIdWithDetails(productId, "DELETED"))
                .thenReturn(List.of(variant));
        when(productMapper.toDetailResponse(product)).thenReturn(response);
        when(productVariantMapper.toResponseList(List.of(variant))).thenReturn(List.of());

        ProductDetailResponse result = service.getAdminProductById(productId);

        assertEquals(productId, result.getId());
        assertEquals("INACTIVE", result.getStatus());
    }

    @Test
    void deleteProductRejectsProductWithRemainingInventory() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder().id(productId).status("ACTIVE").build();
        ProductVariant variant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(product)
                .status("ACTIVE")
                .quantity(1)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByProductIdWithDetails(productId, "DELETED"))
                .thenReturn(List.of(variant));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.deleteProduct(productId));

        assertEquals(StatusCode.CONFLICT, exception.getStatusCode());
    }
}
