package PerfCheckLM;

import com.ibm.wala.classLoader.IMethod;

public class ResultAPI {
  IMethod IM;
  ExpensiveAPI api;
  
  public ResultAPI(IMethod IM, ExpensiveAPI api){
    this.IM = IM;
    this.api = api;
  }
}
