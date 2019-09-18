package com.octopus.utils.exception;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robai on 2017/7/17.
 */
public class ISPException extends Exception{
    String code;
    String msg;
    Map msg_args;
    String realMsg=null;
    Throwable e=null;
    public ISPException(String code,String message){
        super("["+code+"]:"+message);
        this.code=code;
        this.msg=message;
        this.realMsg=message;
    }
    public String toString(){
        return getMessage();
    }
    public ISPException(String code,String message,String[] args){
        super("["+code+"]"+getTransferMsg(message,args)[0]);
        msg_args=(Map)getTransferMsg(message,args)[1];
        this.code=code;
        this.msg=message;
        try {
            this.realMsg = (String)XMLParameter.getExpressValueFromMap(msg, msg_args, null);
        }catch (Exception e){}
    }
    public String getRealMsg(){
        try {
            if (null == realMsg) {
                if (null != msg_args)
                    realMsg = (code == null ? "" : "[" + code + "] ") + (String) XMLParameter.getExpressValueFromMap(msg, msg_args, null);
                else
                    realMsg = (code == null ? "" : "[" + code + "] ") + msg;
            }
            return realMsg;
        }catch (Exception e){
            return "";
        }
    }

    public void setRealMsg(String s){
        realMsg=s;
    }

    static Object[] getTransferMsg(String message,String[] args){
        try {
            Map msg_args = null;
            if (null == message) return null;
            if (null == args) return new Object[]{message, msg_args};
            if (null != args && args.length > 0) {
                List<String> m = StringUtils.getTagsNoMark(message, "[(", ")]");
                if (null != m) {
                    msg_args = new LinkedHashMap();
                    for (int i = 0; i < m.size(); i++) {
                        if (null != args && args.length >= i && null != args[i]) {
                            msg_args.put(m.get(i), args[i]);
                        }
                    }
                }
            }
            return new Object[]{(String) XMLParameter.getExpressValueFromMap(message, msg_args, null), msg_args};
        }catch (Exception e){
            return null;
        }
    }

    public Map getMsgArgs(){
        return msg_args;
    }
    public ISPException(String code,String message,Throwable e){
        super("["+code+"]:"+message!=null?message:e.getMessage());
        this.code=code;
        this.msg=message;
        this.e=e;
    }
    public Throwable getException(){
        return e;
    }
    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg){
        this.msg=msg;
    }
}
