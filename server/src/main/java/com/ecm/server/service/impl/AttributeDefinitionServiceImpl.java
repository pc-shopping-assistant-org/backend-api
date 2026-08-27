package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.request.CreateAttributeDefinitionRequest;
import com.ecm.server.dto.request.UpdateAttributeDefinitionRequest;
import com.ecm.server.dto.response.AttributeDefinitionResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.AttributeDefinitionMapper;
import com.ecm.server.model.AttributeDefinition;
import com.ecm.server.repository.AttributeDefinitionRepository;
import com.ecm.server.repository.CategoryAttributeRepository;
import com.ecm.server.service.AttributeDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeDefinitionServiceImpl implements AttributeDefinitionService {

    public static final String STATUS_DELETED = "DELETED";

    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final AttributeDefinitionMapper attributeDefinitionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> getAllAttributes() {
        // 1. Query all active attribute definitions
        List<AttributeDefinition> attributes = attributeDefinitionRepository.findByStatusNot(STATUS_DELETED);

        // 2. Map entity list to DTO list via MapStruct
        return attributeDefinitionMapper.toResponseList(attributes);
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeDefinitionResponse getAttributeById(UUID id) {
        // 1. Fetch attribute definition by ID
        AttributeDefinition attribute = attributeDefinitionRepository.findById(id)
                .filter(a -> !STATUS_DELETED.equalsIgnoreCase(a.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.ATTRIBUTE_NOT_FOUND));

        // 2. Map entity to response DTO via MapStruct
        return attributeDefinitionMapper.toResponse(attribute);
    }

    @Override
    @Transactional
    public AttributeDefinitionResponse createAttribute(CreateAttributeDefinitionRequest request) {
        // 1. Validate key uniqueness
        if (attributeDefinitionRepository.existsByKey(request.getKey())) {
            throw new BusinessException(StatusCode.CONFLICT, "Attribute with key '" + request.getKey() + "' already exists");
        }

        // 2. Map DTO to entity via MapStruct and persist
        AttributeDefinition attribute = attributeDefinitionMapper.toEntity(request);
        AttributeDefinition savedAttribute = attributeDefinitionRepository.save(attribute);

        // 3. Return created attribute response DTO
        return attributeDefinitionMapper.toResponse(savedAttribute);
    }

    @Override
    @Transactional
    public AttributeDefinitionResponse updateAttribute(UUID id, UpdateAttributeDefinitionRequest request) {
        // 1. Fetch existing attribute definition
        AttributeDefinition attribute = attributeDefinitionRepository.findById(id)
                .filter(a -> !STATUS_DELETED.equalsIgnoreCase(a.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.ATTRIBUTE_NOT_FOUND));

        // 2. Update entity fields via MapStruct @MappingTarget
        attributeDefinitionMapper.updateEntityFromRequest(request, attribute);
        AttributeDefinition updatedAttribute = attributeDefinitionRepository.save(attribute);

        // 3. Return updated attribute response DTO
        return attributeDefinitionMapper.toResponse(updatedAttribute);
    }

    @Override
    @Transactional
    public void deleteAttribute(UUID id) {
        // 1. Find attribute definition entity by ID
        AttributeDefinition attribute = attributeDefinitionRepository.findById(id)
                .filter(a -> !STATUS_DELETED.equalsIgnoreCase(a.getStatus()))
                .orElseThrow(() -> new BusinessException(StatusCode.ATTRIBUTE_NOT_FOUND));

        // 2. Check foreign key constraints: assignment to category groups
        long assignmentCount = categoryAttributeRepository.countByAttributeId(id);
        if (assignmentCount > 0) {
            throw new BusinessException(StatusCode.CONFLICT, "Cannot delete attribute assigned to category specification groups");
        }

        // 3. Perform soft delete
        attribute.setStatus(STATUS_DELETED);
        attributeDefinitionRepository.save(attribute);
        log.info("Soft deleted attribute definition with id: {}", id);
    }
}
