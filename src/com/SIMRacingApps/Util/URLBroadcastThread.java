/**
 * 
 */
package com.SIMRacingApps.Util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.SIMRacingApps.Server;

/**
 * This class broadcasts the server URL to the default port of 28888 every 2 seconds.
 * 
 * Here is an example client program for Java.
 * <pre>
 *  import java.net.DatagramPacket;
 *  import java.net.DatagramSocket;
 *  import java.net.InetAddress;
 *  public class SIMRacingAppsURLClientTest {
 *  
 *      public SIMRacingAppsURLClientTest() {
 *      }
 *  
 *      public static void main(String[] args) {
 *          DatagramSocket socket = null;
 *          try {
 *              //open a socket on the port SRA is broadcasting
 *              socket = new DatagramSocket(28888, InetAddress.getByName("0.0.0.0"));
 *              byte[] recvBuf = new byte[256];
 *              DatagramPacket receivePacket = new DatagramPacket(recvBuf,recvBuf.length);
 *              
 *              while (true) {
 *                  //listen for the packet. SRA is broadcast every 2 seconds
 *                  socket.receive(receivePacket);
 *                  String message = new String(receivePacket.getData()).trim();
 *                  System.out.println(message);
 *                  //make sure the message is from SRA
 *                  if (message.startsWith("SRA=")) {
 *                      String URL = message.substring(4);
 *                      System.out.println("URL = "+URL);
 *                      
 *                      //...take the URL and do something with it.
 *                  }
 *              }
 *          } catch (Exception e) {
 *              e.printStackTrace();
 *          }
 *          finally {
 *              if (socket != null)
 *                  socket.close();
 *          }
 *      }
 *  }
 * </pre>
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2023 Jeffrey Gilliam
 * @since 1.2
 * @license Apache License 2.0
 */
public class URLBroadcastThread {

    private static volatile Thread s_dt = null;
    private static volatile boolean m_stayalive = true;
    private URLBroadcastThread() {
    }

    public static void stop() {
        if (s_dt != null) {
            m_stayalive = false;

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }
    
    public static void start(String URL) {
        if (s_dt == null) {
            s_dt = new Thread( new Runnable() {
                public void run() {
                    Server.logger().info("URL Broadcast Thread is running...");
                    DatagramSocket socket = null;
                    try {
                        int port = Server.getArg("URLBroadcastPort", 28888);
                        socket = new DatagramSocket();
                        socket.setBroadcast(true);
            
                        byte[] sendData = ("SRA=" + URL).getBytes();
                        Server.logger().finest("Broadcasting URL: " + URL + " on port " + port);
                        while (m_stayalive) {
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName("255.255.255.255"),port);
                            socket.send(sendPacket);
            
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                            }
                        }
                    } catch (SocketException e) {
                        Server.logStackTrace(e);
                    } catch (UnknownHostException e) {
                        Server.logStackTrace(e);
                    } catch (IOException e) {
                        Server.logStackTrace(e);
                    }
                    finally {
                        if (socket != null)
                            socket.close();
                    }
                    Server.logger().severe("URL Broadcast Thread Terminated");
                }
            });
            s_dt.setName("URLBroadcastThread");
            s_dt.start();
        }
    }
}
