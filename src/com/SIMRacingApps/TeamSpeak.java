package com.SIMRacingApps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.SIMRacingApps.SIMPlugin;
import com.SIMRacingApps.SIMPlugin.SIMPluginException;
import com.SIMRacingApps.Data;

/**
 * This class implements the interface to the TeamSpeak Application.
 * They provide a ClientQuery plug-in that allows you to control and query various features.
 * The plug-in must be enabled and the TeamSpeak client running for this class to work.
 * <p>
 * The class can be used stand alone, but if you pass in a SIMPlugin to a SIM, 
 * it will update your alias by prepending your car number to it.
 * Another feature is you can get notified who is talking and if they are whispering.
 * Many other things are possible, so stay tuned and ask for new features.
 * <p>
 * See an example test application at the bottom of the source code.
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2021 Jeffrey Gilliam
 * @license Apache License 2.0
 */
public class TeamSpeak {

    private SIMPlugin                      m_SIMplugin;
    private String                         m_hostname    = "localhost";
    private Map<String,Map<String,String>> m_clientlist  = new HashMap<String,Map<String,String>>();
    private String                         m_carnumber   = "";
    private String                         m_drivername  = "";
    private Boolean                        m_stayalive   = false;
    private Long                           m_talkTimeout = 60000L;
    private Long                           m_noopTimer   = 60000L;

    @SuppressWarnings("unused")
    private TeamSpeak() {
    }

    
    /**
     * Constructor to initiate a connection to the TeamSpeak Client when running on same machine as the SIMRacingApps server.
     * @param SIMPlugin The current SIM plug-in or null if not using with a SIM.
     */
    public TeamSpeak(SIMPlugin SIMPlugin) {
        m_SIMplugin = SIMPlugin;
    }
    
    /**
     * Constructor to initiate a connection to the TeamSpeak Client when not running on same machine as the SIMRacingApps server.
     * @param SIMPlugin The current SIM plug-in or null if not using with a SIM.
     * @param host The host or IP address of where the client is running.
     */
    public TeamSpeak(SIMPlugin SIMPlugin,String host) {
        m_SIMplugin = SIMPlugin;
        m_hostname = host;
    }

    /**
     * Called to disconnect from the TeamSpeak Client and stop the thread.
     */
    public void disconnect() {
        Server.logger().info("disconnect()");
        synchronized (m_stayalive) {
            m_stayalive = false;
        }
    }

    private String m_carnumber_cached = "";
    private String m_drivername_cached = "";
    /**
     * This method is to be called by the SIM any time there's new data.
     * I will use this to update the car number and possibly other things in the future.
     */
    public void update() {
        //take this opportunity to update the car number if it has changed.
        if (m_SIMplugin != null) {
            //don't update the car number unless the SIM is connected to a session.
            String number = "";
            String drivername = "";
            if (m_SIMplugin.isConnected()) {
                number = m_SIMplugin.getSession().getCar("ME").getNumber().getString();
                drivername = m_SIMplugin.getSession().getCar("ME").getDriverName(false).getString();
            }
            //Cache the car number so we don't have to wait on the sync unless it has changed.
            if (!number.equals(m_carnumber_cached)) {
                m_carnumber_cached = number;

                synchronized(m_carnumber) {
                    m_carnumber = number;
                }
            }
            if (!drivername.equals(m_drivername_cached)) {
                m_drivername_cached = drivername;

                synchronized(m_drivername) {
                    m_drivername = drivername;
                }
            }
        }
    }
    /**
     * Returns the name of the person that is currently talking in TeamSpeak.
     * It also prepends your car number to your TeamSpeak alias for you.
     * @return The talkers name in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getTalker() {

        //return new Data("TeamSpeakTalker","#61 Jeffrey Gilliam");
        return new Data("TeamSpeakTalker",_getTalker().get("client_nickname"));
    }
    
    /**
     * Returns the word "Whispering" if the talker is whispering, blank if not.
     * @return A string in a {@link com.SIMRacingApps.Data} container.
     */
    public    Data    getWhispering() {
        try {
            if (Integer.parseInt(_getTalker().get("is_whispering")) > 0
            &&  Integer.parseInt(_getTalker().get("is_talking")) > 0
            )
                return new Data("TeamSpeakWhispering","Whispering");
        }
        catch (NumberFormatException e) {}
        
        return new Data("TeamSpeakWhispering","");
    }

