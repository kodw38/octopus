package com.octopus.utils.balance.qps;

import com.octopus.utils.cls.javassist.bytecode.analysis.Executor;
import com.octopus.utils.cls.javassist.bytecode.annotation.IntegerMemberValue;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ThreadPool;
import junit.framework.TestCase;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2018/11/8.
 */
public class Test extends TestCase{
    public Test(){
        //super(method);
    }
    /*public Test(){
        super();
    }*/

    /*public void testNormal_1(){
        long l = System.currentTimeMillis();
        runTest(getDataList_1());
        System.out.println("testNormal_1 cost "+(System.currentTimeMillis()-l));
    }
    public void testNormal_2(){
        long l = System.currentTimeMillis();
        runTest(getDataList_2());
        System.out.println("testNormal_2 cost "+(System.currentTimeMillis()-l));

    }
    public void testNormal_3(){
        long l = System.currentTimeMillis();
        runTest(getDataList_3());
        System.out.println("testNormal_3 cost "+(System.currentTimeMillis()-l));

    }
    public void testNormal_by(){
        long l = System.currentTimeMillis();
        runTest(getDataList_by());
        System.out.println("testNormal_by cost " + (System.currentTimeMillis() - l));

    }*/

    public static void main(String[] args){
        try{
            //Test t = new Test();
            //t.testB_1();
            System.out.println(new Date(321564309));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void testB_1(){
        long l = System.currentTimeMillis();
        QPSManager qps = new QPSManager("ID",10,1000,new String[]{"Type"},"OM",new String[]{"101"},new TestTask(),0,0);
        /*qps.putStatCost("s10000_",10000);
        qps.putStatCost("s3_",3);
        qps.putStatCost("s10_",10);
        qps.putStatCost("s15_",15);
        qps.putStatCost("s20_",20);*/
        qps.receives(getDataList_1());
        qps.waitingFinished();
        System.out.println("------testB_1 cost "+(System.currentTimeMillis()-l));
        for(int i=0;i<1000;i++){
            qps.receive(getSlow(i+""));
            qps.receive(getVerySlow(++i+""));
            qps.receive(getNormal(++i+""));
            qps.receive(getQuick(++i+""));


        }
        qps.waitingFinished();
        System.out.println("-------real testB_1 cost "+(System.currentTimeMillis()-l));
    }
    /*public void testB_2(){
        long l = System.currentTimeMillis();
        QPSManager qps = new QPSManager(3,1000,new String[]{"type"},"OM",new String[]{"101"},new TestTask(),2,4);
        qps.receives(getDataList_2());
        waitEmpty(qps);
        System.out.println("testB_2 cost "+(System.currentTimeMillis()-l));

    }
    public void testB_3(){
        long l = System.currentTimeMillis();
        QPSManager qps = new QPSManager(3,1000,new String[]{"type"},"OM",new String[]{"101"},new TestTask(),2,4);
        qps.receives(getDataList_3());
        waitEmpty(qps);
        System.out.println("testB_3 cost "+(System.currentTimeMillis()-l));

    }
    public void testB_by(){
        long l = System.currentTimeMillis();
        QPSManager qps = new QPSManager(3,1000,new String[]{"type"},"OM",new String[]{"101"},new TestTask(),2,4);
        qps.receives(getDataList_by());
        waitEmpty(qps);
        System.out.println("testB_by cost "+(System.currentTimeMillis()-l));

    }
*/
    void runTest(Object[] datas){
        try {
            AtomicInteger ai = new AtomicInteger(0);
            ExecutorService s = Executors.newFixedThreadPool(10);
            for(Object o:datas) {
                s.execute(new Rt(o,ai));
            }
            while(true) {
                if (ai.get() == datas.length) {
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    class Rt implements Runnable{
        Object o;
        AtomicInteger ai;
        Rt(Object o,AtomicInteger ai){
            this.o=o;
            this.ai=ai;
        }

        @Override
        public void run() {
            try {
                if (null != o && o instanceof Map) {
                    Thread.sleep((Integer) ((Map) o).get("sleep"));

                }
                ai.getAndIncrement();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    void waitEmpty(QPSManager qps){
        while(true) {
            if (qps.isEmpty() && qps.isAssigned) {
                break;
            }
            try {
                Thread.sleep(20);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    Map getQuick(String id){
        HashMap m = new HashMap();
        m.put("sleep",5);
        m.put("Type","s3");
        m.put("ID",id);
        return m;
    }
    Map getSlow(String id){
        HashMap m = new HashMap();
        m.put("sleep",15);
        m.put("Type","s10000");
        m.put("ID",id);
        return m;
    }
    Map getNormal(String id){
        HashMap m = new HashMap();
        m.put("sleep",8);
        m.put("Type","s15");
        m.put("ID",id);
        return m;
    }
    Map getImport(String id){
        HashMap m = new HashMap();
        m.put("sleep",10);
        m.put("Type","s10");
        m.put("ID",id);
        m.put("OM","101");
        return m;
    }
    Map getVerySlow(String id){
        HashMap m = new HashMap();
        m.put("sleep",20);
        m.put("Type","s20");
        m.put("OM","10001");
        m.put("ID",id);
        return m;
    }
    Map[] getDataList_1(){
        List li = new LinkedList();
        for(int i=0;i<300;i++){
            li.add(getSlow(i+""));
            if(i%100==0){
                li.add(getVerySlow(i+""));
            }
        }
        for(int i=310;i<320;i++){
            li.add(getQuick(i+""));
        }
        for(int i=300;i<700;i++){
            li.add(getQuick(i+""));
        }
        for(int i=710;i<720;i++){
            li.add(getNormal(i + ""));
        }
        for(int i=700;i<1000;i++){
            li.add(getNormal(i+""));
        }
        return (Map[])li.toArray(new Map[0]);
    }
    Map[] getDataList_2(){
        List li = new LinkedList();
        for(int i=1000;i<1300;i++){
            li.add(getSlow(i+""));
            if(i%100==0){
                li.add(getVerySlow(i+""));
            }
        }
        for(int i=1310;i<1320;i++){
            li.add(getQuick(i+""));
        }
        for(int i=1300;i<1700;i++){
            li.add(getQuick(i+""));
        }
        for(int i=1710;i<1720;i++){
            li.add(getNormal(i + ""));
        }
        for(int i=1700;i<2000;i++){
            li.add(getNormal(i+""));
        }
        return (Map[])li.toArray(new Map[0]);
    }
    /*Map[] getDataList_2(){
        List li = new LinkedList();
        for(int i=0;i<300;i++){
            li.add(getSlow());
        }
        for(int i=0;i<400;i++){
            li.add(getNormal());
        }
        for(int i=0;i<300;i++){
            li.add(getQuick());
        }
        return (Map[])li.toArray(new Map[0]);
    }
    Map[] getDataList_3(){
        List li = new LinkedList();
        for(int i=0;i<300;i++){
            li.add(getNormal());
        }
        for(int i=0;i<400;i++){
            li.add(getSlow());
        }
        for(int i=0;i<300;i++){
            li.add(getQuick());
        }
        return (Map[])li.toArray(new Map[0]);
    }
    Map[] getDataList_by(){
        List li = new LinkedList();
        for(int i=0;i<1000;i++){
            if(i%3==0){
                li.add(getQuick());
            }
            if(i%3==1){
                li.add(getNormal());
            }
            if(i%3==2){
                li.add(getSlow());
            }
        }
        return (Map[])li.toArray(new Map[0]);
    }*/
}
