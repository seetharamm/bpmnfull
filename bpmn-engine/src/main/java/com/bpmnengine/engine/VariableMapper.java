package com.bpmnengine.engine;

import com.bpmnengine.persistence.entity.ProcessVariableEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps between Java objects and {@link ProcessVariableEntity} storage columns.
 */
public final class VariableMapper {

    private static final Logger log = LoggerFactory.getLogger(VariableMapper.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private VariableMapper() {}

    public static void mapToEntity(ProcessVariableEntity entity, Object value) {
        if (value == null) {
            entity.setType("NULL");
            return;
        }
        switch (value) {
            case String s          -> { entity.setType("STRING");  entity.setTextValue(s); }
            case Boolean b         -> { entity.setType("BOOLEAN"); entity.setTextValue(b.toString()); }
            case Integer i         -> { entity.setType("INTEGER"); entity.setLongValue(i.longValue()); }
            case Long l            -> { entity.setType("LONG");    entity.setLongValue(l); }
            case Double d          -> { entity.setType("DOUBLE");  entity.setDoubleValue(d); }
            case Float f           -> { entity.setType("DOUBLE");  entity.setDoubleValue(f.doubleValue()); }
            case byte[] bytes      -> { entity.setType("BYTES");   entity.setBytesValue(bytes); }
            default                -> {
                // Serialize complex objects as JSON
                try {
                    entity.setType("JSON");
                    entity.setTextValue(mapper.writeValueAsString(value));
                } catch (JsonProcessingException e) {
                    log.warn("Could not serialize variable as JSON, storing as string: {}", e.getMessage());
                    entity.setType("STRING");
                    entity.setTextValue(value.toString());
                }
            }
        }
    }

    public static Object mapFromEntity(ProcessVariableEntity entity) {
        if (entity.getType() == null) return null;
        return switch (entity.getType()) {
            case "NULL"    -> null;
            case "STRING"  -> entity.getTextValue();
            case "BOOLEAN" -> Boolean.parseBoolean(entity.getTextValue());
            case "INTEGER" -> entity.getLongValue() != null ? entity.getLongValue().intValue() : null;
            case "LONG"    -> entity.getLongValue();
            case "DOUBLE"  -> entity.getDoubleValue();
            case "BYTES"   -> entity.getBytesValue();
            case "JSON"    -> {
                try { yield mapper.readValue(entity.getTextValue(), Object.class); }
                catch (JsonProcessingException e) { yield entity.getTextValue(); }
            }
            default -> entity.getTextValue();
        };
    }
}
