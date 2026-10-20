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
 * Manages groups — create, update, delete, member listing, role assignment.
 */
@Service
@Transactional
public class GroupService {

    private final GroupRepository      groupRepo;
    private final RoleRepository       roleRepo;
    private final MembershipRepository membershipRepo;

    public GroupService(GroupRepository groupRepo,
                        RoleRepository roleRepo,
                        MembershipRepository membershipRepo) {
        this.groupRepo      = groupRepo;
        this.roleRepo       = roleRepo;
        this.membershipRepo = membershipRepo;
    }

    public GroupDto create(CreateGroupRequest req) {
        if (groupRepo.existsByName(req.getName())) {
            throw new IdentityException("Group already exists: " + req.getName());
        }
        GroupEntity group = new GroupEntity();
        group.setId(UUID.randomUUID().toString());
        group.setName(req.getName());
        group.setDescription(req.getDescription());
        group.setType(req.getType() != null ? req.getType() : "ORGANISATIONAL");
        group.setParentId(req.getParentId());
        group.setCreatedAt(Instant.now());
        group.setUpdatedAt(Instant.now());
        groupRepo.save(group);

        if (req.getRoleNames() != null) {
            req.getRoleNames().forEach(rn -> assignRoleToGroup(group.getId(), rn));
        }

        return toDto(groupRepo.findById(group.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public GroupDto getById(String id) {
        return toDto(findGroup(id));
    }

    @Transactional(readOnly = true)
    public GroupDto getByName(String name) {
        return toDto(groupRepo.findByName(name)
                .orElseThrow(() -> new IdentityException("Group not found: " + name)));
    }

    @Transactional(readOnly = true)
    public List<GroupDto> listAll() {
        return groupRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDto> listByType(String type) {
        return groupRepo.findByType(type.toUpperCase()).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public GroupDto update(String id, CreateGroupRequest req) {
        GroupEntity group = findGroup(id);
        if (req.getName() != null
                && !req.getName().equals(group.getName())
                && groupRepo.existsByName(req.getName())) {
            throw new IdentityException("Group name already taken: " + req.getName());
        }
        if (req.getName()        != null) group.setName(req.getName());
        if (req.getDescription() != null) group.setDescription(req.getDescription());
        if (req.getType()        != null) group.setType(req.getType());
        if (req.getParentId()    != null) group.setParentId(req.getParentId());
        group.setUpdatedAt(Instant.now());
        return toDto(groupRepo.save(group));
    }

    public void delete(String id) {
        groupRepo.delete(findGroup(id));
    }

    // ── Role assignment ───────────────────────────────────────────────────────

    public GroupDto assignRoleToGroup(String groupId, String roleName) {
        GroupEntity group = findGroup(groupId);
        RoleEntity  role  = roleRepo.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new IdentityException("Role not found: " + roleName));
        group.getRoles().add(role);
        group.setUpdatedAt(Instant.now());
        return toDto(groupRepo.save(group));
    }

    public GroupDto removeRoleFromGroup(String groupId, String roleName) {
        GroupEntity group = findGroup(groupId);
        group.getRoles().removeIf(r -> r.getName().equalsIgnoreCase(roleName));
        group.setUpdatedAt(Instant.now());
        return toDto(groupRepo.save(group));
    }

    // ── Member listing ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MembershipDto> getMembers(String groupId) {
        findGroup(groupId); // existence check
        return membershipRepo.findByGroupId(groupId).stream()
                .map(this::toMembershipDto)
                .collect(Collectors.toList());
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    public GroupDto toDto(GroupEntity g) {
        GroupDto dto = new GroupDto();
        dto.setId(g.getId());
        dto.setName(g.getName());
        dto.setDescription(g.getDescription());
        dto.setType(g.getType());
        dto.setParentId(g.getParentId());
        dto.setCreatedAt(g.getCreatedAt());
        dto.setRoles(g.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet()));
        dto.setMemberCount(g.getMemberships().size());
        return dto;
    }

    private MembershipDto toMembershipDto(MembershipEntity m) {
        MembershipDto dto = new MembershipDto();
        dto.setId(m.getId());
        dto.setUserId(m.getUser().getId());
        dto.setUsername(m.getUser().getUsername());
        dto.setDisplayName(m.getUser().getDisplayName());
        dto.setGroupId(m.getGroup().getId());
        dto.setGroupName(m.getGroup().getName());
        dto.setJoinedAt(m.getJoinedAt());
        dto.setAddedBy(m.getAddedBy());
        return dto;
    }

    private GroupEntity findGroup(String id) {
        return groupRepo.findById(id)
                .orElseThrow(() -> new IdentityException("Group not found: " + id));
    }
}
