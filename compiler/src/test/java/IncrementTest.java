//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import sric.compiler.Util;
import sric.compiler.ast.SModule;

/**
 *
 * @author yangjiandong
 */
public class IncrementTest {
    @Test
    public void test() throws IOException {
        String file = "res/increment";
        String libPath = "../lib";
        
        SModule module = new SModule();
        module.name = "increment";
        module.version = "1.0";
        module.sourcePath = new File(file).getAbsolutePath();
        module.outType = "exe";
        module.scriptMode = true;
        File libDir = new File(libPath);
        //module.depends= listDepends(libDir);

        File sourceDir = new File(file);
        //File libDir = new File(libPath);
        sric.compiler.Compiler compiler =  new sric.compiler.Compiler(module, sourceDir, libPath, libDir.getParent()+"/output/");
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
        
        String changedFile = new File(file + "/s2.sric").getAbsolutePath();
        String changedCode = Files.readString(Path.of(changedFile));
        compiler.updateFile(changedFile, changedCode);
        
        assertTrue(!compiler.log.hasError());
    }
}
