package com.ecm.server.service;

import com.ecm.server.dto.request.CreateAttributeDefinitionRequest;
import com.ecm.server.dto.request.UpdateAttributeDefinitionRequest;
import com.ecm.server.dto.response.AttributeDefinitionResponse;

import java.util.List;
import java.util.UUID;

public interface AttributeDefinitionService {

    List<AttributeDefinitionResponse> getAllAttributes();

    AttributeDefinitionResponse getAttributeById(UUID id);

    AttributeDefinitionResponse createAttribute(CreateAttributeDefinitionRequest request);

    AttributeDefinitionResponse updateAttribute(UUID id, UpdateAttributeDefinitionRequest request);

    void deleteAttribute(UUID id);
}
