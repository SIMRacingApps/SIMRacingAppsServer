package com.SIMRacingApps.Util;

import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.SIMRacingApps.Server;
import com.owlike.genson.Genson;
import com.owlike.genson.stream.JsonStreamException;
import com.sun.jna.platform.win32.Advapi32Util;

/**
 * This class has helper methods for finding and opening files in the user's path or classpath.
 * 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2017 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
public class FindFile {

    private String m_pathname = "";
    private String m_pathnameFound = "";
    private InputStream m_is = null;
    private BufferedInputStream m_bis = null;
    private File m_file = null;
    private URL m_url = null;
    
    @SuppressWarnings("unused")
    private FindFile() {
    }

    /**
     * Constructor for finding a file.
     * It looks in the current directory, then in each directory from the user's path, then the directories in the classpath.
     * If found, it opens an InputStream to the file, so don't forget to close it.
     * 
     * @param pathname The path to the file found.
     * @throws FileNotFoundException When the file cannot be found.
     */
    public FindFile(String pathname) throws FileNotFoundException {

//for the purposes of SRA, I do not want it looking in the current folder first.
//all my files will either be in documents\SIMRacingApps or the user path passed in.
//had an issue with there begin a settings.txt file in the downloads with the exe and it was not using the 
//one in documents
//        try {
//            m_file = new File(pathname);
//            Server.logger().info("FindFile searching in: " + m_file.getAbsolutePath());
//            m_is = new FileInputStream(m_pathnameFound = m_pathname = m_file.getAbsolutePath());
//            m_url = new URL("file:///"+m_pathnameFound);
//            if (m_url != null)
//                Server.logger().finest("FindFile URL found: "+m_url.toExternalForm());
//        }
//        catch (FileNotFoundException | MalformedURLException e) {

            // if the filename is absolute, just open it and return the error
            m_file = new File(pathname);
            //absolute doesn't work on windows when the drive letter is not used.
            if (m_file.isAbsolute() || pathname.startsWith("/") || pathname.startsWith("\\")) {
                Server.logger().finest("FindFile checking absoulte path: " + m_file.toString());
                if (m_file.exists()) {
                    try {
                        m_is = new FileInputStream(m_file);
                        m_pathnameFound = m_file.toString();
                        m_url = new URL("file:///"+m_pathnameFound);
                        m_bis = new BufferedInputStream(m_is);
                        if (m_url != null)
                            Server.logger().finest("FindFile URL found: "+m_url.toExternalForm());
                        return;
                    }
                    catch (FileNotFoundException e1) {} 
                    catch (MalformedURLException e2) {
                        Server.logStackTrace(e2);
                    }
                }
                throw new FileNotFoundException(pathname);
            }
        
            //first look in the user's path
            for (int pathIndex=0; pathIndex < getUserPath().length; pathIndex++) {
                m_file = new File(getUserPath()[pathIndex] + "/" + pathname);
                Server.logger().finest("FindFile searching in user path: " + m_file.toString());
                
                try {
                    m_is = new FileInputStream(m_file);
                    m_pathnameFound = m_file.toString();
                    m_url = new URL("file:///"+m_pathnameFound);
                    m_bis = new BufferedInputStream(m_is);
                    if (m_url != null)
                        Server.logger().finest("FindFile URL found: "+m_url.toExternalForm());
                    return;
                }
                catch (FileNotFoundException e1) {} 
                catch (MalformedURLException e2) {
                    Server.logStackTrace(e2);
                }
            }
            
            //now look down the classpath
            m_file = new File(m_pathnameFound = pathname);
            Server.logger().finest("FindFile searching in classpath: " + m_pathnameFound);
            m_is = com.SIMRacingApps.Util.FindFile.class.getClassLoader().getResourceAsStream(m_pathnameFound);
            if (m_is == null) {
                m_pathnameFound = "";
                Server.logger().finest("FindFile not found in classpath: " + m_pathnameFound);
                throw new FileNotFoundException(pathname);
            }
            //for getResource to work, the path needs to start with a slash
            m_url = com.SIMRacingApps.Util.FindFile.class.getClass().getResource("/"+m_pathnameFound);
            if (m_url != null)
                Server.logger().finest("FindFile URL found: "+m_url.toExternalForm());
            else {
                try {
                    Server.logger().finest("FindFile URL found: file:///"+m_pathnameFound);
                    m_url = new URL("file:///"+m_pathnameFound);
                    //wish this would work
                    //m_url = new URL("rsrc:"+m_pathnameFound);
                } catch (MalformedURLException e) {
                    Server.logStackTrace(e);
                }
            }
//        }
        m_bis = new BufferedInputStream(m_is);
    }

    /**
     * Provided an array of names, this returns the first one found.
     * 
     * @param pathnames An array of files to find.
     * @return The file found.
     * @throws FileNotFoundException When none of the files are found.
     */
    public static FindFile find(ArrayList<String> pathnames) throws FileNotFoundException {
        for (int i=0; i < pathnames.size(); i++) {
            try {
                FindFile file = new FindFile(pathnames.get(i));
                return file;
            }
            catch (FileNotFoundException e) {}
        }
        throw new FileNotFoundException(pathnames.size() > 0 ? pathnames.get(0) : "");
    }
    
    /**
     * Returns the java.io.File object for the file that was found.
     * It may not be able to do certain things if the file is found in a jar.
     * @return The File object.
     */
    public File getFile() {
        return m_file;
    }
    
    private static String m_documents = null;
    
    /**
     * Returns the path to the users Documents location.
     * @return The path to the user's documents location.
     */
    public static String getUserDocumentsPath() {
        if (m_documents == null) {
            m_documents = Advapi32Util.registryGetStringValue(HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "Personal");
        }
        return m_documents;
    }
    
