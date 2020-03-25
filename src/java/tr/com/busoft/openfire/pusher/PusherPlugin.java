package tr.com.busoft.openfire.pusher;

import java.io.File;

import org.jivesoftware.openfire.OfflineMessageStrategy;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PusherPlugin implements Plugin
{
    private static final Logger Log = LoggerFactory.getLogger(PusherPlugin.class);

    private PushNotification pushNotification;
    private PusherIQHandler pusherIQHandler;

    @Override
    public void initializePlugin(PluginManager manager, File pluginDirectory)
    {
        Log.debug("Pusher plugin is initalizing");

        pushNotification = new PushNotification();
        pusherIQHandler = new PusherIQHandler();

        OfflineMessageStrategy.addListener(pushNotification);

        XMPPServer.getInstance().getIQRouter().addHandler(pusherIQHandler);
    }

    @Override
    public void destroyPlugin()
    {
        OfflineMessageStrategy.removeListener(pushNotification);

        XMPPServer.getInstance().getIQRouter().removeHandler(pusherIQHandler);

        Log.debug("Pusher plugin is destroyed");
    }
}
