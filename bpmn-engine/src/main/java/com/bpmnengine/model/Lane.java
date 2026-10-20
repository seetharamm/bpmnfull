package com.bpmnengine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data public class Lane {
    private String id;
    private String name;
    private List<String> flowNodeRefs = new ArrayList<>();
}