//    private static String[] m_userPath = (System.getProperty("user.home")+File.separator+"SIMRacingApps").split("[;]");
    private static String[] m_userPath = null;

    /**
     * Returns an array of the user's paths.
     * If the user has not set their own path, then the users "My Documents" location will be used to create a SIMRacingApps folder.
     * 
     * @return An array of the user's paths.
     */
    public static String[] getUserPath() {
        if (m_userPath == null) {
            String s = getUserDocumentsPath();
            if (s != null && !s.isEmpty()) {
                setUserPath(s + "\\SIMRacingApps");
            }
        }
        return m_userPath;
    }
    
    /**
     * Sets the user's paths by parsing the "path" argument for semicolon as a separator.
     * If the path is null or empty, it will not change the existing path.
     * 
     * If the path is relative, then it will be created in the user's documents folder.
     * 
     * @param path A semicolon separated list of directories to use as the user's path.
     */
    public static void setUserPath(String path) {
        if (path != null && !path.isEmpty()) {
            String[] paths = path.split("[;]");
            for (int i=0; i < paths.length; i++) {
                File file = new File(paths[i]);
                if (!file.isAbsolute())
                    paths[i] = FindFile.getUserDocumentsPath() + "/" + paths[i];
            }
            m_userPath = paths;
        
            try {
                //now add all these folders to the classpath
                ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
    
                URL [] urls = new URL[m_userPath.length];
                for (int i=0; i < m_userPath.length; i++) {
                    new File(m_userPath[i]).mkdirs();  //if these folders do not exist, then create them
                    urls[i] = new URL("file://"+m_userPath[i]);
                }
                
                ClassLoader urlCl = URLClassLoader.newInstance(urls, prevCl);
                Thread.currentThread().setContextClassLoader(urlCl);
            } catch (MalformedURLException e) {
                Server.logStackTrace(Level.SEVERE, "Adding to classpath", e);
            }
        }
    }
    
    /**
     * Returns the name of the file that was found including the path to where it was found.
     * If found in the classpath, the original name is returned.
     * 
     * @return Then name of the file found.
     */
    public String getFileFound() {
        return m_pathnameFound;
    }
    
    /**
     * Returns the InputStream of the file.
     * 
     * @return The InputStream or null of not found
     */
    public InputStream getInputStream() {
        return m_is;
    }

    /**
     * Returns the BufferedInputStream of the file.
     * 
     * @return The BufferedInputStream or null of not found
     */
    public InputStream getBufferedInputStream() {
        return m_bis;
    }
    
    /**
     * Returns a URL to the found file.
     * @return The URL to the file.
     */
    public URL getURL() {
        return m_url;
    }

    /**
     * Copies the src file to the dest file.
     * @param src The source FindFile instance
     * @param dest The dest filename
     */
    public static void copy(FindFile src, File dest) {
        copy(src.getInputStream(),src.getFile(),dest);
    }
    
    public static void copy(InputStream is,File src,File dest) {
        byte[] buffer = new byte[1024];
        int bytesRead;

        FileOutputStream fos = null;
        BufferedOutputStream out = null;
        try {
            fos = new FileOutputStream(dest);
            out = new BufferedOutputStream(fos);
            while ((bytesRead = is.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        catch (FileNotFoundException e) {
            Server.logStackTrace(Level.WARNING, "Cannot open output file: "+ dest.toString(), e);
        } catch (IOException e) {
            Server.logStackTrace(Level.WARNING, "IOExcetion while copying "+ src.toString() + " to " + dest.toString(), e);
        }
        finally {
            try {
                if (out != null) out.close();
                if (fos != null) fos.close();
            } catch (IOException e) {}
        }
    }
    
    /**
     * Closes the InputStream.
     */
    public void close() {
        if (m_bis != null)
            try {
                m_bis.close();
            } catch (IOException e) {
                Server.logStackTrace(e);
            }
        m_bis = null;
        if (m_is != null)
            try {
                m_is.close();
            } catch (IOException e) {
                Server.logStackTrace(e);
            }
        m_is = null;
    }
    
    @Override
    public void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private static Genson m_genson = new Genson();
    private Map<String,Object> m_map = null;
    
    /**
     * Returns the contents of the file parsed as a JSON file as a Map&lt;String,Object&gt; type.
     * To detect a file not found, the map will be empty.
     * @return The Map&lt;String,Object&gt; instance of the parsed JSON file.
     */
    @SuppressWarnings("unchecked")
    public Map<String,Object> getJSON() {
        if (m_map == null && m_is != null) {
            InputStreamReader in = new InputStreamReader( m_is );
            try {
                m_map = m_genson.deserialize(in, Map.class);
                in.close();
                close();
            } catch (IOException e) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
                Server.logStackTrace(Level.SEVERE,"IOException",e);
                m_map = new HashMap<String,Object>();
            } catch (JsonStreamException e) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
                Server.logStackTrace(Level.SEVERE,"JsonStreamException",e);
                m_map = new HashMap<String,Object>();
            }
        }
        return m_map;
    }
    
    @Override
    public String toString() {
        return m_pathnameFound;
    }
}
