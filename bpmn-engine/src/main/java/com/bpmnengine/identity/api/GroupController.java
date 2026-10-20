package com.bpmnengine.identity.api;

import com.bpmnengine.identity.dto.*;
import com.bpmnengine.identity.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST API for group management.
 *
 * POST   /api/identity/groups                         — create group
 * GET    /api/identity/groups                         — list all groups
 * GET    /api/identity/groups?type=ORGANISATIONAL      — list by type
 * GET    /api/identity/groups/{id}                    — get by id
 * GET    /api/identity/groups/by-name/{name}          — get by name
 * PUT    /api/identity/groups/{id}                    — update group
 * DELETE /api/identity/groups/{id}                    — delete group
 * POST   /api/identity/groups/{id}/roles/{roleName}   — assign role to group
 * DELETE /api/identity/groups/{id}/roles/{roleName}   — remove role from group
 * GET    /api/identity/groups/{id}/members            — list members
 */
@RestController
@RequestMapping("/api/identity/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupDto> create(@Valid @RequestBody CreateGroupRequest req) {
        GroupDto created = groupService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<GroupDto> list(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return groupService.listByType(type);
        }
        return groupService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(groupService.getById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<GroupDto> getByName(@PathVariable String name) {
        return ResponseEntity.ok(groupService.getByName(name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupDto> update(@PathVariable String id,
                                            @RequestBody CreateGroupRequest req) {
        return ResponseEntity.ok(groupService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Role assignment ───────────────────────────────────────────────────────

    @PostMapping("/{id}/roles/{roleName}")
    public ResponseEntity<GroupDto> assignRole(@PathVariable String id,
                                                @PathVariable String roleName) {
        return ResponseEntity.ok(groupService.assignRoleToGroup(id, roleName));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<GroupDto> removeRole(@PathVariable String id,
                                                @PathVariable String roleName) {
        return ResponseEntity.ok(groupService.removeRoleFromGroup(id, roleName));
    }

    // ── Members ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MembershipDto>> getMembers(@PathVariable String id) {
        return ResponseEntity.ok(groupService.getMembers(id));
    }
}
