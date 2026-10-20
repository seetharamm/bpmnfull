package com.bpmnengine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data public class IoSpecification {
    private List<DataInput>  dataInputs  = new ArrayList<>();
    private List<DataOutput> dataOutputs = new ArrayList<>();
    private List<String>     inputSets   = new ArrayList<>();
    private List<String>     outputSets  = new ArrayList<>();
}
