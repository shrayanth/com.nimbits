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

package com.nimbits.server.transactions.service.user;

import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.*;
import com.nimbits.client.common.*;
import com.nimbits.client.constants.*;
import com.nimbits.client.enums.*;
import com.nimbits.client.exception.*;
import com.nimbits.client.model.accesskey.*;
import com.nimbits.client.model.common.*;
import com.nimbits.client.model.connection.*;
import com.nimbits.client.model.email.*;
import com.nimbits.client.model.entity.*;
import com.nimbits.client.model.user.User;
import com.nimbits.client.model.user.*;
import com.nimbits.client.service.user.UserService;
import com.nimbits.server.admin.logging.*;
import com.nimbits.server.admin.quota.QuotaFactory;
import com.nimbits.server.communication.email.*;
import com.nimbits.server.settings.SettingsServiceFactory;
import com.nimbits.server.transactions.service.entity.*;
import com.nimbits.server.transactions.service.feed.*;

import javax.servlet.http.*;
import java.util.*;
import java.util.logging.*;


public class UserServiceImpl extends RemoteServiceServlet implements
        UserService, UserServerService {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(UserServiceImpl.class.getName());



    @Override
    public User getHttpRequestUser(final HttpServletRequest req) throws NimbitsException {


        String emailParam = null;
        HttpSession session = null;
        final com.google.appengine.api.users.UserService googleUserService = UserServiceFactory.getUserService();
        String accessKey = null;
        String uuid = null;



        if (req != null) {
//            Enumeration i = req.getHeaderNames();
//
//            while (i.hasMoreElements()) {
//                log.info(i + " " + req.getHeader((String) i.nextElement()));
//            }
            uuid =  req.getParameter(Parameters.uuid.getText());
            emailParam = req.getParameter(Parameters.email.getText());
            accessKey = req.getParameter(Parameters.secret.getText());
            if (Utils.isEmptyString(accessKey)) {
                accessKey = req.getParameter(Parameters.key.getText());
            }
            session = req.getSession();
        }
        EmailAddress email = Utils.isEmptyString(emailParam) ? null : CommonFactoryLocator.getInstance().createEmailAddress(emailParam);

        if (email == null && session != null && session.getAttribute(Parameters.email.getText()) != null) {
            email = (EmailAddress) session.getAttribute(Parameters.email.getText());
        }

        if (email == null && googleUserService.getCurrentUser() != null) {
            email = CommonFactoryLocator.getInstance().createEmailAddress(googleUserService.getCurrentUser().getEmail());
        }


        if (email == null && Utils.isEmptyString(uuid)) {
            throw new NimbitsException("There was no account connected to this request and no key or uuid, nothing to do.");
        }

        boolean anonRequest = false;

        if (! Utils.isEmptyString(uuid) && email==null) { //a request with just a uuid must be public
            List<Entity> anon = EntityServiceFactory.getInstance().findEntityByKey(uuid);
            anonRequest = true;
            if (! anon.isEmpty()) {

                Entity anonEntity = anon.get(0);
                if (anonEntity.getProtectionLevel().equals(ProtectionLevel.everyone)) {

                    email = CommonFactoryLocator.getInstance().createEmailAddress(anon.get(0).getOwner());
                }
                else {
                    throw new NimbitsException("The object you requested was found, but its protection level was set to high to access. Try " +
                            "adding an email address and access key to your request.");
                }

            }
        }

        User user = null;
        if (email != null) {
            final List<Entity> result = EntityServiceFactory.getInstance().getEntityByKey(
                    com.nimbits.server.transactions.service.user.UserServiceFactory.getServerInstance().getAdmin(), //avoid infinite recursion
                    email.getValue(), EntityType.user);



            if (result.isEmpty()) {
                if (googleUserService.getCurrentUser() != null && googleUserService.getCurrentUser().getEmail().equalsIgnoreCase(email.getValue())) {
                    user = createUserRecord(email);
                    user.addAccessKey(authenticatedKey(user));
                } else if (googleUserService.getCurrentUser() != null && !googleUserService.getCurrentUser().getEmail().equalsIgnoreCase(email.getValue())) {
                    throw new NimbitsException("While the current user is authenticated, the email provided does not match " +
                            "the authenticated user, so the system is confused and cannot authenticate the request. " +
                            "Please report this error.");
                } else if (googleUserService.getCurrentUser() == null) {
                    throw new NimbitsException(email.getValue() + " was provided but could not be found. Please log into nimbits with an account " +
                            " registered with google at least once.");
                }


            } else {
                user = (User) result.get(0);
                //
                if (! anonRequest) {
                    if (!Utils.isEmptyString(accessKey)) {
                        //all we have is an email of an existing user, let's see what they can do.
                        final Map<String, Entity> keys = EntityServiceFactory.getInstance().getEntityMap(user, EntityType.accessKey, 1000);
                        for (final Entity k : keys.values()) {
                            if (((AccessKey)k).getCode().equals(accessKey)) {
                                user.addAccessKey((AccessKey) k);
                            }
                        }


                    }
                }
                if (user.isRestricted()) {

                    if (googleUserService.getCurrentUser() != null
                            && googleUserService.getCurrentUser().getEmail().equalsIgnoreCase(user.getEmail().getValue())) {

                        user.addAccessKey(authenticatedKey(user)); //they are logged in
                    }
                }
            }
        }
        else {
            throw new NimbitsException("There was no account connected to this request");
        }

        return user;

    }

    @Override
    public User login(final String requestUri) throws NimbitsException {
        final com.google.appengine.api.users.UserService userService = UserServiceFactory.getUserService();
        final com.google.appengine.api.users.User user = userService.getCurrentUser();
        final User retObj;

        if (user != null) {
            final EmailAddress internetAddress = CommonFactoryLocator.getInstance().createEmailAddress(user.getEmail());


            final List<Entity> list =   EntityServiceFactory.getInstance()
                    .getEntityByKey(
                            com.nimbits.server.transactions.service.user.UserServiceFactory.getServerInstance().getAnonUser(), internetAddress.getValue(),EntityType.user);


            if (list.isEmpty()) {
                LogHelper.log(this.getClass(), "Created a new user");
                retObj = com.nimbits.server.transactions.service.user.UserServiceFactory.getServerInstance().createUserRecord(internetAddress);
                // sendUserCreatedFeed(u);
                // sendWelcomeFeed(u);
            }
            else {
                retObj = (com.nimbits.client.model.user.User) list.get(0);
            }

            retObj.setLoggedIn(true);

            retObj.setUserAdmin(userService.isUserAdmin());

            retObj.setLogoutUrl(userService.createLogoutURL(requestUri));

            retObj.setLastLoggedIn(new Date());
            EntityServiceFactory.getInstance().addUpdateEntity(retObj, retObj);
            retObj.addAccessKey(authenticatedKey(retObj));


            // A user has logged in through google auth - this creates the user

        } else {
            final EntityName name = CommonFactoryLocator.getInstance().createName("anon@nimbits.com", EntityType.user);
            final Entity e = EntityModelFactory.createEntity(name, "", EntityType.user, ProtectionLevel.onlyMe, "", "");
            retObj = UserModelFactory.createUserModel(e);
            retObj.setLoggedIn(false);
            retObj.setLoginUrl(userService.createLoginURL(requestUri));
        }
        return retObj;
    }

    @Override
    public Integer getQuota() throws NimbitsException {
         User user= getHttpRequestUser(this.getThreadLocalRequest());
         return QuotaFactory.getInstance(user.getEmail()).getCount();

    }

    @Override
    public AccessKey authenticatedKey(final Entity user) throws NimbitsException {

        final EntityName name = CommonFactoryLocator.getInstance().createName("AUTHENTICATED_KEY", EntityType.accessKey);
        final Entity en = EntityModelFactory.createEntity(name, "",EntityType.accessKey, ProtectionLevel.onlyMe, user.getKey(), user.getKey());
        return AccessKeyFactory.createAccessKey(en, "AUTHENTICATED_KEY", user.getKey(), AuthLevel.admin);
    }

    @Override
    public com.nimbits.client.model.user.User createUserRecord(final EmailAddress internetAddress) throws NimbitsException {
        final EntityName name = CommonFactoryLocator.getInstance().createName(internetAddress.getValue(), EntityType.user);
        final Entity entity =  EntityModelFactory.createEntity(name, "", EntityType.user, ProtectionLevel.onlyMe,
                name.getValue(), name.getValue(), name.getValue());

        final com.nimbits.client.model.user.User newUser = UserModelFactory.createUserModel(entity);
        // newUser.setSecret(UUID.randomUUID().toString());
        return (User) EntityServiceFactory.getInstance().addUpdateEntity(newUser);
    }

    @Override
    public User getAdmin() throws NimbitsException {
        final String adminStr = SettingsServiceFactory.getInstance().getSetting(SettingType.admin);
        if (Utils.isEmptyString(adminStr)) {
           throw new NimbitsException("Server is missing admin setting!");
        }
        else {
            final User u = new UserModel();
            u.setName(CommonFactoryLocator.getInstance().createName(adminStr, EntityType.user));
            u.setKey(adminStr);
            u.addAccessKey(createAccessKey(u, AuthLevel.admin));
            u.setParent(adminStr);
            u.setUserAdmin(true);

            return u;
        }
    }

    private static AccessKey createAccessKey(final Entity u, final AuthLevel authLevel) throws NimbitsException {

        final Entity en = EntityModelFactory.createEntity(u.getName(), "", EntityType.accessKey, ProtectionLevel.onlyMe,
                u.getName().getValue(),u.getName().getValue());
        return AccessKeyFactory.createAccessKey(en, u.getName().getValue(), u.getName().getValue(),authLevel );

    }

    @Override
    public User getAnonUser()  {
        User u = new UserModel();
        try {
            String adminStr = Const.CONST_ANON_EMAIL;
            u.setName(CommonFactoryLocator.getInstance().createName(adminStr, EntityType.user));
        } catch (NimbitsException e) {
            return u;
        }


        return u;
    }

    @Override
    public User getAppUserUsingGoogleAuth() throws NimbitsException {
        final com.google.appengine.api.users.UserService u = UserServiceFactory.getUserService();
        //u.getCurrentUser().

        User retObj = null;
        if (u.getCurrentUser() != null) {
            final EmailAddress emailAddress = CommonFactoryLocator.getInstance().createEmailAddress(u.getCurrentUser().getEmail());
            List<Entity> result = EntityServiceFactory.getInstance().getEntityByKey(emailAddress.getValue(), EntityType.user);
            if (! result.isEmpty()) {
                retObj = (User) result.get(0);
            }

        }

        return retObj;
    }

    @Override
    public User getUserByKey(final String key, AuthLevel authLevel) throws NimbitsException {
        List<Entity> result = EntityServiceFactory.getInstance().getEntityByKey(key, EntityType.user);
        if (result.isEmpty()) {
            throw new NimbitsException(UserMessages.ERROR_USER_NOT_FOUND);
        } else {
            User retObj = (User) result.get(0);
            AccessKey k = createAccessKey(retObj, authLevel);
            retObj.addAccessKey(k);

            return retObj;
        }

    }

    @Override
    public void sendConnectionRequest(final EmailAddress email) throws NimbitsException {
        final User user = getAppUserUsingGoogleAuth();
        final ConnectionRequest f = UserTransactionFactory.getInstance().makeConnectionRequest(user, email);


        if (f != null) {
            EmailServiceFactory.getInstance().sendEmail(email,  getConnectionInviteEmail(user.getEmail()));
            FeedServiceFactory.getInstance().postToFeed(user, "A connection request has been emailed to " +
                    email.getValue() + ". If they approve, you will see any data object of theirs that have " +
                    "their permission set to be viewable by the public or connections", FeedType.info);


        }


    }

    public static String getConnectionInviteEmail(final CommonIdentifier email) {
        return "<P STYLE=\"margin-bottom: 0in\"> " + email.getValue() +
                " wants to connect with you on <a href = \"http://www.nimbits.com\"> Nimbits! </A></BR></P><BR> \n" +
                "<P><a href = \"http://www.nimbits.com\">Nimbits</A> is a data logging service that you can use to record time series\n" +
                "data, such as sensor readings, GPS Data, stock prices or anything else into Data Points on the cloud.</P>\n" +
                "<BR><P STYLE=\"margin-bottom: 0in\">\n" +
                "</P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">Nimbits uses Google Accounts for\n" +
                "authentication. If you have a gmail account, you can sign into\n" +
                "Nimbits immediately  using that account. You can also register any\n" +
                "email address with google accounts and then sign in to Nimbits.  It\n" +
                "only takes a few seconds to register:\n" +
                "<A HREF=\"https://www.google.com/accounts/NewAccount\">https://www.google.com/accounts/NewAccount</A></P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">\n" +
                "</P>\n" +
                "<BR><P STYLE=\"margin-bottom: 0in\"><A HREF=\"http://app.nimbits.com/\">Sign\n" +
                "into Nimbits</A> to approve this connection request.  <A HREF=\"http://www.nimbits.com/\">Go to \n" +
                "nimbits.com</A> to learn more.</P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">\n" +
                "</P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">\n" +
                "</P>\n" +
                "<BR><BR><P STYLE=\"margin-bottom: 0in\"><STRONG>More about Nimbits Services</STRONG></P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">\n" +
                "</P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">Nimbits is a collection of software " +
                "designed for recording and working with time series data - such as " +
                "readings from a temperature probe, a stock price, or anything else " +
                "that changes over time - even textual and GPS data.  Nimbits allows " +
                "you to create online Data Points that provide a data channel into the " +
                "cloud.\n" +
                "</P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">\n" +
                "</P>\n" +
                "<BR><P STYLE=\"margin-bottom: 0in\">Nimbits Server, a data historian, is " +
                "available at <A HREF=\"http://www.nimbits.com/\">app.nimbits.com</A> " +
                "and provides a collection of web services, APIs and an interactive " +
                "portal enabling you to record data on a global cloud computing " +
                "infrastructure. You can also download and install your own instance " +
                "of a Nimbits Server, write your own software using Nimbits as a " +
                "powerful back end, or use our many free and open source downloads. " +
                "</P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">\n" +
                "</P>\n" +
                "<BR><P STYLE=\"margin-bottom: 0in\">Built on cloud computing architecture,\n" +
                "and optimized to run on Google App Engine, you can run a Nimbits\n" +
                "Server with remarkable uptime and out of the box disaster recover\n" +
                "with zero upfront cost and a generous free quota. Then, only pay for\n" +
                "computing services you use with near limitless and instant\n" +
                "scalability when you need it. A typical 10 point Nimbits System costs\n" +
                "only pennies a week to run, and nothing at all when it's not in use.\n" +
                "</P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">\n" +
                "</P>\n" +
                "<P STYLE=\"margin-bottom: 0in\">As your data flows into a Nimbits Data\n" +
                "Point, values can be compressed, alarms can be triggered,\n" +
                "calculations can be performed and data can even be relayed to\n" +
                "facebook, Twitter or other connected systems.  You can chat with your\n" +
                "data over IM from anywhere, see and share your changing data values\n" +
                "in spreadsheets, diagrams and even on your phone with our free\n" +
                "android app. \n" +
                "</P>";
    }

    @Override
    public List<ConnectionRequest> getPendingConnectionRequests(final EmailAddress email) throws NimbitsException {
        return UserTransactionFactory.getInstance().getPendingConnectionRequests(email);
    }

    @Override
    public List<User> getConnectionRequests(final List<String> connections) throws NimbitsException {
        return UserTransactionFactory.getInstance().getConnectionRequests(connections);
    }

    @Override
    public void connectionRequestReply(final EmailAddress targetEmail,
                                       final EmailAddress requesterEmail,
                                       final Long key,
                                       final boolean accepted) throws NimbitsException {
        final User acceptor = getAppUserUsingGoogleAuth();
        List<Entity> result = EntityServiceFactory.getInstance().getEntityByKey(requesterEmail.getValue(),EntityType.user);

        if (result.isEmpty()) {
            throw new NimbitsException(UserMessages.ERROR_USER_NOT_FOUND);
        } else {
            final User requester = (User) result.get(0);
            EntityName a = CommonFactoryLocator.getInstance().createName(acceptor.getEmail().getValue(), EntityType.userConnection);
            EntityName r = CommonFactoryLocator.getInstance().createName(requester.getEmail().getValue(), EntityType.userConnection);
            final Entity rConnection = EntityModelFactory.createEntity(a, "", EntityType.userConnection, ProtectionLevel.onlyMe,  requester.getKey(), requester.getKey(), UUID.randomUUID().toString());
            final Entity aConnection = EntityModelFactory.createEntity(r, "", EntityType.userConnection, ProtectionLevel.onlyMe, acceptor.getKey(), acceptor.getKey(), UUID.randomUUID().toString());
            Connection ac = ConnectionFactory.createCreateConnection(aConnection);
            Connection rc = ConnectionFactory.createCreateConnection(rConnection);
            EntityServiceFactory.getInstance().addUpdateEntity(acceptor, ac);
            EntityServiceFactory.getInstance().addUpdateEntity(requester,rc);
            UserTransactionFactory.getInstance().updateConnectionRequest(key, requester, acceptor, accepted);

        }






    }


}
