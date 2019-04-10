package com.octopus.utils.ds;

import java.util.Arrays;

/**
 * User: wfgao_000
 * Date: 15-6-11
 * Time: 上午10:43
 */
public class QuickStringKeyMap{
    String[] keys =null;
    Object[] values = null;
    int[] kh=null;
    int size=0;
    int top;

    public QuickStringKeyMap(){
        keys=new String[128];
        values=new Object[128];
        kh=new int[128];
    }
    public synchronized Object get(String key){
        int h = key.hashCode();
        for(int i=0;i<kh.length;i++){
           if(kh[i]==h){
               return values[i];
           }

        }
        return null;

    }
    public boolean containsKey(String key){
        int h = key.hashCode();
        for(int i=0;i<kh.length;i++){
            if(kh[i]==h){
                return true;
            }

        }
        return false;
    }
    public void put(String key,Object value){
            int h = key.hashCode();
            int n=-1;
            int ni=-1;
            for(int i=0;i<kh.length;i++){
                if(ni==-1 && kh[i]==0){
                    ni=i;
                    if(ni>top) break;
                }
                if(kh[i]==h){
                    n=i;
                    values[i]=value;
                    break;
                }

            }
            if(n==-1){
                if(ni>-1){
                    kh[ni]=h;
                    keys[ni]=key;
                    values[ni]=value;
                    size++;
                    if(ni>top)
                        top=ni;
                }else{
                    top=kh.length;
                    keys=extend(keys,128);
                    values=extend(values,128);
                    kh=extend(kh, 128);
                    kh[top]=h;
                    keys[top]=key;
                    values[top]=value;
                    size++;
                }
            }

    }
    public synchronized boolean remove(String key){
            int h = key.hashCode();
            for(int i=0;i<kh.length;i++){
                if(kh[i]==h){
                    keys[i]=null;
                    values[i]=null;
                    kh[i]=0;
                    size--;
                    return true;
                }

            }
            return false;

    }
    public int size(){
        return size;
    }
    public synchronized String[] keys(){

            String[] ret = new String[size];
            int n =0;
            for(int i=0;i<kh.length;i++){
                if(kh[i]!=0){
                   ret[n++]=keys[i];
                }
            }
            return ret;

    }
    public int[] indexs(){
        return null;
    }
    public String getKey(int index){
        return null;
    }
    public Object getValue(int index){
         return null;
    }
    Object[] extend(Object[] obj,int add){
        return Arrays.copyOf(obj,obj.length+add);
    }
    String[] extend(String[] obj,int add){
        return Arrays.copyOf(obj,obj.length+add);
    }
    int[] extend(int[] obj,int add){
        return Arrays.copyOf(obj,obj.length+add);
    }
}
