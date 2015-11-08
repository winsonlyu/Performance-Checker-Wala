/******************************************************************************* 
 * Copyright (c) 2002 - 2006 IBM Corporation. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 *     IBM Corporation - initial API and implementation 
 *******************************************************************************/
package com.ibm.wala.examples.drivers;
  
import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection; 
import java.util.Iterator; 
import java.util.List; 
import java.util.Properties; 
import java.util.Set;
import java.util.Vector; 
import org.eclipse.core.internal.utils.Queue; 
import com.ibm.wala.classLoader.IClass; 
import com.ibm.wala.classLoader.IMethod; 
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil; 
import com.ibm.wala.examples.drivers.PDFCallGraph; 
import com.ibm.wala.examples.drivers.PDFTypeHierarchy; 
import com.ibm.wala.examples.drivers.PDFWalaIR; 
import com.ibm.wala.examples.properties.WalaExamplesProperties; 
import com.ibm.wala.ipa.callgraph.AnalysisCache; 
import com.ibm.wala.ipa.callgraph.AnalysisOptions; 
import com.ibm.wala.ipa.callgraph.AnalysisScope; 
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere; 
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy; 
import com.ibm.wala.ipa.slicer.NormalReturnCaller; 
import com.ibm.wala.ipa.slicer.NormalStatement; 
import com.ibm.wala.ipa.slicer.SDG; 
import com.ibm.wala.ipa.slicer.Statement; 
import com.ibm.wala.ipa.slicer.Statement.Kind; 
import com.ibm.wala.properties.WalaProperties; 
import com.ibm.wala.ssa.IR; 
import com.ibm.wala.ssa.ISSABasicBlock; 
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction; 
import com.ibm.wala.ssa.SSACFG; 
import com.ibm.wala.ssa.SSAInvokeInstruction; 
import com.ibm.wala.ssa.SSACFG.BasicBlock; 
import com.ibm.wala.ssa.SSAInstruction; 
import com.ibm.wala.ssa.SSAOptions; 
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference; 
import com.ibm.wala.util.WalaException; 
import com.ibm.wala.util.collections.Filter; 
import com.ibm.wala.util.config.AnalysisScopeReader; 
import com.ibm.wala.util.debug.Assertions; 
import com.ibm.wala.util.graph.Graph; 
import com.ibm.wala.util.graph.GraphSlicer; 
import com.ibm.wala.util.io.CommandLine; 
import com.ibm.wala.util.io.FileProvider; 
  
/** 
 *  
 * This application is a WALA client: it invokes an SWT TreeViewer to visualize 
 * a Call Graph 
 *  
 * @author sfink 
 */
public class SWTCallGraph { 
  /** 
   * Usage: SWTCallGraph -appJar [jar file name] 
   *  
   * The "jar file name" should be something like 
   * "c:/temp/testdata/java_cup.jar" 
   *  
   * If it's a directory, then we'll try to find all jar files under that 
   * directory. 
   *  
   * @param args 
   * @throws WalaException 
   * @throws IOException 
   */

  private static ArrayList<IClass> workList = new ArrayList<IClass>(); 
  public static void main(String[] args) throws WalaException, IOException { 
    Properties p = CommandLine.parse(args); 
    PDFCallGraph.validateCommandLine(p); 
    run(p); 
  } 
  
