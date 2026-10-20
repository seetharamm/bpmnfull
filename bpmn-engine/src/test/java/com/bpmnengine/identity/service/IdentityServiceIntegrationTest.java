package com.bpmnengine.identity.service;

import com.bpmnengine.identity.dto.CreateGroupRequest;
import com.bpmnengine.identity.dto.CreateRoleRequest;
import com.bpmnengine.identity.dto.CreateUserRequest;
import com.bpmnengine.identity.dto.UserDto;
import com.bpmnengine.identity.entity.*;
import com.bpmnengine.identity.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for UserService, RoleService, GroupService.
 * Runs against the H2 in-memory DB (test profile), which includes seed data from V2 migration.
 */
@SpringBootTest
@Transactional
class IdentityServiceIntegrationTest {

    @Autowired UserService  userService;
    @Autowired RoleService  roleService;
    @Autowired GroupService groupService;

    @Autowired UserRepository       userRepo;
    @Autowired RoleRepository       roleRepo;
    @Autowired GroupRepository      groupRepo;
    @Autowired MembershipRepository membershipRepo;

    // ── Seed data sanity ─────────────────────────────────────────────────────

    @Test
    void seedData_usersExist() {
        assertThat(userRepo.findByUsername("alice")).isPresent();
        assertThat(userRepo.findByUsername("bob")).isPresent();
        assertThat(userRepo.findByUsername("eve")).isPresent();
    }

    @Test
    void seedData_rolesExist() {
        assertThat(roleRepo.findByName("ADMIN")).isPresent();
        assertThat(roleRepo.findByName("HR_MANAGER")).isPresent();
        assertThat(roleRepo.findByName("IT_STAFF")).isPresent();
    }

    @Test
    void seedData_groupsExist() {
        assertThat(groupRepo.findByName("hr_team")).isPresent();
        assertThat(groupRepo.findByName("it_team")).isPresent();
        assertThat(groupRepo.findByName("administrators")).isPresent();
    }

    @Test
    void seedData_membershipsExist() {
        // bob is in hr_team
        UserEntity bob = userRepo.findByUsername("bob").orElseThrow();
        assertThat(groupRepo.findGroupsByUserId(bob.getId()))
                .extracting("name")
                .contains("hr_team");
    }

    // ── User CRUD ─────────────────────────────────────────────────────────────

    @Test
    void createUser_success() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("testuser1");
        req.setEmail("testuser1@example.com");
        req.setPassword("Secret123");
        req.setFirstName("Test");
        req.setLastName("User");

        UserDto dto = userService.create(req);

