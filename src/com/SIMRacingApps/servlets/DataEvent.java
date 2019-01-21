package com.SIMRacingApps.servlets;

import java.io.IOException;
import java.io.PrintWriter;
 

import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.SIMRacingApps.Server;

/**
 * This class implements the "/DataEvent" interface for the HTTP Web Event protocol.
 * You must call the "{@link com.SIMRacingApps.servlets.Data /Data}" service first using the POST method to subscript to the data to receive events on.
 * Then you can call this service to start receiving the events.
 * For example: when using a JavaScript client, use the EventSource() constructor to receive the events.
 * <p>
 * You must include the same "sessionid" query string parameter in the URL that was used when POST was called.
 * Also, you can pass an "interval" parameter, which defaults to 100 milliseconds.
 * 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2019 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
@WebServlet(description = "EventSource access to SIMRacingApps", urlPatterns = { "/DataEvent" }, loadOnStartup=0)
public class DataEvent extends HttpServlet {

    private static final long serialVersionUID = 7171715990357797121L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionid = request.getParameter("sessionid");
        String s = request.getParameter("interval");
        long interval = s.length() > 1 ? Long.parseLong(s): 100L; 
        
        Server.logger().info("DataEvent: Message from SessionId = " + sessionid + ", Interval = " + interval);
        Server.logger().info(String.format("%s is running with interval %d...\n", sessionid,interval));
        
        try {
            //content type must be set to text/event-stream
            response.setContentType("text/event-stream;charset=UTF-8");   
         
            //encoding must be set to UTF-8
            response.setCharacterEncoding("UTF-8");
            
            response.addHeader("Cache-Control", "no-cache, must-revalidate");
            response.addHeader("Connection", "keep-alive");
         
            PrintWriter writer = response.getWriter();
            
            //TODO: EventSource: need a way to detect client disconnects and exit the loop
            while (true) {
                //can't have any line feeds in the data or confuses the parser
                writer.write("data: " + DataService.getJSON(sessionid).toString().replace("\n", "") + "\n\n");
                writer.flush();
                
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                }
            }
        } catch (Exception e1) {
            Server.logStackTrace(Level.WARNING, "while processing Web Events",e1);
        }
        Server.logger().info(String.format("%s is exiting.", sessionid));
    }
 }