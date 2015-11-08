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
import java.util.jar.JarFile;

import org.eclipse.core.internal.utils.Queue;
import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.examples.drivers.LoopInfo.AllLoopInfo;
import com.ibm.wala.examples.drivers.CheckOnDraw.Report;
import com.ibm.wala.examples.drivers.CheckOnDraw.ReportItem;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.examples.drivers.PDFSDG;
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
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
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
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

public class CheckGetSet {

  public static void main(String[] args) throws WalaException, IOException {
    Properties p = CommandLine.parse(args);
    PDFCallGraph.validateCommandLine(p);
    run(p);
  }

  public static ApplicationWindow run(Properties p) throws WalaException, IOException {

    File f = new File("F:\\study\\UST\\FYP\\WALA\\result_test_loop.txt");
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

      ArrayList<Report> results = new ArrayList<Report>();
      
      for (IClass c : cha) {
        String className = c.getName().toString();
        ArrayList<String> getSetName = new ArrayList<String>();
        if (scope.isApplicationLoader(c.getClassLoader()) && !className.contains("support")) {
          String clsName = className.replaceAll("[/]", ".").substring(1, className.length());
          Collection<IMethod> cIM = c.getDeclaredMethods();
          System.out.println("Locating in Class: "+clsName);
          for (Iterator<IMethod> iIM = cIM.iterator(); iIM.hasNext();) {
            IMethod IM = iIM.next();
            if(!(IM.getName().toString().contains("get")||IM.getName().toString().contains("set")))
              continue;
            
            AnalysisOptions option = new AnalysisOptions();
            option.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
            AnalysisCache cache = new AnalysisCache();
            IR ir = cache.getSSACache().findOrCreateIR(IM, Everywhere.EVERYWHERE, option.getSSAOptions());

            if (ir == null)
              System.err.println(IM.getName());

            if (ir != null) {
              // System.out.println(IM.getName());
               if(IM.getName().toString().startsWith("get")||IM.getName().toString().startsWith("set"))
               {
              SSACFG cfg = ir.getControlFlowGraph();
              
              //Check if they are simple getters or setters
       //       if(cfg.getSuccNodeCount(cfg.entry())==1)
        //      {    

      //        }
                if(cfg.getNumberOfNodes()<10)
                {
                   boolean containCondition = false;
                   
              BasicBlock currentNode = cfg.entry();
              Queue<BasicBlock> q = new Queue<BasicBlock>();

              Vector<BasicBlock> processed = new Vector<BasicBlock>();

              q.add(currentNode);

              while (!q.isEmpty()) {
                // System.out.println("=======================Start " +
                // count + "=========================");

                currentNode = q.peek();
                processed.add(currentNode);
                q.remove();

                if (currentNode == null)
                  continue;

                Iterator<ISSABasicBlock> l = cfg.getSuccNodes(currentNode);
                
                while (l.hasNext()) {
                  BasicBlock bb = (BasicBlock) l.next();
                  if (!processed.contains(bb))
                    q.add(bb);

                  List<SSAInstruction> instructions = bb.getAllInstructions();

                  // System.out.println(instructions.size());
                  for (SSAInstruction i : instructions) {
                     
                       if(i.toString().contains("conditional branch"))
                         containCondition = true;
                    }
                  if(containCondition==true)
                    break;
                  }  
                if(containCondition==true)
                break;
                }
           
              //If there are less than 10 nodes and no conditional branch
              if(containCondition==false)
              { getSetName.add(IM.getSignature());
                System.out.println("Getter or Setter Found: "+ IM.getSignature());
                //System.out.println(cfg.getNumberOfNodes());
                }
                }           
                }
              }
           }
            if(getSetName.size()==0)
              continue;
            
            for(IMethod entryPoint : cIM)
            {
              //if(getSetName.contains(entryPoint.getSignature()))
              //  continue;
              
             System.out.println("[wala] checking entry point method " + entryPoint.getSignature());
             bw.write("[wala] checking entry point method " + entryPoint.getSignature() + "\n");
              boolean containGetSet = false;
              //Check all the methods in this class

              ArrayList<String> violationMethod = new ArrayList<String>();
              AnalysisOptions option = new AnalysisOptions();
              option.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
              AnalysisCache cache = new AnalysisCache();
              IR ir = cache.getSSACache().findOrCreateIR(entryPoint, Everywhere.EVERYWHERE, option.getSSAOptions());

              if (ir == null)
                System.err.println(entryPoint.getName());

              if (ir != null) {
                // System.out.println(IM.getName());
                // if(IM.getName().toString().equals("main"))
                // {
                SSACFG cfg = ir.getControlFlowGraph();
              BasicBlock currentNode = cfg.entry();
              Queue<BasicBlock> q = new Queue<BasicBlock>();

              Vector<BasicBlock> processed = new Vector<BasicBlock>();

              q.add(currentNode);

              while (!q.isEmpty()) {
                // System.out.println("=======================Start " +
                // count + "=========================");

                currentNode = q.peek();
                processed.add(currentNode);
                q.remove();

                if (currentNode == null)
                  continue;

                // Check if there is BeginTransation Statement inside the
                // same method with the SQLinsertion

                Iterator<ISSABasicBlock> l = cfg.getSuccNodes(currentNode);

                while (l.hasNext()) {
                  BasicBlock bb = (BasicBlock) l.next();
                  if (!processed.contains(bb))
                    q.add(bb);

                  List<SSAInstruction> instructions = bb.getAllInstructions();

                  // System.out.println(instructions.size());
                  for (SSAInstruction i : instructions) {     
                     //System.out.println("node num"+bb.getNumber()+"\n"+ i.toString());
                    if (i.toString().contains("invokevirtual")
                        && i.toString().contains("> 1")) {
                      
                      int index1 = i.toString().indexOf("<");
                      int index2 = i.toString().indexOf(">");
                      String temp = i.toString().substring(index1, index2+1);
                      int index3= temp.toString().indexOf(",");
                      int index4= temp.toString().indexOf(">");
                      String temp2 = temp.substring(index3+3, index4);
                      //.replaceAll("[/]", ".").replaceAll("[,]", ".")
                      int index5 = temp2.indexOf(",");
                      String temp3 = temp2.substring(0,index5).replaceAll("[/]", ".")+".";
                      String temp4 = temp2.substring(index5+2, temp2.length()-1);
                      String finalString = temp3+temp4;
                      System.out.println(finalString);

                      if(getSetName.contains(finalString))
                      {   
                        
                          containGetSet = true;
                          violationMethod.add(finalString);                          
                          
                      }
                    }
                  }
                }
              } 
              if (containGetSet == true) {
                // check if the report for clsName and entryPoint already
                // exists (for merging)
                boolean clsExists = false;
                for (Report report : results) {
                  if (report.clsName.equals(clsName)) {
                    // the report has already exist, check if the entryPoint
                    // already exists
                    boolean entryPointExists = false;
                    for (ReportItem ri : report.items) {
                      if (ri.methodName.equals(entryPoint.getSignature())) {
                        // the entrypoint also exists
                        for(String s:violationMethod)
                        {
                        if(!ri.targetAPIs.contains(s)){
                          ri.targetAPIs.add(s);
                        }
                        }
                        entryPointExists = true;
                        break;
                      }
                    }
                    if (!entryPointExists) {
                      // the report has already exists, but the entry point
                      // not exists
                      ReportItem ri = new ReportItem(entryPoint.getSignature());
                      for(String s:violationMethod)
                        ri.targetAPIs.add(s);
                      report.items.add(ri);
                    }
                    clsExists = true;
                    break;
                  }
                }
                if (!clsExists) {
                  Report r = new Report(clsName);
                  ReportItem ri = new ReportItem(entryPoint.getSignature());
                  for(String s:violationMethod)
                    ri.targetAPIs.add(s);
                  r.items.add(ri);              
                  results.add(r);
                }

              }

            
            }
          }
        }
        
          // System.out.println("End of Class " + c.getName());
        }
      
      // report final results
      System.out.println("***********************analysis results**********************");
      bw.write("***********************analysis results**********************\n");
      if (results.size() != 0) {
        System.out.println("Detected " + results.size() + " violations.");

        bw.write("Detected " + results.size() + " violations." + "\n");

        for (Report r : results) {
          System.out.println("Class name: " + r.clsName);
          bw.write("Class name: " + r.clsName + "\n");
          for (ReportItem ri : r.items) {
            System.out.println("--Handler name: " + ri.methodName);
            bw.write("--Handler name: " + ri.methodName + "\n");
            System.out.println("--Called inner Getter or Setter: ");
            bw.write("--Called inner Getter or Setter: \n");
            for(String api : ri.targetAPIs){
              System.out.println( api+ "\n");            
              bw.write( api + "\n"+ "\n");
            }
          }
          System.out.println();

          bw.write("\n");


        }
      } else {
        System.out.println("no violation detected!");
        bw.write("no violation detected!\n");
      }
      
      System.out.println("********************end of analysis results******************");
      
      System.out.println("Detected " + results.size() + " violations.");
      bw.write("********************end of analysis results******************");
      
      bw.close();
      return null;
    } catch (Exception e) {
      System.out.println("[error] " + e.getMessage());
      bw.close();
      return null;
    }

  }

}

