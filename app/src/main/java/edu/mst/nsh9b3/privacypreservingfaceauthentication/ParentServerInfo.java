package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import java.util.HashMap;

/**
 * Created by nick on 11/25/15.
 */
public class ParentServerInfo
{
    private String serverName;
    private HashMap<String, String> serverInfo;
    private final String FTPADDRESS = "FTPAddress";
    private final String FTPPORT = "FTPPort";
    private final String FTPUSERNAME = "FTPUsername";
    private final String FTPPASSWORD = "FTPPassword";

    public ParentServerInfo(String serverName, HashMap<String, String> serverInfo)
    {
        this.serverName = serverName;
        this.serverInfo = serverInfo;
    }

    public ParentServerInfo(String serverName)
    {
        this.serverName = serverName;

        serverInfo = new HashMap<String, String>();
        serverInfo.put(FTPADDRESS, "address");
        serverInfo.put(FTPPORT, "port");
        serverInfo.put(FTPUSERNAME, "username");
        serverInfo.put(FTPPASSWORD, "password");
    }

    public String getServerName()
    {
        return serverName;
    }

    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }

    public HashMap<String, String> getServerInfo()
    {
        return serverInfo;
    }

    public void setServerInfo(HashMap<String, String> serverInfo)
    {
        this.serverInfo = serverInfo;
    }

    public String getFtpAddress()
    {
        return serverInfo.get(FTPADDRESS);
    }

    public String getFtpPort()
    {
        return serverInfo.get(FTPPORT);
    }

    public String getFtpUsername()
    {
        return serverInfo.get(FTPUSERNAME);
    }

    public String getFtpPassword()
    {
        return serverInfo.get(FTPPASSWORD);
    }

    public void setFtpAddress(String ftpAddress)
    {
        serverInfo.put(FTPADDRESS, ftpAddress);
    }

    public void setFtpPort(String ftpPort)
    {
        serverInfo.put(FTPPORT, ftpPort);
    }

    public void setFtpUsername(String ftpUsername)
    {
        serverInfo.put(FTPUSERNAME, ftpUsername);
    }

    public void setFtpPassword(String ftpPassword)
    {
        serverInfo.put(FTPPASSWORD, ftpPassword);
    }
}