    private int _getErrorId(Map<String,String> vars) {
        int i = 0;
        
        if (vars != null && vars.get("id") != null) {
            try {
                i = Integer.parseInt(vars.get("id"));
            }
            catch (NumberFormatException e) {}
        }
        return i;
    }
    
    private Map<String,String> _getTalker() {
        Map<String,String> clienttalking = new HashMap<String,String>();
        String nickname = "";
        String is_talking = "0";
        String is_whispering = "0";

        synchronized(m_clientlist) {
            for ( Entry<String,Map<String,String>> client : m_clientlist.entrySet()) {
                if (client.getValue().get("is_whispering").equals("1")
                &&  client.getValue().get("is_talking").equals("1")
                &&  (Long.parseLong(client.getValue().get("timestamp")) + m_talkTimeout) > System.currentTimeMillis()
                ) {
                    nickname      = new String(client.getValue().get("client_nickname"));
                    is_talking    = "1";
                    is_whispering = "1";
                    break;
                }
            }
            if (nickname.isEmpty()) {
                for ( Entry<String,Map<String,String>> client : m_clientlist.entrySet()) {
                    if (client.getValue().get("is_talking").equals("1")
                    &&  (Long.parseLong(client.getValue().get("timestamp")) + m_talkTimeout) > System.currentTimeMillis()
                    ) {
                        nickname      = new String(client.getValue().get("client_nickname"));
                        is_talking    = "1";
                        is_whispering = "0";
                        break;
                    }
                }
            }
        }

        clienttalking.put("client_nickname", nickname);
        clienttalking.put("is_talking", is_talking);
        clienttalking.put("is_whispering", is_whispering);
        return clienttalking;
    }

    /**** internal methods used by the background thread ******/

    @SuppressWarnings("serial")
	private class NotConnectedException extends Exception {
        public NotConnectedException() { super(); }
    }

    private String _decode(String str) {
        str = str.replace("\\\\", "\\[$mksave]");
        str = str.replace("\\s", " ");
        str = str.replace("\\/", "/");
        str = str.replace("\\p", "|");
        str = str.replace("\\b", "\b");
        str = str.replace("\\f", "\f");
        str = str.replace("\\n", "\n");
        str = str.replace("\\r", "\r");
        str = str.replace("\\t", "\t");

        Character cBell = new Character((char)7); // \a (not supported by Java)
        Character cVTab = new Character((char)11); // \v (not supported by Java)

        str = str.replace("\\a", cBell.toString());
        str = str.replace("\\v", cVTab.toString());

        str = str.replace("\\[$mksave]", "\\");
        return str;
    }

    private String _encode(String str)
    {
        str = str.replace("\\", "\\\\");
        str = str.replace(" ", "\\s");
        str = str.replace("/", "\\/");
        str = str.replace("|", "\\p");
        str = str.replace("\b", "\\b");
        str = str.replace("\f", "\\f");
        str = str.replace("\n", "\\n");
        str = str.replace("\r", "\\r");
        str = str.replace("\t", "\\t");

        Character cBell = new Character((char)7); // \a (not supported by Java)
        Character cVTab = new Character((char)11); // \v (not supported by Java)

        str = str.replace(cBell.toString(), "\\a");
        str = str.replace(cVTab.toString(), "\\v");

        return str;
    }

