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
package PerfCheckerRL;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarFile;

import org.eclipse.core.internal.utils.Queue;
import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.drivers.PDFWalaIR;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ide.ui.SWTTreeViewer;
import com.ibm.wala.ide.ui.ViewIRAction;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

/**
 * 
 * This application is a WALA client: it invokes an SWT TreeViewer to visualize
 * a Call Graph
 * 
 * @author sfink
 */
public class checkRL {

  private final static boolean CHECK_GRAPH = false;

  
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
   * @throws ClassNotFoundException 
   */
  public static void main(String[] args) throws WalaException, IOException, ClassNotFoundException {
    Properties p = CommandLine.parse(args);
    PDFCallGraph.validateCommandLine(p);
    run(p);
    
    //String name = "com.actionbarsherlock.internal.view.menu.ActionMenuPresenter$ActionButtonSubmenu";
    //Class cls = Class.forName(name);
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
  public static ApplicationWindow run(Properties p) throws WalaException, IOException {

     
    
    try {
      String appJar = p.getProperty("appJar");
      if (PDFCallGraph.isDirectory(appJar)) {
        appJar = PDFCallGraph.findJarFiles(new String[] { appJar });
      }

      String exclusionFile = p.getProperty("exclusions");

      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, exclusionFile != null ? new File(exclusionFile)
          : (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      ClassHierarchy cha = ClassHierarchy.make(scope);
      
      int numberOfClass = 0;
      ArrayList<Report> results = new ArrayList<Report>();
      ArrayList<Report_whole> results_w = new ArrayList<Report_whole>();
      
      Iterator<IClass> ite = cha.iterator();
      while(ite.hasNext()){
        IClass c = ite.next();
        String className = c.getName().toString();
        String clsName =  className.replaceAll("[/]", ".").substring(1,  className.length());
        if(scope.isApplicationLoader(c.getClassLoader()) && !className.contains("support") && !clsName.contains(".R$"))
        {
          Report report = new Report(clsName);
          System.out.println("[wala] analyzing class " + clsName);
          Collection<IMethod> cIM = c.getDeclaredMethods();
          for(Iterator<IMethod> iIM = cIM.iterator(); iIM.hasNext();)
          {
            
              IMethod IM = iIM.next();
              String mName = IM.getName().toString();
              System.out.println("[wala] --analyzing method " + mName);
              /*if(mName.startsWith("on")){
                
              }
              else*/
              if(true)
              {
                Resource r = new Resource();
                AnalysisOptions option = new AnalysisOptions();
                option.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
                AnalysisCache cache = new AnalysisCache();
                IR ir = cache.getSSACache().findOrCreateIR(IM, Everywhere.EVERYWHERE, option.getSSAOptions());
              
                if(ir != null)
                {
                  SSACFG cfg = ir.getControlFlowGraph();
                  
                  SSAInstruction[] a = cfg.getInstructions();
                  for(int i = 0; i < a.length; i++)
                  {
                      if(a[i] != null && a[i].toString().contains("invoke"))
                      {
                        r.checkResource(a[i].toString());
                      }
                  }
                }
                //if(clsName.contains("ViewDragHelper") && mName.contains("processTouchEvent"))
                  //System.out.println("stop");
                
                ReportItem ri = r.gen_reportitem(IM.getSignature());
                if(ri != null)
                  report.items.add(ri);
              }
          }
          if(report.items.size() != 0)
          {
            results.add(report);
          }
          
          Report_whole report_w = new Report_whole(clsName);
          //if(!clsName.contains("info.guardianproject.mrapp.media.VideoCamera"))
            //continue;
          
          cIM = c.getDeclaredMethods();
          Resource r = new Resource();
          for(Iterator<IMethod> iIM = cIM.iterator(); iIM.hasNext();)
          {
            
              IMethod IM = iIM.next();
              String mName = IM.getName().toString();

              AnalysisOptions option = new AnalysisOptions();
              option.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
              AnalysisCache cache = new AnalysisCache();
              IR ir = cache.getSSACache().findOrCreateIR(IM, Everywhere.EVERYWHERE, option.getSSAOptions());
            
              if(ir != null)
              {
                SSACFG cfg = ir.getControlFlowGraph();
                
                SSAInstruction[] a = cfg.getInstructions();
                for(int i = 0; i < a.length; i++)
                {
                    if(a[i] != null && a[i].toString().contains("invoke"))
                    {
                      r.checkResource_w(a[i].toString());
                    }
                }
              }
                //if(clsName.contains("ViewDragHelper") && mName.contains("processTouchEvent"))
                  //System.out.println("stop");
                
               
          }
          ReportItem ri = r.gen_reportitem_w("Whole");
          if(ri != null)
            report_w.items.add(ri);
          
          if(report_w.items.size() != 0)
          {
            results_w.add(report_w);
          }
        }
      }
      

      //report final results
      System.out.println("***********************analysis results**********************");

      if(results.size() != 0){
        System.out.println("Detected " + results.size() + " violations.");

        for(Report r : results){
          System.out.println("Class name: " + r.clsName);

          for(ReportItem ri : r.items){
            System.out.println("--Handler name: " + ri.methodName);

            System.out.println("--Called resource API without release: ");

            for(ResourceAPI api : ri.targetAPIs){
              System.out.println("----" + api.acquire_clsName + " " + api.acquire_signature + " " + api.release_clsName + " " + api.release_signature);
            }
          }
          System.out.println();
        }
      } else{
        System.out.println("no violation detected!");
      }
          
      System.out.println("********************end of analysis results******************");
      
      //report final results
      System.out.println("***********************analysis results**********************");

      if(results_w.size() != 0){
        System.out.println("Detected " + results_w.size() + " violations.");

        for(Report_whole r : results_w){
          System.out.println("Class name: " + r.clsName);

          for(ReportItem ri : r.items){
            System.out.println("--Handler name: " + ri.methodName);

            System.out.println("--Called resource API without release: ");

            for(ResourceAPI api : ri.targetAPIs){
              System.out.println("----" + api.acquire_clsName + " " + api.acquire_signature + " " + api.release_clsName + " " + api.release_signature);
            }
          }
          System.out.println();
        }
      } else{
        System.out.println("no violation detected!");
      }
          
      System.out.println("********************end of analysis results******************");
      
      return null;
    } catch (Exception e) {
      System.out.println("[error] " + e.getMessage());
      return null;
    }
    
    
  }

  
  public static class Report{
    String clsName;
    ArrayList<ReportItem> items;
    public Report(String cls){
      this.clsName = cls;
      this.items = new ArrayList<ReportItem>();
    }
  }
  
  public static class ReportItem{
    String methodName;
    ArrayList<ResourceAPI> targetAPIs;
    public ReportItem(String m){
      this.methodName = m;
      this.targetAPIs = new ArrayList<ResourceAPI>();
    }
  }
  
  public static class Report_whole{
    String clsName;
    ArrayList<ReportItem> items;
    public Report_whole(String cls){
      this.clsName = cls;
      this.items = new ArrayList<ReportItem>();
    }
  }
  
  public static class ReportItem_whole{
    String methodName;
    ArrayList<ResourceAPI> targetAPIs;
    public ReportItem_whole(String m){
      this.methodName = m;
      this.targetAPIs = new ArrayList<ResourceAPI>();
    }
  }
}
