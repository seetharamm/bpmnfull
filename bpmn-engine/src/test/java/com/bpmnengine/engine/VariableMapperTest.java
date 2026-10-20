package com.bpmnengine.engine;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class VariableMapperTest {

    @Test void stringRoundtrip() {
        var e = new com.bpmnengine.persistence.entity.ProcessVariableEntity();
        VariableMapper.mapToEntity(e, "hello");
        assertThat(e.getType()).isEqualTo("STRING");
        assertThat(VariableMapper.mapFromEntity(e)).isEqualTo("hello");
    }

    @Test void booleanRoundtrip() {
        var e = new com.bpmnengine.persistence.entity.ProcessVariableEntity();
        VariableMapper.mapToEntity(e, true);
        assertThat(e.getType()).isEqualTo("BOOLEAN");
        assertThat(VariableMapper.mapFromEntity(e)).isEqualTo(true);
    }

    @Test void longRoundtrip() {
        var e = new com.bpmnengine.persistence.entity.ProcessVariableEntity();
        VariableMapper.mapToEntity(e, 42L);
        assertThat(e.getType()).isEqualTo("LONG");
        assertThat(VariableMapper.mapFromEntity(e)).isEqualTo(42L);
    }

    @Test void doubleRoundtrip() {
        var e = new com.bpmnengine.persistence.entity.ProcessVariableEntity();
        VariableMapper.mapToEntity(e, 3.14);
        assertThat(e.getType()).isEqualTo("DOUBLE");
        assertThat(VariableMapper.mapFromEntity(e)).isEqualTo(3.14);
    }

    @Test void nullRoundtrip() {
        var e = new com.bpmnengine.persistence.entity.ProcessVariableEntity();
        VariableMapper.mapToEntity(e, null);
        assertThat(e.getType()).isEqualTo("NULL");
        assertThat(VariableMapper.mapFromEntity(e)).isNull();
    }
}