    private boolean _processEvent(ArrayList<Map<String,String>> event, String line) {
        if (line.startsWith("channellist")) {
            Server.logger().finest(String.format("TeamSpeak: _processEvent(): %s", line));
            return true;
        }
        else
        if (line.startsWith("notify")) {
            Server.logger().finest(String.format("TeamSpeak: _processEvent(): %s", line));

            if (event.get(0).containsKey("notifyclientmoved")) {
                //notifyclientmoved schandlerid=1 ctid=29 reasonid=0 clid=4
                if (m_clientlist.containsKey(event.get(0).get("clid"))) {
                    Map<String,String> client = m_clientlist.get(event.get(0).get("clid"));
                    client.put("cid", event.get(0).get("ctid"));
                }
            }

            return true;
        }
        return false;
    }

    private ArrayList<Map<String,String>> _parseLine(String line) {
        ArrayList<Map<String,String>> variables = new ArrayList<Map<String,String>>();

        if (!line.isEmpty()) {
            String a[] = line.split("[|]");

            for (int j=0; j < a.length; j++) {
                String k[] = a[j].split("[ ]");

                Map<String,String> vars = new HashMap<String,String>();
                for (int i=0; i < k.length; i++) {
                    String v[] = k[i].split("=");
                    if (v.length == 2) {
                        vars.put(_decode(v[0]),_decode(v[1]));
                    }
                    else {
                        vars.put(_decode(v[0]),"");
                    }
                }
                variables.add(j,vars);
            }
        }

        return variables;
    }

    private void _disconnect(Socket socketQuery) {
        if (socketQuery != null) {

            try
            {
                Thread.sleep(1000);
                socketQuery.close();
            }
            catch (Exception e){}

            Server.logger().info("TeamSpeak: disconnected");
        }
    }

    private void _authMessage() throws NotConnectedException {
        Server.logger().warning("settings: teamspeak-apikey not valid or not set. See https://github.com/SIMRacingApps/SIMRacingApps/wiki/TeamSpeak-Integration");
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {}
        throw new NotConnectedException();
    }
    
    private boolean _auth(BufferedReader in, PrintStream out) throws Exception {

        String apikey = Server.getArg("teamspeak-apikey","");

        if (apikey.isEmpty())
            return true;
        
        try {
            Server.logger().info(String.format("TeamSpeak: _auth(apikey=%s):",apikey));
            out.printf("auth apikey=%s\n",apikey);

            try
            {
                while (true)
                {
                    String s = in.readLine();
                    if (!s.isEmpty()) {
                        Server.logger().finest(String.format("TeamSpeak Debug: _auth(): %s",s));

                        ArrayList<Map<String,String>> vars = _parseLine(s);
                        if (_processEvent(vars,s)) {
                            //do nothing
                        }
                        else
                        if (vars.get(0).containsKey("error")) {
                            if (_getErrorId(vars.get(0)) == 1796)
                                _authMessage();
                            
                            if (_getErrorId(vars.get(0)) == 1794  
                            ||  _getErrorId(vars.get(0)) == 1540  //convert error
                            ||  _getErrorId(vars.get(0)) == 1799  //invalid server Connection
                            )
                                throw new NotConnectedException();

                            if (_getErrorId(vars.get(0)) != 0)
                                Server.logger().warning(String.format("TeamSpeak: _auth() returned error %d, %s", _getErrorId(vars.get(0)),_decode(vars.get(0).get("msg"))));
                            return false;
                        }
                    }
                }
            }
            catch (SocketTimeoutException e) {
                Server.logger().warning("TeamSpeak: _auth(): SocketTimeoutException: "+e.getMessage());
            }

        }   
        catch (NotConnectedException e) { throw e; }
        catch (Exception e)
        {
            Server.logStackTrace(Level.SEVERE,"TeamSpeak: _auth(): Unexpected Exception caught",e);
            throw e;
        }

        return true;
    }

