package tr.com.busoft.openfire.pusher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.com.busoft.openfire.pusher.Pusher.DeviceType;

public class PusherManager
{
    private static final Logger Log = LoggerFactory.getLogger(PusherManager.class);

    public static List<Pusher> getDeviceList(int pageIndex, int pageSize)
    {
        Connection dbConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        List<Pusher> pusherList = null;
        try
        {
            dbConnection = DbConnectionManager.getConnection();
            statement = dbConnection.createStatement();

            int offset = (pageIndex - 1) * pageSize;

            String sql = String.format("SELECT * FROM ofPusher LIMIT %s, %s", offset, pageSize);

            resultSet = statement.executeQuery(sql);

            pusherList = new ArrayList<Pusher>();
            while (resultSet.next())
            {
                Pusher pusher = new Pusher();

                pusher.Username = resultSet.getString("username");
                pusher.Resource = resultSet.getString("resource");
                pusher.Token = resultSet.getString("token");
                pusher.Type = resultSet.getString("type").equals("ios") ? DeviceType.ios : DeviceType.android;

                pusherList.add(pusher);
            }
        }
        catch (Exception exception)
        {
            Log.error("Error while querying the devices: {}", exception.getMessage());
        }
        finally
        {
            DbConnectionManager.closeConnection(resultSet, statement, dbConnection);
        }

        return pusherList;
    }

    public static List<Pusher> searchDeviceList(String search, int pageIndex, int pageSize)
    {
        Connection dbConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        List<Pusher> pusherList = null;
        try
        {
            dbConnection = DbConnectionManager.getConnection();
            statement = dbConnection.createStatement();

            int offset = (pageIndex - 1) * pageSize;

            String sql = String.format("SELECT * FROM ofPusher WHERE username LIKE '%%%s%%' LIMIT %s, %s", search, offset, pageSize);

            resultSet = statement.executeQuery(sql);

            pusherList = new ArrayList<Pusher>();
            while (resultSet.next())
            {
                Pusher pusher = new Pusher();

                pusher.Username = resultSet.getString("username");
                pusher.Resource = resultSet.getString("resource");
                pusher.Token = resultSet.getString("token");
                pusher.Type = resultSet.getString("type").equals("ios") ? DeviceType.ios : DeviceType.android;

                pusherList.add(pusher);
            }
        }
        catch (Exception exception)
        {
            Log.error("Error while querying the devices: {}", exception.getMessage());
        }
        finally
        {
            DbConnectionManager.closeConnection(resultSet, statement, dbConnection);
        }

        return pusherList;
    }

    public static int getTotalDeviceCount()
    {
        Connection dbConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        int count = 0;
        try
        {
            dbConnection = DbConnectionManager.getConnection();
            statement = dbConnection.createStatement();

            String sql = String.format("SELECT COUNT(*) FROM ofPusher");

            resultSet = statement.executeQuery(sql);

            resultSet.next();

            count = resultSet.getInt(1);
        }
        catch (Exception exception)
        {
            Log.error("Error while querying the devices: {}", exception.getMessage());
        }
        finally
        {
            DbConnectionManager.closeConnection(resultSet, statement, dbConnection);
        }

        return count;
    }

    public static int getSearchTotalDeviceCount(String search)
    {
        Connection dbConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        int count = 0;
        try
        {
            dbConnection = DbConnectionManager.getConnection();
            statement = dbConnection.createStatement();

            String sql = String.format("SELECT COUNT(*) FROM ofPusher WHERE username LIKE '%%%s%%'", search);

            resultSet = statement.executeQuery(sql);

            resultSet.next();

            count = resultSet.getInt(1);
        }
        catch (Exception exception)
        {
            Log.error("Error while querying the devices: {}", exception.getMessage());
        }
        finally
        {
            DbConnectionManager.closeConnection(resultSet, statement, dbConnection);
        }

        return count;
    }

    public static void deleteDevice(String username, String resource)
    {
        Connection dbConnection = null;
        Statement statement = null;
        try
        {
            dbConnection = DbConnectionManager.getConnection();
            statement = dbConnection.createStatement();

            String sql = String.format("DELETE FROM ofPusher WHERE username = '%s' AND resource = '%s'", username, resource);

            statement.executeUpdate(sql);
        }
        catch (Exception exception)
        {
            Log.error("Error while querying the devices: {}", exception.getMessage());
        }
        finally
        {
            DbConnectionManager.closeConnection(statement, dbConnection);
        }
    }

    public static String getProperty(String property)
    {
        return JiveGlobals.getProperty(property, "");
    }

    public static void writeCredentialFileContent(String content, String type)
    {
        // Write data out to conf/pusher.p8 file.
        Writer writer = null;
        try
        {
            // Create the conf folder if required
            File file = new File(JiveGlobals.getHomeDirectory(), "conf");
            if (!file.exists())
            {
                file.mkdir();
            }

            if (type.equals("ios"))
            {
                file = new File(JiveGlobals.getHomeDirectory() + File.separator + "conf", "pusher-apns.p8");
            }
            else if (type.equals("android"))
            {
                file = new File(JiveGlobals.getHomeDirectory() + File.separator + "conf", "pusher-fcm.json");
            }
            else
            {
                throw new Exception("Unexpected content type while writing file");
            }

            // Delete the old pusher.p8 file if it exists
            if (file.exists())
            {
                file.delete();
            }
            // Create new pusher.p8 with data
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writer.write(content);
        }
        catch (Exception exception)
        {
            Log.error("Error while writing to file: {}", exception.getMessage());
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException exception)
                {
                    Log.error("Error while closing writer: {}", exception.getMessage());
                }
            }
        }
    }
}