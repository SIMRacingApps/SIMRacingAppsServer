package com.SIMRacingApps.servlets;

import java.io.*;
import java.net.URLDecoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.SIMRacingApps.Server;
import com.SIMRacingApps.Util.SendKeys;

/**
 * The sendkeys servlet parses the keys to send from the URL and passes them to the {@link com.SIMRacingApps.Util.SendKeys#sendKeys(String)} method.
 * You can also pass the keys in a parameter called "keys" as well as pass them in the body of a POST.
 * Remember that data stuffed in the URL has to be encoded.
 * The servlet, returns the keys that were sent as a plain/text stream.
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2023 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
@WebServlet(description = "Sends Key Strokes to active process", urlPatterns = { "/sendkeys/*" })
public class sendkeys extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Default Constructor.
     * @see HttpServlet#HttpServlet()
     */
    public sendkeys() {
        super();
    }

    /**
     * Initializes the servlet. 
     * <p>
     * @param config The Servlet's configuration information.
     * @throws ServletException Thrown so the server can log the error.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        Server.logger().info("init() called");
        super.init(config);
    }

    /**
     * Gets called when the container destroys this object
     */
    public void destroy() {
        Server.logger().info("distroy() called");
        DataService.stop();
    }

    /**
     * The doGet method gets called when a HTTP GET request comes in.
     * <p>
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     * @param request The request information
     * @param response The response object.
     * @throws ServletException If there is a Servlet Exception
     * @throws IOException If there is an IO Exception 
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String data = request.getParameter("keys");
        String path = data != null ? ("/"+data) : request.getPathInfo();

        //see if the user passed the parameters as REST and make the call to get the data now
        if (path != null) {
            response.getOutputStream().println(SendKeys.sendKeys(URLDecoder.decode(path, "UTF-8").substring(1)));
        }
        else
        if (data != null) {
            response.getOutputStream().println(SendKeys.sendKeys(URLDecoder.decode(data, "UTF-8").substring(1)));
        }
    }

    /**
     * The doPost method gets call when a HTTP POST message comes in.
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     * @param request The request information
     * @param response The response object.
     * @throws ServletException If there is a Servlet Exception
     * @throws IOException If there is an IO Exception 
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String data = request.getParameter("keys");
        String path = data != null ? ("/"+data) : request.getPathInfo();

        //see if the user passed the parameters as REST and make the call to get the data now
        if (path != null) {
            response.getOutputStream().println(SendKeys.sendKeys(URLDecoder.decode(path, "UTF-8").substring(1)));
        }
        else
        if (data != null) {
            response.getOutputStream().println(SendKeys.sendKeys(URLDecoder.decode(data, "UTF-8").substring(1)));
        }
        else {
            BufferedReader br = request.getReader();
            char[] cbuf = new char[1024];
            if (br.read(cbuf) > 0) {
                response.getOutputStream().println(SendKeys.sendKeys(cbuf.toString()));
            }
        }
    }
}
