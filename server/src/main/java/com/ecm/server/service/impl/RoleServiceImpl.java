package com.ecm.server.service.impl;

import com.ecm.server.common.StatusCode;
import com.ecm.server.dto.response.RoleResponse;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.mapper.RoleMapper;
import com.ecm.server.model.Role;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        // 1. Query all role entities from database
        List<Role> roles = roleRepository.findAll();

        // 2. Map role entities to DTO list via MapStruct
        return roleMapper.toResponseList(roles);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
        // 1. Find role by ID
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StatusCode.NOT_FOUND, "Role not found with id: " + id));

        // 2. Map role entity to DTO via MapStruct
        return roleMapper.toResponse(role);
    }
}
