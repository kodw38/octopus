package com.octopus.utils.xml.auto;

/**
 * User: wfgao_000
 * Date: 15-4-23
 * Time: 下午5:53
 */
public class ResultCheck {
    public static int INPUT_CHECK_FAULT = 10;//check input fault
    public static int INPUT_CHECK_ERROR = 20;//happen error
    public static int RESULT_CHECK_FAULT= 30;//happen error
    public static int RESULT_CHECK_SUCCESS= 40;//happen error
    Object ret;
    boolean isSuccess;
    int status;
    public ResultCheck(){}
    public ResultCheck(boolean is,Object ret){
        this.ret=ret;
        this.isSuccess=is;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public ResultCheck(boolean is,Object ret,int point){
        this.ret=ret;
        this.isSuccess=is;
        this.status=point;
    }
    public boolean isSuccess() {
        return isSuccess;
    }

    public int getStatus() {
        return status;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public Object getRet() {
        return ret;
    }

    public void setRet(Object ret) {
        this.ret = ret;
    }
}
