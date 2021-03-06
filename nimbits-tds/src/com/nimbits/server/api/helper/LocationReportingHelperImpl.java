package com.nimbits.server.api.helper;


import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.nimbits.client.common.Utils;
import com.nimbits.client.enums.MemCacheKey;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.server.process.task.TaskFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

public class LocationReportingHelperImpl implements LocationReportingHelper {
    private final MemcacheService cache;
    protected final static Logger log = Logger.getLogger(LocationReportingHelperImpl.class.getName());

    public LocationReportingHelperImpl() {
        cache = MemcacheServiceFactory.getMemcacheService(MemCacheKey.defaultNamespace.name());
    }

    @Override
    public void reportLocation(final Entity entity,final String location) {
        String key = entity.getUUID() + MemCacheKey.location;

        if ((! cache.contains(key)) || (cache.contains(key) && ! cache.get(key).equals(location)) ) {

            TaskFactory.getInstance().startCoreLocationTask(entity, location);
            cache.put(key, location);
        }
        else {
            log.info("did not report location since it is already in the cache" + location);
        }


    }

    @Override
    public void reportLocation(HttpServletRequest req, Entity entity) {
        if (req!= null) {
            final String gps = req.getHeader("X-AppEngine-CityLatLong");
            if (! Utils.isEmptyString(gps)) {
                log.info("Reporting location: " + gps);
                LocationReportingHelperFactory.getInstance().reportLocation(entity, gps);
            }
        }
    }


}
