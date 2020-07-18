package com.octopus.tools.utils;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.img.IImgIdent;
import com.octopus.utils.img.ImageUtils;
import com.octopus.utils.img.impl.BackgroundValidateCode;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-11-20
 * Time: 下午2:57
 */
public class ImgIdentTool extends XMLDoObject {
    transient static Log log = LogFactory.getLog(ImgIdentTool.class);
    Map<String,IImgIdent> imgIdents = new HashMap();
    public ImgIdentTool(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);

        //imgIdents.put("delline",new LineValidateCode(""));
        imgIdents.put("background",new BackgroundValidateCode());

    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env,Map input,Map output,Map cfg) throws Exception {
        if(null != input) {
            if(input.containsKey("verifyImage")) {
                Object o = input.get("verifyImage");
                if (o instanceof InputStream) {
                    InputStream in = (InputStream) o;
                    if (null != input) {
                        BufferedImage img = ImageIO.read(in);

                        //ImageIO.write(img,"png",new File("/home/stock/pngs/"+System.currentTimeMillis()+".png"));
//                String[] b= FileUtils.getFileContent("c:/log/b.txt");
//                return b[0];


                        String s = imgIdents.get(input.get("verifyType")).getValidatecode(img, (Integer) input.get("len"), (String) input.get("tempdir"));
                        if (StringUtils.isNotBlank(s)) {
                            return s;
                        }

                    }
                } else {
                    if (log.isDebugEnabled()) {
                        System.out.println(o);
                    }
                }
            }else if(input.containsKey("op")){
                String op = (String)input.get("op");
                if("compare".equalsIgnoreCase(op)){
                    String type = (String)input.get("type");
                    if("hash".equalsIgnoreCase(type)){
                        Object f1 = input.get("file1");
                        Object f2 = input.get("file2");
                        int ret=0;
                        if(f1 instanceof String && f2 instanceof String) {
                            ret = ImageUtils.comparePHash((String)f1, (String)f2);
                        }else if(f1 instanceof String && f2 instanceof ByteArrayOutputStream )
                            ret = ImageUtils.comparePHash((String)f1,(ByteArrayOutputStream)f2);
                        if(ret>=5){
                            return false;
                        }else{
                            return true;
                        }
                    }
                }
            }
        }
        return null;

    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map cfg) throws Exception {
        if(null != input && null != input.get("verifyImage") && ((InputStream)input.get("verifyImage")).available()>0 && null != input && null != input.get("verifyType") && null != input.get("len")){
            return true;
        }else if(input.containsKey("op")){
            return true;
        }
        return false;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input,Map output, Map cfg, Object ret) throws Exception {
        if(log.isDebugEnabled()) {
            System.out.println("    img ident:"+ret);
        }
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

}
