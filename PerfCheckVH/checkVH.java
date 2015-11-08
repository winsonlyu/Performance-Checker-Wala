package PerfCheckVH;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.core.internal.utils.Queue;

import PerfCheckLM.Resource;

import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.examples.drivers.PDFControlDependenceGraph;
import com.ibm.wala.examples.drivers.PDFSDG;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewLauncher;
import com.ibm.wala.viz.PDFViewUtil;


public class checkVH {
  private static final String[] TARGET_CLASSES = {"android.widget.BaseAdapter", "android.widget.ListAdapter", "android.widget.SpinnerAdapter"}; 
  private static final String TARGET_METHOD = "getView";
  private static final String[] VIEW_INFLATION_APIS = {"inflate(ILandroid/view/ViewGroup;)Landroid/view/View"};
  private static final String[] VIEW_FINDING_APIS = {"findViewById(I)Landroid/view/View"}; 
  private static final boolean VERBOSE = false;
  
  /**
   * see {@link #main(String[])} for command-line arguments
   * @return 
   * 
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws IOException
   */
  public static void main(String[] args) throws WalaException, IllegalArgumentException, CancelException, IOException {
    // parse the command-line into a Properties object
    Properties p = CommandLine.parse(args);
    // validate that the command-line has the expected format
    validateCommandLine(p);

    // run the applications
    run(p.getProperty("appJar"));
    
  }

  /**
   * Should the slice be a backwards slice?
   */
  private static boolean goBackward(Properties p) {
    return !p.getProperty("dir", "backward").equals("forward");
  }
    
