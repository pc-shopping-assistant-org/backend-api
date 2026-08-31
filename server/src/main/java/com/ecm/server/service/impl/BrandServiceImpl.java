package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateBrandRequest;
import com.ecm.server.dto.request.UpdateBrandRequest;
import com.ecm.server.dto.response.BrandResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.BrandMapper;
import com.ecm.server.model.Brand;
import com.ecm.server.repository.BrandRepository;
import com.ecm.server.repository.FileRepository;
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
    private final FileRepository fileRepository;
    private final BrandMapper brandMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        // 1. Retrieve all non-deleted brands from database
        List<Brand> brands = brandRepository.findByStatusNot(STATUS_DELETED);

        // 2. Map normalized brand/file references to response DTOs via MapStruct
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
        brand.setImageFileId(resolveImageFileId(request.getFileId()));
        brand.setSeoName(toSeoName(request.getName()));
        if (brandRepository.existsBySeoName(brand.getSeoName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Brand SEO name already exists");
        }
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

        // DELETED must go through deleteBrand(), which protects products that
        // still reference this brand.
        validateMutableStatus(request.getStatus());

        // 2. Validate name uniqueness if changed
        if (!brand.getName().equalsIgnoreCase(request.getName()) && brandRepository.existsByName(request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Brand with name '" + request.getName() + "' already exists");
        }

        // 3. Update entity fields via MapStruct @MappingTarget
        String previousName = brand.getName();
        brandMapper.updateEntityFromRequest(request, brand);
        UUID imageFileId = resolveImageFileId(request.getFileId());
        if (imageFileId != null) {
            brand.setImageFileId(imageFileId);
        }
        if (!previousName.equalsIgnoreCase(request.getName())) {
            String seoName = toSeoName(request.getName());
            if (brandRepository.existsBySeoName(seoName) && !seoName.equalsIgnoreCase(brand.getSeoName())) {
                throw new BusinessException(StatusCode.CONFLICT, "Brand SEO name already exists");
            }
            brand.setSeoName(seoName);
        }
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

    private String toSeoName(String name) {
        return java.text.Normalizer.normalize(name == null ? "" : name.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private void validateMutableStatus(String status) {
        if (status != null && !"ACTIVE".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new BusinessException(StatusCode.BAD_REQUEST,
                    "Only ACTIVE or INACTIVE is allowed here; use the delete endpoint for DELETED");
        }
    }

    private UUID resolveImageFileId(UUID fileId) {
        if (fileId != null) {
            return fileRepository.findById(fileId)
                    .filter(file -> "ACTIVE".equalsIgnoreCase(file.getStatus()))
                    .orElseThrow(() -> new BusinessException(StatusCode.IMAGE_NOT_FOUND,
                            "Referenced brand image file was not found"))
                    .getId();
        }
        return null;
    }
}
