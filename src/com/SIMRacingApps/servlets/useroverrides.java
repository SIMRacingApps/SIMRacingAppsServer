package com.SIMRacingApps.servlets;

import java.io.*;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.SIMRacingApps.Server;
import com.SIMRacingApps.Util.FindFile;

/**
 * Implements the save useroverrides servlet.
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2020 Jeffrey Gilliam
 * @since 1.6
 * @license Apache License 2.0
 */
@WebServlet(urlPatterns = { "useroverrides" })
public class useroverrides extends HttpServlet {
    private static final long serialVersionUID = 1L;
    

    /**
     * Default Constructor.
     * @see HttpServlet#HttpServlet()
     */
    public useroverrides() {
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
        Server.logger().finest(String.format("doGet() called"));
        
        FindFile file = null;
        try {
            file = new FindFile("useroverrides.css");
            
            Server.logger().finer(String.format("doGet() loading %s",file.getFileFound()));
            
            ServletOutputStream out = response.getOutputStream();
            response.setContentType("text/plain");

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
        catch (FileNotFoundException e) {
        }
        
        Server.logger().finer(String.format("doGet() %s not found", "useroverrides.css"));
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
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

        File dest = null;
        try {
            FindFile file = new FindFile("useroverrides.css");
            dest   = file.getFile();
        }
        catch (FileNotFoundException e) {
            String outputFolder = FindFile.getUserPath()[0]; //upload it the first directory in the userpath.
            dest = new File(outputFolder + "\\useroverrides.css");
        }
//outputFolder = "C:/temp";
        
        Server.logger().info("Saving useroverrides to: " + dest.toString());
        ServletOutputStream out = response.getOutputStream();
        
        try {
            int bytesWritten = 0;

            FileOutputStream fos = null;
            BufferedOutputStream out1 = null;
            try {
                byte[] data = request.getParameter("useroverrides").getBytes();
                fos = new FileOutputStream(dest);
                out1 = new BufferedOutputStream(fos);
                out1.write(data);
                bytesWritten = data.length;
                out1.close();
                Server.logger().info(String.format("%d bytes written",bytesWritten));
            }
            catch (FileNotFoundException e) {
                Server.logStackTrace(Level.WARNING, "Cannot open output file: "+ dest.toString(), e);
            } catch (IOException e) {
                Server.logStackTrace(Level.WARNING, "IOExcetion while saving "+ dest.toString(), e);
            }
            finally {
                try {
                    if (out1 != null) out1.close();
                    if (fos != null) fos.close();
                } catch (IOException e) {}
            }
        }
        catch (Exception e) {
            out.println("Exception while saving: " + dest.getPath() + "\r\n" + e.getMessage());
            Server.logger().severe("Exception saving: " + dest.getPath() + "\r\n" + e.getMessage());
        }
        finally {
        }
        
        out.println(
              "<html><body>"
            + dest.toString() 
            + " saved.<br />"
            + "You must reload the affected App/Widget for these to take effect.<br />"
            + "For Electron, make the App/Widget active, the press Ctrl-R.<br />"
            + "All other clients/browers refer to their help on how to refresh them.<br />"
            + "You can close this window.<br />"
            + "You may have to right click the header, then select close"
            + "</body></html>"
        );
        out.flush();

    }
}
