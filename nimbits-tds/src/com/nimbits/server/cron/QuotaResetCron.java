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

package com.nimbits.server.cron;

import com.google.appengine.api.datastore.*;
import com.nimbits.client.constants.Const;
import com.nimbits.client.enums.*;
import com.nimbits.client.exception.NimbitsException;
import com.nimbits.client.model.common.*;
import com.nimbits.client.model.email.*;
import com.nimbits.client.model.user.User;
import com.nimbits.server.email.*;
import com.nimbits.server.quota.*;
import com.nimbits.server.user.UserTransactionFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Benjamin Sautner
 * User: bsautner
 * Date: 3/28/12
 * Time: 1:04 PM
 */
public class QuotaResetCron  extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(QuotaResetCron.class.getName());

    @Override
    @SuppressWarnings(Const.WARNING_UNCHECKED)
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            processGet(req, resp);
        } catch (NimbitsException e) {
            log.severe(e.getMessage());
        }


    }

    protected void processGet(HttpServletRequest req, HttpServletResponse resp) throws NimbitsException, IOException {
        final List<User> users = UserTransactionFactory.getInstance().getUsers();
        StringBuilder sb = new StringBuilder(users.size() * 100);

        Quota quota;
        int c;


        final DatastoreService store = DatastoreServiceFactory.getDatastoreService();

        final Query q = new Query("UserEntity").setKeysOnly();

        sb.append("<html><body><table>");
        EmailAddress em;
        for (final Entity e : store.prepare(q).asList(FetchOptions.Builder.withLimit(5000))) {
            em = CommonFactoryLocator.getInstance().createEmailAddress(e.getKey().getName());

            quota = QuotaFactory.getInstance(em);
            c = quota.getCount();
            if (c > 1) {
                sb.append("<tr><td>").append(em .getValue()).append("</td><td>").append(c).append("</td></tr>");
            }
            quota.resetCounter();

            }
            sb.append("</table></body></html>");
            resp.getWriter().print(sb.toString());
            EmailAddress me = CommonFactoryLocator.getInstance().createEmailAddress("bsautner@gmail.com");
            EmailServiceFactory.getInstance().sendEmail(me, me, sb.toString(),  "Quota Report");

        }



}
