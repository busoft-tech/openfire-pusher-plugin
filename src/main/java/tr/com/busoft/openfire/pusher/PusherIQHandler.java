package tr.com.busoft.openfire.pusher;

import java.sql.Connection;
import java.sql.Statement;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

public class PusherIQHandler extends IQHandler
{
    private static final Logger Log = LoggerFactory.getLogger(PusherIQHandler.class);

    private static final String ELEMENT_NAME = "enable | disable";
    private static final String ELEMENT_NAMESPACE = "urn:xmpp:pusher";

    public PusherIQHandler()
    {
        super("Pusher IQ Handler");
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException
    {
        final IQ result = IQ.createResultIQ(packet);

        if (packet.isResponse())
        {
            Log.debug("Silently ignoring if packet is not response: {}", packet);
            return null;
        }

        if (IQ.Type.set != packet.getType())
        {
            Log.debug("Ignoring requests thar are not 'set': {}", packet);

            result.setError(PacketError.Condition.bad_request);
            return result;
        }

        Element node = packet.getChildElement();
        String nodeName = node.getName();
        switch (nodeName)
        {
            case "enable":
            {
                Element tokenNode = node.element("token");
                if (tokenNode == null)
                {
                    Log.debug("Ignoring an enable request if it does not include token: {}", packet);
                    result.setError(PacketError.Condition.bad_request);
                    return result;
                }

                Connection dbConnection = null;
                Statement statement = null;
                try
                {
                    dbConnection = DbConnectionManager.getConnection();
                    statement = dbConnection.createStatement();

                    JID sender = packet.getFrom();
                    String senderUsername = sender.getNode();
                    String resource = sender.getResource();
                    String token = tokenNode.getStringValue();

                    String sql = String.format("INSERT INTO ofPusher (username, resource, token) VALUES ('%s', '%s', '%s') ON DUPLICATE KEY UPDATE token = VALUES(token)", senderUsername, resource, token);
                    statement.executeUpdate(sql);
                }
                catch (Exception exception)
                {
                    Log.error("Error while enabling " + exception.getMessage());
                    result.setError(PacketError.Condition.bad_request);
                }
                finally
                {
                    try
                    {
                        DbConnectionManager.closeConnection(statement, dbConnection);
                    }
                    catch (Exception exception)
                    {
                        Log.error("Error while closing dbconnections " + exception.getMessage());
                        result.setError(PacketError.Condition.bad_request);
                    }
                }
                break;
            }
            case "disable":
            {
                Connection dbConnection = null;
                Statement statement = null;
                try
                {
                    dbConnection = DbConnectionManager.getConnection();
                    statement = dbConnection.createStatement();

                    JID sender = packet.getFrom();
                    String senderUsername = sender.getNode();
                    String sql = String.format("DELETE FROM ofPusher WHERE username = '%s'", senderUsername);

                    statement.executeUpdate(sql);
                }
                catch (Exception exception)
                {
                    Log.error("Error while deleting " + exception.getMessage());

                    result.setError(PacketError.Condition.bad_request);
                }
                finally
                {
                    try
                    {
                        DbConnectionManager.closeConnection(statement, dbConnection);
                    }
                    catch (Exception exception)
                    {
                        Log.error("Error while closing dbconnections " + exception.getMessage());

                        result.setError(PacketError.Condition.bad_request);
                    }
                }
                break;
            }
            default:
            {
                Log.debug("Ignoring a requests that does not include disable or enable: {}", packet);

                result.setError(PacketError.Condition.bad_request);
                return result;
            }
        }

        return result;
    }

    @Override
    public IQHandlerInfo getInfo()
    {
        return new IQHandlerInfo(ELEMENT_NAME, ELEMENT_NAMESPACE);
    }
}