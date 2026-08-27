package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateBrandRequest;
import com.ecm.server.dto.request.UpdateBrandRequest;
import com.ecm.server.dto.response.BrandResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.BrandMapper;
import com.ecm.server.model.Brand;
import com.ecm.server.repository.BrandRepository;
import com.ecm.server.repository.ProductRepository;
import com.ecm.server.service.BrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    public static final String STATUS_DELETED = "DELETED";

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandMapper brandMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        // 1. Retrieve all non-deleted brands from database
        List<Brand> brands = brandRepository.findByStatusNot(STATUS_DELETED);

        // 2. Map entity list to response DTO list via MapStruct
        return brandMapper.toResponseList(brands);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(UUID id) {
        // 1. Fetch brand entity by ID
        Brand brand = brandRepository.findById(id)
                .filter(b -> !STATUS_DELETED.equalsIgnoreCase(b.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.BRAND_NOT_FOUND));

        // 2. Map entity to response DTO via MapStruct
        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        // 1. Check brand name uniqueness
        if (brandRepository.existsByName(request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Brand with name '" + request.getName() + "' already exists");
        }

        // 2. Map DTO to entity via MapStruct and persist
        Brand brand = brandMapper.toEntity(request);
        Brand savedBrand = brandRepository.save(brand);

        // 3. Map and return response DTO
        return brandMapper.toResponse(savedBrand);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(UUID id, UpdateBrandRequest request) {
        // 1. Retrieve existing brand entity
        Brand brand = brandRepository.findById(id)
                .filter(b -> !STATUS_DELETED.equalsIgnoreCase(b.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.BRAND_NOT_FOUND));

        // 2. Validate name uniqueness if changed
        if (!brand.getName().equalsIgnoreCase(request.getName()) && brandRepository.existsByName(request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Brand with name '" + request.getName() + "' already exists");
        }

        // 3. Update entity fields via MapStruct @MappingTarget
        brandMapper.updateEntityFromRequest(request, brand);
        Brand updatedBrand = brandRepository.save(brand);

        // 4. Return updated brand response DTO
        return brandMapper.toResponse(updatedBrand);
    }

    @Override
    @Transactional
    public void deleteBrand(UUID id) {
        // 1. Find brand entity by ID
        Brand brand = brandRepository.findById(id)
                .filter(b -> !STATUS_DELETED.equalsIgnoreCase(b.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.BRAND_NOT_FOUND));

        // 2. Check foreign key constraints: associated products
        long productCount = productRepository.countByBrandId(id);
        if (productCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete brand associated with existing products");
        }

        // 3. Perform soft delete
        brand.setStatus(STATUS_DELETED);
        brandRepository.save(brand);
        log.info("Soft deleted brand with id: {}", id);
    }
}
