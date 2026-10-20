package com.bpmnengine.identity.service;

import com.bpmnengine.identity.dto.*;
import com.bpmnengine.identity.entity.*;
import com.bpmnengine.identity.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages roles — create, update, delete, list users with a given role.
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepo;

    public RoleService(RoleRepository roleRepo) {
        this.roleRepo = roleRepo;
    }

    public RoleDto create(CreateRoleRequest req) {
        String normalised = req.getName().toUpperCase().trim().replace(" ", "_");
        if (roleRepo.existsByName(normalised)) {
            throw new IdentityException("Role already exists: " + normalised);
        }
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID().toString());
        role.setName(normalised);
        role.setDescription(req.getDescription());
        role.setCreatedAt(Instant.now());
        role.setUpdatedAt(Instant.now());
        return toDto(roleRepo.save(role));
    }

    @Transactional(readOnly = true)
    public RoleDto getById(String id) {
        return toDto(findRole(id));
    }

    @Transactional(readOnly = true)
    public RoleDto getByName(String name) {
        return toDto(roleRepo.findByName(name.toUpperCase())
                .orElseThrow(() -> new IdentityException("Role not found: " + name)));
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listAll() {
        return roleRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public RoleDto update(String id, CreateRoleRequest req) {
        RoleEntity role = findRole(id);
        if (req.getName() != null) {
            String normalised = req.getName().toUpperCase().trim();
            if (!normalised.equals(role.getName()) && roleRepo.existsByName(normalised)) {
                throw new IdentityException("Role name already taken: " + normalised);
            }
            role.setName(normalised);
        }
        if (req.getDescription() != null) role.setDescription(req.getDescription());
        role.setUpdatedAt(Instant.now());
        return toDto(roleRepo.save(role));
    }

    public void delete(String id) {
        roleRepo.delete(findRole(id));
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    public RoleDto toDto(RoleEntity r) {
        RoleDto dto = new RoleDto();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setDescription(r.getDescription());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUserCount(r.getUsers().size() + (int) r.getGroups().stream()
                .mapToLong(g -> g.getMemberships().size()).sum());
        return dto;
    }

    private RoleEntity findRole(String id) {
        return roleRepo.findById(id)
                .orElseThrow(() -> new IdentityException("Role not found: " + id));
    }
}
