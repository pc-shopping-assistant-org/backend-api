package com.ecm.server.service;

import com.ecm.server.dto.request.SupplierFilterRequest;
import com.ecm.server.mapper.SupplierMapper;
import com.ecm.server.repository.ProductSupplierRepository;
import com.ecm.server.repository.SupplierRepository;
import com.ecm.server.service.impl.SupplierServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductSupplierRepository productSupplierRepository;
    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierServiceImpl service;

    @Test
    void getSuppliersNormalizesStatusFilter() {
        when(supplierRepository.findSuppliersInitial(any(), eq("INACTIVE"), any(Pageable.class)))
                .thenReturn(List.of());
        when(supplierMapper.toResponseList(any())).thenReturn(List.of());

        service.getSuppliers(SupplierFilterRequest.builder()
                .status(" inactive ")
                .limit(10)
                .build());

        verify(supplierRepository).findSuppliersInitial(any(), eq("INACTIVE"), any(Pageable.class));
    }
}
