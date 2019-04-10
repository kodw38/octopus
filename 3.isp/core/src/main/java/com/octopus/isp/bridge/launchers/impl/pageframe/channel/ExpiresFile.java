package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.StringUtils;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-19
 * Time: 下午2:11
 */
public class ExpiresFile extends XMLDoObject {
    private static final int EXPIRE_TYPE_ACCESS = 1;
    private static final int EXPIRE_TYPE_MODIFY = 2;

    private ExpirePatternConfig[] patternConfig = null;

    public ExpiresFile(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);

        try {
            XMLMakeup[] ps = xml.getChild("property");
            List paraList = new ArrayList();
            for (XMLMakeup p:ps) {
                String paraName = (String) p.getProperties().getProperty("name");
                String paraValue = (String)p.getProperties().getProperty("value");

                ExpirePatternConfig patternConf = new ExpirePatternConfig();
                patternConf.urlRegExp = tranformUrl2RegExp(paraName);
                if(paraValue.charAt(0)=='A'){
                    patternConf.expireType=EXPIRE_TYPE_ACCESS;
                    patternConf.expireMillisTime = Integer.parseInt(StringUtils.substring(paraValue, 1));
                    paraList.add(patternConf);
                }
                else if(paraValue.charAt(0)=='M'){
                    patternConf.expireType=EXPIRE_TYPE_MODIFY;
                    patternConf.expireMillisTime = Integer.parseInt(StringUtils.substring(paraValue,1));
                    paraList.add(patternConf);
                }

            }
            patternConfig = (ExpirePatternConfig[])paraList.toArray(new ExpirePatternConfig[0]);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters par = (RequestParameters)env;
        String requestURI = par.getRequestURI();

        long timeMillis = -1;
        if (patternConfig != null && patternConfig.length != 0) {
            try {
                String path="";
                try{
                    path =StringUtils.substring(requestURI, ((String) par.getRequestProperties().get("ContextPath")).length());
                }catch(Exception e){
                    return null;
                }
                for (int i = 0; i < patternConfig.length; i++) {
                    if (patternConfig[i].urlRegExp.match(path)==true) {
                        if (patternConfig[i].expireType == EXPIRE_TYPE_ACCESS) {
                            Calendar cal = new GregorianCalendar();
                            cal.setTimeInMillis(System.currentTimeMillis());
                            cal.add(GregorianCalendar.SECOND,patternConfig[i].expireMillisTime);
                            timeMillis =  cal.getTimeInMillis();
                        }else if (patternConfig[i].expireType == EXPIRE_TYPE_MODIFY) {
                            //modify
                            Calendar cal = new GregorianCalendar();
                            File f = getRequestURLAbsoluteFile((HttpServletRequest)par.get("${request}"));
                            if(f!=null){
                                cal.setTimeInMillis(f.lastModified());
                            }
                            else{
                                cal.setTimeInMillis(System.currentTimeMillis());
                            }
                            cal.add(GregorianCalendar.SECOND,patternConfig[i].expireMillisTime);
                            timeMillis = cal.getTimeInMillis();
                        }else {
                            break;
                        }
                        break;
                    }
                }
            }
            catch (Exception ex) {
            }
        }

        HttpServletResponse response = (HttpServletResponse)par.get("${response}");;
        if (timeMillis > 0) {
            response.addHeader("Cache-Control", "Private");
            response.addDateHeader("Expires", timeMillis);

        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,null);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    class ExpirePatternConfig{
        RE urlRegExp = null;
        int expireType=-1;
        int expireMillisTime=0;
    }

    private File getRequestURLAbsoluteFile(HttpServletRequest request){
        File rtn = null;
        try {
            rtn = new File(request.getSession().getServletContext().getRealPath(request.getServletPath()));
        }catch (Exception ex) {
            throw  new RuntimeException(ex);
        }
        return rtn;
    }


    private RE tranformUrl2RegExp(String pStr) throws RESyntaxException {
        String tmp = StringUtils.replace(pStr, ".", "\\.");
        tmp = StringUtils.replace(tmp, "*", ".*");
        tmp +="\\b";
        return new RE(tmp);
    }
}