    private void _getClientList(BufferedReader in, PrintStream out) throws Exception {

        try {
            Server.logger().finest("TeamSpeak Debug: _getClientList():");

            //m_clientlist.clear();

            out.printf("clientlist\n");

            try
            {
                while (true)
                {
                    String s = in.readLine();
                    if (!s.isEmpty()) {
                        Server.logger().finest(String.format("TeamSpeak Debug: _getClientList(): %s",s));

                        ArrayList<Map<String,String>> arrayvars = _parseLine(s);

                        for (int j=0; j < arrayvars.size(); j++) {
                            Map<String,String> vars = arrayvars.get(j);

                            if (_processEvent(arrayvars,s)) {
                                //do nothing
                            }
                            else
                            if (vars.containsKey("error")) {
                                if (_getErrorId(vars) == 1796)
                                    _authMessage();
                                
                                if (_getErrorId(vars) == 1794  
                                ||  _getErrorId(vars) == 1540  //convert error
                                ||  _getErrorId(vars) == 1799  //invalid server Connection
                                )
                                    throw new NotConnectedException();

                                if (_getErrorId(vars) != 0)
                                    Server.logger().warning(String.format("TeamSpeak: _getClientList(): returned error %d, %s", _getErrorId(vars),_decode(vars.get("msg"))));

                                return;
                            }
                            else {

                                String clid = vars.get("clid");
                                Map<String,String> client = new HashMap<String,String>();

                                client.put("client_nickname", vars.get("client_nickname"));
                                client.put("is_talking",      m_clientlist.containsKey(clid) ? m_clientlist.get(clid).get("is_talking"): "0");
                                client.put("is_whispering",   m_clientlist.containsKey(clid) ? m_clientlist.get(clid).get("is_whispering"): "0");
                                client.put("timestamp", String.format("%d", System.currentTimeMillis()));

                                Server.logger().finest(String.format("TeamSpeak Debug: _getClientList(): %s = %s",
                                        clid,
                                        client.get("client_nickname")
                                ));

                                synchronized (m_clientlist) {
                                    m_clientlist.put(clid,client);
                                }
                            }
                        }

                    }
                }
            }
            catch (SocketTimeoutException e) {
                Server.logger().warning("TeamSpeak: _getClientList(): SocketTimeoutException: "+e.getMessage());
            }
        }
        catch (NotConnectedException e) { throw e; }
        catch (Exception e)
        {
            Server.logStackTrace(Level.SEVERE,"TeamSpeak: getClientList(): Unexpected Exception caught: "+e.getMessage(),e);
            throw e;
        }
    }

    private Map<String,String> _getClientInfo(BufferedReader in, PrintStream out, String clid) throws Exception {

        Map<String,String> client = new HashMap<String,String>();

        try {

            out.printf("clientvariable clid=%s client_nickname client_is_talker\n",clid);

            try
            {

                while (true)
                {
                    String s = in.readLine();
                    if (!s.isEmpty()) {
                        Server.logger().finest(String.format("TeamSpeak Debug: _getClientInfo(): %s",s));

                        ArrayList<Map<String,String>> vars = _parseLine(s);
                        if (_processEvent(vars,s)) {
                            //do nothing
                        }
                        else
                        if (vars.get(0).containsKey("error")) {
                            if (_getErrorId(vars.get(0)) == 1796)
                                _authMessage();
                            
                            if (_getErrorId(vars.get(0)) == 1794  
                            ||  _getErrorId(vars.get(0)) == 1540  //convert error
                            ||  _getErrorId(vars.get(0)) == 1799  //invalid server Connection
                            )
                                throw new NotConnectedException();

                            if (_getErrorId(vars.get(0)) != 0)
                                Server.logger().warning(String.format("TeamSpeak: _getClientInfo(): returned error %d, %s",
                                        _getErrorId(vars.get(0)),
                                        _decode(vars.get(0).get("msg"))
                                ));
                            return client;
                        }
                        else {

                            client = vars.get(0);
                            client.put("is_talking", "0");
                            client.put("is_whispering", "0");
                            client.put("timestamp", String.format("%d", System.currentTimeMillis()));
                        }
                    }
                }
            }
            catch (SocketTimeoutException e) {
                Server.logger().warning("TeamSpeak: _getClientInfo(): SocketTimeoutException: "+e.getMessage());
            }

        }
        catch (NotConnectedException e) { throw e; }
        catch (Exception e)
        {
            Server.logStackTrace(Level.SEVERE,"TeamSpeak: _getClientInfo(): Unexpected Exception caught",e);
            throw e;
        }

        return client;
    }

