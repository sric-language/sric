/*
 * see license.txt
 */
package sric.lsp;

import java.util.*;

import com.google.gson.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import sric.compiler.CompilerLog;
import sric.compiler.CompilerLog.CompilerErr;
import sric.compiler.ast.Loc;
import sric.lsp.JsonRpc.*;

/**
 * Sends messages back to the client
 * 
 * @author Tony
 *
 */
public class MessageSender {

    private Gson gson;
    private Gson gsonNull;
    private LspLogger log;
    
    public MessageSender(Gson gson, LspLogger log) {
        this.gson = gson;
        this.log = log;
        this.gsonNull = new GsonBuilder()
                .serializeNulls()
                .create();
    }
    
    public void sendMessage(Object msg) {
        sendMessage(msg, false);
    }

    public void sendMessage(Object msg, boolean serializeNulls) {
        try {
            String message;
            if (serializeNulls) {
                message = gsonNull.toJson(msg);
            }
            else {
                message = gson.toJson(msg);
            }
            
            byte[] buf = message.getBytes("UTF-8");
            
            String output = String.format("Content-Length: %d\r\n\r\n", buf.length);
            System.out.print(output);
            System.out.write(buf);
            System.out.flush();
            
            log.log("Sent: " + message);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(MessageSender.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MessageSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendDiagnostics(Workspace workspace, String documentUri) {
        Document doc = workspace.getDocument(documentUri);
        ArrayList<CompilerErr> errors = doc.compiler.log.errors;
        
        PublishDiagnosticsParams params = new PublishDiagnosticsParams();
        params.uri = documentUri;
        
        if(!errors.isEmpty()) {
            params.diagnostics = new ArrayList<>();
            for(CompilerErr error : errors) {
                Diagnostic d = new Diagnostic();                
                d.message = error.msg;
                d.severity = 1;
                d.source = error.loc.file;
                d.range = LspUtil.fromSrcPosLine(error.loc, 0);

                params.diagnostics.add(d);
            }
        }
        else {
            params.diagnostics = new ArrayList<>();
        }
        
        RpcNotificationMessage notification = new RpcNotificationMessage();
        notification.method = "textDocument/publishDiagnostics";
        notification.params = params;
        
        sendMessage(notification);
    }
}
