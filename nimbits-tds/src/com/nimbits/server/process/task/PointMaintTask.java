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

package com.nimbits.server.process.task;

import com.nimbits.client.enums.*;
import com.nimbits.client.exception.*;
import com.nimbits.client.model.entity.*;
import com.nimbits.client.model.point.*;
import com.nimbits.client.model.user.*;
import com.nimbits.client.model.valueblobstore.*;
import com.nimbits.server.admin.common.ServerInfoImpl;
import com.nimbits.server.admin.logging.*;
import com.nimbits.server.admin.system.*;
import com.nimbits.server.gson.*;
import com.nimbits.server.transactions.service.user.*;
import com.nimbits.server.transactions.service.value.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;

public class PointMaintTask extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(PointMaintTask.class.getName());


    @Override
    public void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

        try {
            processPost(req);

        } catch (Exception ex) {
            LogHelper.logException(this.getClass(), ex);
        }

    }


    protected static void processPost(final HttpServletRequest req) throws NimbitsException {


        final String j = req.getParameter(Parameters.json.getText());
        final Point entity = GsonFactory.getInstance().fromJson(j, PointModel.class);
      //  final User u = UserServiceFactory.getInstance().getUserByKey(entity.getOwner(), AuthLevel.admin);
        if (entity.getExpire() > 0) {
            TaskFactory.getInstance().startDeleteDataTask(
                    entity,
                    true, entity.getExpire());
        }
        consolidateBlobs(entity);
        TaskFactory.getInstance().startCoreTask(null, entity, Action.update, ServerInfoImpl.getFullServerURL(req));

    }



    protected  static void consolidateBlobs(final Entity e) throws NimbitsException {
        final List<ValueBlobStore> stores = ValueTransactionFactory.getDaoInstance(e).getAllStores();
        if (! stores.isEmpty()) {

            log.info("Consolidating " + stores.size() + " blob stores");
            final Collection<Long> dates = new ArrayList<Long>(stores.size());
            final Collection<Long> dupDates = new ArrayList<Long>(stores.size());
            for (final ValueBlobStore store : stores) {
                //consolidate blobs that have more than one date.

                if ( dates.contains(store.getTimestamp().getTime()) && ! dupDates.contains(store.getTimestamp().getTime())) {

                    dupDates.add(store.getTimestamp().getTime());
                }
                else {

                    dates.add(store.getTimestamp().getTime());
                }
            }
            SystemServiceFactory.getInstance().updateSystemPoint("Fragmented Dates Merges By Point Maint", dupDates.size(), false);
            for (Long l : dupDates) {
               ValueTransactionFactory.getDaoInstance(e).consolidateDate(new Date(l));

            }
        }
    }


}

