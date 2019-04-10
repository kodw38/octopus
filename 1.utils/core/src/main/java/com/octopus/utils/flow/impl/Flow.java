package com.octopus.utils.flow.impl;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.bftask.IBFTask;
import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.flow.ForkRunnable;
import com.octopus.utils.flow.IExpress;
import com.octopus.utils.flow.IFlow;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-8-23
 * Time: 下午4:02
 */
public class Flow extends XMLDoObject implements IFlow{
    ArrayList<IBFTask> nodes;
    IExpress express;
    XMLDoObject executor;
    IBFTask error;

    private ThreadLocal<double[]> ignoreNodes = new ThreadLocal<double[]>();
    private ThreadLocal<Double> jumpSeq = new ThreadLocal<Double>();

    public Flow(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    public boolean isExist(double  seq){
        for(IBFTask node:nodes){
            if(seq==((XMLObject)node).getSeq()){
                return true;
            }
        }
        return false;
    }

    public void doFlow(FlowParameters parameters) throws Exception {
        parameters.clearStatus();

        if(null == error){
            for(IBFTask node:nodes){
                if("error".equals(((XMLObject)node).getXmlExtProperties().getProperty("type"))){
                    error=node;
                    break;
                }
            }
        }

        for(IBFTask node:nodes){
            if(parameters.isStop())break;

            if(parameters.isInterrupt()){
                if(parameters.getNextTask()>0) {
                    if(parameters.getNextTask()!=((XMLObject)node).getSeq())continue;
                }else break;
            }

            if(node.equals(error)) continue;

            if(((XMLObject)node).getSeq()<0) continue;

            if(null != jumpSeq.get() && jumpSeq.get()>0){
                if(((XMLObject)node).getSeq()<jumpSeq.get()){
                    continue;
                }else {
                    jumpSeq.set(new Double (-1));
                }
            }
            doNode(node,parameters);
        }

    }

    IExpress getExpress(){
        return (IExpress)getPropertyObject("express");
    }

    public double[] setIgnoreNodeSeqs(String ignoreNodeSeqs1){
        double[] ignoreNodeSeqs=null;
        String[] is = ignoreNodeSeqs1.split(",");
        if(ArrayUtils.isNotEmpty(is)){
            ignoreNodeSeqs=new double[is.length];
            for(int i=0;i<is.length;i++){
                ignoreNodeSeqs[i]=Double.parseDouble(is[i]);
            }
        }
        return ignoreNodeSeqs;
    }

    /**
     * 一般、判断、分叉、会签、子流程
     * type="normal/judge/fork/subflow/error"
     * nextjumpto="targetseq" nextignore="" state="valid/invalid" resultexpr="" subid=""
     * @param node
     * @param parameters
     */
    public void doNode(IBFTask node,FlowParameters parameters) throws Exception {
        try{
        String state=((XMLObject)node).getXmlExtProperties().getProperty("state");
        if(StringUtils.isNotBlank(state) && state.equals("invalid")) return;

        if(ArrayUtils.isNotEmpty(ignoreNodes.get()) && ArrayUtils.isInDoubleArray(ignoreNodes.get(), ((XMLObject) node).getSeq())) return;

        String nextignore=((XMLObject)node).getXmlExtProperties().getProperty("nextignore");
        if(StringUtils.isNotBlank(nextignore)){
            ignoreNodes.set(setIgnoreNodeSeqs(nextignore));
        }


        String type=((XMLObject)node).getXmlExtProperties().getProperty("type");
        if(type.equals("normal")){
            node.doTask(parameters);
            if(parameters.isError()){
                toError(parameters);
            }
            String jumpto=((XMLObject)node).getXmlExtProperties().getProperty("nextjumpto");
            if(StringUtils.isNotBlank(jumpto)){
                String[] tos = jumpto.split(",");
                if(tos.length>0){
                    System.err.println("this node type is normal so only jump to :"+tos[0]);
                }
                toJump(Double.parseDouble(tos[0]),((XMLObject)node).getSeq(),parameters);
            }

        }else if(type.equals("judge")){
            String expr=((XMLObject)node).getXmlExtProperties().getProperty("resultexpr");
            node.doTask(parameters);
            boolean isjump=false;
            if(StringUtils.isNotBlank(expr)){
                double jumpseq= getExpress().express(expr, parameters);
                isjump=toJump(jumpseq,((XMLObject)node).getSeq(),parameters);
            }
            if(!isjump){
                String jumpto=((XMLObject)node).getXmlExtProperties().getProperty("nextjumpto");
                if(StringUtils.isNotBlank(jumpto)){
                    String[] tos = jumpto.split(",");
                    if(tos.length>0){
                        System.err.println("this node type is judge so only jump to :"+tos[0]);
                    }
                    toJump(Double.parseDouble(tos[0]),((XMLObject)node).getSeq(),parameters);
                }
            }
        }else if(type.equals("fork")){
            node.doTask(parameters);
            String jumpto=((XMLObject)node).getXmlExtProperties().getProperty("nextjumpto");
            if(StringUtils.isNotBlank(jumpto)){
                String[] tos = jumpto.split(",");
                double[] seqs = new double[tos.length];
                for(int i=0;i<tos.length;i++){
                    seqs[i]=Double.parseDouble(tos[i]);
                }
                tofork(seqs,parameters);
            }
        }else if(type.equals("subflow")){
            String subid=((XMLObject)node).getXmlExtProperties().getProperty("subid");
            if(((FlowMgr)getParent()).isExist(subid)){
                ((FlowMgr)getParent()).doFlow(subid,parameters);
            }else{
                throw new Exception("not find sub flowid "+subid);
            }
        }else{
            throw new Exception("not support node type "+type);
        }
        }catch (Exception e){
            if(null != error) {
                error.doTask(parameters);
            }else {
                throw e;
            }
        }
    }

    /**
     * 异常处理并结束流程
     * @param parameters
     */
    void toError(FlowParameters parameters)throws Exception{
        if(null == error)
            throw new Exception("the flow not set node of type is error",parameters.getException());
        error.doTask(parameters);
    }

    /**
     * 跳转到指定seq的node继续往下处理
     * @param seq
     * @param parameters
     * @return
     */
    boolean  toJump(double seq,double curseq,FlowParameters parameters) throws Exception {
        if(isExist(seq) && seq>curseq){
            jumpSeq.set(seq);
            return true;
        }else if(seq<0){
            parameters.setInterrupt();
            for(IBFTask t:nodes){
                if(((XMLObject)t).getSeq()==seq){
                    parameters.setInterrupt();
                    doNode(t,parameters);
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 跳转到seqs处理完后，继续完成原流程的fork下一个节点处理。
     * fork处理的节点都是单独的一个节点，seq<0
     * fork使用并行线程处理
     * @param seqs
     * @param parameters
     * @return
     */
    boolean tofork(double [] seqs,FlowParameters parameters) throws CloneNotSupportedException {
        ForkRunnable[] bfs = new ForkRunnable[seqs.length];
        for(int i=0;i<seqs.length;i++){
            for(IBFTask t:nodes){
                if(((XMLObject)t).getSeq()==seqs[i]){
                    bfs[i]=new ForkRunnable();
                    bfs[i].setFlow(this);
                    bfs[i].setTask(t);
                    bfs[i].setParameters((FlowParameters)parameters.clone());
                    break;
                }
            }
            if(bfs[i]==null)
                return false;
        }
        //并非处理，并等待集体的返回结果
        try{
            ExecutorUtils.teamWork(bfs);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        List li = new ArrayList();
        for(ForkRunnable r:bfs){
            li.add(r.getParameters().getResult());
        }
        parameters.setResult(li);
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env,Map input,Map output,Map config) throws Exception {
        //todo jsoncfg
        ((FlowParameters)env).setParameter(input);
        doFlow((FlowParameters)env);
        return env.getResult();
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output,Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input,Map output, Map config, Object ret) throws Exception {
        if(log.isDebugEnabled()){
            if(env instanceof BFParameters)
                System.out.println("the flow trace is:\n"+((BFParameters)env).getTaskPath());
        }
        if(null != ret && ret instanceof ResultCheck){
            return (ResultCheck)ret;
        }else{
            return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid,XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        throw new Exception("now support rollback");
    }

}
