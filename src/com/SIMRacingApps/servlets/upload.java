package com.SIMRacingApps.servlets;

import java.io.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Request;

import com.SIMRacingApps.Server;
import com.SIMRacingApps.Util.FindFile;

/**
 * Implements the upload servlet to extract the uploaded zip file to the users directory.
 * The ZIP File must constructed with the paths of the files relative to the root folder of /SIMRacingApps.
 * <pre>
 * For Example:
 *    apps\MyNewApp\Black-Background-Metal-800x480.png
 *    apps\MyNewApp\default.css
 *    apps\MyNewApp\default.html
 *    apps\MyNewApp\default.js
 *    apps\MyNewApp\icon.png
 *    apps\MyNewApp\listing.properties
 *    widgets\MyNewWidget\MyNewWidget.css
 *    widgets\MyNewWidget\MyNewWidget.html
 *    widgets\MyNewWidget\MyNewWidget.js
 *    widgets\MyNewWidget\icon.png
 *    widgets\MyNewWidget\listing.properties
 * </pre>
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2024 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
@WebServlet(urlPatterns = { "upload" })
public class upload extends HttpServlet {
    private static final long serialVersionUID = 1L;
    

    /**
     * Default Constructor.
     * @see HttpServlet#HttpServlet()
     */
    public upload() {
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
    
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
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
        ServletOutputStream out = response.getOutputStream();
        
        request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
        
        final Part filePart = request.getPart("file");
        if (filePart == null) {
            out.println("No filename found in the uploaded data. Press BACK and choose a .sra File first.");
            return;
        }
        
        InputStream filecontent = null;
        String outputFolder = FindFile.getUserPath()[0]; //upload it the first directory in the userpath.
//outputFolder = "C:/temp";
        
        String filename = getFileName(filePart);
        
        response.setContentType("text/plain");
        
        if (filename.isEmpty()) {
            out.println("No filename found in the uploaded data. Press BACK and choose a .sra File first.");
            return;
        }
        
        out.println("Please close this window and restart server.\r\n\r\n");

        Server.logger().info("Uploading: " + filename);
        File name = new File("");
        try {
            filecontent = filePart.getInputStream();
            out.println("Processing: "+filename+"\r\n");
            out.flush();
            
            ZipInputStream zip = new ZipInputStream(filecontent);
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                name = new File(outputFolder + "/" + zipEntry.getName());
                String entryName = zipEntry.getName();
                
                if (entryName.startsWith("/")                        //I don't think this is possible, but check it anyway
                ||  entryName.equalsIgnoreCase("useroverrides.css")  //don't overwrite the user defined css file.
                ) {
                    out.println("Not Processed: " + entryName);
                    Server.logger().info("Not Processed: " + entryName);
                }
                else {
                    if (!zipEntry.isDirectory()) {
                        File dir  = new File(name.getParent());
                        dir.mkdirs(); //make any directories we may need before creating the file.
                        
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        String action = name.isFile() ? "Overwriting: " : "Creating   : ";
        
                        out.println(action + name.getPath());
                        Server.logger().info(action + name.getPath());
                        
                        FileOutputStream fos = new FileOutputStream(name);
                        while ((bytesRead = zip.read(buffer)) > 0) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        fos.close();
                    }
                }
            }
            zip.close();
        }
        catch (Exception e) {
            out.println("Exception while extracing: " + name.getPath() + "\r\n" + e.getMessage());
            Server.logger().severe("Exception while extracing: " + name.getPath() + "\r\n" + e.getMessage());
        }
        finally {
            if (filecontent != null)
                filecontent.close();
        }
    }
    private String getFileName(final Part part) {
        final String partHeader = part.getHeader("content-disposition");
        Server.logger().info("Part Header = " + partHeader);
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(
                        content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return "";
    }
}
