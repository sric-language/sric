const vscode = require("vscode");
const vscode_languageclient = require("vscode-languageclient");
const cp = require('child_process');
const path = require('node:path');

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

    var sricHome = config.get("sricHome");
    if (!sricHome) {
        vscode.window.showErrorMessage("Could not start Sric language server due to missing setting: sric.sricHome");
        return;
    }
    
    var debugLsp = config.get("debugLsp");
    var binaryPath = "java";
    if (debugLsp) {
        binaryPath += " -agentlib:jdwp=transport=dt_socket,address=localhost:5005,server=n,suspend=y";
    }
    binaryPath += " -cp "+sricHome+"/bin/gson-2.8.6.jar" + path.delimiter + sricHome+"/bin/sric-1.0.jar sric.compiler.Main";

    var args = "-lib "+sricHome+"/lib -lsp";

    var debugLog = config.get("languageServerLog");
    if (debugLog) {
        args += " -verbose";
    }

    var failFast = (!!config.get("failFast")) || false; 
    // var clearOnRun = (!!config.get("clearTestOutput")) || true;
    // var testOutputPath = config.get("testOutputPath");

    var serverOptions = {
        command: binaryPath,
        args: [ args ],
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
    console.log(serverOptions);

    var client = new vscode_languageclient.LanguageClient("SricLanguageServer", "SricLanguageServer", serverOptions, clientOptions);
    client.start(); 
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