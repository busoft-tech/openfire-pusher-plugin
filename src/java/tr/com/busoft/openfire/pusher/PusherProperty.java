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

    public static final String PROPERTY_NAME_FCM_PROJECT_ID = "pusher.google.fcm.projectId";

    public static String FCM_PROJECT_ID = JiveGlobals.getProperty(PROPERTY_NAME_FCM_PROJECT_ID, "");

    public static final String PROPERTY_NAME_APNS_TEAM_ID = "pusher.apple.apns.teamId";
    public static final String PROPERTY_NAME_APNS_KEY = "pusher.apple.apns.key";
    public static final String PROPERTY_NAME_APNS_BUNDLE_ID = "pusher.apple.apns.bundleId";
    public static final String PROPERTY_NAME_APNS_SANDBOX = "pusher.apple.apns.sandbox";

    public static String APNS_TEAM_ID = JiveGlobals.getProperty(PROPERTY_NAME_APNS_TEAM_ID, "");
    public static String APNS_KEY = JiveGlobals.getProperty(PROPERTY_NAME_APNS_KEY, "");
    public static String APNS_BUNDLE_ID = JiveGlobals.getProperty(PROPERTY_NAME_APNS_BUNDLE_ID, "");
    public static Boolean APNS_SANDBOX_ENABLED = JiveGlobals.getBooleanProperty(PROPERTY_NAME_APNS_SANDBOX);

    @Override
    public void propertySet(String property, Map<String, Object> params)
    {
        Object value = params.get("value");
        Log.debug("Property :{}: has been set new value: {}", property, value);
        switch (property)
        {
            case PROPERTY_NAME_FCM_PROJECT_ID:
            {
                FCM_PROJECT_ID = (String) value;
                break;
            }
            case PROPERTY_NAME_APNS_TEAM_ID:
            {
                APNS_TEAM_ID = (String) value;
                break;
            }
            case PROPERTY_NAME_APNS_KEY:
            {
                APNS_KEY = (String) value;
                break;
            }
            case PROPERTY_NAME_APNS_BUNDLE_ID:
            {
                APNS_BUNDLE_ID = (String) value;
                break;
            }
            case PROPERTY_NAME_APNS_SANDBOX:
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
            case PROPERTY_NAME_FCM_PROJECT_ID:
            {
                FCM_PROJECT_ID = null;
                break;
            }
            case PROPERTY_NAME_APNS_TEAM_ID:
            {
                APNS_TEAM_ID = null;
                break;
            }
            case PROPERTY_NAME_APNS_KEY:
            {
                APNS_KEY = null;
                break;
            }
            case PROPERTY_NAME_APNS_BUNDLE_ID:
            {
                APNS_BUNDLE_ID = null;
                break;
            }
            case PROPERTY_NAME_APNS_SANDBOX:
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