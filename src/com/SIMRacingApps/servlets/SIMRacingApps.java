package com.SIMRacingApps.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
 * This class is responsible for serving all the content.
 * It first looks in the users home folder to see if they have overridden it.
 * Then it looks for the resource specified in the URL using the classpath, opens it and returns the contents.
 * <p>
 * Example(s):
 * <p>
 * http://localhost/SIMRacingApps/com/SIMRacingApps/Tracks/daytona_oval.json
 * <p>
 * http://localhost/SIMRacingApps/com/SIMRacingApps/Tracks/daytona.png
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2017 Jeffrey Gilliam
 * @since 1.1
 * @license Apache License 2.0
 */
@WebServlet(description = "Returns the requested resource doGet()", urlPatterns = { "/SIMRacingApps", "/SIMRacingApps/*" }, loadOnStartup=1)
public class SIMRacingApps extends HttpServlet {

    private static final long serialVersionUID = -5933328492604509566L;
    

    public SIMRacingApps() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        Server.logger().info("init() called");
        super.init(config);
//If I ever want to drop this into a container, I may need to add this to the list of directories to search
//        String realpath = getServletContext().getRealPath("");
    }

    public void distroy() throws ServletException {
        Server.logger().info("distroy() called");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String resource = URLDecoder.decode(request.getRequestURI(),"UTF-8");
        if (Server.isLogLevelFinest())
            Server.logger().finest(String.format("doGet(%s) called", resource));
        
        response.addHeader("Access-Control-Allow-Origin", "http://simracingapps.com");

        //for some apps still using Resource, just replace it. Everything is now relative to /SIMRacingApps, even resource in the jar.
        resource = resource.replace("//", "/").replace("/SIMRacingApps/Resource/", "");
        if (resource.startsWith("/SIMRacingApps"))
            resource = resource.substring(14);
        
        if (resource.startsWith("/"))
            resource = resource.substring(1);
        
        if (resource.endsWith("/"))
            resource = resource.substring(0,resource.length()-1);
        
        if (resource.isEmpty())
            resource = "default.html";
        
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
            
            if (Server.isLogLevelFiner())
                Server.logger().finer(String.format("doGet(%s) loading resource", filename));
            
            //IE 10 or 11 would not retrieve the css files without the mime type being set
            ServletContext context = getServletContext();
            if (context != null) {
                String mimeType = context.getMimeType(filename);
                if (Server.isLogLevelFiner())
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
        
        if (Server.isLogLevelFiner())
            Server.logger().finer(String.format("doGet(%s) resource not found", resource));
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getOutputStream().println(resource + " not found");
    }
}
