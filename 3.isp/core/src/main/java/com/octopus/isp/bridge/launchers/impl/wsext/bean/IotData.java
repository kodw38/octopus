package com.octopus.isp.bridge.launchers.impl.wsext.bean;/**
 * Created by admin on 2020/7/17.
 */

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;

import javax.servlet.AsyncContext;

/**
 * @ClassName IotData
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/7/17 17:17
 * @Version 1.0
 **/
    public class IotData{
        public IotData(){

        }
        public IotData(AsyncContext context){
            RequestParameters rp = (RequestParameters)context.getRequest().getAttribute("RequestParameters");
            if(null != rp){
                requestId=rp.getRequestId();
                loginCode=rp.getSession().getUserName();
                action=rp.getTargetNames()[0];
                inputData=rp.getInputParameter();
                if(null != rp.getResult()){
                    outputData=rp.getResult();
                }
                isWithLocal= StringUtils.isTrue((String)rp.getQueryStringMap().get("withLocal"));
            }
        }
        String requestId;
        String loginCode;
        String action;
        Object inputData;
        Object outputData;
        boolean isWithLocal;

    public boolean isWithLocal() {
        return isWithLocal;
    }

    public void setWithLocal(boolean withLocal) {
        isWithLocal = withLocal;
    }

    public Object getOutputData() {
            return outputData;
        }

        public void setOutputData(Object outputData) {
            this.outputData = outputData;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getLoginCode() {
            return loginCode;
        }

        public void setLoginCode(String loginCode) {
            this.loginCode = loginCode;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public Object getInputData() {
            return inputData;
        }

        public void setInputData(Object inputData) {
            this.inputData = inputData;
        }
    }
