package com.bpmnengine.identity.service;

import com.bpmnengine.identity.dto.*;
import com.bpmnengine.identity.entity.*;
import com.bpmnengine.identity.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages users — create, update, delete, role assignment, group membership.
 * Passwords are always stored as BCrypt hashes (strength 10).
 */
@Service
@Transactional
public class UserService {

    private final UserRepository       userRepo;
    private final RoleRepository       roleRepo;
    private final GroupRepository      groupRepo;
    private final MembershipRepository membershipRepo;
    private final BCryptPasswordEncoder bcrypt;

    public UserService(UserRepository userRepo,
                       RoleRepository roleRepo,
                       GroupRepository groupRepo,
                       MembershipRepository membershipRepo) {
        this.userRepo       = userRepo;
        this.roleRepo       = roleRepo;
        this.groupRepo      = groupRepo;
        this.membershipRepo = membershipRepo;
        this.bcrypt         = new BCryptPasswordEncoder(10);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public UserDto create(CreateUserRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IdentityException("Username already taken: " + req.getUsername());
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IdentityException("Email already registered: " + req.getEmail());
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(bcrypt.encode(req.getPassword()));
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setDisplayName(req.getDisplayName() != null
                ? req.getDisplayName()
                : req.getFirstName() + " " + req.getLastName());
        user.setEnabled(true);
        user.setLocked(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepo.save(user);

        // Assign direct roles
        if (req.getRoleNames() != null) {
            req.getRoleNames().forEach(rn -> assignRoleToUser(user.getId(), rn));
        }

        // Join groups
        if (req.getGroupNames() != null) {
            req.getGroupNames().forEach(gn -> addUserToGroup(user.getId(), gn, null));
        }

        return toDto(userRepo.findById(user.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public UserDto getById(String id) {
        return toDto(findUser(id));
    }

    @Transactional(readOnly = true)
    public UserDto getByUsername(String username) {
        return toDto(userRepo.findByUsername(username)
                .orElseThrow(() -> new IdentityException("User not found: " + username)));
    }

    @Transactional(readOnly = true)
    public Page<UserDto> listAll(Pageable pageable) {
        return userRepo.findAll(pageable).map(this::toDto);
    }

    public UserDto update(String id, UpdateUserRequest req) {
        UserEntity user = findUser(id);

        if (req.getEmail() != null) {
            if (userRepo.existsByEmail(req.getEmail())
                    && !req.getEmail().equals(user.getEmail())) {
                throw new IdentityException("Email already in use: " + req.getEmail());
            }
            user.setEmail(req.getEmail());
        }
        if (req.getFirstName()   != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()    != null) user.setLastName(req.getLastName());
        if (req.getDisplayName() != null) user.setDisplayName(req.getDisplayName());
        if (req.getEnabled()     != null) user.setEnabled(req.getEnabled());
        if (req.getLocked()      != null) user.setLocked(req.getLocked());
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            user.setPasswordHash(bcrypt.encode(req.getNewPassword()));
        }
        user.setUpdatedAt(Instant.now());
        return toDto(userRepo.save(user));
    }

    public void delete(String id) {
        UserEntity user = findUser(id);
        userRepo.delete(user);
    }

    // ── Role management ───────────────────────────────────────────────────────

    public UserDto assignRoleToUser(String userId, String roleName) {
        UserEntity user = findUser(userId);
        RoleEntity role = roleRepo.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new IdentityException("Role not found: " + roleName));
        user.getRoles().add(role);
        user.setUpdatedAt(Instant.now());
        return toDto(userRepo.save(user));
    }

    public UserDto removeRoleFromUser(String userId, String roleName) {
        UserEntity user = findUser(userId);
        user.getRoles().removeIf(r -> r.getName().equalsIgnoreCase(roleName));
        user.setUpdatedAt(Instant.now());
        return toDto(userRepo.save(user));
    }

    @Transactional(readOnly = true)
    public Set<String> getEffectiveRoles(String userId) {
        UserEntity user = findUser(userId);
        Set<String> roles = new HashSet<>();
        user.getRoles().forEach(r -> roles.add(r.getName()));
        user.getMemberships().forEach(m ->
                m.getGroup().getRoles().forEach(r -> roles.add(r.getName()))
        );
        return roles;
    }

    @Transactional(readOnly = true)
    public boolean hasRole(String userId, String roleName) {
        return getEffectiveRoles(userId).contains(roleName.toUpperCase());
    }

    // ── Group membership ──────────────────────────────────────────────────────

    public MembershipDto addUserToGroup(String userId, String groupName, String addedBy) {
        UserEntity  user  = findUser(userId);
        GroupEntity group = groupRepo.findByName(groupName)
                .orElseThrow(() -> new IdentityException("Group not found: " + groupName));

        if (membershipRepo.existsByUserIdAndGroupId(user.getId(), group.getId())) {
            throw new IdentityException(
                    user.getUsername() + " is already a member of " + groupName);
        }

        MembershipEntity mem = new MembershipEntity();
        mem.setId(UUID.randomUUID().toString());
        mem.setUser(user);
        mem.setGroup(group);
        mem.setJoinedAt(Instant.now());
        mem.setAddedBy(addedBy);
        membershipRepo.save(mem);

        return toMembershipDto(mem);
    }

    public void removeUserFromGroup(String userId, String groupName) {
        UserEntity  user  = findUser(userId);
        GroupEntity group = groupRepo.findByName(groupName)
                .orElseThrow(() -> new IdentityException("Group not found: " + groupName));
        if (!membershipRepo.existsByUserIdAndGroupId(user.getId(), group.getId())) {
            throw new IdentityException(user.getUsername() + " is not a member of " + groupName);
        }
        membershipRepo.deleteByUserIdAndGroupId(user.getId(), group.getId());
    }

    @Transactional(readOnly = true)
    public List<MembershipDto> getMemberships(String userId) {
        findUser(userId); // existence check
        return membershipRepo.findByUserId(userId).stream()
                .map(this::toMembershipDto)
                .collect(Collectors.toList());
    }

    // ── Lookups for BPMN task resolution ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserDto> findByGroupName(String groupName) {
        return userRepo.findByGroupName(groupName).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> findByEffectiveRole(String roleName) {
        return userRepo.findByEffectiveRoleName(roleName.toUpperCase()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── Password verification ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public boolean verifyPassword(String username, String rawPassword) {
        return userRepo.findByUsername(username)
                .map(u -> bcrypt.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    public UserDto toDto(UserEntity u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setDisplayName(u.getDisplayName());
        dto.setEnabled(u.isEnabled());
        dto.setLocked(u.isLocked());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        dto.setLastLoginAt(u.getLastLoginAt());

        Set<String> directRoles = u.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
        dto.setRoles(directRoles);

        Set<String> groups = u.getMemberships().stream()
                .map(m -> m.getGroup().getName())
                .collect(Collectors.toSet());
        dto.setGroups(groups);

        Set<String> effective = new HashSet<>(directRoles);
        u.getMemberships().forEach(m ->
                m.getGroup().getRoles().forEach(r -> effective.add(r.getName()))
        );
        dto.setEffectiveRoles(effective);
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

    private UserEntity findUser(String id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IdentityException("User not found: " + id));
    }
}
