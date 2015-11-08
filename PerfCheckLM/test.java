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

import java.io.File;
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

/**
 * 
 * This application is a WALA client: it invokes an SWT TreeViewer to visualize
 * a Call Graph
 * 
 * @author sfink
 */
public class test {
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
   */
  public static void main(String[] args) throws WalaException {
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
   */
  public static ApplicationWindow run(Properties p) throws WalaException {

    try {
      String appJar = p.getProperty("appJar");
      if (PDFCallGraph.isDirectory(appJar)) {
        appJar = PDFCallGraph.findJarFiles(new String[] { appJar });
      }

      String exclusionFile = p.getProperty("exclusions");

      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, exclusionFile != null ? new File(exclusionFile)
          : (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      ClassHierarchy cha = ClassHierarchy.make(scope);

      DataDependenceOptions dOptions = PDFSDG.getDataDependenceOptions(p);
      ControlDependenceOptions cOptions = PDFSDG.getControlDependenceOptions(p);
   // find entry point onCreate in MainActivity
      /*
      IClass c2 = cha.lookupClass(TypeReference.findOrCreate(
          ClassLoaderReference.Application,
          "Linfo/staticfree/android/twentyfourhour/Analog24HClock"));
      List<Entrypoint> entries = new ArrayList<Entrypoint>();
      Atom atom = Atom.findOrCreateUnicodeAtom("onSizeChanged");
      Collection<IMethod> allMethods = c2.getAllMethods();
      for (IMethod m : allMethods) {
        if (m.getName().equals(atom)
            && (m.getDeclaringClass().getClassLoader().getReference()
                .getName().equals(Atom
                .findOrCreateUnicodeAtom("Application")))) {
          entries.add(new DefaultEntrypoint(m, cha));
        }
      }
      AnalysisOptions options = new AnalysisOptions(scope, entries);
      */
      
      Iterable<Entrypoint> entrypoints = null;
      JarFile jar = new JarFile(appJar);
      if (jar.getManifest() != null) {
        String mainClass = jar.getManifest().getMainAttributes().getValue("Main-Class");
        if (mainClass != null) {
          entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "L" + mainClass.replace('.', '/'));
        }
      }
      if (entrypoints == null) {
        entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
      }
      
      AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
      options.setReflectionOptions(ReflectionOptions.ONE_FLOW_TO_CASTS_NO_METHOD_INVOKE);
      
      // //
      // build the call graph
      // //
      CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
      //com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null, null);
      CallGraph cg = builder.makeCallGraph(options,null);

      SDG sdg = new SDG(cg, builder.getPointerAnalysis(), dOptions, cOptions);
      
   // find the call statement of interest
      CGNode callerNode = SlicerTest.findMethod(cg, "slice");
      Statement s = SlicerTest.findCallTo(callerNode, "add");
      System.err.println("Statement: " + s);
      
   // compute the slice as a collection of statements
      Collection<Statement> slice = null;
      if (true) {
        slice = Slicer.computeBackwardSlice(s, cg, builder.getPointerAnalysis(), dOptions, cOptions);
      } else {
        // for forward slices ... we actually slice from the return value of
        // calls.
        s = getReturnStatementForCall(s);
        System.out.println("Statement: " + s);
        slice = Slicer.computeForwardSlice(s, cg, builder.getPointerAnalysis(), dOptions, cOptions);
      }
      SlicerTest.dumpSlice(slice);

      // create a view of the SDG restricted to nodes in the slice
      Graph<Statement> g = pruneSDG(sdg, slice);

      System.out.println("Number of Nodes: " + g.getNumberOfNodes());
      /*
      for(Entrypoint entryPoint : entrypoints){
        IMethod m = entryPoint.getMethod();
        Set<CGNode> candidates = cg.getNodes(m.getReference());
        
        ArrayList<CGNode> reachable = new ArrayList<CGNode>();
        CGNode start = null;
        Iterator iter = candidates.iterator();
        Queue<CGNode> q = new Queue<CGNode>();
        while (iter.hasNext()) {
          start = (CGNode) iter.next();
          q.add(start);
        }
        
        while(!q.isEmpty()){
          CGNode node = q.peek();
          q.remove();
          
          Iterator<CGNode> temp = cg.getSuccNodes(node);
          while(temp.hasNext())
          {
            CGNode next = temp.next();
            if(!reachable.contains(next))
            {
              String s = next.getMethod().getName().toString();
              reachable.add(next);
              q.add(next);
            }
          }
        }
        
        CGNode target = null;
        for(CGNode node: reachable) {
          if(node.getMethod().getName().toString().equals("openConnection"))
            target = node;
        }
        
        while(target != start)
        {
          System.out.println(target.getMethod().getSignature());

            Iterator<CGNode> temp = cg.getPredNodes(target);
            target=temp.next();

        }
      }
      
      */
      
     // System.out.println(CallGraphStats.getStats(cg));
      /*
      int numberOfClass = 0;
      
      for(IClass c: cha){
        String className = c.getName().toString();
        if(scope.isApplicationLoader(c.getClassLoader()) && !className.contains("support"))
        {
          numberOfClass++;
          Collection<IMethod> cIM = c.getDeclaredMethods();
          //System.out.println(cIM.size());
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
                  System.out.println(IM.getName());
                  if(IM.getName().toString().equals("test"))
                  {
                    SSACFG cfg = ir.getControlFlowGraph();
                    System.out.println("Number of Nodes: " + cfg.getNumberOfNodes());
                    
                    BasicBlock currentNode = cfg.entry();
                    Queue<BasicBlock> q = new Queue<BasicBlock>();
                    
                    Vector<BasicBlock> processed = new Vector<BasicBlock>();
                    
                    q.add(currentNode);
                    
                    int count = 1;
                    while(!q.isEmpty())
                    {
                      System.out.println("=======================Start " + count + "=========================");
                      
                      currentNode = q.peek();
                      processed.add(currentNode);
                      q.remove();
                      
                      if(currentNode == null)
                        continue;
                      
                      Iterator<ISSABasicBlock> l = cfg.getSuccNodes(currentNode);
                      
                      while(l.hasNext())
                      {
                        BasicBlock bb = (BasicBlock) l.next();
                        if(!processed.contains(bb))
                          q.add(bb);

                            
                        
                        List<SSAInstruction> instructions = bb.getAllInstructions();
                        
                        System.out.println(instructions.size());
                        for(SSAInstruction i: instructions)
                        {
            
                          System.out.println(i.toString());
                        }
                      }
                      
                      System.out.println("=======================End " + count + "=========================");
                      count++;
                    }
                  }
              }
          }
        
          System.out.println("End of Class " + c.getName());
          
        }
      }
      System.out.println("number of classes:" + numberOfClass);
      */
      
      if (CHECK_GRAPH) {
        GraphIntegrity.check(cg);
      }

      Properties wp = null;
      try {
        wp = WalaProperties.loadProperties();
        wp.putAll(WalaExamplesProperties.loadProperties());
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFWalaIR.PDF_FILE;
      String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
      String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
      String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);

      // create and run the viewer
      final SWTTreeViewer v = new SWTTreeViewer();
      v.setGraphInput(cg);
      v.setRootsInput(InferGraphRoots.inferRoots(cg));
      v.getPopUpActions().add(new ViewIRAction(v, cg, psFile, dotFile, dotExe, gvExe));
      v.run();
      return v.getApplicationWindow();

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  /**
   * If s is a call statement, return the statement representing the normal return from s
   */
  public static Statement getReturnStatementForCall(Statement s) {
    if (s.getKind() == Kind.NORMAL) {
      NormalStatement n = (NormalStatement) s;
      SSAInstruction st = n.getInstruction();
      if (st instanceof SSAInvokeInstruction) {
        SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) st;
        if (call.getCallSite().getDeclaredTarget().getReturnType().equals(TypeReference.Void)) {
          throw new IllegalArgumentException("this driver computes forward slices from the return value of calls.\n" + ""
              + "Method " + call.getCallSite().getDeclaredTarget().getSignature() + " returns void.");
        }
        return new NormalReturnCaller(s.getNode(), n.getInstructionIndex());
      } else {
        return s;
      }
    } else {
      return s;
    }
  }
  
  /**
   * return a view of the sdg restricted to the statements in the slice
   */
  public static Graph<Statement> pruneSDG(SDG sdg, final Collection<Statement> slice) {
    Filter<Statement> f = new Filter<Statement>() {
      @Override
      public boolean accepts(Statement o) {
        return slice.contains(o);
      }
    };
    return GraphSlicer.prune(sdg, f);
  }
}
