package com.bpmnengine.identity.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class MembershipDto {
    private String id;
    private String userId;
    private String username;
    private String displayName;
    private String groupId;
    private String groupName;
    private Instant joinedAt;
    private String addedBy;
}
