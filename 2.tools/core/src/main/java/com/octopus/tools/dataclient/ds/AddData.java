package com.octopus.tools.dataclient.ds;

import net.sf.json.JSONArray;

import java.util.Map;

/**
 * User: Administrator
 * Date: 14-10-10
 * Time: 下午5:51
 */
public class AddData {
    String opCode;
    Map[] data;

    public AddData(){}
    public AddData(String opCode,Map[] data){
        this.opCode=opCode;
        this.data=data;
    }

    public String getOpCode() {
        return opCode;
    }

    public void setOpCode(String opCode) {
        this.opCode = opCode;
    }

    public Map[] getData() {
        return data;
    }

    public void setData(Map[] data) {
        this.data = data;
    }

    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("opCode:"+opCode).append(",").append("datas:").append(JSONArray.fromObject(data).toString());
        return sb.toString();
    }

}