    private Map<String,String> _getME(BufferedReader in, PrintStream out) throws Exception {

        Map<String,String> client = new HashMap<String,String>();

        try {
            out.printf("whoami\n");

            try
            {

                while (true)
                {
                    String s = in.readLine();
                    if (!s.isEmpty()) {
                        Server.logger().finest(String.format("TeamSpeak Debug: _getME(): %s",s));

                        ArrayList<Map<String,String>> vars = _parseLine(s);
                        if (_processEvent(vars,s)) {
                            //do nothing
                        }
                        else
                        if (vars.get(0).containsKey("error")) {
                            if (_getErrorId(vars.get(0)) == 1796)
                                _authMessage();
                            
                            if (_getErrorId(vars.get(0)) == 1794  
                            ||  _getErrorId(vars.get(0)) == 1540  //convert error
                            ||  _getErrorId(vars.get(0)) == 1799  //invalid server Connection
                            )
                                throw new NotConnectedException();

                            if (_getErrorId(vars.get(0)) != 0)
                                Server.logger().warning(String.format("TeamSpeak: _getME() returned error %d, %s", _getErrorId(vars.get(0)),_decode(vars.get(0).get("msg"))));
                            return client;
                        }
                        else {
                            client = vars.get(0);
                        }
                    }
                }
            }
            catch (SocketTimeoutException e) {
                Server.logger().warning("TeamSpeak: _getME(): SocketTimeoutException: "+e.getMessage());
            }

        }
        catch (NotConnectedException e) { throw e; }
        catch (Exception e)
        {
            Server.logStackTrace(Level.SEVERE,"TeamSpeak: _getME(): Unexpected Exception caught",e);
            throw e;
        }

        return client;
    }

    private boolean _setNickname(BufferedReader in, PrintStream out, String nickname) throws NotConnectedException {
        boolean result = false;

        try {
            Map<String,String> me = _getME(in,out);
            Map<String,String> client = _getClientInfo(in,out,me.get("clid"));

            //if nickname already set, then setting it again will give a already exists error
            if (client.get("client_nickname").equals(nickname)) {
                Server.logger().finest(String.format("TeamSpeak: Nickname was already set to %s", nickname));
                result = true;
            }
            else {
                out.printf("clientupdate client_nickname=%s\n",_encode(nickname));

                try
                {

                    while (true)
                    {
                        String s = in.readLine();
                        if (!s.isEmpty()) {
                            Server.logger().finest(String.format("TeamSpeak Debug: _setNickname(): %s",s));

                            ArrayList<Map<String,String>> vars = _parseLine(s);
                            if (_processEvent(vars,s)) {
                                //do nothing
                            }
                            else
                            if (vars.get(0).containsKey("error")) {
                                if (_getErrorId(vars.get(0)) == 1796)
                                    _authMessage();
                                
                                if (_getErrorId(vars.get(0)) == 1794  
                                ||  _getErrorId(vars.get(0)) == 1540  //convert error
                                ||  _getErrorId(vars.get(0)) == 1799  //invalid server Connection
                                )
                                    throw new NotConnectedException();

                                if (_getErrorId(vars.get(0)) != 0) {
                                    Server.logger().warning(String.format("TeamSpeak: _setNickname(): returned error %d, %s", _getErrorId(vars.get(0)),_decode(vars.get(0).get("msg"))));
                                    return false;
                                }

                                Server.logger().finest(String.format("TeamSpeak: Nickname set to %s", nickname));
                                result = true;
                                break;
                            }
                        }
                    }
                }
                catch (SocketTimeoutException e) {
                    Server.logger().warning("TeamSpeak: _setNickname(): SocketTimeoutException: "+e.getMessage());
                }

                _getClientList(in,out); //reload the client list
            }
        }
        catch (NotConnectedException e) { throw e; }
        catch (Exception e)
        {
            Server.logStackTrace(Level.SEVERE,"TeamSpeak: _setNickname(): Unexpected Exception caught",e);
        }
        return result;
    }

