package PerfCheckerRL;

public class ResourceAPI {
  String acquire_clsName;
  String acquire_signature;
  String release_clsName;
  String release_signature;
  boolean acquire;
  boolean relesee;
  int count;
  
  
  public ResourceAPI(String acquire_clsName, String acquire_signature, String release_clsName, String release_signature){
    this.acquire_clsName = acquire_clsName;
    this.release_clsName = release_clsName;
    this.acquire_signature = acquire_signature;
    this.release_signature = release_signature;
    this.count = 0;
    
    acquire = false;
    relesee = false;
  }
  
  public void setCount(int i)
  {
    count = i;
  }
  
  public int getCount()
  {
    return count;
  }
  
  public void match(String sig)
  {
    if(sig.contains(acquire_clsName) && sig.contains(acquire_signature))
        count++;
    else if(sig.contains(release_clsName) && sig.contains(release_signature))
        count--;
      
  }
  
  public void match_w(String sig)
  {
    if(sig.contains(acquire_clsName) && sig.contains(acquire_signature))
    {
      acquire = true;
    }
    else if(sig.contains(release_clsName) && sig.contains(release_signature))
    {
      relesee = true;
    }
      
  }
}
