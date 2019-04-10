package com.octopus.tools.dataclient.ds;

import net.sf.json.JSONObject;

import java.util.Map;

/**
 * User: Administrator
 * Date: 14-10-10
 * Time: 下午5:58
 */
public class MetaCnd {
    String opCode;
    Map cnd;
    public MetaCnd(){};
    public MetaCnd(String opCode,Map cnd){
        this.opCode=opCode;
        this.cnd=cnd;
    }

    public String getOpCode() {
        return opCode;
    }

    public void setOpCode(String opCode) {
        this.opCode = opCode;
    }

    public Map getCnd() {
        return cnd;
    }

    public void setCnd(Map cnd) {
        this.cnd = cnd;
    }
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("opCode:"+opCode).append(",").append("data:").append(JSONObject.fromObject(cnd).toString());
        return sb.toString();
    }
}
