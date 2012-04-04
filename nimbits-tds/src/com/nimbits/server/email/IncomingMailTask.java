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

package com.nimbits.server.email;

import com.nimbits.client.enums.EntityType;
import com.nimbits.client.enums.Parameters;
import com.nimbits.client.exception.NimbitsException;
import com.nimbits.client.model.common.CommonFactoryLocator;
import com.nimbits.client.model.email.EmailAddress;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.entity.EntityName;
import com.nimbits.client.model.point.Point;
import com.nimbits.client.model.user.User;
import com.nimbits.client.model.value.Value;
import com.nimbits.client.model.value.ValueModelFactory;
import com.nimbits.server.entity.EntityServiceFactory;
import com.nimbits.server.feed.FeedServiceFactory;
import com.nimbits.server.point.PointServiceFactory;
import com.nimbits.server.user.UserTransactionFactory;
import com.nimbits.server.value.RecordedValueServiceFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.logging.Logger;

public class IncomingMailTask extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    //  private final Map<String, Point> points = new HashMap<String, Point>();
    private static final Logger log = Logger.getLogger(IncomingMailTask.class.getName());


    @Override
    public void doPost(final HttpServletRequest req, final HttpServletResponse resp) {

        final String fromAddress = req.getParameter(Parameters.fromAddress.getText());
        final String inContent = req.getParameter(Parameters.inContent.getText());

        final EmailAddress internetAddress = CommonFactoryLocator.getInstance().createEmailAddress(fromAddress);
        final User u;
        try {
            log.info("Incoming mail post: " + internetAddress);
            u = UserTransactionFactory.getInstance().getNimbitsUser(internetAddress);

            final String content = inContent.replaceAll("\n", "").replaceAll("\r", "");
            final String Data[] = content.split(";");
            log.info("Incoming mail post: " + inContent);

            if (u != null) {
                 if (Data.length > 0) {
                    for (String s : Data) {
                        processLine(u, s);
                    }
                }
            } else {
                log.severe("Null user for incoming mail:" + fromAddress);

            }
        } catch (NimbitsException e) {
            log.severe(e.getMessage());
        }
        //TODO add users to list of spammers

    }

    void processLine(final User u, final String s) throws NimbitsException {
        final String emailLine[] = s.split(",");
        final EntityName pointName = CommonFactoryLocator.getInstance().createName(emailLine[0], EntityType.point);

        Entity e = EntityServiceFactory.getInstance().getEntityByName(u, pointName,EntityType.point);
        final Point point = PointServiceFactory.getInstance().getPointByKey(e.getKey());

        if (point != null) {
            sendValue(u, point, emailLine);
        }
    }

    private static void sendValue(final User u,
                                  final Point point,
                                  final String k[]) {


        long timestamp;
        Double v = 0.0;
        String note;

        if (k != null && k.length > 1) {

            try {
                v = Double.valueOf(k[1].trim());
            } catch (NumberFormatException e1) {
                log.info("Invalid mail message from: " + u.getEmail() + " " + k[0] + "," + k[1]);
            }

            if (k.length == 3) {

                try {
                    String ts = k[2].trim();
                    timestamp = Long.parseLong(ts);
                } catch (NumberFormatException e) {
                    timestamp = new Date().getTime();
                    log.info("Invalid mail message from: " + u.getEmail() + " " + k[0] + "," + k[1] + "," + k[2]);
                }
            } else {
                timestamp = new Date().getTime();
            }
            if (k.length == 4) {
                note = (k[3].trim());
            } else {
                note = "";
            }
            final Value value = ValueModelFactory.createValueModel(0.0, 0.0, v, new Date(timestamp), point.getKey(), note);
            try {
                RecordedValueServiceFactory.getInstance().recordValue(u, point, value, false);
            } catch (NimbitsException e) {
                log.severe(e.getMessage());
                if (u != null) {
                    FeedServiceFactory.getInstance().postToFeed(u, e);
                }
            }
        }


    }

}
