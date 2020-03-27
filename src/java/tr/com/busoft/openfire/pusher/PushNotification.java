package tr.com.busoft.openfire.pusher;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import org.dom4j.Element;
import org.dom4j.Namespace;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import us.raudi.pushraven.FcmResponse;
import us.raudi.pushraven.Pushraven;
import us.raudi.pushraven.configs.AndroidConfig;
import us.raudi.pushraven.configs.AndroidConfig.Priority;

public class PushNotification implements OfflineMessageListener
{
    private static final Logger Log = LoggerFactory.getLogger(PushNotification.class);

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
            if (isChatState(message) || isReceipt(message))
            {
                return;
            }

            Log.debug("Message Stored Offline");
            Log.debug(message.toString());
            Log.debug("Push notification will be sent");

            dbconnection = DbConnectionManager.getConnection();

            JID sender = message.getFrom();
            String senderUsername = sender.getNode();

            JID receiver = message.getTo();
            String receiverUsername = receiver.getNode();
            String receiverResource = receiver.getResource();

            Log.debug("From: " + sender.toString());
            Log.debug("To: " + receiver.toString());

            String sql = String.format("SELECT type, token FROM ofPusher WHERE username = '%s' AND resource = '%s'", receiverUsername, receiverResource);
            statement = dbconnection.createStatement();
            resultSet = statement.executeQuery(sql);

            if (!resultSet.next())
            {
                Log.debug("Receiver: {} does not have token so notification will not be sent", receiverUsername);
                return;
            }

            String token = resultSet.getString("token");
            String type = resultSet.getString("type");

            String messageBody = message.getBody();

            if (type.equals("android"))
            {
                Log.debug("Receiver's device is android");

                File fcmCredentialFile = new File(PusherProperty.FCM_CREDENTIAL_FILE_PATH);
                Pushraven.setCredential(fcmCredentialFile);
                Pushraven.setProjectId(PusherProperty.FCM_PROJECT_ID);

                HashMap<String, String> data = new HashMap<String, String>();
                data.put("title", senderUsername);
                data.put("body", messageBody);

                AndroidConfig androidCfg = new AndroidConfig()
                                                .priority(Priority.HIGH);

                us.raudi.pushraven.Message ravenMessage = new us.raudi.pushraven.Message()
                                                                                .data(data)
                                                                                .token(token)
                                                                                .android(androidCfg);

                FcmResponse response = Pushraven.push(ravenMessage);

                Log.debug("Android FCM response " + response.getMessage());
            }
            else if (type.equals("ios"))
            {
                Log.debug("Receiver's device is ios");

                ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder()
                                                        .setAlertTitle(senderUsername)
                                                        .setThreadId(receiverUsername)
                                                        .setMutableContent(true);

                String payload = payloadBuilder.setAlertBody(messageBody).buildWithDefaultMaximumLength();

                String tokenSanitized = TokenUtil.sanitizeTokenString(token);
                SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(tokenSanitized, PusherProperty.APNS_BUNDLE_ID, payload);

                File apnsCredentialFile = new File(PusherProperty.APNS_PKCS8_FILE_PATH);
                ApnsClientBuilder builder = new ApnsClientBuilder()
                                                .setSigningKey(ApnsSigningKey.loadFromPkcs8File(apnsCredentialFile, PusherProperty.APNS_TEAM_ID, PusherProperty.APNS_KEY));

                sendPushToProduction(pushNotification, builder);

                if (PusherProperty.APNS_SANDBOX_ENABLED)
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
            Log.error("Error while sending push " + exception.getMessage());
            return;
        }
        finally
        {
            DbConnectionManager.closeConnection(resultSet, statement, dbconnection);
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
                        Log.error(prefix + " Notification rejected by the APNs gateway: " + pushNotificationResponse.getRejectionReason() + "token: " + token);

                        if (pushNotificationResponse.getTokenInvalidationTimestamp() != null)
                        {
                            Log.error("\t…and the token is invalid as of " + pushNotificationResponse.getTokenInvalidationTimestamp() + "token: " + token);
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

    private static boolean isChatState(final Message message)
    {
        boolean result = false;
        Iterator<?> it = message.getElement().elementIterator();

        while (it.hasNext())
        {
            Object item = it.next();

            if (item instanceof Element)
            {
                Element el = (Element) item;
                if (Namespace.NO_NAMESPACE.equals(el.getNamespace()))
                {
                    continue;
                }

                if (el.getNamespaceURI().equals("http://jabber.org/protocol/chatstates") && !(el.getQualifiedName().equals("active")))
                {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    private static boolean isReceipt(final Message message)
    {
        boolean result = false;
        if ((message.getExtension("received", "urn:xmpp:receipts") != null) || (message.getExtension("seen", "urn:xmpp:receipts")) != null)
        {
            result = true;
        }

        return result;
    }
}