    private boolean _registerEvents(BufferedReader in, PrintStream out) throws NotConnectedException {
        boolean result = false;

        try {
            out.printf("clientnotifyregister schandlerid=0 event=any\n");

            try
            {
                while (true)
                {
                    String s = in.readLine();
                    if (!s.isEmpty()) {
                        Server.logger().finest(String.format("TeamSpeak Debug: _registerEvents(): %s",s));

                        ArrayList<Map<String,String>> vars = _parseLine(s);
                        if (vars.get(0).containsKey("error")) {
                            if (_getErrorId(vars.get(0)) == 1796)
                                _authMessage();
                            
                            if (_getErrorId(vars.get(0)) == 1794  
                            ||  _getErrorId(vars.get(0)) == 1540  //convert error
                            ||  _getErrorId(vars.get(0)) == 1799  //invalid server Connection
                            )
                                throw new NotConnectedException();

                            if (_getErrorId(vars.get(0)) != 0)
                                Server.logger().warning(String.format("TeamSpeak: _registerEvents(): returned error %d, %s", _getErrorId(vars.get(0)),_decode(vars.get(0).get("msg"))));
                            else
                                result = true;
                            break;
                        }
                    }
                }
            }
            catch (SocketTimeoutException e) {
                Server.logger().warning("TeamSpeak: _registerEvents(): SocketTimeoutException: "+e.getMessage());
            }

        }
        catch (NotConnectedException e) { throw e; }
        catch (Exception e)
        {
            Server.logStackTrace(Level.SEVERE,"TeamSpeak: _registerEvents(): Unexpected Exception caught",e);
        }

        return result;
    }

    private Object m_threadLock = new Object();
    private Thread m_thread = null;

