package com.SIMRacingApps;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * This class defines wrappers to all of the calls to the Windows API.
 * This way, all the platform specific stuff is all in one place.
 * All methods are static, so there is no need to instantiate this class as an object.
 * @author Jeffrey Gilliam
 * @since 1.0
 * @copyright Copyright (C) 2015 - 2019 Jeffrey Gilliam
 * @license Apache License 2.0
 */

public class Windows {
    /** Extends the Kernel32 interface and add the methods we need that are not already there in jna 4.0.0 */
    protected interface myKernel32 extends Kernel32 {
        static final myKernel32 instance = (myKernel32)Native.loadLibrary("kernel32", myKernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        HANDLE OpenFileMapping(int lfProtect, boolean bInherit, String lpName);
        HANDLE OpenEvent(int i, boolean bManualReset, String lpName );
        boolean SetConsoleTitle(String lpTitle);
        
        //https://msdn.microsoft.com/en-us/library/windows/desktop/aa363428%28v=vs.85%29.aspx
        boolean PurgeComm(HANDLE hFile, WinNT.DWORD dwFlags);
        //https://msdn.microsoft.com/en-us/library/windows/desktop/aa363254%28v=vs.85%29.aspx
        boolean EscapeCommFunction(HANDLE hFile, WinNT.DWORD dwFunc);
    }

    /** Extends the User32 interface and add the methods we need that are not already there in jna 4.0.0 */
    protected interface myUser32 extends User32 {
        static final myUser32 instance = (myUser32)Native.loadLibrary("user32", myUser32.class, W32APIOptions.DEFAULT_OPTIONS);

//        BOOL WINAPI SendNotifyMessage(
//                  _In_  HWND hWnd,
//                  _In_  UINT Msg,
//                  _In_  WPARAM wParam,
//                  _In_  LPARAM lParam
//                );
        boolean SendNotifyMessage(WinDef.HWND hWnd, int Msg, int wParam, int lParam);
        boolean SendMessage(WinDef.HWND hWnd, int Msg, int wParam, int lParam);
        boolean BringWindowToTop(WinDef.HWND hWnd);
        boolean AllowSetForegroundWindow(WinDef.DWORD dwProcessId);
    }

    /*Error Codes*/
    public static int ERROR_FILE_NOT_FOUND = WinNT.ERROR_FILE_NOT_FOUND;
    public static int m_lastError = 0;
    
