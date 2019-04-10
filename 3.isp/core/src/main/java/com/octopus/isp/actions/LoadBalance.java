package com.octopus.isp.actions;

import com.alibaba.otter.canal.example.StringUtils;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.ha.AutoHABalance;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.httpclient.HttpException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by robai on 2017/7/11.
 */
public class LoadBalance extends XMLDoObject {
    public LoadBalance(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null!= input){
            String type = (String)input.get("type");
            //需要排除的实例列表名称
            List<String> excludeIns = (List)input.get("excludeIns");
            if(StringUtils.isNotBlank(type)) {
                //可以用的实例列表名称
                List<Map> list = (List) input.get("insList");
                if(null != list) {
                    synchronized (list) {

                        //如果类型为轮询
                        List<Map> ret = new ArrayList();
                        Map m = null;
                        List rm = new ArrayList();

                        for (int i = 0; i < list.size(); i++) {
                            if (log.isDebugEnabled()) {
                                log.debug("instance list:" + list.get(i));
                            }

                            if (null != list.get(i).get("isConnected") && !(Boolean) list.get(i).get("isConnected")) {
                                //获取出现异常的实例名称,放入异常列表中
                                rm.add(list.get(i));
                            }
                            if (!(null != list.get(i).get("isConnected") && !(Boolean) list.get(i).get("isConnected"))
                                    && (null == excludeIns || (ArrayUtils.isNotLikeInStringList(excludeIns, (String) list.get(i).get("INS_ID"))))) {
                                //排除指定不包含的实例,把一个可用的实例作为返回并把返回的实例放入最后
                                m = list.remove(i);
                                list.add(m);
                                if ("roundrobin".equals(type)) {
                                    break;
                                }
                            }
                        }
                        //移除异常实例
                        for (Object o : rm) {
                            list.remove(o);
                        }
                        if ("automatic".equals(type)) {
                            m = getAutomaticIns(list, (String) input.get("srvId"), (Map) input.get("data"), env);
                        }
                        if (null != m) {
                            ret.add((Map) m);
                            ((Map) m).put("GetOutTime", System.currentTimeMillis());
                        }
                        env.addParameter("~assignInsKey", xmlid);
                        if (log.isDebugEnabled()) {
                            log.debug("maybe invoke service [" + ArrayUtils.toJoinString(env.getTargetNames()) + "] used instance [" + ret.get(0) + "]");
                        }
                        return ret;
                    }
                }

            }
            String op = (String)input.get("op");
            if(StringUtils.isNotBlank(op)){
                //使用完成后反馈分配的实例信息
                if("setStatus".equals(op)){
                    Throwable e = env.getException();
                    String assignInsKey = (String)env.getParameter("~assignInsKey");
                    env.removeParameter("~assignInsKey");
                    if(StringUtils.isNotBlank(assignInsKey)) {
                        //获取之前分配的实例信息
                        List<Map> ls = (List) env.getParameter("${"+assignInsKey+"}");
                        if (null != ls && ls.size() > 0) {
                            Map ret = ls.get(0);
                            if (null != ret) {
                                ret.put("PutInTime", System.currentTimeMillis());
                                if (null != ret.get("GetOutTime")) {
                                    ret.put("Cost", ((Long) ret.get("PutInTime") - (Long) ret.get("GetOutTime")));
                                }
                                Object o = input.get("msg");
                                //如果网络异常设置该实例为不可用
                                if ((null != e &&
                                        (ExceptionUtil.getRootCase(e) instanceof HttpException
                                                || ExceptionUtil.getRootCase(e) instanceof ConnectException
                                                || ExceptionUtil.getRootCase(e) instanceof SocketTimeoutException))
                                        || (null != o && o instanceof Map && null != ((Map) o).get("is_error")
                                        && StringUtils.isTrue((String) ((Map) o).get("is_error")) && ((String) ((Map) o).get("errorcode")).equals("S-404"))) {
                                    //如果出现网络异常，设置可链接属性为不可链接
                                    ret.put("isConnected", false);
                                    log.error("remove loadBalance instance " + ret);
                                    //return "disConnect";
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return true;
    }

    /**
     * 根据 服务（服务等级）+参数（参数等级，具体参数规则）+实例信息（安全等级，业务取向，资源使用信息）=选中的实例
     * @param insList 可以用的实例列表
     * @param svName 服务名称
     * @param inputData 输入参数
     * @param env 环境
     * @return
     */
    Map getAutomaticIns(List<Map> insList,String svName,Map inputData,XMLParameter env){
        String svlevel = null;//getServerLevel(svName);
        String datalevel = null;//getDataLevel(inputData, env);
        List<Map> resourceInfo = null;//getInstResourceUsedInfo(insList);
        return AutoHABalance.getNextIns(insList,resourceInfo,svName,svlevel,inputData,datalevel,env);
    }
}
