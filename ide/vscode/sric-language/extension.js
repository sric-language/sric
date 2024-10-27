const vscode = require("vscode");
const vscode_languageclient = require("vscode-languageclient");
const cp = require('child_process');


class FailFastErrorHandler {
    error(_error, _message, count) {
        console.log("Error: " + _message);
        console.log(_error);
        vscode.window.showErrorMessage("An error occurred: " + _message + " from " + _error );
        return ErrorAction.Shutdown;
    }
    closed() {
        vscode.window.showErrorMessage("The Sric language server has crashed");
        return CloseAction.DoNotRestart;
    }
}

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
    var config = vscode.workspace.getConfiguration("sric");
        
    var binaryPath = config.get("languageServerPath");
    if (!binaryPath) {
        vscode.window.showErrorMessage("Could not start Sric language server due to missing setting: sric.languageServerPath");
        return;
    }

    var args = config.get("languageServerArguments");
    if (!args) {
        vscode.window.showErrorMessage("Could not start Sric language server due to missing setting: sric.languageServerArguments");
        return;
    }
    args = args.split(" ");

    binaryPath = "java -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=5005 -cp C:/workspace/code/safeC/bin/gson-2.8.6.jar;C:/workspace/code/safeC/bin/sric-1.0-SNAPSHOT.jar sric.compiler.Main -lsp -lib C:/workspace/code/safeC/compiler/res/lib";
    
    // var debugLog = config.get("languageServerLog");
    // if (debugLog) {
    //     args += " -verbose";
    // }
    
    // var libraryPath = config.get("libraryPath");
    // if (libraryPath) {
    //     args += " -lib \"" + libraryPath + "\"";
    // }

    var failFast = (!!config.get("failFast")) || false; 
    // var clearOnRun = (!!config.get("clearTestOutput")) || true;
    
    // var testOutputPath = config.get("testOutputPath");

    var serverOptions = {
        command: binaryPath,
        //args: [ args ],
        options: { shell: true },
    };

    var clientOptions = {
        documentSelector: [{ scheme: 'file', language: 'sric' }],
        errorHandler: failFast ? new FailFastErrorHandler() : null,
    };

    // register custom commands
    
    // Test Current File
    // context.subscriptions.push(vscode.commands.registerCommand('sric.runTestsInCurrentFile', function(args) {
    //     var docName = vscode.window.activeTextEditor.document.uri.fsPath;
    //     if(docName == null) {
    //         return;
    //     }
        
    //     console.log("Running test command for '" + docName + "'");
    //     if(clearOnRun) {
    //         getOutputChannel().clear()
    //     }
    
	// 	var args = "";
	// 	if(testOutputPath) {
	// 		args += " -outputDir \"" + testOutputPath + "\"";
	// 	}
	
    //     var cmd = binaryPath
    //     if(libraryPath) {
    //         cmd += " -lib " + libraryPath
    //     }
    //     cmd += " -testFile -run " + args + " " + docName
        
    //     exec(cmd);
    // }));

    console.log("Running Sric Language server...");

    var debug = true;
    var client = new vscode_languageclient.LanguageClient("SricLanguageServer", "SricLanguageServer", serverOptions, clientOptions, debug);
    client.start(); 
    //context.subscriptions.push(client.start());
}

function exec(cmd) {
    return cp.exec(cmd, (err, stdout, stderr) => {
        var out = getOutputChannel();
        out.appendLine(stdout);
        if(stderr != null) {
            out.appendLine(stderr);
        }
        
        out.show(true);
        
        console.log('stdout: ' + stdout);
        console.log('stderr: ' + stderr);
        if (err) {
            console.log('error: ' + err);
            vscode.window.showErrorMessage("An error occurred: " + err);
        }
    });
}

function deactivate() {
}

var _channel = null;
function getOutputChannel() {
    if (!_channel) {
        _channel = vscode.window.createOutputChannel('Sric Test');
    }
    return _channel;
}

module.exports = {
    activate,
    deactivate
}