package com.ibm.wala.examples.drivers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntPair;

public class LoopInfo {
  private int loopHead;
  
  private int loopOutNode;
  
  private HashSet<Integer> loopBodyNodes = new HashSet<Integer>();
  
  private ArrayList<IntPair> backEdges = new ArrayList<IntPair> ();
  
  private ArrayList<LoopInfo> nestLoops = new ArrayList<LoopInfo>();
  public LoopInfo(int head ){
      this.loopHead = head;
  }
  
  public void setOutNode(int outNode){
      this.loopOutNode = outNode;
  }
  public int getOutNode(){
      return this.loopOutNode;
  }
  
  public int getHead(){
      return this.loopHead;
  }
  
  public ArrayList<LoopInfo> getNestLoops(){
      return this.nestLoops;
  }
  
  public void addNestLoop(LoopInfo li){
      nestLoops.add(li);
  }
  
  public HashSet<Integer> getBodyNodes(){
      return this.loopBodyNodes;
  }
  
  public ArrayList<IntPair> getBackEdges(){
      return this.backEdges;
  }
  
  public void addBodyNode(int node){
      this.loopBodyNodes.add(node);
  }
  
  public void addBackEdge(IntPair ip){
      this.backEdges.add(ip);
  }
  

  
  
  public String toString(){
      StringBuffer sb = new StringBuffer();
      sb.append("  == Loop == \n");
      sb.append("  Loop Head: "+this.loopHead+"\n");
      sb.append("  Loop Out Node: "+this.loopOutNode+"\n");
      sb.append("  Loop Body Nodes: ");
      for(Iterator<Integer> it = loopBodyNodes.iterator();it.hasNext();){
          sb.append(it.next()+" ");
      }
      sb.append("\n");
      sb.append("  BackEdge(s): ");
      for(int i=0;i<backEdges.size();i++){
          IntPair ip = backEdges.get(i);
          sb.append(ip.getX()+" -> "+ip.getY()+" ");
      }
      sb.append("\n");
      if(nestLoops.size()==0){
          sb.append("  Nest Loops: No nest loops\n");
      }else{
          sb.append("  Nest Loops: ***\n");
          for(int i=0;i<nestLoops.size();i++){
              sb.append(nestLoops.get(i));
          }
          sb.append("  End Nest Loops: ***\n");
      }
      sb.append("  End Loop === \n");
      
      return sb.toString();
  }
  
  static class AllLoopInfo{
    
    private HashMap<Integer, LoopInfo> loop = new HashMap<Integer, LoopInfo>();
    
    public Collection<Integer> getHeads(){
        return loop.keySet();
    }
    
    public Collection<LoopInfo> getLoops(){
        return loop.values();
    }
    
    public void addLoop(LoopInfo li){
        if(loop.containsKey(li.getHead())){
            //already contain this headNode, merge loop
            LoopInfo lOld = loop.get(li.getHead());
            //add back edge
            lOld.addBackEdge(li.getBackEdges().get(0));//without merge, every loop only has one back edge
            //add loop node
            for(Iterator<Integer> it = li.getBodyNodes().iterator();it.hasNext();){
                lOld.addBodyNode(it.next());
            }
        }else{
            loop.put(li.getHead(), li);
        }
    }
    
    //find Nest loops. simply compare the nodes they contained. 
    public void computeNestLoops(){
        for(Iterator<LoopInfo> it1 = getLoops().iterator();it1.hasNext();){
            LoopInfo out = it1.next();
            for(Iterator<LoopInfo> it2 = getLoops().iterator();it2.hasNext();){
                LoopInfo in = it2.next();
                if(out.getHead() == in.getHead()||out.getBodyNodes().size()<=in.getBodyNodes().size()){
                    //same loopinfo or out loop size <= inner loop size
                    //cannot be nest loop
                    continue;
                }
                if(out.getBodyNodes().containsAll(in.getBodyNodes())){
                    out.addNestLoop(in);
                }
            }
            
        }
    }

    public String toString(){
        
        StringBuffer sb = new StringBuffer();
        sb.append("===== All Loop ==========\n");
        for(Iterator<LoopInfo> it = getLoops().iterator();it.hasNext();){
             sb.append(it.next());
        }
        return sb.toString();
    }
    
  }
  

public static AllLoopInfo getAllLoopInfo(SSACFG ssacfg){
  //compute back edges in the cfg
  IBinaryNaturalRelation result=Acyclic.computeBackEdges(ssacfg,ssacfg.getBasicBlock(1));
  //create AllLoopInfo datastructure
  AllLoopInfo ali = new AllLoopInfo();
  
  //iterate back edges
  for(Iterator<IntPair> it = result.iterator();it.hasNext();){
    IntPair ip = it.next();
    int source = ip.getX();
    int dest = ip.getY();
    
    //new instance of loopInfo
    LoopInfo li = new LoopInfo(dest);
    
    //add back edge
    li.addBackEdge(ip);
    HashSet<Integer> hsBBloop = new HashSet<Integer>();
    
    //get Loop Nodes
    getLoopNode(source, dest, ssacfg, hsBBloop);

    //add nodes to LoopInfo
    for(Iterator<Integer> itt = hsBBloop.iterator();itt.hasNext();){
         li.addBodyNode(itt.next());
    }
    
    //compute Loop Out Node: the sucNode of Head, but not in the loop
    Collection<ISSABasicBlock> c1 = ssacfg.getNormalSuccessors(ssacfg.getBasicBlock(dest));
    for(Iterator<ISSABasicBlock> itBB = c1.iterator();itBB.hasNext();){
        ISSABasicBlock bb = itBB.next();
        if(!hsBBloop.contains(ssacfg.getNumber(bb))){
            li.setOutNode(ssacfg.getNumber(bb));
        }
        
    }

    //add loop into AllLoopInfo, will see if need merge or add new loopinfo to it. 
    ali.addLoop(li);
  }
  ali.computeNestLoops();
  return ali;
}

public static void getLoopNode(int backEdgeSource, int backEdgeDest, SSACFG ssacfg, HashSet<Integer> hs){
  hs.add(backEdgeSource);
  hs.add(backEdgeDest);
  Collection<ISSABasicBlock> c1 = ssacfg.getNormalPredecessors(ssacfg.getBasicBlock(backEdgeSource));
  for(Iterator<ISSABasicBlock> it = c1.iterator();it.hasNext();){
      int num = ssacfg.getNumber(it.next());
      if(!hs.contains(num)){
          hs.add(num);     
          getLoopNode(num,ssacfg,hs);
      }
  }
  
}

public static void getLoopNode(int currentBB, SSACFG ssacfg, HashSet<Integer> hs){
  Collection<ISSABasicBlock> c1 = ssacfg.getNormalPredecessors(ssacfg.getBasicBlock(currentBB));
  for(Iterator<ISSABasicBlock> it = c1.iterator();it.hasNext();){
      int num = ssacfg.getNumber(it.next());
      if(!hs.contains(num)){
          hs.add(num);
          getLoopNode(num,ssacfg,hs);
      }
  }
}
}


