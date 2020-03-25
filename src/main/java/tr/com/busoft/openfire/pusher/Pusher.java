package tr.com.busoft.openfire.pusher;

public class Pusher
{
    public String Username;
    public String Resource;
    public String Token;
    public DeviceType Type;

    public enum DeviceType
    {
        ios,
        android
    }

    public String getUsername()
    {
        return this.Username;
    }

    public String getResource()
    {
        return this.Resource;
    }

    public String getToken()
    {
        return this.Token;
    }

    public String getType()
    {
        return this.Type.toString();
    }
}