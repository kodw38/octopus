package com.octopus.tools.deploy;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.impl.excel.ExcelReader;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.exception.ExceptionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * input:{op:'copy',src:'',replace:'',target:''}
 * input:{op:'shell',src:'',replace:'',target:''}
 * input:{op:'command',src:'',replace:'',target:''}
 * input:{op:'load',type:'mysql',src:'',replace:'',target:''}
 * input:{op:'load',type:'excel',src:'',replace:'',target:''}
 *
 * User: wfgao_000
 * Date: 15-11-11
 * Time: 下午9:06
 */
public class AutoDeploy extends XMLDoObject {
    Deploy deploy = new Deploy();
    CommandMgr commandMgr=null;
    PropertiesMgr propertiesMgr=null;
    public AutoDeploy(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        String command = (String)input.get("command");
        String range = (String)input.get("range");//execute range
        String param = (String)input.get("pars");
        String configExcel = (String)input.get("config");//excel config file path
        String script = (String)input.get("script"); //the path of scripts folder
        String op = (String)input.get("op"); //the path of scripts folder
        Object remote = env.getParameter("websocket-remote");//web-socket obj
        if(StringUtils.isBlank(configExcel)){
            configExcel = (String)config.get("config");
        }
        if(StringUtils.isBlank(script)){
            script = (String)config.get("script");
        }
        //send message to web-socket
        if(null != remote){
            ExecutorUtils.synWork(remote, "sendText", new Class[]{String.class}, new Object[]{"begin execute command ["+command+"] range ["+range+"]"});
        }
        if(null == commandMgr) {
            commandMgr = new CommandMgr(configExcel, script);
        }
        if(null == propertiesMgr){
            propertiesMgr = new PropertiesMgr(configExcel);
        }
        if("getAllOps".equals(op)){
            return propertiesMgr.getCommands();
        }
        OutputStream out = null;
        if (null != input && null != input.get("log") && "out".equals(input.get("log")) && null != env.getParameter("${response}")) {
            out = ((HttpServletResponse) env.getParameter("${response}")).getOutputStream();
        }
        try {
             String sb = deploy.executeScript(propertiesMgr, commandMgr, command, range, param, out, remote);
            if(null != sb){
                return sb;
            }
        } catch (Exception e) {
            if (null != remote) {
                ExecutorUtils.synWork(remote, "sendText", new Class[]{String.class}, new Object[]{"execute command [" + command + "] error:"});
                String[] ss = ExceptionUtils.getRootCauseStackTrace(e);
                for (String s : ss) {
                    ExecutorUtils.synWork(remote, "sendText", new Class[]{String.class}, new Object[]{s});
                }
            }else {
                throw e;
            }
        }
        if (null != remote) {
            ExecutorUtils.synWork(remote, "sendText", new Class[]{String.class}, new Object[]{"finished execute command [" + command + "] range [" + range + "]"});
        }

        return "finished it";
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
       /* if(StringUtils.isBlank((String)input.get("config")) || StringUtils.isBlank((String)input.get("command")) || StringUtils.isBlank((String)input.get("range")))
            return false;*/
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }
}
