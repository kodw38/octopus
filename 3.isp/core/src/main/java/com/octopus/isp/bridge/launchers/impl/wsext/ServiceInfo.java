package com.octopus.isp.bridge.launchers.impl.wsext;

import com.octopus.utils.alone.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * User: wfgao_000
 * Date: 15-8-10
 * Time: 下午12:02
 */
public class ServiceInfo {
    String serviceName;
    List<ClassInfo> classList=new ArrayList<ClassInfo>();
    String desc;

    public ServiceInfo(){}

    public ServiceInfo(String serviceName,String className,String methodName,String[] parClassNames,String returnClassName){
        this.serviceName = serviceName;
        ClassInfo i = new ClassInfo();
        i.setClassName(className);
        List lm = new ArrayList();
        MethodInfo mi = new MethodInfo();
        mi.setMethodName(methodName);
        mi.setReturnType(returnClassName);
        if(null != parClassNames && parClassNames.length>0){
        List<ParInfo> ps = new ArrayList<ParInfo>();
        for(int j=0;j<parClassNames.length;j++){
            ParInfo p = new ParInfo();
            p.setName("par"+j);
            p.setTye(parClassNames[j]);
            ps.add(p);
        }
        mi.setParInfos(ps);
        }
        lm.add(mi);
        i.setMethodList(lm);
        classList.add(i);
    }

    public String getDesc() {
        return desc;
    }
    public void addClassInfo(ClassInfo c){
        classList.add(c);
    }
    public void addClassInfo(String className,String method,String[][] pars,String retType){
        ClassInfo c=null;
        if(className.contains("/")){
            className = StringUtils.replace(className, "/", ".");
        }else if(className.contains("\\")){
            className = StringUtils.replace(className, "\\", ".");
        }
        for(ClassInfo i:classList){
            if(i.getClassName().equals(className)){
                c=i;
                break;
            }
        }
        if(c==null){
            c = new ClassInfo();
            c.setClassName(className);
            classList.add(c);
        }
        MethodInfo mi=null;
        for(MethodInfo m:c.getMethodList()){
            if(m.getMethodName().equals(method)){
                mi=m;
                break;
            }
        }
        if(null == mi){
            mi = new MethodInfo();
            mi.setMethodName(method);
            mi.setReturnType(retType);
            c.addMethod(mi);
        }
        if(null != pars){
            for(String[] par:pars){
                mi.addPar(par[0],par[1]);
            }
        }

    }
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<ClassInfo> getClassList() {
        return classList;
    }

    public void setClassList(List<ClassInfo> classList) {
        this.classList = classList;
    }

    public class ClassInfo{
        String className;
        List<MethodInfo> methodList=new ArrayList<MethodInfo>();

        public String getClassName() {
            return className;
        }
        public void addMethod(MethodInfo m){
            methodList.add(m);
        }
        public void setClassName(String className) {
            this.className = className;
        }

        public List<MethodInfo> getMethodList() {
            return methodList;
        }

        public void setMethodList(List<MethodInfo> methodList) {
            this.methodList = methodList;
        }
    }
    public class MethodInfo{
        String methodName;
        String returnType;
        List<ParInfo> parInfos = new LinkedList<ParInfo>();

        public String getMethodName() {
            return methodName;
        }
        public void addPar(ParInfo p){
            parInfos.add(p);
        }
        public void addPar(String name,String type){
            ParInfo p = new ParInfo();
            p.setName(name);
            p.setTye(type);
            parInfos.add(p);
        }
        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getReturnType() {
            return returnType;
        }

        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }

        public List<ParInfo> getParInfos() {
            return parInfos;
        }

        public void setParInfos(List<ParInfo> parInfos) {
            this.parInfos = parInfos;
        }
    }
    public class ParInfo{
        String name;
        String tye;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTye() {
            return tye;
        }

        public void setTye(String tye) {
            this.tye = tye;
        }
    }
}
