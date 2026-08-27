package com.ecm.server.service;

import com.ecm.server.dto.request.CreateBrandRequest;
import com.ecm.server.dto.request.UpdateBrandRequest;
import com.ecm.server.dto.response.BrandResponse;

import java.util.List;
import java.util.UUID;

public interface BrandService {

    List<BrandResponse> getAllBrands();

    BrandResponse getBrandById(UUID id);

    BrandResponse createBrand(CreateBrandRequest request);

    BrandResponse updateBrand(UUID id, UpdateBrandRequest request);

    void deleteBrand(UUID id);
}
