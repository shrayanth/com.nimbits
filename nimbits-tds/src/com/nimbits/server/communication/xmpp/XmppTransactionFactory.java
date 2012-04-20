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

package com.nimbits.server.communication.xmpp;

import com.nimbits.server.transactions.dao.xmpp.*;

/**
 * Created by Benjamin Sautner
 * User: bsautner
 * Date: 3/15/12
 * Time: 1:18 PM
 */
public class XmppTransactionFactory {

    private XmppTransactionFactory() {
    }

    private static class XmppTransactionHolder {
        static final XmppTransaction instance = new XmppDaoImpl();

        private XmppTransactionHolder() {
        }
    }

    public static XmppTransaction getInstance() {
        return XmppTransactionHolder.instance;
    }

}