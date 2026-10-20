package com.bpmnengine.identity.api;

import com.bpmnengine.identity.dto.*;
import com.bpmnengine.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API for user management.
 *
 * POST   /api/identity/users                      — create user
 * GET    /api/identity/users                      — list users (paged)
 * GET    /api/identity/users/{id}                 — get user by id
 * GET    /api/identity/users/by-username/{name}   — get user by username
 * PUT    /api/identity/users/{id}                 — update user
 * DELETE /api/identity/users/{id}                 — delete user
 * POST   /api/identity/users/{id}/roles/{role}    — assign role
 * DELETE /api/identity/users/{id}/roles/{role}    — revoke role
 * GET    /api/identity/users/{id}/roles           — effective roles
 * GET    /api/identity/users/{id}/memberships     — list memberships
 * POST   /api/identity/users/{id}/memberships     — join group
 * DELETE /api/identity/users/{id}/memberships/{group} — leave group
 * POST   /api/identity/users/verify-password      — verify credentials
 * GET    /api/identity/users/by-group/{group}     — users in a group
 * GET    /api/identity/users/by-role/{role}       — users with an effective role
 */
@RestController
@RequestMapping("/api/identity/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest req) {
        UserDto created = userService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<UserDto> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username") String sort) {
        return userService.listAll(PageRequest.of(page, size, Sort.by(sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getByUsername(username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable String id,
                                           @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Role management ───────────────────────────────────────────────────────

    @PostMapping("/{id}/roles/{roleName}")
    public ResponseEntity<UserDto> assignRole(@PathVariable String id,
                                               @PathVariable String roleName) {
        return ResponseEntity.ok(userService.assignRoleToUser(id, roleName));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<UserDto> revokeRole(@PathVariable String id,
                                               @PathVariable String roleName) {
        return ResponseEntity.ok(userService.removeRoleFromUser(id, roleName));
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<Set<String>> getEffectiveRoles(@PathVariable String id) {
        return ResponseEntity.ok(userService.getEffectiveRoles(id));
    }

    // ── Group membership ──────────────────────────────────────────────────────

    @GetMapping("/{id}/memberships")
    public ResponseEntity<List<MembershipDto>> getMemberships(@PathVariable String id) {
        return ResponseEntity.ok(userService.getMemberships(id));
    }

    @PostMapping("/{id}/memberships")
    public ResponseEntity<MembershipDto> joinGroup(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String groupName = body.get("groupName");
        String addedBy   = body.get("addedBy");
        return ResponseEntity.ok(userService.addUserToGroup(id, groupName, addedBy));
    }

    @DeleteMapping("/{id}/memberships/{groupName}")
    public ResponseEntity<Void> leaveGroup(@PathVariable String id,
                                            @PathVariable String groupName) {
        userService.removeUserFromGroup(id, groupName);
        return ResponseEntity.noContent().build();
    }

    // ── Query helpers ─────────────────────────────────────────────────────────

    @GetMapping("/by-group/{groupName}")
    public ResponseEntity<List<UserDto>> byGroup(@PathVariable String groupName) {
        return ResponseEntity.ok(userService.findByGroupName(groupName));
    }

    @GetMapping("/by-role/{roleName}")
    public ResponseEntity<List<UserDto>> byRole(@PathVariable String roleName) {
        return ResponseEntity.ok(userService.findByEffectiveRole(roleName));
    }

    // ── Password verification ─────────────────────────────────────────────────

    @PostMapping("/verify-password")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(
            @RequestBody Map<String, String> body) {
        boolean ok = userService.verifyPassword(body.get("username"), body.get("password"));
        return ResponseEntity.ok(Map.of("valid", ok));
    }
}
