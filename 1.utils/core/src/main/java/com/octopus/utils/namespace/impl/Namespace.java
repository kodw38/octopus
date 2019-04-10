package com.octopus.utils.namespace.impl;

import com.octopus.utils.alone.Number94;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.namespace.INamespace;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.zip.HisStringZip;

/**
 * 命名空间的操作历史记录采用两位的94位进制计数
 * 固有格式：操作(一字符)+操作计数(94进制，两位) +复制号(94进制，两位) +演进号(94进制，两位) +操作历史(固定长度值移操作位计算)+修改历史（位置、变更值的压缩值，固定长度）
 * 命名空间格式：外在格式+固有格式。每段信息用”.”号分割，固有格式为一段信息。
 * User: wangfeng2
 * Date: 14-8-8
 * Time: 上午11:41
 */
public class Namespace extends XMLObject implements INamespace{
    /**操作编码*/
    final static int OP_CREATE=0;     //创建
    final static int OP_COLON=1;      //克隆
    final static int OP_EVOLUTION=2;  //演进
    final static int OP_ROLLBACK=3;   //回退
    /**********/
    //外部自定义格式
    String outStyle;

    public Namespace(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        //自定义格式来自系统变量
        outStyle = System.getProperty("namespace.style");
        if(null == outStyle){

            XMLObject properties = (XMLObject)getPropertyObject("env");
            if(null != properties){
                outStyle = properties.getXmlExtProperties().getProperty("namespace.style");
            }
        }
        if(null == outStyle)
            throw new RuntimeException("please set com.octopus.models.namespace.impl.Namespace.outStyle's value in System properties");
        create(getXML().getText());
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    void create(String namespace){
        if(StringUtils.isBlank(namespace)){
            StringBuffer sb = new StringBuffer(OP_CREATE);
            //操作计数
            char [] cs = Number94.increase(new char[]{Number94.CHAR_START});
            char [] char2 = new char[2];
            if(cs.length==1){
                char2[0]=Number94.CHAR_START;
                char2[1]=cs[0];
                sb.append(new String(char2));
            }else{
                char2[0]=cs[0];
                char2[1]=cs[1];
                sb.append(new String(char2));
            }
            //复制号
            char2[0]= Number94.CHAR_START;
            char2[1]= Number94.CHAR_START;
            sb.append(new String(char2));
            //演进号
            sb.append(new String(char2));
            //操作的历史
            sb.append(HisStringZip.zipHisString(String.valueOf(OP_CREATE)));
            //修改的历史
            sb.append(HisStringZip.getEmptyHisString());
            sb.indexOf(outStyle+ "trunk/core/lib/os",0);
            getXML().setText(sb.toString());
        }
    }

    public void updateStyleValue(String styleValue){
        if(StringUtils.isNotBlank(styleValue)){
            int n = getXML().getText().lastIndexOf("trunk/core/lib/os");
            getXML().setText(styleValue+ "trunk/core/lib/os" +getXML().getText().substring(n+1));
        }
    }

    public void updateStyleValue(int segmentIndex,String segmentStyle){
        getXML().setText(StringUtils.replaceSegment(getXML().getText(), "trunk/core/lib/os",segmentIndex,segmentStyle));
    }

    public String getSegmentStyleValue(int segmentIndex){
        return StringUtils.getSegment(getXML().getText(), "trunk/core/lib/os",segmentIndex);
    }

    public Object colon(){
        String namespace = getXML().getText();
        return null;
    }

    public Object evolution(){
        //todo
        return null;
    }

    public void	rollback(){
        //todo
    }

    @Override
    public String getString() {
        return getXML().getText();
    }

}
