package com.bpmnengine.api.dto;

import lombok.Data;

/** Response from a successful deployment. */
@Data
public class DeployProcessResponse {
    private String  processDefinitionId;
    private String  processKey;
    private int     version;
    private boolean isNewKey;    // true if this is the first version for this key
    private String  message;

    public DeployProcessResponse(String id, String key, int version, boolean isNewKey) {
        this.processDefinitionId = id;
        this.processKey          = key;
        this.version             = version;
        this.isNewKey            = isNewKey;
        this.message = isNewKey
                ? "Process '" + key + "' deployed as version " + version + " (new key)"
                : "Process '" + key + "' redeployed as version " + version;
    }
}
