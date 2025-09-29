//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


/**
 *
 * @author yangjiandong
 */
public class NegativeTest {
    
    @Test
    public void test() throws IOException {
        String libPath = "../lib";
        File file = new File("res/negative/closureConst.sric");

        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file.getPath(), libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertFalse(res, file.getName());

        String str = compiler.log.toString();
        String name = file.getName().substring(0, file.getName().lastIndexOf("."));
        GoldenTest.verifyGolden(str, "negative", name+".txt");
    }
    
    @Test
    public void testCompletion() throws IOException {
        String libPath = "../lib";
        File file = new File("res/negative/testCompletion2.sric");

        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file.getPath(), libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        
        String src = Files.readString(file.toPath());
        compiler.updateFile(file.getPath(), src);

    }
    
    @Test
    public void testAll() throws IOException {
        String libPath = "../lib";
        File[] list = new File("res/negative").listFiles();
        for (File file : list) {
            if (!file.getName().endsWith(".sric")) {
                continue;
            }

            sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file.getPath(), libPath);
            compiler.genCode = false;
            boolean res = compiler.run();
            assertFalse(res, file.getName());

            String str = compiler.log.toString();
            String name = file.getName().substring(0, file.getName().lastIndexOf("."));
            GoldenTest.verifyGolden(str, "negative", name+".txt");
        }
    }
}
