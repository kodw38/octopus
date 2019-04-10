package com.octopus.utils.bftask;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLParameter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BFParameters extends XMLParameter implements Serializable{
    protected boolean isStop=false;
    protected StringBuffer taskPath = new StringBuffer();      //经过的管道路径
    protected boolean isInterrupt;
    protected List<String> jumpTaskList = new ArrayList();
    protected double nextTask=0;
    boolean cycleused;
    public BFParameters(){
        cycleused=false;
    }

    public BFParameters(boolean cycleused){
        this.cycleused = cycleused;
    }
    public void clearStatus(){
        isStop=false;
        put("^isstop","false");
        isInterrupt=false;
        put("^isinterrupt","false");
        jumpTaskList.clear();
        nextTask=0;
        put("^nexttask","0");
    }
    /*public Object getResult() {
        if(null == result && ArrayUtils.isNotEmpty(getChild("result")) && StringUtils.isNotBlank(getChild("result")[0].getText())){
            result=StringUtils.toXMLRetainChar(getChild("result")[0].getText());
        }
        return result;
    }*/

    public double getNextTask() {
        return nextTask;
    }

    public void setNextTask(double nextTask) {
        this.nextTask = nextTask;
        put("^nexttask",String.valueOf(nextTask));
    }

    /*public void setResult(Object ret) {
        this.result = ret;
        if(null != ret && ArrayUtils.isEmpty(getChild("result"))){
            XMLMakeup res = new XMLMakeup();
            res.setName("result");
            addChild(res);
        }
        if(!ArrayUtils.isEmpty(getChild("result"))){
            if(null ==ret)
                getChild("result")[0].setText("");
            else
                getChild("result")[0].setText(StringUtils.toXMLShiftChar(ret.toString()));
        }
    }*/
    public Throwable getException() {
        if(null == exception && isError() && StringUtils.isNotBlank(get("^exception"))){
            exception = new Exception((String)get("^exception"));
        }
        return exception;
    }

    public boolean isStop() {
        if(StringUtils.isNotBlank(get("^isstop"))){
            isStop=Boolean.valueOf((String)get("^isstop"));
        }
        return isStop;
    }

    public void setStop() {
        isStop = true;
        put("^isstop","true");
    }

    public void setParameter(Object parameter){
       put("parameter",parameter);
    }

    public Object getParameter(){
        return get("parameter");
    }

    public void setInterrupt(){
        isInterrupt=true;
        put("^isinterrupt","true");
    }
    public boolean isInterrupt(){
        if(StringUtils.isNotBlank(get("^isinterrupt"))){
            isInterrupt=Boolean.valueOf((String)get("^isinterrupt"));
        }
        return isInterrupt;
    }
    public void setContinue(){
        isInterrupt=false;
        put("^isinterrupt","false");
    }
    public synchronized String getTaskPath(){
        if(!cycleused){
        if(taskPath.length()==0 && StringUtils.isNotBlank(get("^trace"))){
            taskPath.append(get("^trace"));
        }
        return taskPath.toString();

        }
        return null;
    }

    public void setCycleused(boolean cycleused) {
        this.cycleused = cycleused;
    }

    public synchronized void addTaskCode(String taskCode){

        /*if(false) {
            if (!cycleused) {
                if (taskPath.length() == 0)
                    taskPath.append(taskCode);
                else
                    taskPath.append("->").append(taskCode);
                put("^trace", taskPath.toString());
            }
        }*/

    }

	public Object get(String key){
        /*if(data.size()==0 && ArrayUtils.isNotEmpty(getChild("data")) && getChild("data")[0].getChildren().size()>0){
            initmap(getChild("data")[0],data);
        }*/
		return getParameter(key);
	}
	public void put(String key,Object value){
        /*if(ArrayUtils.isEmpty(getChild("data"))){
            XMLMakeup d = new XMLMakeup();
            d.setName("data");
            addChild(d);
        }*/
        //putdata(getChild("data")[0],key,value,data.containsKey(key));
		addParameter(key, value);
	}
	public boolean containsKey(String key){
        /*if(data.size()==0 && ArrayUtils.isNotEmpty(getChild("data")) && getChild("data")[0].getChildren().size()>0){
            initmap(getChild("data")[0],data);
        }*/
		return super.containsParameter(key);
	}

    public Map getData(){
        /*if(data.size()==0 && ArrayUtils.isNotEmpty(getChild("data")) && getChild("data")[0].getChildren().size()>0){
            initmap(getChild("data")[0],data);
        }
        return data;*/
        return getReadOnlyParameter();
    }

    public List<String> getJumpTaskList() {
        if(jumpTaskList.size()==0 && StringUtils.isNotBlank(get("^jumps"))){
            String jumps = (String)get("^jumps");
            String[] ss = jumps.split(",");
            for(String s:ss)
                jumpTaskList.add(s.trim());
        }
        return jumpTaskList;
    }

    public void setJumpTaskList(List<String> jumpTaskList) {
        this.jumpTaskList = jumpTaskList;
        if(null != jumpTaskList){
            StringBuffer sb = new StringBuffer();
            for(String s:jumpTaskList){
                if(sb.length()!=0)
                sb.append(",");
                sb.append(s);
            }

            put("^jumps", sb.toString());
        }
    }

    /**
     * <d kt="" vt="" k=""></d>
     * @param data
     * @param map
     */
    void initmap(XMLMakeup data,Map map){
        List<XMLMakeup> ls= data.getChildren();
        /*for(XMLMakeup x:ls){
            map.put(ObjectUtils.getObjectFromString(x.getProperties().getProperty("kt"), x.getProperties().getProperty("k")),
                    ObjectUtils.getObjectFromString(x.getProperties().getProperty("vt"), x.getText()));
        }*/
    }

    void putdata(XMLMakeup data,Object key,Object value,boolean isexist){
        XMLMakeup t=null;
        if(isexist){
            List<XMLMakeup> ls=data.getChildren();
            /*for(XMLMakeup x:ls){
                if(key.equals(ObjectUtils.getObjectFromString(x.getProperties().getProperty("kt"), x.getProperties().getProperty("k")))){
                    t = x;
                    break;
                }
            }*/
        }else {
            t = new XMLMakeup();
            t.setName("d");
        }
        /*t.getProperties().put("kt",ObjectUtils.getTypeString(key));
        t.getProperties().put("vt",ObjectUtils.getTypeString(value));
        t.getProperties().put("k",ObjectUtils.getObjectString(key));
        t.setText(ObjectUtils.getObjectString(value));*/
        if(!isexist)
            data.addChild(t);
    }


}
