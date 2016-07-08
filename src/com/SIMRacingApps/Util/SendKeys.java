/**
 * 
 */
package com.SIMRacingApps.Util;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.SIMRacingApps.Server;

/**
 * This class emulates the syntax of the Visual Basic SendKeys class found at
 * <a href="https://msdn.microsoft.com/en-us/library/aa248599%28v=vs.60%29.aspx">SendKeys</a>
 * <p>
 * Only the key names found at 
 * <a href="http://docs.oracle.com/javase/8/docs/api/java/awt/event/KeyEvent.html">KeyEvent</a> 
 * by using what comes after the "VK_". The following VB names have been crossed referenced to the VK_ names as well.
 * <pre>
 *  BACKSPACE   BACK_SPACE
 *  BKSP        BACK_SPACE
 *  BS          BACK_SPACE
 *  CAPSLOCK    CAPS_LOCK
 *  CTRL        CONTROL
 *  DEL         DELETE
 *  INS         INSERT
 *  PGDN        PAGE_DOWN
 *  PGUP        PAGE_UP
 *  WINKEY      WINDOWS
 *  
 * Example: 
 *   1. Set the default delay.
 *   2. Run Notepad, wait for it to run.
 *   3. Enter some text.
 *   4. Open the about dialog.
 * 
 *    {DELAY=50}{WINDOWS}rnotepad{ENTER}{DELAY 1000}hello world{ALT}ha
 * </pre> 
 * @author Jeffrey Gilliam
 * @copyright Copyright (C) 2015 - 2016 Jeffrey Gilliam
 * @since 1.0
 * @license Apache License 2.0
 */
public class SendKeys {

    public enum Driver {
        AWT, WINDOWS
    };
    
    private static Driver m_driver = Driver.WINDOWS;
    private static Robot m_bot = null;
    private static String m_windowName = "";
    private static Map<String,String> m_keymap = new HashMap<String,String>();
    
    private static synchronized void _init() {
        if (m_bot == null)
            m_delay = Integer.parseInt(Server.getArg("sendkeysdelay","6"));
        
            try {
                if (m_driver == Driver.WINDOWS)
                    m_bot = new com.SIMRacingApps.Windows.Robot(m_windowName);
                else
                    m_bot = new Robot();
                
                //These aliases are from Visual Basic's SendKeys that are not in KeyEvent
                //https://msdn.microsoft.com/en-us/library/aa248599%28v=vs.60%29.aspx
                m_keymap.put("BACKSPACE",   "{BACK_SPACE}");
                m_keymap.put("BKSP",        "{BACK_SPACE}");
                m_keymap.put("BS",          "{BACK_SPACE}");
                m_keymap.put("CAPSLOCK",    "{CAPS_LOCK}");
                m_keymap.put("CTRL",        "{CONTROL}");
                m_keymap.put("DEL",         "{DELETE}");
                m_keymap.put("INS",         "{INSERT}");
                m_keymap.put("PGDN",        "{PAGE_DOWN}");
                m_keymap.put("PGUP",        "{PAGE_UP}");
                m_keymap.put("WINKEY",      "{WINDOWS}");
                
                //remap !#$&*()_+|<>?:" that require a SHIFT to their words so it can find them in the KeyEvent class
                m_keymap.put("~",           "{TILDE}");               m_keymap.put("TILDE",             "{SHIFT}{BACK_QUOTE}");
                m_keymap.put("!",           "{EXCLAMATION_MARK}");    m_keymap.put("EXCLAMATION_MARK",  "{SHIFT}1");
                m_keymap.put("@",           "{AT}");                  m_keymap.put("AT",                "{SHIFT}2");
                m_keymap.put("#",           "{NUMBER_SIGN}");         m_keymap.put("NUMBER_SIGN",       "{SHIFT}3");
                m_keymap.put("$",           "{DOLLAR}");              m_keymap.put("DOLLAR",            "{SHIFT}4");
                m_keymap.put("%",           "{PERCENT}");             m_keymap.put("PERCENT",           "{SHIFT}5");
                m_keymap.put("^",           "{CARAT}");               m_keymap.put("CARAT",             "{SHIFT}6");
                m_keymap.put("&",           "{APERSAND}");            m_keymap.put("APERSAND",          "{SHIFT}7");
                m_keymap.put("*",           "{ASTERISK}");            m_keymap.put("ASTERISK",          "{SHIFT}8");
                m_keymap.put("(",           "{LEFT_PARENTHESIS}");    m_keymap.put("LEFT_PARENTHESIS",  "{SHIFT}9");
                m_keymap.put(")",           "{RIGHT_PARENTHESIS}");   m_keymap.put("RIGHT_PARENTHESIS", "{SHIFT}0");
                m_keymap.put("_",           "{UNDERSCORE}");          m_keymap.put("UNDERSCORE",        "{SHIFT}{MINUS}");
                m_keymap.put("+",           "{PLUS}");                m_keymap.put("PLUS",              "{SHIFT}{EQUALS}");
                m_keymap.put("|",           "{VIRTICAL_BAR}");        m_keymap.put("VIRTICAL_BAR",      "{SHIFT}{BACK_SLASH}");
                m_keymap.put("<",           "{LESS}");                m_keymap.put("LESS",              "{SHIFT}{COMMA}");
                m_keymap.put(">",           "{GREATER}");             m_keymap.put("GREATER",           "{SHIFT}{PERIOD}");
                m_keymap.put("?",           "{QUESTION}");            m_keymap.put("QUESTION",          "{SHIFT}{SLASH}");
                m_keymap.put(":",           "{COLON}");               m_keymap.put("COLON",             "{SHIFT}{SEMICOLON}");
                m_keymap.put("\"",          "{QUOTEDBL}");            m_keymap.put("QUOTEDBL",          "{SHIFT}{QUOTE}");
                
                //remap `-=[]\;',./ to their words
                m_keymap.put("\t",          "{TAB}");
                m_keymap.put("`",           "{BACK_QUOTE}");
                m_keymap.put("-",           "{MINUS}");
                m_keymap.put("=",           "{EQUALS}");
                m_keymap.put("[",           "{OPEN_BRACKET}");
                m_keymap.put("]",           "{CLOSE_BRACKET}");
                m_keymap.put("\\",          "{BACK_SLASH}");
                m_keymap.put(";",           "{SEMICOLON}");
                m_keymap.put("'",           "{QUOTE}");
                m_keymap.put(",",           "{COMMA}");
                m_keymap.put(".",           "{PERIOD}");
                m_keymap.put("/",           "{SLASH}");
                
                
            } 
            catch (AWTException e1) {
                Server.logStackTrace(e1);
            }
            finally {}
    }
    
