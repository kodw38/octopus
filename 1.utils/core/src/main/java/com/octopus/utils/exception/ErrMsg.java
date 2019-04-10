package com.octopus.utils.exception;

/**
 * Created by robai on 2018/1/15.
 */
public class ErrMsg {
    String code;
    String msg;
    public ErrMsg(){}
    public ErrMsg(String code,String msg){
        this.code=code;
        this.msg=msg;
    }
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
