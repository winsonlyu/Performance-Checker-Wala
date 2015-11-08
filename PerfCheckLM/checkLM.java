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
package PerfCheckLM;

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
public class checkLM {

  private final static boolean CHECK_GRAPH = false;
  private static ArrayList<IClass> workList = new ArrayList<IClass>();
  private static ArrayList<IMethod> entryPoints = new ArrayList<IMethod>();
  
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
      
      int numberOfClass = 0;
      ArrayList<Report> results = new ArrayList<Report>();
      
      Iterator<IClass> ite = cha.iterator();
      while(ite.hasNext()){
        IClass c = ite.next();
        String className = c.getName().toString();
        if(scope.isApplicationLoader(c.getClassLoader()) && !className.contains("support") /*&& !className.contains("overlay")*/)
        {
          String clsName = className.replaceAll("[/]", ".").substring(1,  className.length());
          //System.out.println(clsName);
          Class cls = null;
          try{
            cls = Class.forName(clsName);
          
          }
          catch(ExceptionInInitializerError e){
            System.out.println("[error ExceptionInInitializerError] " + e.getMessage());
            continue;
          }
          catch(NoClassDefFoundError e){
            //e.printStackTrace(System.err);
            System.out.println("[error NoClassDefFoundError] " + e.getMessage());
            continue;
          }
          catch(ClassNotFoundException e){
            System.out.println("[error ClassNotFoundException] " + e.getMessage());
            continue;
          }
          catch(ClassFormatError e){
            System.out.println("[error ClassFormatError] " + e.getMessage());
            continue;
          }
          catch(UnsatisfiedLinkError e){
            System.out.println("[error UnsatisfiedLinkError] " + e.getMessage());
            continue;
          }
          catch(VerifyError e){
            System.out.println("[error VerifyError] " + e.getMessage());
            continue;
          }
          catch(NoSuchFieldError e){
            System.out.println("[error NoSuchFieldError] " + e.getMessage());
            continue;
          }
          catch(NoSuchMethodError e){
            System.out.println("[error NoSuchMethodError] " + e.getMessage());
            continue;
          }
          catch(IncompatibleClassChangeError e){
            System.out.println("[error IncompatibleClassChangeError] " + e.getMessage());
            continue;
          }
          
          if(Resource.VERBOSE){
            System.out.println("[perf checker] finding class " + clsName);
          }
          if(cls.isEnum() || cls.isInterface()){
            if(Resource.VERBOSE){
              System.out.println("[perf checker] skipping interface/enum");
            }
            continue;
          }
          numberOfClass++;
          
          boolean interesting = false;
          
          Class superCls = cls;
          while(true){
            superCls = superCls.getSuperclass();
            if(superCls != null){
              //check if super class is a class that we are interested in
              interesting = Resource.targetCls.contains(superCls.getName());
              if(interesting){
                break;
              }
            } else {
              break;
            }
          }
          
          if(interesting){
          //if(true){
            System.out.println("[wala] located class " + clsName);
            bw.write("[wala] located class " + clsName + "\n");
            workList.add(c);
          } else{
            if(Resource.VERBOSE){
              System.out.println("[perf checker] skipping class " + clsName);
            }
          }
        }
      }
      