        assertThat(dto.getId()).isNotBlank();
        assertThat(dto.getUsername()).isEqualTo("testuser1");
        assertThat(dto.getEmail()).isEqualTo("testuser1@example.com");
        assertThat(dto.isEnabled()).isTrue();
        assertThat(dto.isLocked()).isFalse();
    }

    @Test
    void createUser_duplicateUsername_throws() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("alice"); // already exists
        req.setEmail("newalice@example.com");
        req.setPassword("Secret123");

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(IdentityException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void createUser_duplicateEmail_throws() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("brandnew");
        req.setEmail("alice@example.com"); // already exists
        req.setPassword("Secret123");

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(IdentityException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void getUser_byUsername() {
        UserDto dto = userService.getByUsername("bob");
        assertThat(dto.getUsername()).isEqualTo("bob");
        assertThat(dto.getFirstName()).isEqualTo("Bob");
    }

    @Test
    void getUser_notFound_throws() {
        assertThatThrownBy(() -> userService.getByUsername("doesNotExist"))
                .isInstanceOf(IdentityException.class)
                .hasMessageContaining("User not found");
    }

    // ── Role assignment ───────────────────────────────────────────────────────

    @Test
    void assignRole_andRevoke() {
        // Create a fresh user
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("roletest");
        req.setEmail("roletest@example.com");
        req.setPassword("Pass123!");
        UserDto user = userService.create(req);

        // Assign AUDITOR role
        UserDto updated = userService.assignRoleToUser(user.getId(), "AUDITOR");
        assertThat(updated.getRoles()).contains("AUDITOR");

        // Revoke it
        UserDto revoked = userService.removeRoleFromUser(user.getId(), "AUDITOR");
        assertThat(revoked.getRoles()).doesNotContain("AUDITOR");
    }

    @Test
    void effectiveRoles_includeGroupRoles() {
        // bob is in hr_team, which has HR_MANAGER and HR_STAFF roles
        UserEntity bob = userRepo.findByUsername("bob").orElseThrow();
        Set<String> effective = userService.getEffectiveRoles(bob.getId());

        assertThat(effective).containsAnyOf("HR_MANAGER", "HR_STAFF");
    }

    // ── Group membership ──────────────────────────────────────────────────────

    @Test
    void addAndRemove_membership() {
        // Create user and add to it_team
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("memtest");
        req.setEmail("memtest@example.com");
        req.setPassword("Pass123!");
        UserDto user = userService.create(req);

        userService.addUserToGroup(user.getId(), "it_team", "admin");

        assertThat(userService.getMemberships(user.getId()))
                .extracting("groupName")
                .contains("it_team");

        // Remove
        userService.removeUserFromGroup(user.getId(), "it_team");
        assertThat(userService.getMemberships(user.getId()))
                .extracting("groupName")
                .doesNotContain("it_team");
    }

    @Test
    void addMembership_duplicate_throws() {
        // bob is already in hr_team
        UserEntity bob = userRepo.findByUsername("bob").orElseThrow();
        assertThatThrownBy(() -> userService.addUserToGroup(bob.getId(), "hr_team", "admin"))
                .isInstanceOf(IdentityException.class)
                .hasMessageContaining("already a member");
    }

    // ── Group service ─────────────────────────────────────────────────────────

    @Test
    void createGroup_success() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("new_test_group");
        req.setDescription("Test group");
        req.setType("ORGANISATIONAL");

        var dto = groupService.create(req);
        assertThat(dto.getId()).isNotBlank();
        assertThat(dto.getName()).isEqualTo("new_test_group");
    }

    @Test
    void assignRoleToGroup_andVerify() {
        var group = groupRepo.findByName("hr_team").orElseThrow();
        int before = groupService.getById(group.getId()).getRoles().size();

        groupService.assignRoleToGroup(group.getId(), "FINANCE");

        int after = groupService.getById(group.getId()).getRoles().size();
        assertThat(after).isGreaterThan(before);
    }

    @Test
    void getMembers_ofHrTeam() {
        var group = groupRepo.findByName("hr_team").orElseThrow();
        var members = groupService.getMembers(group.getId());
        // bob, carol, dave, mia are seeded into hr_team
        assertThat(members).hasSizeGreaterThanOrEqualTo(3);
        assertThat(members).extracting("username").contains("bob", "carol", "dave");
    }

    // ── Role service ──────────────────────────────────────────────────────────

    @Test
    void createRole_success() {
        CreateRoleRequest req = new CreateRoleRequest();
        req.setName("TEST_ROLE");
        req.setDescription("A test role");

        var dto = roleService.create(req);
        assertThat(dto.getName()).isEqualTo("TEST_ROLE");
    }

    @Test
    void createRole_duplicate_throws() {
        CreateRoleRequest req = new CreateRoleRequest();
        req.setName("ADMIN"); // already seeded

        assertThatThrownBy(() -> roleService.create(req))
                .isInstanceOf(IdentityException.class)
                .hasMessageContaining("Role already exists");
    }

    @Test
    void listAll_roles() {
        var roles = roleService.listAll();
        assertThat(roles).hasSizeGreaterThanOrEqualTo(9); // 9 seeded
        assertThat(roles).extracting("name").contains("ADMIN", "HR_MANAGER", "IT_STAFF");
    }

    // ── Password verification ─────────────────────────────────────────────────

    @Test
    void verifyPassword_withNewUser() {
        // Create a user with a known password and verify it
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("pwdtest");
        req.setEmail("pwdtest@example.com");
        req.setPassword("MySecret42!");
        userService.create(req);

        assertThat(userService.verifyPassword("pwdtest", "MySecret42!")).isTrue();
        assertThat(userService.verifyPassword("pwdtest", "wrong")).isFalse();
    }

    // ── findByGroupName ───────────────────────────────────────────────────────

    @Test
    void findByGroupName_returnsGroupMembers() {
        var users = userService.findByGroupName("it_team");
        assertThat(users).extracting("username").containsAnyOf("eve", "frank");
    }

    @Test
    void findByEffectiveRole_includesGroupMembers() {
        // hr_team has HR_MANAGER role → bob, carol, dave should appear
        var users = userService.findByEffectiveRole("HR_MANAGER");
        assertThat(users).extracting("username").containsAnyOf("bob", "carol", "dave");
    }
}
