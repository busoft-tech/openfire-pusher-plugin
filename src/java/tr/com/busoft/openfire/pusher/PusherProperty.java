package tr.com.busoft.openfire.pusher;

import java.io.File;
import java.util.Map;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PusherProperty implements PropertyEventListener
{
    private static final Logger Log = LoggerFactory.getLogger(PusherProperty.class);

    public static final String FCM_CREDENTIAL_FILE_PATH = JiveGlobals.getHomeDirectory() + File.separator + "conf" + File.separator + "pusher-fcm.json";
    public static final String APNS_PKCS8_FILE_PATH = JiveGlobals.getHomeDirectory() + File.separator + "conf" + File.separator + "pusher-apns.p8";

    public static String FCM_PROJECT_ID = JiveGlobals.getProperty("pusher.google.fcm.projectId", "");

    public static String APNS_TEAM_ID = JiveGlobals.getProperty("pusher.apple.apns.teamId", "");
    public static String APNS_KEY = JiveGlobals.getProperty("pusher.apple.apns.key", "");
    public static String APNS_BUNDLE_ID = JiveGlobals.getProperty("pusher.apple.apns.bundleId", "");
    public static Boolean APNS_SANDBOX_ENABLED = JiveGlobals.getBooleanProperty("pusher.apple.apns.sandbox");

    @Override
    public void propertySet(String property, Map<String, Object> params)
    {
        Object value = params.get("value");
        Log.debug("Property :{}: has been set new value: {}", property, value);
        switch (property)
        {
            case "pusher.google.fcm.projectId":
            {
                FCM_PROJECT_ID = (String) value;
                break;
            }
            case "pusher.apple.apns.teamId":
            {
                APNS_TEAM_ID = (String) value;
                break;
            }
            case "pusher.apple.apns.key":
            {
                APNS_KEY = (String) value;
                break;
            }
            case "pusher.apple.apns.bundleId":
            {
                APNS_BUNDLE_ID = (String) value;
                break;
            }
            case "pusher.apple.apns.sandbox":
            {
                APNS_SANDBOX_ENABLED = (Boolean) value;
                break;
            }
        }
    }

    @Override
    public void propertyDeleted(String property, Map<String, Object> params)
    {
        Log.debug("Property :{}: has been deleted", property);
        switch (property)
        {
            case "pusher.google.fcm.projectId":
            {
                FCM_PROJECT_ID = null;
                break;
            }
            case "pusher.apple.apns.teamId":
            {
                APNS_TEAM_ID = null;
                break;
            }
            case "pusher.apple.apns.key":
            {
                APNS_KEY = null;
                break;
            }
            case "pusher.apple.apns.bundleId":
            {
                APNS_BUNDLE_ID = null;
                break;
            }
            case "pusher.apple.apns.sandbox":
            {
                APNS_SANDBOX_ENABLED = false;
                break;
            }
        }
    }

    @Override
    public void xmlPropertySet(String property, Map<String, Object> params)
    {

    }

    @Override
    public void xmlPropertyDeleted(String property, Map<String, Object> params)
    {

    }
}