  /** 
   * @param p 
   *            should contain at least the following properties: 
   *            <ul> 
   *            <li>appJar should be something like 
   *            "c:/temp/testdata/java_cup.jar" 
   *            <li>algorithm (optional) can be one of: 
   *            <ul> 
   *            <li> "ZERO_CFA" (default value) 
   *            <li> "RTA" 
   *            </ul> 
   *            </ul> 
   *  
   * @throws WalaException 
   * @throws IOException 
   */
  public static void run(Properties p) throws WalaException, IOException { 
    boolean has_on_scroll_state_changed_listener = false;
    boolean use_switch = false;
    List<String> result = new ArrayList<String>();
    int count =0;
    File f = new File("result_test.txt"); 
    FileWriter fw = new FileWriter(f.getAbsoluteFile()); 
    BufferedWriter bw = new BufferedWriter(fw); 
    
    try { 
      String appJar = p.getProperty("appJar"); 
      if (PDFCallGraph.isDirectory(appJar)) { 
        appJar = PDFCallGraph.findJarFiles(new String[] { appJar }); 
      } 
  
      String exclusionFile = p.getProperty("exclusions"); 
  
      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, exclusionFile != null ? new File(exclusionFile) 
          : (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS)); 
  
      ClassHierarchy cha = ClassHierarchy.make(scope); 
  
      boolean is_image = false;
     boolean is_resized = false;
 //*************************************************************************************************************8//    
      for(IClass c: cha){ 
       
        String className = c.getName().toString(); 
        if(scope.isApplicationLoader(c.getClassLoader()) && !className.contains("support")) 
        { String clsName = className.replaceAll("[/]", ".").substring(1,  className.length()); 
        //System.out.println(clsName); 
        
     //   System.out.println("[wala] located class " + clsName); 
           workList.add(c); 
    //   System.out.println("[wala]  analyzing class "+ className);
          Collection<IMethod> cIM = c.getDeclaredMethods(); 
          
    //      System.out.println(cIM.size()); 
          for(Iterator<IMethod> iIM = cIM.iterator(); iIM.hasNext();) 
          { 
              IMethod IM = iIM.next(); 
                
              AnalysisOptions option = new AnalysisOptions(); 
              option.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes()); 
              AnalysisCache cache = new AnalysisCache(); 
              IR ir = cache.getSSACache().findOrCreateIR(IM, Everywhere.EVERYWHERE, option.getSSAOptions()); 
                
              if(ir == null) 
               System.err.println(IM.getName()); 
                
              if(ir != null) 
              { 
                  //System.out.println(IM.getName()); 
                         
                 if( IM.getDeclaringClass().toString().contains("BitmapFactory")&&IM.getReturnType().toString().contains("Bitmap")&&!IM.getDeclaredExceptions().toString().contains("eclipse"))
                  {           
                   System.out.println("!!!!!!!!![wala] located class " + clsName); 
                    is_image=true;
                    System.out.println("here it is!!!!!~"+ IM.getClass().toString());
                    result.add(IM.toString());
                  }
                 if(IM.getDeclaringClass().toString().contains("BitmapFactory$Options"))
                 {   
                   is_resized = true;
                 }  
                    
                  
              } 
          } 
          
      //    System.out.println("End of Class " + c.getName()); 
            
        } 
      } 
//******************************************************************************************************************//
     
      
      
      System.out.println("***********************analysis results**********************"); 
      bw.write("***********************analysis results**********************\n");       
      if(!is_resized&&is_image){
        System.out.println("The application use Bitmap without resize.");
        for(int i=0;i<result.size();i++){
          System.out.println(result.get(i));
        }
      }
      System.out.println("********************end of analysis results******************"); 
      bw.write("********************end of analysis results******************"); 

      Properties wp = null; 
      try { 
        wp = WalaProperties.loadProperties(); 
        wp.putAll(WalaExamplesProperties.loadProperties()); 
      } catch (WalaException e) { 
        e.printStackTrace(); 
        Assertions.UNREACHABLE(); 
      } 
      wp.getProperty(WalaExamplesProperties.DOT_EXE); 
      wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE); 
  
      // create and run the viewer 
 /*     final SWTTreeViewer v = new SWTTreeViewer(); 
      v.setGraphInput(cg); 
      v.setRootsInput(InferGraphRoots.inferRoots(cg)); 
      v.getPopUpActions().add(new ViewIRAction(v, cg, psFile, dotFile, dotExe, gvExe)); 
      v.run(); 
      return v.getApplicationWindow(); 
  */
    } catch (Exception e) { 
      e.printStackTrace(); 
       
    } 
  } 
    
 

} 