    private SendKeys() {
        // TODO Auto-generated constructor stub
    }
    
    private static int m_delay = 6;

    /**
     * Returns the current default delay in milliseconds.
     * @return The current delay in milliseconds
     */
    public static int getDelay() { _init(); return m_delay; }
    
    /**
     * Sets the default delay.
     * 
     * This can also be specified with the command line argument "sendkeysdelay".
     * 
     * @param ms The number of milliseconds.
     */
    public static void setDelay(int ms) {
        _init();
        m_delay = ms;
    }
    
    /**
     * Sets the driver that sendkeys should use.
     * The choices are enumerated in the Driver enum (AWT,WINDOWS).
     * @param driver The type of driver to use, AWT or WINDOWS
     */
    public static void setDriver(Driver driver) {
        m_driver = driver;
    }
    
    /**
     * Delays for the specified number of milliseconds.
     *
     * @param ms The number of milliseconds.
     */
    public static void delay(int ms) {
        _init();
        m_bot.delay(ms);        
    }
    
    public static void setWindowName(String name) {
        m_windowName = name;
    }
    
    /**
     * This method parses the string and sends the keys.
     * @param keystrokes The keystrokes to send.
     * @return The keys sent.
     */
    public static synchronized String sendKeys(String keystrokes) {
        Server.logger().fine("Server.sendkeys("+keystrokes+")");
        StringBuffer sent = new StringBuffer();
        _init();
        
        int delay = m_delay;

        int delayOnce = delay;
        ArrayList<Integer> releaseOnEndOfGroup = new ArrayList<Integer>();
        ArrayList<Integer> releaseAfterNext    = new ArrayList<Integer>();
        
        for(int i = 0; i < keystrokes.length(); i++) {
            String keyCode = Character.toString(keystrokes.charAt(i));
            String codeSent = keyCode;
            
            if (keyCode.equals("{")) {
                int end = keystrokes.indexOf("}", i+1);
                if (end < 0) {
                    keyCode = keystrokes.substring(i+1).toUpperCase();
                    i = keystrokes.length() - 1;
                }
                else {
                    keyCode = keystrokes.substring(i+1, end).toUpperCase();
                    i = end; 
                }
                codeSent = "{" + keyCode + "}";
            }
            else {
                
                //These are shortcuts from Visual Basics version.
//                if (keyCode.equals("~")) keyCode = "ENTER";
//                if (keyCode.equals("+")) keyCode = "SHIFT";
//                if (keyCode.equals("^")) keyCode = "CONTROL";
//                if (keyCode.equals("%")) keyCode = "ALT";
//                if (keyCode.equals("@")) keyCode = "WINDOWS";
            }
            
            if (keyCode.startsWith("DELAY")) {
                String[] s = keyCode.split("[= ]");
                if (s.length == 2) {
                    try {
                        if (keyCode.startsWith("DELAY=")) {
                            delay = delayOnce = Integer.parseInt(s[1]);
                        }
                        else {
                            delayOnce = Integer.parseInt(s[1]);
                        }
                        sent.append(codeSent);
                    } catch (NumberFormatException e) {
                        Server.logStackTrace(e);
                    }
                }
            }
            else
            if (keyCode.startsWith("SLEEP")) {
                String[] s = keyCode.split("[= ]");
                if (s.length == 2) {
                    try {
                        m_bot.delay(Integer.parseInt(s[1]));
                        sent.append(codeSent);
                    } catch (NumberFormatException e) {
                        Server.logStackTrace(e);
                    }
                }
            }
            else {
                Field f;
                int keyEvent = KeyEvent.VK_UNDEFINED;
                try {
                    if (m_keymap.containsKey(keyCode)) {
                        //send it back through send keys with the same delay
                        sent.append(sendKeys(String.format("{DELAY=%d}{DELAY %d}%s",delay,delayOnce,m_keymap.get(keyCode))));
                    }
                    else {
                        String code = "VK_" + keyCode.toUpperCase();
                        
                        //if end of a sequence group, then release the key
                        if (releaseOnEndOfGroup.size() > 0 && keyCode.equals(")")) {
                            
                            keyEvent = releaseOnEndOfGroup.get(releaseOnEndOfGroup.size()-1);
                            releaseOnEndOfGroup.remove(releaseOnEndOfGroup.size()-1);
                            
                            if (keyEvent != KeyEvent.VK_UNDEFINED) {
                                m_bot.delay(m_delay);
                                m_bot.keyRelease(keyEvent);
                            }
                        }
                        else {
                            
                            //see if the code can be found in the KeyEvent class
                            try {
                                f = KeyEvent.class.getField(code);
                                keyEvent = f.getInt(null);
                            } 
                            catch (NoSuchFieldException e) {
                                if (keyCode.length() == 1) {
                                    keyEvent = KeyEvent.getExtendedKeyCodeForChar(keyCode.charAt(0));
                                }
                            }
                            
                            //if we found it, press it
                            if (keyEvent != KeyEvent.VK_UNDEFINED) {
                                m_bot.delay(delayOnce);
                                delayOnce = delay;
                                
                                //the KeyEvent class only defines the lowercase codes, so we need to shift for upper case
                                if (keyCode.matches("[A-Z]")) {
                                    m_bot.keyPress(KeyEvent.VK_SHIFT);
                                    m_bot.delay(m_delay);
                                }
                                
                                m_bot.keyPress(keyEvent);
                                
                                if (keyCode.matches("[A-Z]")) {
                                    m_bot.delay(m_delay);
                                    m_bot.keyRelease(KeyEvent.VK_SHIFT);
                                }
                                sent.append(codeSent);
                            }
                            else {
                                Server.logger().warning("keyEvent("+keyCode+") not found");
                            }
                            
                            if (releaseAfterNext.size() > 0) {
                                
                                int keyEvent2 = releaseAfterNext.get(releaseAfterNext.size()-1);
                                releaseAfterNext.remove(releaseAfterNext.size()-1);
                                
                                if (keyEvent2 != KeyEvent.VK_UNDEFINED) {
                                    m_bot.delay(m_delay);
                                    m_bot.keyRelease(keyEvent2);
                                }
                            }
    
                            //if the next character is an open paren, then don't release it now
                            //put it in the queue to be released when the closed paren is seen
                            if ((i+1) < keystrokes.length() && Character.toString(keystrokes.charAt(i+1)).equals("(")) {
                                releaseOnEndOfGroup.add(keyEvent);
                                i++; //advance to the next character
                            }
                            else
                            //see if key was a modifier and don't release it now
                            //since it is not in a group, send the next key before releasing it
                            if (keyEvent == KeyEvent.VK_SHIFT
                            ||  keyEvent == KeyEvent.VK_ALT
                            ||  keyEvent == KeyEvent.VK_CONTROL
                            ||  keyEvent == KeyEvent.VK_WINDOWS
                            ||  keyEvent == KeyEvent.VK_META
                            ) {
                                releaseAfterNext.add(keyEvent);
                            }
                            else {
                                //otherwise release the key
                                if (keyEvent != KeyEvent.VK_UNDEFINED) {
                                    m_bot.delay(m_delay);
                                    m_bot.keyRelease(keyEvent);
                                }
                            }
                        }
                        
                        delayOnce = delay;
                    }                    
                } catch (SecurityException e) {
                    Server.logStackTrace(e);
                } catch (IllegalArgumentException e) {
                    Server.logStackTrace(e);
                } catch (IllegalAccessException e) {
                    Server.logStackTrace(e);
                }
            }
        }
        Server.logger().fine("SendKeys.sendKeys() Sent: ("+sent.toString()+")");
        return sent.toString();
    }
}
