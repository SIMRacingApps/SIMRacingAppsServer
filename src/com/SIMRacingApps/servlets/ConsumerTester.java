package com.SIMRacingApps.servlets;

import java.io.*;
import java.util.*;

import com.owlike.genson.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.SIMRacingApps.Server;

/**
 * This class implements the "/ConsumerTester" interface for the HTTP protocol.
 * It will be used to test the DataPublisher.Publish class.
 * It will take in the json string a log it, then return a fake return string.
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2021 Jeffrey Gilliam
 * @since 1.2
 * @license Apache License 2.0
 */
@WebServlet(description = "SIMRacingApps Consumer Tester", urlPatterns = { "/ConsumerTester" }, loadOnStartup=0)
public class ConsumerTester extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Genson genson;

    /**
     * Default Constructor.
     * @see HttpServlet#HttpServlet()
     */
    public ConsumerTester() {
        super();
    }

    /**
     * Initializes the servlet by getting the IP address and version information. 
     * It displays this information in the log and in the title bar of the server window.
     * The loadOnStartup option is enabled, so this gets called as soon as the server starts up.
     * <p>
     * @param config The Servlet's configuration information.
     * @throws ServletException Thrown so the server can log the error.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        Server.logger().info("init() called");
        super.init(config);
        genson = new Genson();
    }

    /**
     * Gets called when the container destroys this object
     */
    public void destroy() {
        Server.logger().info("distroy() called");
    }

    /**
     * The doGet method gets called when a HTTP GET request comes in.
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     * @param request The request information
     * @param response The response object.
     * @throws ServletException If there is a Servlet Exception
     * @throws IOException If there is an IO Exception 
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

    /**
     * The doPost method gets call when a HTTP POST message comes in.
     * 
     * TODO: Paste the issue contents from http://issues.SIMRacingApps.com/71
     * 
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     * @param request The request information
     * @param response The response object.
     * @throws ServletException If there is a Servlet Exception
     * @throws IOException If there is an IO Exception 
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        @SuppressWarnings("unchecked")
        Map<String,Object> d = genson.deserialize(request.getReader(), Map.class);

        Server.logger().info(String.format("doPost(): Input = %s",genson.serialize(d)));

        Map<String,Object> result_map = new HashMap<String,Object>();
        
        result_map.put("MaxTires", 3);
        
        String result = genson.serialize(result_map);
        
        //add these headers to try and prevent the various browsers from caching this data
        response.addHeader("Expires", "Sat, 01 Mar 2014 00:00:00 GMT");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache, must-revalidate");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(result);
        out.flush();
        
    }
}
