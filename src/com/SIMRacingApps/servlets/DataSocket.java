package com.SIMRacingApps.servlets;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import com.SIMRacingApps.Server;

/**
 * This class implements the "/DataSocket" interface for the HTTP Web Socket protocol.
 * You must call the "{@link com.SIMRacingApps.servlets.Data /Data}" service first using the POST method to subscript to the data to receive events on.
 * For example: when using a JavaScript client, use the WebSocket() constructor to create a connection.
 * <p>
 * Once the connection is opened, it listens for messages on the socket in the form of "sessionid". 
 * So, a client would need to send the "sessionid" as a message to the socket every time it wants more data.
 * 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2021 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
public class DataSocket extends Endpoint implements MessageHandler.Whole<String> {

    private Session m_session;
    private String m_sessionid = "";
    private RemoteEndpoint.Async m_remote;

    /**
     * OnOpen allows us to intercept the creation of a new session.
     * The session class allows us to send data to the user.
     * In the method onOpen, we'll let the user know that the handshake was 
     * successful.
     * 
     * @param session An instance to a Session.
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) 
    { 
        this.m_session = session; 
        this.m_remote = this.m_session.getAsyncRemote(); 
        this.m_session.addMessageHandler(this);
        Server.logger().info("DataSocket: Session " + session.getId() + " has opened a connection"); 
    }
 
    /**
     * The user closes the connection.
     * 
     * Note: you can't send messages to the client from this method
     * 
     * @param session An instance to a Session.
     * @param closeReason The reason the socket was closed.
     */
    @Override 
    public void onClose(Session session, CloseReason closeReason) 
    { 
        Server.logger().info("DataSocket: Session " + m_session.getId() + ", "+m_sessionid+": has ended");
        super.onClose(session,closeReason); 
        this.m_session = null; 
        this.m_sessionid = "";
        this.m_remote = null; 
    } 

    /**
     * When a user sends a message to the server, this method will intercept the message
     * and allow us to react to it. For now the message is read as a String.
     * 
     * @param params A semicolon separated list of parameters. 
     *               The first parameter is the session id.
     */
    @Override
    public void onMessage(String params) {
        String s[] = params.split("[;]");
        String sessionid=s[0];
        if (this.m_session != null && this.m_session.isOpen() && this.m_remote != null) 
        { 
            if (!sessionid.equals(m_sessionid)) {
                Server.logger().info("DataSocket: Session " + m_session.getId() + ", "+sessionid+": is requesting data");
                m_sessionid = sessionid;
            }
            this.m_remote.sendText(DataService.getJSON(m_sessionid).toString()); 
        } 
    }
}