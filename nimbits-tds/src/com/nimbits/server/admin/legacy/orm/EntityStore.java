/*
 * Copyright (c) 2010 Tonic Solutions LLC.
 *
 * http://www.nimbits.com
 *
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, eitherexpress or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.server.admin.legacy.orm;

import com.google.appengine.api.blobstore.*;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.*;
import com.nimbits.client.common.*;
import com.nimbits.client.enums.*;
import com.nimbits.client.exception.*;
import com.nimbits.client.model.common.*;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.entity.*;
import com.nimbits.client.model.point.*;

import javax.jdo.annotations.*;
import java.util.*;

/**
 * Created by bsautner
 * User: benjamin
 * Date: 2/6/12
 * Time: 6:24 PM
 */

@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable = "false")
public class EntityStore {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    protected Key key;

    @Persistent
    private String name;

    @Persistent
    private String uuid;

    @Persistent
    private String description;

    @Persistent
    private Integer entityType;

    @Persistent
    private Integer protectionLevel;

    @Persistent
    private String parent;

    @Persistent
    private String owner;

    @NotPersistent
    private int alertType;

    @Persistent
    private BlobKey blobKey;

    @NotPersistent
    private boolean readOnly;

    @SuppressWarnings("unused")
    protected EntityStore() {

    }


    public EntityStore(final Entity entity) throws NimbitsException {

        final EntityName saferName = CommonFactoryLocator.getInstance().createName(entity.getName().getValue(), entity.getEntityType());
        if (Utils.isEmptyString(entity.getKey())) {

       //    this.key = KeyFactory.createKey(EntityStore.class.getSimpleName(), UUID.randomUUID().toString());
             if (entity.getEntityType().equals(EntityType.user)) {
              this.key = KeyFactory.createKey(UserEntity.class.getSimpleName(), saferName.getValue());
            }

            else if (entity.getEntityType().equals(EntityType.point)) {
                this.key =  KeyFactory.createKey(PointEntity.class.getSimpleName(), entity.getOwner() + '/' + saferName.getValue());
            }
            else {
                this.key = KeyFactory.createKey(SimpleEntity.class.getSimpleName(), UUID.randomUUID().toString());
            }

        }
        else {
            this.key = KeyFactory.createKey(SimpleEntity.class.getSimpleName(), entity.getKey());
        }
        this.uuid = entity.getUUID();
        this.name = saferName.getValue();
        this.description = entity.getDescription();
        this.entityType = entity.getEntityType().getCode();
        this.parent = entity.getParent();
        this.owner = entity.getOwner();
        this.protectionLevel = entity.getProtectionLevel().getCode();
        if (! Utils.isEmptyString(entity.getBlobKey()))  {
            this.blobKey = new BlobKey(entity.getBlobKey());
        }

    }

    public EntityStore(final Class<?> cls, final Entity entity) throws NimbitsException {

        final EntityName saferName = CommonFactoryLocator.getInstance().createName(entity.getName().getValue(), entity.getEntityType());
        if (Utils.isEmptyString(entity.getKey())) {
            if (entity.getEntityType().equals(EntityType.user)) {
                this.key = KeyFactory.createKey(cls.getSimpleName(), saferName.getValue());
            }
            else if (entity.getEntityType().equals(EntityType.point)) {
                this.key =  KeyFactory.createKey(cls.getSimpleName(), entity.getOwner() + '/' + saferName.getValue());
            }
            else {
                this.key = KeyFactory.createKey(cls.getSimpleName(), UUID.randomUUID().toString());
            }

        }
        else {
            this.key = KeyFactory.createKey(cls.getSimpleName(), entity.getKey());
        }
        this.uuid = entity.getUUID();
        this.name = saferName.getValue();
        this.description = entity.getDescription();
        this.entityType = entity.getEntityType().getCode();
        this.parent = entity.getParent();
        this.owner = entity.getOwner();
        this.protectionLevel = entity.getProtectionLevel().getCode();
        if (! Utils.isEmptyString(entity.getBlobKey()))  {
            this.blobKey = new BlobKey(entity.getBlobKey());
        }

    }


    public EntityName getName() {
        try {
            return name != null ? CommonFactoryLocator.getInstance().createName(name, EntityType.get(this.entityType)) : null;
        } catch (NimbitsException e) {
            return null;
        }
        catch (NullPointerException e) {
            return null;
        }
    }


    public String getUUID() {
        return uuid;
    }


    public void setUUID(final String uuid) {
        this.uuid = uuid;
    }


    public void setName(final EntityName name) throws NimbitsException {
        final EntityName saferName = CommonFactoryLocator.getInstance().createName(name.getValue(), EntityType.get(this.entityType));
        this.name = saferName.getValue();
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(final String description) {
        this.description = description;
    }


    public EntityType getEntityType() {
        return EntityType.get(entityType);
    }


    public void setEntityType(final EntityType entityType) {
        this.entityType = entityType.getCode();
    }


    public String getKey() {

        return  this.key.getName();
    }

//
//    public void setEntity(String entity) {
//        this.entity = entity;
//    }


    public String getParent() {
        return (parent);
    }


    public void setParent(final String parent) {
        this.parent = parent;
    }


    public ProtectionLevel getProtectionLevel() {
        return ProtectionLevel.get(protectionLevel);
    }


    public void setProtectionLevel(final ProtectionLevel protectionLevel) {
        this.protectionLevel = protectionLevel.getCode();
    }


    public String getOwner() {
        return (owner);
    }


    public void setOwner(final String owner) {
        this.owner = owner;
    }


    public AlertType getAlertType() {
        return AlertType.get(this.alertType);
    }


    public void setAlertType(final AlertType alertType) {
        this.alertType=(alertType.getCode());
    }


    public boolean isReadOnly() {
        return this.readOnly;
    }


    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

//
//    public String getUUID() {
//        return this.entity.toString();
//
//    }
//
//
//    public void setUUID(String newUUID) {
//        this.entity = newUUID;
//    }


    public String getBlobKey() {
        return blobKey != null ? this.blobKey.getKeyString() : null;
    }


    public void setBlobKey(final String blobKey) {
        if (! Utils.isEmptyString(blobKey)) {
            this.blobKey = new BlobKey(blobKey);
        }

    }


    public void setPoints(final List<Point> points) {
        //not implemented
    }


    public void update(Entity update) throws NimbitsException {
        //auto generated
    }


}