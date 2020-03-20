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
import org.jivesoftware.openfire.OfflineMessageListener;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import us.raudi.pushraven.FcmResponse;
import us.raudi.pushraven.Notification;
import us.raudi.pushraven.Pushraven;
import us.raudi.pushraven.configs.AndroidConfig;
import us.raudi.pushraven.configs.AndroidConfig.Priority;

public class PushNotification implements OfflineMessageListener
{
    private static final Logger Log = LoggerFactory.getLogger(PushNotification.class);

    private final String FCM_CREDENTIAL_FILE_PATH = JiveGlobals.getProperty("pusher.google.fcm.path", "");
    private final String FCM_PROJECT_ID = JiveGlobals.getProperty("pusher.google.fcm.projectId", "");

    private final String APNS_PKCS8_FILE_PATH = JiveGlobals.getProperty("pusher.apple.apns.path", "");
    private final String APNS_TEAM_ID = JiveGlobals.getProperty("pusher.apple.apns.teamId", "");
    private final String APNS_KEY = JiveGlobals.getProperty("pusher.apple.apns.key", "");
    private final String APNS_BUNDLE_ID = JiveGlobals.getProperty("pusher.apple.apns.bundleId", "");
    private final Boolean APNS_SANDBOX_ENABLED = JiveGlobals.getBooleanProperty("pusher.apple.apns.sandbox");

    public PushNotification()
    {
        try
        {
            Pushraven.setCredential(new File(FCM_CREDENTIAL_FILE_PATH));
            Pushraven.setProjectId(FCM_PROJECT_ID);
        }
        catch (Exception exception)
        {
            Log.error("Error while initializing pusher" + exception.getMessage());
        }
    }

    @Override
    public void messageBounced(Message message)
    {

    }

    @Override
    public void messageStored(Message message)
    {
        Connection dbconnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try
        {
            dbconnection = DbConnectionManager.getConnection();

            JID sender = message.getFrom();
            String senderUsername = sender.getNode();

            Log.debug("Sender is: " + senderUsername);

            JID receiver = message.getTo();
            String receiverUsername = receiver.getNode();
            String receiverResource = receiver.getResource();

            Log.debug("User :" + receiverUsername + ": is not online, push notification will be sent");

            String sql = String.format("SELECT token FROM ofPusher WHERE username = '%s' AND resource = '%s'", receiverUsername, receiverResource);
            statement = dbconnection.createStatement();
            resultSet = statement.executeQuery(sql);

            resultSet.next();
            String token = resultSet.getString("token");

            String messageBody = message.getBody();
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

                Log.debug("Android FCM response" + response.getMessage());
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
            Log.error("Error while sending push" + exception.getMessage());
            return;
        }
        finally
        {
            try
            {
                dbconnection.close();
                statement.close();
                resultSet.close();
            }
            catch (Exception exception)
            {
                Log.error("Error while closing dbconnections" + exception.getMessage());
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
}