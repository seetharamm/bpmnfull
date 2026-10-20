package com.bpmnengine.identity.service;

import com.bpmnengine.identity.repository.GroupRepository;
import com.bpmnengine.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves BPMN task candidates (users / groups) against the identity store.
 *
 * Used by the engine when creating UserTask records to find the actual users
 * who should see a task in their worklist.
 */
@Service
public class IdentityService {

    private final UserRepository  userRepo;
    private final GroupRepository groupRepo;

    public IdentityService(UserRepository userRepo, GroupRepository groupRepo) {
        this.userRepo  = userRepo;
        this.groupRepo = groupRepo;
    }

    /**
     * Returns the usernames of all users in the given group.
     * Returns an empty list if the group does not exist.
     */
    @Transactional(readOnly = true)
    public List<String> getUsernamesInGroup(String groupName) {
        return userRepo.findByGroupName(groupName).stream()
                .filter(u -> u.isEnabled() && !u.isLocked())
                .map(u -> u.getUsername())
                .collect(Collectors.toList());
    }

    /**
     * Returns the usernames of all active users who hold the given role
     * (directly or through group membership).
     */
    @Transactional(readOnly = true)
    public List<String> getUsernamesWithRole(String roleName) {
        return userRepo.findByEffectiveRoleName(roleName.toUpperCase()).stream()
                .filter(u -> u.isEnabled() && !u.isLocked())
                .map(u -> u.getUsername())
                .collect(Collectors.toList());
    }

    /**
     * Returns true when a user is an active, non-locked member of the given group.
     */
    @Transactional(readOnly = true)
    public boolean isUserInGroup(String username, String groupName) {
        return userRepo.findByGroupName(groupName).stream()
                .anyMatch(u -> u.getUsername().equals(username)
                            && u.isEnabled()
                            && !u.isLocked());
    }

    /**
     * Returns true when a user holds the given role (directly or via group).
     */
    @Transactional(readOnly = true)
    public boolean userHasRole(String username, String roleName) {
        return userRepo.findByUsername(username)
                .map(user -> {
                    boolean direct = user.getRoles().stream()
                            .anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
                    boolean viaGroup = user.getMemberships().stream()
                            .flatMap(m -> m.getGroup().getRoles().stream())
                            .anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
                    return direct || viaGroup;
                })
                .orElse(false);
    }
}
