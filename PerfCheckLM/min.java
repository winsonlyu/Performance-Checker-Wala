package PerfCheckLM;

import java.io.IOException;
import java.util.Random;

public class min {

        public int mValue = 0;
 
        public int min(){
          int temp1 = 1;
          int temp2 = 2;
          if(temp1 > temp2)
            return temp1;
          else
            return temp2;
        }
        
        public int getValue_min() {
            
          return min();
          
            //return mValue;
        }
 
        public void setValue(int value) {
            this.mValue = value;
        }
    
   public void test() {
          final int count = 1000000;
   
          int[] values = new int[count];
          Random rand = new Random(System.nanoTime());
          for(int i=0; i<count; i++){
              values[i ] = rand.nextInt();
          }
   
   
          // 直接写入/读取
          int v = 0;
          long begin = System.currentTimeMillis();
          for(int i=0; i<count; i++){
              mValue = values[i ];
              v = mValue;
          }
          long end = System.currentTimeMillis();
          System.out.println("直接写入/读取：" + (end - begin)/1000.0 + "s");
   
          // 使用getter、setter写入/读取
          begin = System.currentTimeMillis();
          for(int i=0; i<count; i++){
              setValue(values[i ]);
              v = getValue_min();
          }
          end = System.currentTimeMillis();
          System.out.println("使用getter、setter写入/读取：" + (end - begin)/1000.0 + "s");
   
          // 空循环参照
          begin = System.currentTimeMillis();
          for(int i=0; i<count; i++){
          }
          end = System.currentTimeMillis();
          System.out.println("空循环参照：" + (end - begin)/1000.0 + "s");
          System.out.println("测试完成！");
      }
   
    public static void main(String[] arg)
    {
      min inline = new min();
      inline.test();
    }
}
