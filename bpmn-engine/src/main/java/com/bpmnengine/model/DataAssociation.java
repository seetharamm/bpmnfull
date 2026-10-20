package com.bpmnengine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data public class DataAssociation {
    private String id;
    private List<String> sourceRefs = new ArrayList<>();
    private String targetRef;
    private String transformation;
}
