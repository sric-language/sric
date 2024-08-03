//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.Token;
import sc2.compiler.backend.CppGenerator;
import sc2.compiler.parser.Parser;
import sc2.compiler.parser.Tokenizer;

/**
 *
 * @author yangjiandong
 */
public class ParserTest {
    @Test
    public void testSys() throws IOException {
        String file = "target/test-classes/sys.sc";
        String src = Files.readString(Path.of(file));
        
        CompilerLog log = new CompilerLog();
        FileUnit unit = new FileUnit(file);
        Parser parser = new Parser(log, src, unit);
        parser.parse();
        
        CppGenerator generator = new CppGenerator(System.out);
        unit.walk(generator);
        //System.out.println(file);
    }
    
    @Test
    public void testAll() throws IOException {
        File[] list = new File("target/test-classes").listFiles();
        for (File file : list) {
            if (!file.getName().endsWith(".sc")) {
                continue;
            }
            String src = Files.readString(file.toPath());
        
            CompilerLog log = new CompilerLog();
            FileUnit unit = new FileUnit(file.getPath());
            Parser parser = new Parser(log, src, unit);
            parser.parse();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            CppGenerator generator = new CppGenerator(new PrintStream(stream));
            unit.walk(generator);
            
            String str = stream.toString("UTF-8");
            String name = file.getName().substring(0, file.getName().lastIndexOf("."));
            GoldenTest.verifyGolden(str, "parser", name+".cpp");
        }
    }
}
