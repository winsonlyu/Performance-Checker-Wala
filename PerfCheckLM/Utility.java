package PerfCheckLM;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.strings.Atom;

public class Utility {

  /**
   * check if a class's super class belongs to the target classes list
   * @param clsName the class's super class name
   * @return true if yes, false otherwise
   */
  public static boolean isAnonymousInnerClass(String clsName){
    if(clsName.contains("$")){
      String[] splits = clsName.split("\\$");
      String end = splits[splits.length-1];
      try{
        Integer.parseInt(end);
        return true;
      } catch(java.lang.NumberFormatException e){
        return false;
      }
    }
    return false;
  }
  
  /**
   * Check if a method is an expensive api call
   * @param signature
   * @return true if yes, false otherwise
   */
  public static boolean matchTargetAPI(IMethod m){
    for(ExpensiveAPI api : Resource.targetAPIs){
      if(api.signature.equals(m.getName().toString()) && m.getSignature().contains(api.clsName)){
        return true;
      }
    }
    return false;
  }
  
  public static ResultAPI matchTargetAPI_CFG(IMethod IM){
    if(IM.getDeclaringClass().getClassLoader().getReference().getName().equals(Atom.findOrCreateUnicodeAtom("Primordial")))
       return null;
    
    AnalysisOptions option = new AnalysisOptions();
    option.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
    AnalysisCache cache = new AnalysisCache();
    IR ir = cache.getSSACache().findOrCreateIR(IM, Everywhere.EVERYWHERE, option.getSSAOptions());
    
    if(ir == null)
      return null;
    
    SSACFG cfg = ir.getControlFlowGraph();
    
    SSAInstruction[] a = cfg.getInstructions();
    for(int i = 0; i < a.length; i++)
    {
      if(a[i] != null && a[i].toString().contains("invoke"))
      {
        for(ExpensiveAPI api : Resource.targetAPIs){
          String s = api.clsName;
          s = s.replaceAll("[.]", "/");
          if(a[i].toString().contains(s) &&a[i].toString().contains(api.signature)){
            return new ResultAPI(IM,api);
          }
        }
      }
    }
    return null;
  }
}
