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

public class CheckInsert {

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
        if (scope.isApplicationLoader(c.getClassLoader()) && !className.contains("support")) {
          String clsName = className.replaceAll("[/]", ".").substring(1, className.length());
          Collection<IMethod> cIM = c.getDeclaredMethods();
          // System.out.println(cIM.size());
          for (Iterator<IMethod> iIM = cIM.iterator(); iIM.hasNext();) {
            IMethod IM = iIM.next();

            AnalysisOptions option = new AnalysisOptions();
            option.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
            AnalysisCache cache = new AnalysisCache();
            IR ir = cache.getSSACache().findOrCreateIR(IM, Everywhere.EVERYWHERE, option.getSSAOptions());

            if (ir == null)
              System.err.println(IM.getName());

            if (ir != null) {
              // System.out.println(IM.getName());
              // if(IM.getName().toString().equals("main"))
              // {
             
              SSACFG cfg = ir.getControlFlowGraph();
              // check Loop with SQLInsert
              boolean containLoopAndInsert = false;
              // int LoopHead = 0;
              AllLoopInfo ali = LoopInfo.getAllLoopInfo(ir.getControlFlowGraph());
              for (Iterator<LoopInfo> it = ali.getLoops().iterator(); it.hasNext();) {
                LoopInfo next = it.next();
             
                // System.out.println(next);

                // LoopHead = next.getHead();
                for (Iterator<Integer> itt = next.getBodyNodes().iterator(); itt.hasNext();) {
                  BasicBlock bb = cfg.getBasicBlock(itt.next());
                  List<SSAInstruction> instructions = bb.getAllInstructions();
                  // System.out.println("Node Number:"+bb.getNumber());
                  for (SSAInstruction i : instructions) {
                   // System.out.println(i.toString());                   
                    if ((i.toString().contains("Landroid/database/sqlite/SQLiteDatabase") && i.toString().contains("insert"))||
                        (i.toString().contains("Landroid/database/sqlite/SQLiteStatement") && i.toString().contains("executeInsert"))
                        ) {
                      containLoopAndInsert = true;
                      System.out.println("Loop With Insertion Detected");
                    }
                  }

                }
                // System.out.println("Number of Nodes: " +
                // cfg.getNumberOfNodes());
            
                if (containLoopAndInsert == true) {

                  System.out.println("[wala] checking entry point method " + IM.getSignature());
                  bw.write("[wala] checking entry point method " + IM.getSignature() + "\n");


                  boolean isBatchInsert = false;
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
          
                      //   System.out.println("node num"+bb.getNumber()+"\n"+ i.toString());
                        if (i.toString().contains("Landroid/database/sqlite/SQLiteDatabase")
                            && i.toString().contains("beginTransaction")) {
                          isBatchInsert = true;
                     
                        }
                      }
                    }

                    // System.out.println("=======================End " + count
                    // + "=========================");

                  }
                  if (isBatchInsert == false) {
                    // check if the report for clsName and entryPoint already
                    // exists (for merging)                       
                    boolean clsExists = false;
                    for (Report report : results) {
                      if (report.clsName.equals(clsName)) {
                        // the report has already exist, check if the entryPoint
                        // already exists
                        boolean entryPointExists = false;
                        for (ReportItem ri : report.items) {
                          if (ri.methodName.equals(IM.getSignature())) {
                            // the entrypoint also exists

                            entryPointExists = true;
                            break;
                          }
                        }
                        if (!entryPointExists) {
                          // the report has already exists, but the entry point
                          // not exists
                          ReportItem ri = new ReportItem(IM.getSignature());
                          report.items.add(ri);
                        }
                        clsExists = true;
                        break;
                      }
                    }
                    if (!clsExists) {
                      Report r = new Report(clsName);
                      ReportItem ri = new ReportItem(IM.getSignature());
                      r.items.add(ri);
                      results.add(r);
                    }

                  }

                }
              }
            }
          }
          // System.out.println("End of Class " + c.getName());

        }
      }
      // report final results
      System.out.println("***********************analysis results**********************");
      bw.write("***********************analysis results**********************\n");
      if (results.size() != 0) {
        System.out.println("Detected " + results.size() + " direct insertion.");

        bw.write("Detected " + results.size() + " direct insertion." + "\n");

        for (Report r : results) {
          System.out.println("Class name: " + r.clsName);
          bw.write("Class name: " + r.clsName + "\n");
          for (ReportItem ri : r.items) {
            System.out.println("--Handler name: " + ri.methodName);
            bw.write("--Handler name: " + ri.methodName + "\n");
          }
          System.out.println();
          System.out.println("Suggest to use Batch Insert\n" + "Format:\n" + "db.beginTransaction();\n" + "try {\n" + "...\n"
              + "db.setTransactionSuccessful();\n" + "} finally {\n" + "db.endTransaction();}");
          bw.write("\n");
          bw.write("Suggest to use Batch Insert\n" + "Format:\n" + "db.beginTransaction();\n" + "try {\n" + "...\n"
              + "db.setTransactionSuccessful();\n" + "} finally {\n" + "db.endTransaction();}");

        }
      } else {
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

}
