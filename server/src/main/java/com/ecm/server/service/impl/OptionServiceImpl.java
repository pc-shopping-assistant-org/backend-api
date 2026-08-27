package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateOptionRequest;
import com.ecm.server.dto.request.UpdateOptionRequest;
import com.ecm.server.dto.response.OptionResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.OptionMapper;
import com.ecm.server.model.Option;
import com.ecm.server.repository.OptionRepository;
import com.ecm.server.repository.VariantOptionRepository;
import com.ecm.server.service.OptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptionServiceImpl implements OptionService {

    public static final String STATUS_DELETED = "DELETED";

    private final OptionRepository optionRepository;
    private final VariantOptionRepository variantOptionRepository;
    private final OptionMapper optionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<OptionResponse> getOptions(String type) {
        // 1. Query options filtered by type or retrieve all active options
        List<Option> options = (type != null && !type.isBlank())
                ? optionRepository.findByTypeIgnoreCaseAndStatusNot(type.trim(), STATUS_DELETED)
                : optionRepository.findByStatusNot(STATUS_DELETED);

        // 2. Map entity list to DTO list via MapStruct
        return optionMapper.toResponseList(options);
    }

    @Override
    @Transactional(readOnly = true)
    public OptionResponse getOptionById(UUID id) {
        // 1. Retrieve option entity by ID
        Option option = optionRepository.findById(id)
                .filter(o -> !STATUS_DELETED.equalsIgnoreCase(o.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.OPTION_NOT_FOUND));

        // 2. Map entity to response DTO via MapStruct
        return optionMapper.toResponse(option);
    }

    @Override
    @Transactional
    public OptionResponse createOption(CreateOptionRequest request) {
        // 1. Validate option name uniqueness
        if (optionRepository.existsByName(request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Option with name '" + request.getName() + "' already exists");
        }

        // 2. Map DTO to entity via MapStruct and persist
        Option option = optionMapper.toEntity(request);
        Option savedOption = optionRepository.save(option);

        // 3. Map and return response DTO
        return optionMapper.toResponse(savedOption);
    }

    @Override
    @Transactional
    public OptionResponse updateOption(UUID id, UpdateOptionRequest request) {
        // 1. Fetch existing option entity
        Option option = optionRepository.findById(id)
                .filter(o -> !STATUS_DELETED.equalsIgnoreCase(o.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.OPTION_NOT_FOUND));

        // 2. Validate name uniqueness if changed
        if (!option.getName().equalsIgnoreCase(request.getName()) && optionRepository.existsByName(request.getName())) {
            throw new BusinessException(StatusCode.CONFLICT, "Option with name '" + request.getName() + "' already exists");
        }

        // 3. Update entity fields via MapStruct @MappingTarget
        optionMapper.updateEntityFromRequest(request, option);
        Option updatedOption = optionRepository.save(option);

        // 4. Return updated option response DTO
        return optionMapper.toResponse(updatedOption);
    }

    @Override
    @Transactional
    public void deleteOption(UUID id) {
        // 1. Fetch option entity by ID
        Option option = optionRepository.findById(id)
                .filter(o -> !STATUS_DELETED.equalsIgnoreCase(o.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.OPTION_NOT_FOUND));

        // 2. Check foreign key constraints: associated product variants
        long usageCount = variantOptionRepository.countByOptionId(id);
        if (usageCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete option associated with existing product variants");
        }

        // 3. Perform soft delete
        option.setStatus(STATUS_DELETED);
        optionRepository.save(option);
        log.info("Soft deleted option with id: {}", id);
    }
}