    /**
     * The Windows.Robot class is a subclass of the java.awt.Robot class.
     * It was created to be able to use PostMessage to send the keys directly to the SIM without it having to have focus.
     * This first version uses WM_CHAR, because WM_KEYDOWN and WM_KEYUP were causing duplicates.
     * 
     * NOTE: Currently not all keys are supported, only the ASCII characters so chat can work. 
     * When support for MACROs is written, all the keys will be mapped and tested.
     * 
     * @author Jeffrey Gilliam
     * @copyright Copyright (C) 2015 Jeffrey Gilliam
     * @since 1.0
     * @license Apache License 2.0
     */
    public static class Robot extends java.awt.Robot {
        private String m_windowName = "";
        private WinDef.HWND m_hWindow = null;
        private final Map<Integer,Integer> m_keys = new HashMap<Integer,Integer>(); 
        private final Map<Integer,Integer> m_shiftKeys = new HashMap<Integer,Integer>(); 
        @SuppressWarnings("unused")
        private Robot()  throws java.awt.AWTException {}
        public Robot(String windowName) throws java.awt.AWTException {
            m_windowName = windowName;
            
            //These maps are to convert the java.awt.KeyEvent mappings to the Windows Keyboard when shifted.
            //U.S. Keyboard.
            //TODO: Implement other keyboards. Will have the user pass in an argument for the keyboard they have.
            m_shiftKeys.put(KeyEvent.VK_BACK_QUOTE,     (int)'~');
            m_shiftKeys.put(KeyEvent.VK_1,              (int)'!');
            m_shiftKeys.put(KeyEvent.VK_2,              (int)'@');
            m_shiftKeys.put(KeyEvent.VK_3,              (int)'#');
            m_shiftKeys.put(KeyEvent.VK_4,              (int)'$');
            m_shiftKeys.put(KeyEvent.VK_5,              (int)'%');
            m_shiftKeys.put(KeyEvent.VK_6,              (int)'^');
            m_shiftKeys.put(KeyEvent.VK_7,              (int)'&');
            m_shiftKeys.put(KeyEvent.VK_8,              (int)'*');
            m_shiftKeys.put(KeyEvent.VK_9,              (int)'(');
            m_shiftKeys.put(KeyEvent.VK_0,              (int)')');
            m_shiftKeys.put(KeyEvent.VK_MINUS,          (int)'_');
            m_shiftKeys.put(KeyEvent.VK_EQUALS,         (int)'+');
            m_shiftKeys.put(KeyEvent.VK_OPEN_BRACKET,   (int)'{');
            m_shiftKeys.put(KeyEvent.VK_CLOSE_BRACKET,  (int)'}');
            m_shiftKeys.put(KeyEvent.VK_BACK_SLASH,     (int)'|');
            m_shiftKeys.put(KeyEvent.VK_SEMICOLON,      (int)':');
            m_shiftKeys.put(KeyEvent.VK_QUOTE,          (int)'"');
            m_shiftKeys.put(KeyEvent.VK_COMMA,          (int)'<');
            m_shiftKeys.put(KeyEvent.VK_PERIOD,         (int)'>');
            m_shiftKeys.put(KeyEvent.VK_SLASH,          (int)'?');
            
            //non-shifted KeyEvent values that need to be mapped to windows keys.
            m_keys.put(     KeyEvent.VK_QUOTE,          (int)'\'');
            m_keys.put(     KeyEvent.VK_BACK_QUOTE,     (int)'`');
            m_keys.put(     KeyEvent.VK_AMPERSAND,      (int)'&');
            m_keys.put(     KeyEvent.VK_ASTERISK,       (int)'*');
            m_keys.put(     KeyEvent.VK_QUOTEDBL,       (int)'"');
            m_keys.put(     KeyEvent.VK_LESS,           (int)'<');
            m_keys.put(     KeyEvent.VK_GREATER,        (int)'>');
            m_keys.put(     KeyEvent.VK_BRACELEFT,      (int)'{');
            m_keys.put(     KeyEvent.VK_BRACERIGHT,     (int)'}');
            m_keys.put(     KeyEvent.VK_AT,             (int)'@');
            m_keys.put(     KeyEvent.VK_COLON,          (int)':');
            m_keys.put(     KeyEvent.VK_CIRCUMFLEX,     (int)'^');
            m_keys.put(     KeyEvent.VK_DOLLAR,         (int)'$');
            m_keys.put(     KeyEvent.VK_EXCLAMATION_MARK,(int)'!');
            m_keys.put(     KeyEvent.VK_LEFT_PARENTHESIS,(int)'(');
            m_keys.put(     KeyEvent.VK_NUMBER_SIGN,    (int)'#');
            m_keys.put(     KeyEvent.VK_PLUS,           (int)'+');
            m_keys.put(     KeyEvent.VK_RIGHT_PARENTHESIS,(int)')');
            m_keys.put(     KeyEvent.VK_UNDERSCORE,     (int)'_');


        }
        
