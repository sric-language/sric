/*
 * see license.txt
 */
package sric.lsp;

import java.io.*;

/**
 * Logger for LSP (as stdout is already in use)
 */
public class LspLogger {

    private RandomAccessFile log;
    private boolean enableLog;
    
    private static LspLogger instance = new LspLogger();
    
    public void init(boolean enableLog) {
        try {
            this.enableLog = enableLog;
            if(enableLog) {
                this.log = new RandomAccessFile(new File("./sric-lsp.log"), "rw");
                this.log.setLength(0);
            }
        }
        catch(Exception e) {
            System.err.println("Unable to create litac log file: " + e);
            e.printStackTrace();
        }
    }

    public static LspLogger cur() {
        return instance;
    }

    public void log(String message) {
        try {
            if(this.enableLog) {
                this.log.writeBytes(message);
                this.log.writeBytes("\n");
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
