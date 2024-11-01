//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author yangjiandong
 */
public class MiscTest {
    String libPath = "../lib";
    
    @Test
    public void testEnum() throws IOException {
        String file = "res/code/testEnum.sric";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testTypealias() throws IOException {
        String file = "res/code/testTypealias.sric";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testParam() throws IOException {
        String file = "res/code/testParam.sric";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testArray() throws IOException {
        String file = "res/code/testArray.sric";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testOperator() throws IOException {
        String file = "res/code/testOperator.sric";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testClosure() throws IOException {
        String file = "res/code/testClosure.sric";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
}
