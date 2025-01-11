/*
 * see license.txt
 */
package sric.lsp;

import java.io.*;

import com.google.gson.*;

import sric.lsp.JsonRpc.*;

/**
 * The litaC language server
 */
public class LanguageServer {

    private LspLogger log;
    private Gson gson;
    private boolean isInitialized;
    
    private RequestHandler handler;
    private MessageSender sender;
    
    public LanguageServer(String libPath, boolean debug) {        
        this.isInitialized = false;
        
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
        
        this.log = new LspLogger(debug);
        
        this.sender = new MessageSender(this.gson, this.log);
        this.handler = new RequestHandler(new Workspace(libPath, log), sender, this.log);
    }
    
    
    private String readContents(BufferedInputStream reader, int contentLength) throws IOException {
        byte[] contents = new byte[contentLength];
        reader.read(contents);
        
        String raw = new String(contents, 0, contentLength, "UTF-8");
        return raw;
    }
    
    public String readLine(BufferedInputStream reader, int maxLength) throws IOException {
        byte[] data = new byte[maxLength];
        int size = 0;
        while (true) {
            int c = reader.read();
            if (c == -1) break;
            if (c == '\r') {
                //read \n
                reader.read();
                break;
            }
            data[size] = (byte)c;
            ++size;
            if (size == maxLength) {
                break;
            }
        }
        return new String(data, 0, size, "UTF-8");
    }
    
    public void start() throws IOException {        
        try(BufferedInputStream reader = new BufferedInputStream(System.in)) {
            boolean isRunning = true;
            while(isRunning) {      
                log.log("Waiting for request...");
                
                String line = readLine(reader, 4096).trim();
                log.log("Received line: " + line);
                
                final String prefix = "Content-Length: ";
                if(!line.startsWith(prefix)) {
                    log.log("Received an invalid message '" + line + "'");
                    break;
                }
                
                final int contentLength = Integer.valueOf(line.substring(prefix.length()));
                if(contentLength < 0) {
                    log.log("Received an invalid content length '" + contentLength + "'");
                    break;
                }
                
                
                final String emptyLine = readLine(reader, 4096).trim();
                if(!emptyLine.equals("")) {
                    log.log("Received an invalid message format 'Missing new line'");
                    break;    
                }
                
                
                final String raw = readContents(reader, contentLength);
                log.log("Received message: '" + raw + "'");                
                
                RpcRequest msg;
                try {
                    msg = gson.fromJson(raw, RpcRequest.class);
                }
                catch (Exception e) {
                    log.log("Parse message error: "+raw);
                    continue;
                }
                switch(msg.method) {
                    case "initialize": {
                        InitializationParams init = gson.fromJson(msg.params, InitializationParams.class);
                        this.handler.handleInitialize(msg, init);
                        this.isInitialized = true;
                        break;
                    }
                    case "shutdown": {                        
                        break;
                    }
                    case "exit": {
                        isRunning = false;
                        break;
                    }
                    default: {
                        if(!this.isInitialized) {
                            RpcResponse response = new RpcResponse();
                            response.id = msg.id;
                            response.error = new ResponseError();
                            response.error.code = ErrorCodes.ServerNotInitialized.getValue();
                            this.sender.sendMessage(response);
                            break;
                        }
                        
                        switch(msg.method) {
                            case "textDocument/didOpen": {     
                                this.handler.handleTextDocumentDidOpen(msg, gson.fromJson(msg.params, DidOpenParams.class));
                                break;
                            }
                            case "textDocument/didClose": {
                                this.handler.handleTextDocumentDidClose(msg, gson.fromJson(msg.params, DidCloseParams.class));
                                break;
                            }
                            case "textDocument/didChange": {
                                this.handler.handleTextDocumentDidChange(msg, gson.fromJson(msg.params, DidChangeParams.class));
                                break;
                            }
                            case "textDocument/didSave": {
                                this.handler.handleTextDocumentDidSave(msg, gson.fromJson(msg.params, DidSaveTextDocumentParams.class));
                                break;
                            }
                            case "textDocument/definition": {
                                this.handler.handleTextDocumentDefinition(msg, gson.fromJson(msg.params, TextDocumentPositionParams.class));
                                break;
                            }
                            case "textDocument/documentSymbol": {
                                this.handler.handleTextDocumentDocumentSymbol(msg, gson.fromJson(msg.params, DocumentSymbolParams.class));
                                break;
                            }
                            case "textDocument/completion": {
                                this.handler.handleTextDocumentCompletion(msg, gson.fromJson(msg.params, CompletionParams.class));
                                break;
                            }
                            case "textDocument/references": {
                                this.handler.handleTextDocumentReferences(msg, gson.fromJson(msg.params, ReferenceParams.class));
                                break;
                            }
                            case "workspace/symbol": {
                                this.handler.handleWorkspaceSymbol(msg, gson.fromJson(msg.params, WorkspaceSymbolParams.class));
                                break;
                            }
                        }
                        
                        break;
                    }
                }
                    
            }
        }
        
        log.log("Normal shutdown");  
    }
    
    public void shutdown() {
        
    }
}