    public void startListener() {
        synchronized (m_threadLock) { 
            if (m_thread == null) {
                m_stayalive = true;
                m_thread = new Thread( new Runnable() {
                    public void run() {
    
                        Server.logger().info("start() is running...");
                        boolean stayalive = true;
                        String carnumber = "xxx";
                        boolean refreshClientList = true;
                        Socket socketQuery = null;
                        boolean eventsRegistered = false;
                        BufferedReader in = null;
                        PrintStream out = null;
                        Long lastNoop = 0L;
    
                        while (stayalive) {
                            synchronized (m_stayalive) {
                                stayalive = m_stayalive;
                            }
                            
                            try {
                                if (socketQuery == null || !socketQuery.isConnected()) {
                                    try  // Open socket connection to TS telnet port
                                    {
                                        Server.logger().finest("TeamSpeak: Connecting to ClientQuery Plug-in...");
                                        socketQuery = new Socket(m_hostname,25639); //This port it not configurable on the ClientQuery plug-in
                                        eventsRegistered = false;
                                        refreshClientList = true;
                                    }
                                    catch (ConnectException e) {
                                        Server.logger().finest("TeamSpeak: Not running or ClientQuery Plug-in not enabled");
                                        socketQuery = null;
                                        try { Thread.sleep(10000); } catch (InterruptedException e2) {}
                                    }
                                    catch (Exception e)
                                    {
                                        Server.logStackTrace(Level.SEVERE,"TeamSpeak: listener(): unexpected exception: "+e.getMessage(),e);
                                        socketQuery = null;
                                        try { Thread.sleep(10000); } catch (InterruptedException e2) {}
                                    }
                                }
    
                                if (socketQuery != null && !eventsRegistered) {
                                    if (socketQuery.isConnected())
                                    {
                                        try
                                        {
                                            in = new BufferedReader(new InputStreamReader(socketQuery.getInputStream(), "UTF-8"));
                                            out = new PrintStream(socketQuery.getOutputStream(), true, "UTF-8");
    
                                            socketQuery.setSoTimeout(5000);
    
                                            String serverIdent = in.readLine();
                                            if (!serverIdent.equals("TS3 Client"))
                                            {
                                                socketQuery.close();
                                                socketQuery = null;
                                                Server.logger().warning(String.format("TeamSpeak: warning, not a known TeamSpeak Client (%s)",serverIdent));
                                            }
    
                                            socketQuery.setSoTimeout(500);
    
                                            try
                                            {
                                                while (true)
                                                {
                                                    String s = in.readLine(); // Catch useless lines after connecting
                                                    Server.logger().finest("TeamSpeak: Debug: "+s);
                                                }
                                            }
                                            catch (SocketTimeoutException e) {
                                                Server.logger().finest("TeamSpeak: startListener(): SocketTimeoutException flushing...: "+e.getMessage());
                                            }
                                            Server.logger().info("TeamSpeak: Connected to ClientQuery Plug-in");
                                            _auth(in,out);
    
                                            eventsRegistered = _registerEvents(in,out);
                                        }
                                        catch (Exception e)
                                        {
                                            _disconnect(socketQuery);
                                            socketQuery = null;
                                        }
                                    }
                                    else
                                    {
                                        _disconnect(socketQuery);
                                        socketQuery = null;
                                    }
                                } //if (socketQuery != null)
    
                                if (socketQuery != null && socketQuery.isConnected()) {
                                    if (Server.getArg("teamspeak-carnumber", true)) {
                                        synchronized(m_carnumber) {
                                            if (!carnumber.equals(m_carnumber)) {
                                                Map<String,String> me = _getME(in,out);
                                                Map<String,String> client = _getClientInfo(in,out,me.get("clid"));
        
                                                String nickname = client.get("client_nickname");
                                                if (nickname != null) {
                                            		nickname = nickname.replaceFirst("^(#?)([ ]*?)(\\d*)([ ]?)", "");
                                                	if (!m_carnumber.isEmpty())
                                                		nickname = "#"+m_carnumber+" "+(m_drivername.isEmpty() || !Server.getArg("teamspeak-update-name", false) ? nickname : m_drivername);
        
        	                                        _setNickname(in,out,nickname);
        	                                        carnumber = m_carnumber;
                                                }
                                            }
                                        }
                                    }
    
                                    if (refreshClientList == true) {
                                        _getClientList(in,out);
                                        refreshClientList = false;
                                    }
    
                                    try {
                                        //do this every so often to keep the telnet server alive
                                        if ((lastNoop + m_noopTimer) < System.currentTimeMillis()) {
                                            _getME(in,out);
                                            lastNoop = System.currentTimeMillis();
                                        }
                                        
                                        String s = in.readLine();
                                        if (s != null && !s.isEmpty()) {
                                            try {
                                                Server.logger().finest(String.format("TeamSpeak Debug: Listener(): %s",s));
        
                                                ArrayList<Map<String,String>> vars = _parseLine(s);
                                                if (vars.get(0).containsKey("error")) {
                                                    if (_getErrorId(vars.get(0)) != 0) {
                                                        Server.logger().warning(String.format("TeamSpeak: Listener(): returned error %d, %s", _getErrorId(vars.get(0)),_decode(vars.get(0).get("msg"))));
                                                    }
                                                }
                                                else
                                                if (vars.get(0).containsKey("notifytalkstatuschange")) {
                                                    synchronized(m_clientlist) {
                                                        if (vars.get(0).get("isreceivedwhisper").equals("1"))
                                                            m_clientlist.get(vars.get(0).get("clid")).put("is_whispering", "1");
                                                        else
                                                            m_clientlist.get(vars.get(0).get("clid")).put("is_whispering", "0");
        
                                                        m_clientlist.get(vars.get(0).get("clid")).put("is_talking", vars.get(0).get("status"));
                                                        m_clientlist.get(vars.get(0).get("clid")).put("timestamp", String.format("%d", System.currentTimeMillis()));
    
                                                        Server.logger().finest(String.format("TeamSpeak Debug: Listener(): %s, is_talking=%s, is_whispering=%s",
                                                            m_clientlist.get(vars.get(0).get("clid")).get("client_nickname"),
                                                            m_clientlist.get(vars.get(0).get("clid")).get("is_talking"),
                                                            m_clientlist.get(vars.get(0).get("clid")).get("is_whispering")
                                                        ));
                                                    }
                                                }
                                                else
                                                if (vars.get(0).containsKey("notifyclientleftview")
                                                ||  vars.get(0).containsKey("notifycliententerview")
                                                ||  vars.get(0).containsKey("notifyclientupdated")
                                                ) {
                                                    refreshClientList = true;
                                                }
                                                else
                                                    _processEvent(vars,s);
                                            }
                                            catch (Exception e) {
                                                //any exception generated here we will just log to see what they are
                                                //I don't want parsing errors to stop the reading of the socket.
                                                Server.logger().warning("TeamSpeak: startListener().read(): Exception: "+e.getMessage());
                                                _disconnect(socketQuery);
                                                socketQuery = null;
                                            }
                                        }
                                    }
                                    catch (SocketTimeoutException e) {
                                        //Server.logger().finest("TeamSpeak: startListener(): SocketTimeoutException: "+e.getMessage());
                                    }
                                }
                            }
                            catch (NotConnectedException e) {
                                Server.logger().info("TeamSpeak: startListener(): NotConnected. Retrying in 10 seconds...");
                                _disconnect(socketQuery);
                                socketQuery = null;
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException e1) {
                                }
                            }
                            catch (Exception e)
                            {
                                Server.logStackTrace(Level.SEVERE,"TeamSpeak: startListener(): Unknown Exception caught",e);
                                _disconnect(socketQuery);
                                socketQuery = null;
                            }
                        } //while (stayalive)
    
                        Server.logger().info(String.format("TeamSpeak: Event Listener Terminating. stayalive = %s",Boolean.toString(stayalive)));
                        m_thread = null;
                    } //run()
                }); //new Thread()
    
                m_thread.setName("TeamSpeak.Listener");
                m_thread.start();
    
            }//m_thread == null
        }
    } //startListener()

    /* just for testing */
    public static void main(String[] args)
    {
        try {
            SIMPlugin connector = SIMPlugin.createSIMPlugin("iRacing");
            connector.setPlay("Charlotte1.iRacing");
            
            TeamSpeak ts = new TeamSpeak(connector,"localhost");
            ts.startListener();
    
            System.err.printf("TeamSpeak: listening\n");
    
            String prev_name = "";
    
            while (true) {
                String name = ts.getTalker().getString();
                if (!prev_name.equals(name)) {
                    if (name.isEmpty()) {
                        System.err.printf("TeamSpeak: Not Talking = %s\n", prev_name);
                        prev_name = "";
                    }
                    else {
                        prev_name = name;
                        if (ts.getWhispering().getString().isEmpty())
                            System.err.printf("TeamSpeak:    Talking = %s\n", prev_name);
                        else
                            System.err.printf("TeamSpeak: Whispering = %s\n", prev_name);
                    }
                }
            }
        } catch (SIMPluginException e) {
            Server.logStackTrace(Level.SEVERE,e);
        }
    }
    /**/

}
