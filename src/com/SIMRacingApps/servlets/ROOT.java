package com.SIMRacingApps.servlets;

import java.io.*;
import java.net.URLDecoder;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.SIMRacingApps.Server;
import com.SIMRacingApps.Util.FindFile;

/**
 * Implements the ROOT servlet to redirect to /SIMRacingApps if the resource is not found
 * 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2017 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
@WebServlet(description = "ROOT, redirects to /SIMRacingApps if resource not found", urlPatterns = { "", "/favicon.png", "/favicon.ico" }, loadOnStartup=1)
public class ROOT extends HttpServlet {
    private static final long serialVersionUID = 1L;
    

    /**
     * Default Constructor.
     * @see HttpServlet#HttpServlet()
     */
    public ROOT() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        Server.logger().info("init() called");
        super.init(config);
    }

    public void distroy() throws ServletException {
        Server.logger().info("distroy() called");
    }
    
    /**
     * Redirect all GETs to /SIMRacingApps if resource is not found
     * <p>
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     * @param request The request information
     * @param response The response object.
     * @throws ServletException If there is a Servlet Exception
     * @throws IOException If there is an IO Exception 
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String resource = URLDecoder.decode(request.getRequestURI(),"UTF-8");
        if (Server.logger().getLevel().intValue() >= Level.FINEST.intValue())
            Server.logger().finest(String.format("doGet(%s) called", resource));
        
        if (resource.equals("/")) {
            if (Server.logger().getLevel().intValue() >= Level.FINEST.intValue())
                Server.logger().finest(String.format("doGet(%s) redirecting to /SIMRacingApps", resource));
            if (request.getQueryString() == null || request.getQueryString().isEmpty())
                response.sendRedirect("/SIMRacingApps");
            else
                response.sendRedirect("/SIMRacingApps?"+request.getQueryString());
            return;
        }

        response.addHeader("Access-Control-Allow-Origin", "http://simracingapps.com");

        //TODO: Have a ROOT Folder for fully qualified requests. But for now, serve up from SIMRacingApps.
        
        if (resource.startsWith("/"))
            resource = resource.substring(1);
        
        String filename = "";
        FindFile file = null;
        try {
            file = new FindFile(filename = resource + "/default.html");
        }
        catch (FileNotFoundException e) {
            if (!resource.isEmpty()) {
                try {
                    file = new FindFile(filename = resource);
                }
                catch (FileNotFoundException e2) {
                }
            }
        }
        
        if (file != null) {
            
            if (Server.logger().getLevel().intValue() >= Level.FINER.intValue())
                Server.logger().finer(String.format("doGet(%s) loading resource", filename));
            
            //IE 10 or 11 would not retrieve the css files without the mime type being set
            ServletContext context = getServletContext();
            if (context != null) {
                String mimeType = context.getMimeType(filename);
                if (Server.logger().getLevel().intValue() >= Level.FINER.intValue())
                    Server.logger().finer(String.format("%s.doGet(%s) content mime-type = %s", this.getClass().getName(),filename,mimeType));
                response.setContentType(mimeType);
            }

            ServletOutputStream out = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            try {
                while ((bytesRead = file.getInputStream().read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                file.close();
            }
            catch (NullPointerException e) {} //this will occur when reading a directory in a jar file. Just ignore for now.
            
            out.flush();
            return;
        }
        
        if (Server.logger().getLevel().intValue() >= Level.FINER.intValue())
            Server.logger().finer(String.format("doGet(%s) resource not found", resource));
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}
