package tr.com.busoft.openfire.pusher;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.PresenceManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import us.raudi.pushraven.FcmResponse;
import us.raudi.pushraven.Notification;
import us.raudi.pushraven.Pushraven;
import us.raudi.pushraven.configs.AndroidConfig;
import us.raudi.pushraven.configs.AndroidConfig.Priority;

public class PusherPlugin implements Plugin, PacketInterceptor
{
    private final static Logger Log = LoggerFactory.getLogger(PusherPlugin.class);

    String FCM_CREDENTIAL_FILE_PATH = JiveGlobals.getProperty("pusher.google.fcm.path", "");
    String FCM_PROJECT_ID = JiveGlobals.getProperty("pusher.google.fcm.projectId", "");

    String APNS_PKCS8_FILE_PATH = JiveGlobals.getProperty("pusher.apple.apns.path", "");
    String APNS_TEAM_ID = JiveGlobals.getProperty("pusher.apple.apns.teamId", "");
    String APNS_KEY = JiveGlobals.getProperty("pusher.apple.apns.key", "");
    String APNS_BUNDLE_ID = JiveGlobals.getProperty("pusher.apple.apns.bundleId", "");
    Boolean APNS_SANDBOX_ENABLED = JiveGlobals.getBooleanProperty("pusher.apple.apns.sandbox");

    XMPPServer xmppServer;
    InterceptorManager interceptorManager;
    UserManager userManager;
    PresenceManager presenceManager;

    @Override
    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException
    {
        if (!processed || !incoming || !(packet instanceof Message))
        {
            return;
        }

        Message message = (Message) packet;
        String messageBody = message.getBody();
        if (message.getType() != Message.Type.chat || messageBody == null)
        {
            Log.debug("Message type is not chat or body is empty");
            return;
        }

        JID receiver = message.getTo();
        String receiverResource = receiver.getResource();
        if (receiverResource == null)
        {
            Log.debug("Receiver has no resource");
            return;
        }

        User user;
        String receiverUsername = receiver.getNode();
        try
        {
            Log.debug("Receiver is: " + receiverUsername);
            user = userManager.getUser(receiverUsername);
        }
        catch (Exception exception)
        {
            Log.error(exception.getMessage());
            return;
        }

        boolean isOnline = presenceManager.isAvailable(user);
        if (!isOnline)
        {
            Log.debug("User :" + receiverUsername + ": is not online, push notification will be sent");

            JID sender = message.getFrom();
            String senderUsername = sender.getNode();

            Log.debug("Sender is: " + senderUsername);

            Connection dbconnection = null;
            Statement statement = null;
            try
            {
                dbconnection = DbConnectionManager.getConnection();

                String sql = String.format("SELECT token FROM ofPusher WHERE username = '%s' AND resource = '%s'", receiverUsername, receiverResource);
                statement = dbconnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql);

                resultSet.next();
                String token = resultSet.getString("token");

                if (receiverResource.contains("android"))
                {
                    Log.debug("Receiver's device is android");

                    Notification notification = new Notification()
                                                    .title(senderUsername)
                                                    .body(messageBody);

                    AndroidConfig androidCfg = new AndroidConfig()
                                                    .priority(Priority.HIGH);

                    us.raudi.pushraven.Message ravenMessage = new us.raudi.pushraven.Message()
                                                                                    .notification(notification)
                                                                                    .token(token)
                                                                                    .android(androidCfg);

                    FcmResponse response = Pushraven.push(ravenMessage);

                    Log.debug(response.getMessage());
                }
                else if (receiverResource.contains("ios"))
                {
                    Log.debug("Receiver's device is ios");

                    ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder()
                                                            .setAlertTitle(senderUsername)
                                                            .setThreadId(receiverUsername)
                                                            .setMutableContent(true);

                    String payload = payloadBuilder.setAlertBody(messageBody).buildWithDefaultMaximumLength();

                    String tokenSanitized = TokenUtil.sanitizeTokenString(token);
                    SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(tokenSanitized, APNS_BUNDLE_ID, payload);

                    ApnsClientBuilder builder = new ApnsClientBuilder()
                                                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(APNS_PKCS8_FILE_PATH), APNS_TEAM_ID, APNS_KEY));

                    sendPushToProduction(pushNotification, builder);

                    if (APNS_SANDBOX_ENABLED)
                    {
                        sendPushToSandbox(pushNotification, builder);
                    }
                }
                else
                {
                    throw new Exception("Could not decide device");
                }
            }
            catch (Exception exception)
            {
                Log.error(exception.getMessage());
                return;
            }
            finally
            {
                try
                {
                    dbconnection.close();
                    statement.close();
                }
                catch (Exception exception)
                {
                    Log.error(exception.getMessage());
                }
            }
        }
    }

    private void sendPushToProduction(SimpleApnsPushNotification pushNotification, ApnsClientBuilder builder) throws Exception
    {
        Log.debug("Push notification will be sent to production");

        builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST);
        ApnsClient apnsClient = builder.build();
        sendPush(apnsClient, pushNotification, pushNotification.getToken(), true);
    }

    private void sendPushToSandbox(SimpleApnsPushNotification pushNotification, ApnsClientBuilder builder) throws Exception
    {
        Log.debug("Push notification will be sent to sandbox");

        builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST);
        ApnsClient apnsClient = builder.build();
        sendPush(apnsClient, pushNotification, pushNotification.getToken(), false);
    }

    private void sendPush(ApnsClient client, SimpleApnsPushNotification pushNotification, String token, boolean isProduction)
    {
        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationSandboxFuture = client.sendNotification(pushNotification);

        String prefix = isProduction ? "(Production)" : "(Sandbox)";

        sendNotificationSandboxFuture.addListener(
            future ->
            {
                if (future.isSuccess())
                {
                    PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = sendNotificationSandboxFuture.getNow();

                    if (pushNotificationResponse.isAccepted())
                    {
                        Log.debug(prefix + " Push notification accepted by APNs gateway.");
                    }
                    else
                    {
                        Log.error(prefix + " Notification rejected by the APNs gateway: " + pushNotificationResponse.getRejectionReason());

                        if (pushNotificationResponse.getTokenInvalidationTimestamp() != null)
                        {
                            Log.error("\t…and the token is invalid as of " + pushNotificationResponse.getTokenInvalidationTimestamp());
                        }
                    }
                }
                else
                {
                    Log.error(prefix + " Notification not sent to device: " + token + " exception was: " + future.cause().getMessage());
                    future.cause().printStackTrace();
                }
            }
        );
    }

    @Override
    public void initializePlugin(PluginManager manager, File pluginDirectory)
    {
        Log.debug("Push notification plugin is initalizing");

        xmppServer = XMPPServer.getInstance();

        userManager = xmppServer.getUserManager();
        presenceManager = xmppServer.getPresenceManager();

        interceptorManager = InterceptorManager.getInstance();
        interceptorManager.addInterceptor(this);

        try
        {
            Pushraven.setCredential(new File(FCM_CREDENTIAL_FILE_PATH));
            Pushraven.setProjectId(FCM_PROJECT_ID);
        }
        catch (Exception exception)
        {
            Log.error(exception.getMessage());
        }
    }

    @Override
    public void destroyPlugin()
    {
        interceptorManager.removeInterceptor(this);
        Log.debug("Push notification plugin is destroyed");
    }
}
