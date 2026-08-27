package com.ecm.server.service;

import com.ecm.server.dto.request.CreateOptionRequest;
import com.ecm.server.dto.request.UpdateOptionRequest;
import com.ecm.server.dto.response.OptionResponse;

import java.util.List;
import java.util.UUID;

public interface OptionService {

    List<OptionResponse> getOptions(String type);

    OptionResponse getOptionById(UUID id);

    OptionResponse createOption(CreateOptionRequest request);

    OptionResponse updateOption(UUID id, UpdateOptionRequest request);

    void deleteOption(UUID id);
}