  /**
   * Compute a slice from a call statements, dot it, and fire off the PDF viewer to visualize the result
   * 
   * @param appJar should be something like "c:/temp/testdata/java_cup.jar"
   * @param mainClass should be something like "c:/temp/testdata/java_cup.jar"
   * @param srcCaller name of the method containing the statement of interest
   * @param srcCallee name of the method called by the statement of interest
   * @param goBackward do a backward slice?
   * @param dOptions options controlling data dependence
   * @param cOptions options controlling control dependence
   * @return a Process running the PDF viewer to visualize the dot'ted representation of the slice
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws WalaException 
   */
  public static Process run(String appJar) throws IllegalArgumentException, CancelException,
      IOException, WalaException {

      // create an analysis scope representing the appJar as a J2SE application
      AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, (new FileProvider())
          .getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      // build a class hierarchy, call graph, and system dependence graph
      ClassHierarchy cha = ClassHierarchy.make(scope);

      List<Report> reports = new ArrayList<checkVH.Report>();
    
    
      //now iterate and check each class
      for(IClass c: cha)
      {
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
            //System.out.println("[error ExceptionInInitializerError] " + e.getMessage());
            continue;
          }
          catch(NoClassDefFoundError e){
            //e.printStackTrace(System.err);
           // System.out.println("[error NoClassDefFoundError] " + e.getMessage());
            continue;
          }
          catch(ClassNotFoundException e){
            //System.out.println("[error ClassNotFoundException] " + e.getMessage());
            continue;
          }
          catch(ClassFormatError e){
            //System.out.println("[error ClassFormatError] " + e.getMessage());
            continue;
          }
          catch(NoSuchFieldError e){
            //System.out.println("[error UnsatisfiedLinkError] " + e.getMessage());
            continue;
          }
          catch(UnsatisfiedLinkError e){
            //System.out.println("[error UnsatisfiedLinkError] " + e.getMessage());
            continue;
          }
          catch(VerifyError e){
            //System.out.println("[error UnsatisfiedLinkError] " + e.getMessage());
            continue;
          }
          catch(NoSuchMethodError e){
            //System.out.println("[error UnsatisfiedLinkError] " + e.getMessage());
            continue;
          }
          catch(IncompatibleClassChangeError e){
            //System.out.println("[error UnsatisfiedLinkError] " + e.getMessage());
            continue;
          }
          catch(AssertionError e){
            //System.out.println("[error UnsatisfiedLinkError] " + e.getMessage());
            continue;
          }
          catch(Error e){
            //System.out.println("[error UnsatisfiedLinkError] " + e.getMessage());
            continue;
          }
          
          if(VERBOSE){
            System.out.println("[perf checker] finding class " + clsName);
          }
          if(cls.isEnum() || cls.isInterface()){
            if(VERBOSE){
              System.out.println("[perf checker] skipping interface/enum");
            }
            continue;
          }
 
          boolean interesting = false;
          
          Class superCls = cls;
          while(true){
            superCls = superCls.getSuperclass();
            if(superCls != null){
              //check if super class is a class that we are interested in
              interesting = matchTargetClasses(superCls.getName());
              if(interesting){
                break;
              }
            } else {
              break;
            }
          }
        
        if(interesting){
          //if(!c.getName().toString().contains("BookmarksAdapter"))
            //continue;
          System.out.println("[wala] analyzing class " + clsName);
          Report r = checkViewHolderViolation(c);
          if(r != null){
            reports.add(r);
            //System.out.println("VH violation found!");
          }
          
        } else{
          if(VERBOSE){
            System.out.println("[perf checker] skipping class " + clsName);
          }
        }
      } 
    }
    
    //report final results
    System.out.println("***********************analysis results**********************");
    int nViolations = 0;
    List<String> msgs = new ArrayList<String>();
    for(Report r : reports){
      if(r.violate){
        nViolations++;
        msgs.add(r.generateReport());
      }
    }
    if(nViolations != 0){
      System.out.println(nViolations + " violations detected\n");
      for(String msg : msgs){
        System.out.println(msg);
      }
    } else{
      System.out.println("no violation detected");
    }
    System.out.println("********************end of analysis results******************");
    
    return null;
  }
  
  /**
   * check if a class's super class belongs to the target classes list
   * @param clsName the class's super class name
   * @return true if yes, false otherwise
   */
  public static boolean matchTargetClasses(String clsName){
   // System.out.println(clsName);
    for(int i = 0; i < TARGET_CLASSES.length; i++){
      if(TARGET_CLASSES[i].equals(clsName)){
        return true;
      }
    }
    return false;
  }
  
  /**
   * this method check if an adapter class's implementation follows view holder pattern or not
   * @param sc
   * @return null if the analysis failed (e.g., CFG construction failed etc.), a report indicating if the view holder pattern is violated or not
   * @throws WalaException 
   */
  public static Report checkViewHolderViolation(IClass c) throws WalaException{
    Report report = null;
    
    Collection<IMethod> cIM = c.getAllMethods();
    
    IMethod target = null;
    for(Iterator<IMethod> iIM = cIM.iterator(); iIM.hasNext();)
    {
        IMethod IM = iIM.next();
        String mName = IM.getName().toString();
        //System.out.println(mName);
        if(mName.equals(TARGET_METHOD)){
          target = IM;
        }
    } 
    
    if(target != null){
      System.out.println("[wala] target method found, start anlaysis");
      
      //System.out.println(target.getSignature());
      AnalysisOptions option = new AnalysisOptions();
      option.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
      AnalysisCache cache = new AnalysisCache();
      IR ir = cache.getSSACache().findOrCreateIR(target, Everywhere.EVERYWHERE, option.getSSAOptions());
      
      if(ir == null)
        return report;
      
      //build CFG for the target method
      SSACFG cfg = ir.getControlFlowGraph();
      
      if(cfg.getNumberOfNodes() <= 4)
        return report;
      
      SSAInstruction[] a = cfg.getInstructions();
      for(int i = 0; i < a.length; i++)
      {
        if(a[i] != null)
          System.out.println(a[i].toString());
        
      }
      
      ControlDependenceGraph<SSAInstruction, ISSABasicBlock> cdg = new ControlDependenceGraph<SSAInstruction, ISSABasicBlock>(cfg);

      //System.out.println(cdg.getNumberOfNodes());

      
      //prepare report
      report = new Report();
      
      List<ISSABasicBlock> nullCheckPDGNodes = new ArrayList<ISSABasicBlock>();
      List<ISSABasicBlock> viewInflationPDGNodes = new ArrayList<ISSABasicBlock>();
      List<ISSABasicBlock> viewFindingPDGNodes = new ArrayList<ISSABasicBlock>();
      
      
      //find all null check PDG nodes
      Iterator<ISSABasicBlock> iter = cdg.iterator();
     
      while(iter.hasNext())
      {
        ISSABasicBlock next = iter.next();
        //System.out.println(next);
        Iterator<SSAInstruction> iter2 = next.iterator();
        while(iter2.hasNext())
        {
          SSAInstruction nextIns = iter2.next();
          //conditional branch(eq) v3,v10:#null
          //System.out.println(nextIns.getDef() + " " + nextIns);
          if(nextIns.toString().contains("conditional branch(ne)") || nextIns.toString().contains("conditional branch(eq)"))
          {
            nullCheckPDGNodes.add(next);
            break;
          }
          
          for(String inflationAPI : VIEW_INFLATION_APIS){
            //view inflation api
            if(nextIns.toString().contains(inflationAPI)){
              viewInflationPDGNodes.add(next);
            }
          }
          
          for(String findingAPI : VIEW_FINDING_APIS){
            //view finding api
            if(nextIns.toString().contains(findingAPI)){
              viewFindingPDGNodes.add(next);
            }
          }
        }
      }     

      //System.out.println("Null checker: " + nullCheckPDGNodes.size() );
      //System.out.println("viewInflation checker: " + viewInflationPDGNodes.size() );
      //System.out.println("viewFinding checker: " + viewFindingPDGNodes.size() );
      
      if(nullCheckPDGNodes.size() == 0){
        
        System.out.println(c.getName());
        //the getView method did not utilize the recycled view
        if(!report.violate){
          report.violate = true;
        }
        report.methodName = c.getName() + " " + target.getSignature();
        if(report.recycledViewUsed){
          report.recycledViewUsed = false;
        }
      }
      else{
        
        //first, check if view inflation is control dependent on the null checking of recycled view
        for(ISSABasicBlock viewInflationNode : viewInflationPDGNodes){
          boolean dependent = false;
          
          for(ISSABasicBlock nullCheckNode : nullCheckPDGNodes){
            Vector<ISSABasicBlock> reachable = new Vector<ISSABasicBlock>();
            Queue<ISSABasicBlock> q = new Queue<ISSABasicBlock>();
            q.add(nullCheckNode);
          
            while(!q.isEmpty()){
              ISSABasicBlock node = q.peek();
              q.remove();
              
              Iterator<ISSABasicBlock> temp = cdg.getSuccNodes(node);
              while(temp.hasNext())
              {
                ISSABasicBlock next = temp.next();
                if(!reachable.contains(next))
                {
                  reachable.add(next);
                  q.add(next);
                }
              }
            }
          
            if(reachable.contains(viewInflationNode))
            {
              dependent = true;
              break;
            }
            
            if(!dependent){
              if(!report.violate){
                report.violate = true;
                if(report.methodName == null){
                  report.methodName = c.getName() + " " + target.getSignature();
                }
                if(report.conditionalViewInflation){
                  report.conditionalViewInflation = false;
                }
              }
            }
           }
        }
        
        //second, check if view finding is control dependent on the null checking of recycled view
        for(ISSABasicBlock viewFindingNode : viewFindingPDGNodes){
          //if the viewFindingNode is control dependent on any null checking node of recycled view, the pattern is not violated
          //otherwise the pattern is violated
          boolean dependent = false;
          for(ISSABasicBlock nullCheckNode : nullCheckPDGNodes){
            Vector<ISSABasicBlock> reachable = new Vector<ISSABasicBlock>();
            Queue<ISSABasicBlock> q = new Queue<ISSABasicBlock>();
            q.add(nullCheckNode);
          
            while(!q.isEmpty()){
              ISSABasicBlock node = q.peek();
              q.remove();
              
              Iterator<ISSABasicBlock> temp = cdg.getSuccNodes(node);
              while(temp.hasNext())
              {
                ISSABasicBlock next = temp.next();
                if(!reachable.contains(next))
                {
                  reachable.add(next);
                  q.add(next);
                }
              }
            }
          
            if(reachable.contains(viewFindingNode))
            {
              dependent = true;
              break;
            }
          }
          
          if(!dependent){
            if(!report.violate){
              report.violate = true;
              if(report.methodName == null){
                report.methodName = c.getName() + " " + target.getSignature();
              }
              if(report.conditionalViewFinding){
                report.conditionalViewFinding = false;
              }
            }
          }
        }
      }

      /*
      Properties wp = null;
      try {
        wp = WalaProperties.loadProperties();
        wp.putAll(WalaExamplesProperties.loadProperties());
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFControlDependenceGraph.PDF_FILE;
      String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar
          + PDFTypeHierarchy.DOT_FILE;
      String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
      String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);
      
      DotUtil.dotify(cdg, PDFViewUtil.makeIRDecorator(ir), dotFile, psFile, dotExe);

      PDFViewUtil.launchPDFView(psFile, gvExe);*/
      
      }
    return report;
  }
  
  public static class Report{
    static final String MSG_RECYCLE_VIEW_NOT_USED = "Recycled view is not used";
    static final String MSG_UNCONDITIONAL_VIEW_INFLATION = "View inflation is not conditional (by checking if the recycled view can be utilized)";
    static final String MSG_UNCONDITIONAL_VIEW_FINDING = "View finding is not conditional (by checking if the recycled view can be utilized)";
    boolean violate = false;
    String methodName = null;
    boolean recycledViewUsed = true;
    boolean conditionalViewInflation = true;
    boolean conditionalViewFinding = true;
    
    public String generateReport(){
      if(violate){
        StringBuilder sb = new StringBuilder();
        sb.append("inefficient method: " + this.methodName + "\n");
        if(!recycledViewUsed){
          sb.append("----" + MSG_RECYCLE_VIEW_NOT_USED + "\n");
        }
        if(!conditionalViewInflation){
          sb.append("----" + MSG_UNCONDITIONAL_VIEW_INFLATION + "\n");
        }
        if(!conditionalViewFinding){
          sb.append("----" + MSG_UNCONDITIONAL_VIEW_FINDING + "\n");
        }
        sb.append("\n");
        return sb.toString();
      } else{
        return null;
      }
    }
  }
  
  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage:
   * <ul>
   * <li>args[0] : "-appJar"
   * <li>args[1] : something like "c:/temp/testdata/java_cup.jar"
   * <li>args[2] : "-mainClass"
   * <li>args[3] : something like "Lslice/TestRecursion" *
   * <li>args[4] : "-srcCallee"
   * <li>args[5] : something like "print" *
   * <li>args[4] : "-srcCaller"
   * <li>args[5] : something like "main"
   * </ul>
   * 
   * @throws UnsupportedOperationException if command-line is malformed.
   */
  static void validateCommandLine(Properties p) {
    if (p.get("appJar") == null) {
      throw new UnsupportedOperationException("expected command-line to include -appJar");
    }

  }
  
}
