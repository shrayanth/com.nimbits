package com.nimbits.client.model.entity;

import com.nimbits.client.enums.AlertType;
import com.nimbits.client.enums.EntityType;
import com.nimbits.client.enums.ProtectionLevel;

import java.io.Serializable;

/**
 * Created by Benjamin Sautner
 * User: BSautner
 * Date: 2/7/12
 * Time: 10:50 AM
 */
public interface Entity  extends Serializable {
    EntityName getName();

    void setName(EntityName name);

    String getDescription();

    void setDescription(String description);

    EntityType getEntityType();

    void setEntityType(EntityType entityType);

    String getUUID();

    void setUUID(String entityUUID);

    String getParentUUID();

    void setParentUUID(String parentUUID);

     ProtectionLevel getProtectionLevel();

    void setProtectionLevel(ProtectionLevel protectionLevel);

    String getOwnerUUID();

    void setOwnerUUID(String ownerUUID);

    AlertType getAlertType();

    void setAlertType(AlertType alertType);

    boolean isReadOnly();

    void setReadOnly(boolean readOnly);
}