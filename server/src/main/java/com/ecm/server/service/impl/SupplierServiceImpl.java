package com.ecm.server.service.impl;

import com.ecm.server.common.CursorPageResponse;
import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateSupplierRequest;
import com.ecm.server.dto.request.SupplierFilterRequest;
import com.ecm.server.dto.request.UpdateSupplierRequest;
import com.ecm.server.dto.response.SupplierResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.SupplierMapper;
import com.ecm.server.model.Supplier;
import com.ecm.server.repository.ProductSupplierRepository;
import com.ecm.server.repository.SupplierRepository;
import com.ecm.server.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    public static final String STATUS_DELETED = "DELETED";

    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<SupplierResponse> getSuppliers(SupplierFilterRequest request) {
        // 1. Query limit + 1 supplier records using cursor pagination
        int limit = request.getSanitizedLimit();
        int queryLimit = limit + 1;
        UUID cursorUuid = (request.getCursor() != null && !request.getCursor().isBlank())
                ? UUID.fromString(request.getCursor())
                : null;
        String keywordPattern = (request.getKeyword() != null && !request.getKeyword().isBlank())
                ? "%" + request.getKeyword().trim().toLowerCase() + "%"
                : null;
        String statusFilter = normalizeEnumFilter(request.getStatus());

        Pageable pageable = PageRequest.of(0, queryLimit);
        List<Supplier> suppliers = (cursorUuid == null)
                ? supplierRepository.findSuppliersInitial(keywordPattern, statusFilter, pageable)
                : supplierRepository.findSuppliersAfterCursor(cursorUuid, keywordPattern, statusFilter, pageable);

        // 2. Transform entity list to DTO list via MapStruct
        List<SupplierResponse> dtoList = supplierMapper.toResponseList(suppliers);

        // 3. Build cursor page response
        return CursorPageResponse.of(dtoList, limit, item -> item.getId().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(UUID id) {
        // 1. Fetch supplier entity by ID
        Supplier supplier = supplierRepository.findById(id)
                .filter(s -> !STATUS_DELETED.equalsIgnoreCase(s.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Supplier not found with id: " + id));

        // 2. Map entity to response DTO via MapStruct
        return supplierMapper.toResponse(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        // 1. Verify supplier uniqueness
        if (supplierRepository.existsByName(request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Supplier with name '" + request.getName() + "' already exists");
        }
        if (request.getEmail() != null && supplierRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(StatusCode.EMAIL_ALREADY_EXISTS);
        }
        if (request.getPhone() != null && supplierRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
        }

        // 2. Map DTO to entity via MapStruct and persist
        Supplier supplier = supplierMapper.toEntity(request);
        Supplier savedSupplier = supplierRepository.save(supplier);

        // 3. Map and return response DTO
        return supplierMapper.toResponse(savedSupplier);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(UUID id, UpdateSupplierRequest request) {
        // 1. Retrieve existing supplier entity
        Supplier supplier = supplierRepository.findById(id)
                .filter(s -> !STATUS_DELETED.equalsIgnoreCase(s.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Supplier not found with id: " + id));

        // DELETED must go through deleteSupplier(), which protects the
        // normalized product-supplier links.
        validateMutableStatus(request.getStatus());

        // 2. Validate email and phone uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(supplier.getEmail()) && supplierRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(StatusCode.EMAIL_ALREADY_EXISTS);
        }
        if (request.getPhone() != null && !request.getPhone().equals(supplier.getPhone()) && supplierRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(StatusCode.PHONE_ALREADY_EXISTS);
        }

        // 3. Update entity fields via MapStruct @MappingTarget
        supplierMapper.updateEntityFromRequest(request, supplier);
        Supplier updatedSupplier = supplierRepository.save(supplier);

        // 4. Return updated response DTO
        return supplierMapper.toResponse(updatedSupplier);
    }

    @Override
    @Transactional
    public void deleteSupplier(UUID id) {
        // 1. Find supplier entity by ID
        Supplier supplier = supplierRepository.findById(id)
                .filter(s -> !STATUS_DELETED.equalsIgnoreCase(s.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Supplier not found with id: " + id));

        // 2. Check foreign key constraints: associated products
        long productCount = productSupplierRepository.countBySupplierId(id);
        if (productCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete supplier associated with existing products");
        }

        // 3. Perform soft delete
        supplier.setStatus(STATUS_DELETED);
        supplierRepository.save(supplier);
        log.info("Soft deleted supplier with id: {}", id);
    }

    private void validateMutableStatus(String status) {
        if (status != null && !"ACTIVE".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Only ACTIVE or INACTIVE is allowed here; use the delete endpoint for DELETED");
        }
    }

    private String normalizeEnumFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
