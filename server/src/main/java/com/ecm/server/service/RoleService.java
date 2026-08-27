package com.ecm.server.service;

import com.ecm.server.dto.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(UUID id);
}
