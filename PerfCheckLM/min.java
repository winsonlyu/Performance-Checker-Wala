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
   
   
          // ֱ��д��/��ȡ
          int v = 0;
          long begin = System.currentTimeMillis();
          for(int i=0; i<count; i++){
              mValue = values[i ];
              v = mValue;
          }
          long end = System.currentTimeMillis();
          System.out.println("ֱ��д��/��ȡ��" + (end - begin)/1000.0 + "s");
   
          // ʹ��getter��setterд��/��ȡ
          begin = System.currentTimeMillis();
          for(int i=0; i<count; i++){
              setValue(values[i ]);
              v = getValue_min();
          }
          end = System.currentTimeMillis();
          System.out.println("ʹ��getter��setterд��/��ȡ��" + (end - begin)/1000.0 + "s");
   
          // ��ѭ������
          begin = System.currentTimeMillis();
          for(int i=0; i<count; i++){
          }
          end = System.currentTimeMillis();
          System.out.println("��ѭ�����գ�" + (end - begin)/1000.0 + "s");
          System.out.println("������ɣ�");
      }
   
    public static void main(String[] arg)
    {
      min inline = new min();
      inline.test();
    }
}
