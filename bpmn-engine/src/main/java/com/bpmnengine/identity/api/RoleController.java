package com.bpmnengine.identity.api;

import com.bpmnengine.identity.dto.*;
import com.bpmnengine.identity.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST API for role management.
 *
 * POST   /api/identity/roles          — create role
 * GET    /api/identity/roles          — list all roles
 * GET    /api/identity/roles/{id}     — get role by id
 * GET    /api/identity/roles/by-name/{name} — get role by name
 * PUT    /api/identity/roles/{id}     — update role
 * DELETE /api/identity/roles/{id}     — delete role
 */
@RestController
@RequestMapping("/api/identity/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleDto> create(@Valid @RequestBody CreateRoleRequest req) {
        RoleDto created = roleService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<RoleDto> listAll() {
        return roleService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<RoleDto> getByName(@PathVariable String name) {
        return ResponseEntity.ok(roleService.getByName(name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> update(@PathVariable String id,
                                           @RequestBody CreateRoleRequest req) {
        return ResponseEntity.ok(roleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
