package com.octopus.utils.alone;

/**
 * Number88就是88进制的数字,只有正数
 * User: wangfeng2
 * Date: 14-8-8
 * Time: 上午11:55
 */
public class Number62 {
    char[] cs;
    public static char CHAR_START=48;
    final static char[] Num88= {
            48,49,50,51,52,53,54,55,56,57,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122
    };
    public static void main(String[] args){
        for(char c:Num88)
            System.out.println(((int)c)+"-"+(c));
    }
    public Number62(){
        cs=new char[]{CHAR_START};
    }
    public synchronized String increase(){
        cs = increase(cs);
        return new String(cs);
    }
    public static char[] increase(char[] num){
        int i;
        for(i=num.length-1;i>=0;i--){
            if(num[i]==Num88[Num88.length-1]){
                num[i]=Num88[0];
            }else{
                num[i]=(char)(num[i]+1);
                break;
            }
        }
        if(i<0){
            char[] ret = new char[num.length+1];
            System.arraycopy(num,0,ret,1,num.length);
            ret[0]=Num88[1];
            return ret;
        }
        return num;
    }

    public static char[] decrease(char[] num){
        int i;
        for(i=num.length-1;i>=0;i--){
            if(num[i]==Num88[0]){
                num[i]=Num88[Num88.length-1];
            }else{
                num[i]=(char)(num[i]-1);
                break;
            }
        }
        if(num[0]==Num88[0]){
            char[] ret = new char[num.length-1];
            System.arraycopy(num,1,ret,0,num.length-1);
            return ret;
        }
        if(i<0){
            return new char[]{Num88[0]};
        }
        return num;
    }

    public static long get10Number(char[] num){
        long ret =0;
        long c = 1;
        for(int i=num.length-1;i>=0;i--){
            ret +=(num[i]-Num88[0])*c;
            c*=94;
        }
        return ret;
    }

}