      for(IClass c : workList){
        String className = c.getName().toString();
        String clsName =  className.replaceAll("[/]", ".").substring(1,  className.length());
        System.out.println("[wala] analyzing class " + clsName);
        bw.write("[wala] analyzing class " + clsName + "\n");
        
        ArrayList<IMethod> entryPoints = new ArrayList<IMethod>();
        
        if(clsName.contains("LiveMeetingActivity") || clsName.contains("AttendingMeetingActivity"))
          continue;
        
        Collection<IMethod> cIM = c.getDeclaredMethods();
        //System.out.println(cIM.size());
        for(Iterator<IMethod> iIM = cIM.iterator(); iIM.hasNext();)
        {
            IMethod IM = iIM.next();
            String mName = IM.getName().toString();
            if(mName.startsWith("on")){
              entryPoints.add(IM);
            }
        } 
        
      //check for each entry point to see if they transitively call expensive apis
        for(IMethod entryPoint : entryPoints){
          System.out.println("[wala] checking entry point method " + entryPoint.getSignature());
          bw.write("[wala] checking entry point method " + entryPoint.getSignature() + "\n");
          List<Entrypoint> entries = new ArrayList<Entrypoint>();
          entries.add(new DefaultEntrypoint(entryPoint, cha));

          AnalysisOptions options = new AnalysisOptions(scope, entries);
          // //
          // build the call graph
          // //
          com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null,
              null);
          CallGraph cg = builder.makeCallGraph(options,null);
          
          boolean flag = false;
          if(entryPoint.getName().toString().contains("onCreate") && c.getName().toString().contains("LiveMeetingActivity"))
          {
            System.out.println("break");
            flag = true;
          }
          Set<CGNode> candidates = cg.getNodes(entryPoint.getReference());
          ArrayList<CGNode> reachable = new ArrayList<CGNode>();
          
          Iterator iter = candidates.iterator();
          Queue<CGNode> q = new Queue<CGNode>();
          while (iter.hasNext()) {
            q.add((CGNode) iter.next());
          }
          
          while(!q.isEmpty()){
            CGNode node = q.peek();
            q.remove();
            
            if(flag)
              System.out.println(node.toString());
            
            Iterator<CGNode> temp = cg.getSuccNodes(node);
            while(temp.hasNext())
            {
              CGNode next = temp.next();
              String s = next.getMethod().getName().toString();
              
              if(s.contains("java.util.Date") && s.contains("getTime"))
                continue;
              if(!reachable.contains(next))
              {
                
                reachable.add(next);
                //if(next.getMethod().getDeclaringClass().getClassLoader().getReference().getName().equals(Atom.findOrCreateUnicodeAtom("Application")))
                  //continue;
                q.add(next);
              }
            }
          }
          
          for(CGNode node: reachable) {
            if(Utility.matchTargetAPI(node.getMethod())){
              //check if the report for clsName and entryPoint already exists (for merging)
              boolean clsExists = false;
              for(Report report : results){
                if(report.clsName.equals(clsName)){
                  //the report has already exist, check if the entryPoint already exists
                  boolean entryPointExists = false;
                  for(ReportItem ri : report.items){
                    if(ri.methodName.equals(entryPoint.getSignature())){
                      //the entrypoint also exists
                      if(!ri.targetAPIs.contains(node.getMethod().getSignature())){
                        ri.targetAPIs.add(node.getMethod().getSignature());
                      }
                      entryPointExists = true;
                      break;
                    }
                  }
                  if(!entryPointExists){
                    //the report has already exists, but the entry point not exists
                    ReportItem ri = new ReportItem(entryPoint.getSignature());
                    ri.targetAPIs.add(node.getMethod().getSignature());
                    report.items.add(ri);
                  }
                  clsExists = true;
                  break;
                }
              }
              if(!clsExists){
                Report r = new Report(clsName);
                ReportItem ri = new ReportItem(entryPoint.getSignature());
                ri.targetAPIs.add(node.getMethod().getSignature());
                r.items.add(ri);
                results.add(r);
              }
            }
          }
          /*
          if(clsName.contains("TintBrowserActivity") && entryPoint.getName().toString().contains("onCreate")){
            CGNode target = null;
            CGNode start = candidates.iterator().next();
            for(CGNode node: reachable) {
              if(node.getMethod().getName().toString().equals("openStream"))
                target = node;
            }
            
            while(target != start)
            {
              System.out.println(target.getMethod().getSignature());
  
                Iterator<CGNode> temp = cg.getPredNodes(target);
                
                target=temp.next();
  
            }
          }*/
        }
      }

      //report final results
      System.out.println("***********************analysis results**********************");
      bw.write("***********************analysis results**********************\n");
      if(results.size() != 0){
        System.out.println("Detected " + results.size() + " violations.");
        bw.write("Detected " + results.size() + " violations." + "\n");
        for(Report r : results){
          System.out.println("Class name: " + r.clsName);
          bw.write("Class name: " + r.clsName + "\n");
          for(ReportItem ri : r.items){
            System.out.println("--Handler name: " + ri.methodName);
            bw.write("--Handler name: " + ri.methodName + "\n");
            System.out.println("--Called expensive API: ");
            bw.write("--Called expensive API: \n");
            for(String api : ri.targetAPIs){
              System.out.println("----" + api);
              bw.write("----" + api + "\n");
            }
          }
          System.out.println();
        }
      } else{
        System.out.println("no violation detected!");
        bw.write("no violation detected!\n");
      }
          
      System.out.println("********************end of analysis results******************");
      bw.write("********************end of analysis results******************");
      
      bw.close();
      return null;
    } catch (Exception e) {
      System.out.println("[error] " + e.getMessage());
      bw.close();
      return null;
    }
    
    
  }
  
  public static class WorkItem{
    String clsName;
    ArrayList<IMethod> entryPoints;
    public WorkItem(String clsName, ArrayList<IMethod> entries){
      this.clsName = clsName;
      this.entryPoints = entries;
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
    ArrayList<String> targetAPIs;
    public ReportItem(String m){
      this.methodName = m;
      this.targetAPIs = new ArrayList<String>();
    }
  }
}
