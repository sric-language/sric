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
        String file = "res/code/testEnum.sc";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testTypealias() throws IOException {
        String file = "res/code/testTypealias.sc";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testParam() throws IOException {
        String file = "res/code/testParam.sc";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testArray() throws IOException {
        String file = "res/code/testArray.sc";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testOperator() throws IOException {
        String file = "res/code/testOperator.sc";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testClosure() throws IOException {
        String file = "res/code/testClosure.sc";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
}
