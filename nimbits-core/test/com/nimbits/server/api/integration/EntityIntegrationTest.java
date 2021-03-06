package com.nimbits.server.api.integration;

import com.nimbits.client.enums.Action;
import com.nimbits.client.enums.EntityType;
import com.nimbits.client.enums.Parameters;
import com.nimbits.client.enums.ProtectionLevel;
import com.nimbits.client.exception.NimbitsException;
import com.nimbits.client.model.common.CommonFactoryLocator;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.entity.EntityModel;
import com.nimbits.client.model.entity.EntityModelFactory;
import com.nimbits.client.model.entity.EntityName;
import com.nimbits.client.model.point.Point;
import com.nimbits.client.model.point.PointModelFactory;
import com.nimbits.server.gson.GsonFactory;
import com.nimbits.server.http.HttpCommonFactory;
import org.junit.Test;

import java.util.UUID;


public class EntityIntegrationTest {


   private final static String TEST_PATH = "http://localhost:8080/service/entity";

 //private final static String TEST_PATH = "http://nimbits.com:8080/core/service/entity";
    @Test
    public void testPost() throws NimbitsException {
        EntityName name = CommonFactoryLocator.getInstance().createName("foo", EntityType.point);

        Entity e = EntityModelFactory.createEntity(name, "local unit test", EntityType.point, ProtectionLevel.everyone,
                "test@example.com", "test@example.com", UUID.randomUUID().toString());

        String entity = GsonFactory.getInstance().toJson(e);

        final String params =  Parameters.entity.getText() + '=' + entity
                + '&' + Parameters.action.getText() + '=' + Action.update.getCode()
                + '&' + Parameters.instance.getText() + '=' + "http://localhost:8081";


        final String response =
                HttpCommonFactory.getInstance().doPost(TEST_PATH, params);



    }
    @Test
    public void gsonTest() {

        String json = "{\"highAlarm\":0.0,\"expire\":90,\"unit\":null,\"lowAlarm\":0.0,\"highAlarmOn\":false,\"lowAlarmOn\":false,\"idleAlarmOn\":false,\"idleSeconds\":0,\"idleAlarmSent\":false,\"filterType\":0,\"filterValue\":0.1,\"values\":null,\"value\":null,\"name\":\"temperature8\",\"key\":\"sorpa.lab@gmail.com/temperature8\",\"description\":\"\",\"entityType\":1,\"protectionLevel\":2,\"alertType\":0,\"parent\":\"2ac5f806-a162-4767-9f06-9843dbbdf355\",\"owner\":\"sorpa.lab@gmail.com\",\"readOnly\":false,\"uuid\":\"95deb2e6-550f-4a07-ae25-bf1aa0f3653d\",\"dateCreated\":null,\"children\":null}";
        Entity entity = GsonFactory.getInstance().fromJson(json, EntityModel.class);

    }

    @Test
    public void testPointPost() throws NimbitsException {
        EntityName name = CommonFactoryLocator.getInstance().createName("foo", EntityType.point);

        Entity e = EntityModelFactory.createEntity(name, "local unit test", EntityType.point, ProtectionLevel.everyone,
                "test@example.com", "test@example.com", UUID.randomUUID().toString());
        Point p = PointModelFactory.createPointModel(e);
        p.setDescription("whole point unit test");

        String entity = GsonFactory.getInstance().toJson(p);

        final String params =  Parameters.entity.getText() + '=' + entity
                + '&' + Parameters.action.getText() + '=' + Action.update.getCode()
                + '&' + Parameters.instance.getText() + '=' + "http://localhost:8081";


        final String response =
                HttpCommonFactory.getInstance().doPost(TEST_PATH, params);



    }


}