        public void delay(int ms) {
            if (ms > 0) {
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        private boolean m_shift = false;
        private int KeyEventToWindows(int key) {
            int k = key;
            
            if (key == KeyEvent.VK_ENTER) {
                k = 0x0D;   //VK_RETURN
            }
            
            if (m_shift) {
                if (m_shiftKeys.containsKey(k))
                    k = m_shiftKeys.get(k);
            }
            else {
                if (key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z)
                    k += 0x20;  //convert to lower case
                else
                if (key >= 0x61 /*a*/ && key <= 0x7A /*z*/) {  
                    //TODO: Map the lower case keys to Windows keys when Macro's are supported. For now just ASCII
                    k = 0;
                }
                else
                if (m_keys.containsKey(k))
                    k = m_keys.get(k);
            }
            
            return k;
        }
        
        public void keyPress(int key) {
            if (key == KeyEvent.VK_SHIFT)
                m_shift = true;
            else {
                m_hWindow = User32.INSTANCE.FindWindow(null,m_windowName);
                WinNT.WPARAM k = new WinNT.WPARAM(KeyEventToWindows(key));
                if (m_hWindow != null && k.intValue() > 0) {
                    Server.logger().finer("keyPress("+String.format("0x%04x",k.intValue())+")");
                    User32.INSTANCE.PostMessage(m_hWindow, User32.WM_CHAR, k, new WinNT.LPARAM(0));
                    //myUser32.instance.SendMessage(m_hWindow, User32.WM_KEYDOWN, k.intValue(), 0);
                }
            }
        }
        
        public void keyRelease(int key) {
            if (key == KeyEvent.VK_SHIFT)
                m_shift = false;
            else {
                return;
            }
//            m_hWindow = User32.INSTANCE.FindWindow(null,m_windowName);
//            WinNT.WPARAM k = new WinNT.WPARAM(KeyEventToWindows(key));
//            if (m_hWindow != null && k.intValue() > 0) {
//                Server.logger().finer("keyRelease("+String.format("0x%04x",key.intValue)+")");
//                myUser32.instance.SendMessage(m_hWindow, User32.WM_KEYUP, k.intValue(), 0);
//            }
        }
    }
    
    /**
     * Defines a wrapper for the MsgId to hide Windows definition details.
     */
    public static class MsgId {
        private WinDef.UINT m_id;
        public MsgId(WinDef.UINT id) {
            m_id = id;
        }
        protected int get() {
            return m_id.intValue();
        }
    }
    
    /** Defines a wrapper for a HANDLE to hide the Windows definition details */
    public static class Handle {
        private WinNT.HANDLE m_handle;
        public Handle(WinNT.HANDLE handle) {
            m_handle = handle;
        }
        protected WinNT.HANDLE get() {
            return m_handle;
        }
    }
    
    /** Defines a wrapper for a Pointer to hide the JNA definition details */
    public static class Pointer {
        private com.sun.jna.Pointer m_pointer;
        public Pointer(com.sun.jna.Pointer pointer) {
            m_pointer = pointer;
        }
        protected com.sun.jna.Pointer get() {
            return m_pointer;
        }
        public byte[] getByteArray(long offset, int arraySize) {
            return m_pointer.getByteArray(offset, arraySize);
        }
        public ByteBuffer getByteBuffer(long offset, long length) {
            return m_pointer.getByteBuffer(offset, length);
        }
    }
    
    /**
     * Closes a previously opened Handle
     * @param h The handle to close
     */
    public static void closeHandle(Handle h) {
        if (h != null) {
            Kernel32.INSTANCE.CloseHandle(h.get());
            m_lastError = Kernel32.INSTANCE.GetLastError();
        }
    }
    
    /**
     * Returns the last error number to compare against the Error Codes defined above.
     * @return The error number.
     */
    public static int getLastError() {
        return m_lastError;
    }
    
    /**
     * Returns the text error message associated with the last error number.
     * @return An error message.
     */
    public static String getLastErrorMessage() {
        PointerByReference pbr = new PointerByReference();
        int error = getLastError();
        
        try {
            Kernel32.INSTANCE.FormatMessage(
                    WinNT.FORMAT_MESSAGE_FROM_SYSTEM | WinNT.FORMAT_MESSAGE_ALLOCATE_BUFFER
                ,    com.sun.jna.Pointer.NULL
                ,    error
                ,     0                  //dwLanguageId
                ,    pbr
                ,    0
                ,    com.sun.jna.Pointer.NULL        //va_list
            );
            
            String s = Boolean.getBoolean("w32.ascii") ? pbr.getValue().getString(0) : pbr.getValue().getWideString(0);
            return String.format("(%d,0x%x): %s",error,error,s.replace("\r", "").replace("\n", ""));
        }
        finally {
            Kernel32.INSTANCE.LocalFree(pbr.getValue());
        }
    }

    public static void keyPress(WinDef.HWND hWin, int key) {
        
    }
    
    /**
     * Returns a Pointer to the memory where the file was mapped to.
     * @param h A Handle to a memory mapped file.
     * @return A Pointer to the memory.
     */
    public static Pointer mapViewOfFile(Handle h) {
        if (h == null)
            return null;
        
        com.sun.jna.Pointer pSharedMem = Kernel32.INSTANCE.MapViewOfFile(
            h.get(),
            WinNT.SECTION_MAP_READ,     //according to MSDN this should be WinNT.FILE_MAP_READ but JNA doesn't define.
                                        //In memoryapi.h, it is the same as SECTION_MAP_READ in winnt.h
            0,                          //OffsetHigh
            0,                          //OffsetLow
            0                           //NumberOfBytesToMap, 0 means all
        );
        m_lastError = Kernel32.INSTANCE.GetLastError();
        
        return pSharedMem == null ? null : new Pointer(pSharedMem);
    }


    /**
     * Return a Handle to a COM port
     * 
     * There is some good information here 
     * <a href="https://msdn.microsoft.com/en-us/library/ms810467.aspx"></a> 
     * about how to use the Window serial port API. 
     * @param portNumber The port number to open as an integer
     * @return A Handle object to the open port, null if error.
     */
    public static Handle openCommPort(int portNumber) {
        WinBase.SECURITY_ATTRIBUTES s = null;
        WinNT.HANDLE t = null;
        String comPort = "\\\\.\\COM" + Integer.toString(portNumber);
        m_lastError = 0;
        WinNT.HANDLE h = Kernel32.INSTANCE.CreateFile(
                            comPort,                                                    //lpFileName 
                            WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,                   //dwDesiredAcess
                            0,                                                          //dwSharedMode
                            s,                                                          //lpSecurityAttributes
                            WinNT.OPEN_EXISTING,                                        //dwCreationDisposition
                            WinNT.FILE_ATTRIBUTE_NORMAL,                                //dwFlagsAndAttributes
                            t                                                           //hTemplateFile
        );
        m_lastError = Kernel32.INSTANCE.GetLastError();
        
        if (h == null || m_lastError != 0)
            return null;
        
        //https://msdn.microsoft.com/en-us/library/windows/desktop/aa363428%28v=vs.85%29.aspx
        if (!myKernel32.instance.PurgeComm(h,new WinNT.DWORD(0x000F/*PURGE_RXABORT|PURGE_RXCLEAR|PURGE_TXABORT|PURGE_TXCLEAR*/))) {
            m_lastError = Kernel32.INSTANCE.GetLastError();
            Kernel32.INSTANCE.CloseHandle(h);
            return null;
        }
        
        //get the current config and set the flow control to disabled
        WinBase.DCB dcb = new WinBase.DCB();
        if (!Kernel32.INSTANCE.GetCommState(h, dcb)) {
            m_lastError = Kernel32.INSTANCE.GetLastError();
            Kernel32.INSTANCE.CloseHandle(h);
            return null;
        }
        
        if (dcb.controllBits.getfRtsControl() != WinBase.RTS_CONTROL_DISABLE 
        ||  dcb.controllBits.getfDtrControl() != WinBase.DTR_CONTROL_DISABLE
        ) {
            dcb.controllBits.setfRtsControl(WinBase.RTS_CONTROL_DISABLE);
            dcb.controllBits.setfDtrControl(WinBase.DTR_CONTROL_DISABLE);
            
            if (!Kernel32.INSTANCE.SetCommState(h, dcb)) {
                m_lastError = Kernel32.INSTANCE.GetLastError();
                Kernel32.INSTANCE.CloseHandle(h);
                return null;
            }
            try {Thread.sleep(60);} catch (InterruptedException e) {}
        }
        
        //try to lower the RTS line to see if the handle is valid.
        //https://msdn.microsoft.com/en-us/library/windows/desktop/aa363254%28v=vs.85%29.aspx
        if (!myKernel32.instance.EscapeCommFunction(h,new WinNT.DWORD(4 /*WinNT.CLRRTS*/))) {
            m_lastError = Kernel32.INSTANCE.GetLastError();
            Kernel32.INSTANCE.CloseHandle(h);
            return null;
        }
        if (!myKernel32.instance.EscapeCommFunction(h,new WinNT.DWORD(6 /*WinNT.CLRDTR*/))) {
            m_lastError = Kernel32.INSTANCE.GetLastError();
            Kernel32.INSTANCE.CloseHandle(h);
            return null;
        }
        
        return new Handle(h);
    }
    
    /**
     * Returns a Handle to an Event.
     * @param eventName The name of the event.
     * @return A Handle for the event.
     */
    public static Handle openEvent(String eventName) {
        WinNT.HANDLE h = myKernel32.instance.OpenEvent(
                WinNT.SYNCHRONIZE,
                false,         //Manual Reset
                eventName      //event name
            );
        m_lastError = Kernel32.INSTANCE.GetLastError();
        return h == null ? null : new Handle(h);
    }
    
    /**
     * Returns a Handle to a memory mapped file.
     * @param filename The memory map name.
     * @return A Handle to the file.
     */
    public static Handle openFileMapping(String filename) {
        WinNT.HANDLE hMemMapFile = myKernel32.instance.OpenFileMapping(
                WinNT.SECTION_MAP_READ   //according to MSDN this should be WinNT.FILE_MAP_READ but JNA doesn't define.
                                         //In memoryapi.h, it is the same as SECTION_MAP_READ in winnt.h
           ,    false                    //Do not inherit the name
           ,    filename
        );
        m_lastError = Kernel32.INSTANCE.GetLastError();
       
        return hMemMapFile == null ? null : new Handle(hMemMapFile);
    }

    /**
     * Sends a message to the specified window and does not wait on it to be received
     * @param hWin A handle to an existing window.
     * @param id The MsgId to send to.
     * @param wParam A 32 bit word.
     * @param lParam A 64 bit word.
     */
    public static void postMessage(WinDef.HWND hWin, MsgId id, int wParam, int lParam) {
        if (id != null) {
            WinDef.WPARAM w = new WinDef.WPARAM(wParam);
            WinDef.LPARAM l = new WinDef.LPARAM(lParam);
            myUser32.instance.PostMessage(hWin, id.get(), w, l);
            m_lastError = Kernel32.INSTANCE.GetLastError();
        }
    }

    /**
     * Returns the MsgId of named message.
     * @param name The name of the message
     * @return The MsgId or null on failure
     */
    public static MsgId registerWindowMessage(String name) {
        WinDef.UINT id = new WinDef.UINT( User32.INSTANCE.RegisterWindowMessage(name));
        m_lastError = Kernel32.INSTANCE.GetLastError();
        if (id.intValue() == 0)
            return null;
        return new MsgId(id);
    }
    
    /**
     * Sends a message to anyone listening to the specified MsgId
     * @param id The MsgId to send to.
     * @param wParam A 32 bit word.
     * @param lParam A 64 bit word.
     */
    public static void sendNotifyMessage(MsgId id, int wParam, int lParam) {
        if (id != null) {
            WinDef.WPARAM w = new WinDef.WPARAM(wParam);
            WinDef.LPARAM l = new WinDef.LPARAM(lParam);
            myUser32.instance.SendNotifyMessage(User32.HWND_BROADCAST, id.get(), w.intValue(), l.intValue());
            m_lastError = Kernel32.INSTANCE.GetLastError();
        }
    }

    /**
     * This method either sets or clears the RTS signal on the comm port.
     * @param hCommPort A handle to an open comm port
     * @param flag true sets RTS, false clears RTS
     * @return true if successful
     */
    public static boolean setCommPortRTS(Handle hCommPort, boolean flag) {
        if (hCommPort == null) {
            m_lastError = WinNT.ERROR_INVALID_HANDLE;
            return false;
        }
        
        //https://msdn.microsoft.com/en-us/library/windows/desktop/aa363254%28v=vs.85%29.aspx
        boolean b = myKernel32.instance.EscapeCommFunction(
                hCommPort.get(),
                flag ? new WinNT.DWORD(3 /*WinNT.SETRTS*/) : new WinNT.DWORD(4 /*WinNT.CLRRTS*/)
        );
        m_lastError = Kernel32.INSTANCE.GetLastError();
        return b;
    }
    
    /**
     * This method either sets or clears the DTR signal on the comm port.
     * @param hCommPort A handle to an open comm port
     * @param flag true sets DTR, false clears DTR
     * @return true if successful
     */
    public static boolean setCommPortDTR(Handle hCommPort, boolean flag) {
        if (hCommPort == null) {
            m_lastError = WinNT.ERROR_INVALID_HANDLE;
            return false;
        }
        
        //https://msdn.microsoft.com/en-us/library/windows/desktop/aa363254%28v=vs.85%29.aspx
        boolean b = myKernel32.instance.EscapeCommFunction(
                hCommPort.get(),
                flag ? new WinNT.DWORD(5 /*WinNT.SETDTR*/) : new WinNT.DWORD(6 /*WinNT.CLRDTR*/)
        );
        m_lastError = Kernel32.INSTANCE.GetLastError();
        return b;
    }
    
    /**
     * Sets the title of the console window.
     * @param title The title.
     */
    public static void setConsoleTitle(String title) {
        myKernel32.instance.SetConsoleTitle(title);
        m_lastError = Kernel32.INSTANCE.GetLastError();
    }
    
    /**
     * Finds the window with the given title and brings it to the foreground.
     * If no window exists, then nothing changes.
     * 
     * @param windowClass The window class or null
     * @param windowTitle The window title or null
     * @return true if success, false if not.
     */
    public static boolean setForegroundWindow(String windowClass, String windowTitle) {
        WinDef.HWND hSIMWindow = User32.INSTANCE.FindWindow(windowClass,windowTitle);
        m_lastError = Kernel32.INSTANCE.GetLastError();
        if (hSIMWindow == null) {
            Server.logger().info("Cannot find window ("+windowTitle+")");
            return false;
        }
        else {
            WinDef.HWND hWndForeground = User32.INSTANCE.GetForegroundWindow();
            m_lastError = Kernel32.INSTANCE.GetLastError();
            if (hWndForeground.equals(hSIMWindow))
                return true;
            
            for( int counter = 0; counter < 3; counter++) {
                
                //m_lastError = Kernel32.INSTANCE.GetLastError();
                int ForegroundThread = User32.INSTANCE.GetWindowThreadProcessId(hWndForeground,null);
                int SIMThread = User32.INSTANCE.GetWindowThreadProcessId(hSIMWindow,null);
                Server.logger().fine("Foreground Window is not " + windowTitle + ", trying to move to the foreground...(SIMThread="+Integer.toString(SIMThread)+",ForegroundThread="+Integer.toString(ForegroundThread)+")");
                
                if (SIMThread != 0 && ForegroundThread != 0) {
                    if (SIMThread == ForegroundThread) {
                        User32.INSTANCE.SetForegroundWindow(hSIMWindow);
                    }
                    else {
                        //TODO: This does not work if the current window is the Task Manager, Console, or System window.
                        //      But it is not failing, it just doesn't do it.
                        //      https://msdn.microsoft.com/en-us/library/ms810439.aspx
                        if (!User32.INSTANCE.AttachThreadInput(new WinNT.DWORD(ForegroundThread),new WinNT.DWORD(SIMThread),true)) {
                            m_lastError = Kernel32.INSTANCE.GetLastError();
                            Server.logger().fine("AttachThreadInput(true) Failed " + Windows.getLastErrorMessage());
                        }
                        if (!myUser32.instance.BringWindowToTop(hSIMWindow)) {
                            m_lastError = Kernel32.INSTANCE.GetLastError();
                            Server.logger().fine("BringWindowToTop() Failed " + Windows.getLastErrorMessage());
                        }
                        
                        //myUser32.instance.SendMessage(hWndForeground, 0x0008/*WinNT.WM_KILLFOCUS*/, 0, 0);
                        //myUser32.instance.AllowSetForegroundWindow(new WinNT.DWORD(-1/*WinNT.ASFW_ANY*/));
                        //User32.INSTANCE.SetForegroundWindow(hSIMWindow);
                        //User32.INSTANCE.SetFocus(hSIMWindow);
                        //myUser32.instance.SendMessage(hSIMWindow, 0x0007/*WinNT.WM_SETFOCUS*/, (int)hWndForeground.toNative(), 0);
                            
                        if (!User32.INSTANCE.AttachThreadInput(new WinNT.DWORD(ForegroundThread),new WinNT.DWORD(SIMThread),false)) {
                            m_lastError = Kernel32.INSTANCE.GetLastError();
                            Server.logger().fine("AttachThreadInput(false) Failed " + Windows.getLastErrorMessage());
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Server.logStackTrace(Level.WARNING,e);
                }
                
                hWndForeground = User32.INSTANCE.GetForegroundWindow();
                m_lastError = Kernel32.INSTANCE.GetLastError();
                if (hWndForeground.equals(hSIMWindow))
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Removes the memory pointer mapping.
     * @param p The Pointer to release.
     */
    public static void unmapViewOfFile(Pointer p) {
        if (p != null) {
            Kernel32.INSTANCE.UnmapViewOfFile(p.get());
            m_lastError = Kernel32.INSTANCE.GetLastError();
        }
    }
    
    /**
     * Waits for a specified amount of time for an event to occur.
     * @param h A Handle to an event.
     * @param timeout Amount of time to wait in milliseconds.
     */
    public static void waitForSingleObject(Handle h, int timeout) {
        if (h != null) {
            Kernel32.INSTANCE.WaitForSingleObject(h.get(), timeout);
            m_lastError = Kernel32.INSTANCE.GetLastError();
        }
    }
}
