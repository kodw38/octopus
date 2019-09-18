package com.octopus.utils.xml;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * User: wf
 * Date: 2008-8-19
 * Time: 22:32:00
 */
public class XMLMakeup implements Serializable,Comparable{
    private Properties properties = new Properties();
    private String text;
    private String name;
    private List<XMLMakeup> children =new ArrayList();
    private XMLMakeup parent;
    private String sourcePath;
    private List<String> dynkeys=new LinkedList<String>();

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getId(){
        if(this.getProperties().containsKey("id") && StringUtils.isNotBlank(this.getProperties().getProperty("id"))){
            return this.getProperties().getProperty("id");
        }else if(this.getProperties().containsKey("key") && StringUtils.isNotBlank(this.getProperties().getProperty("key"))){
            return this.getProperties().getProperty("key");
        }else if(this.getProperties().containsKey("seq") && StringUtils.isNotBlank(this.getProperties().getProperty("seq"))){
            return this.getName()+"_"+this.getProperties().getProperty("seq");
        }else{
            return this.getName();
        }
    }
    public Properties getProperties() {
        return properties;
    }

    /**
     * 根据孩子中的属性key或name，和text值构成一个properties对象
     * @param chilename
     * @return
     */
    public Properties getPropertiesByChildNameAndText(String chilename){
        XMLMakeup[] ms = getChild(chilename);
        if(null != ms && ms.length>0){
            Properties p = new Properties();
            for(XMLMakeup m:ms){
                if(null != m){
                    String k = m.getProperties().getProperty("key");
                    if(null ==k){
                        k = m.getProperties().getProperty("name");
                    }
                    if(StringUtils.isNotBlank(k)) {
                        p.put(k, m.getText());
                    }
                }
            }
            return p;
        }
        return null;
    }

    /**
     * 把孩子中的属性 ,kenName值作为key，valueName值作为value构成成一个properties对象
     * @param chilename
     * @return
     */
    public Properties getPropertiesByChildProperty(String chilename,String keyName,String valueName){
        XMLMakeup[] ms = getChild(chilename);
        if(null != ms && ms.length>0){
            Properties p = new Properties();
            for(XMLMakeup m:ms){
                if(null != m){
                    String k = m.getProperties().getProperty(keyName);
                    String v = m.getProperties().getProperty(valueName);
                    if(StringUtils.isNotBlank(k) && StringUtils.isNotBlank(v)){
                        p.put(k, v);
                    }
                }
            }
            return p;
        }
        return null;
    }

    public Properties getPropertiesByChildNameValue(){
        return getPropertiesByChildProperty("property","name","value");
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getType(){
    	return this.properties.getProperty("type");
    }
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<XMLMakeup> getChildren() {
        return children;
    }

    public void setChildren(ArrayList children) {
        this.children = children;
    }

    public XMLMakeup getParent() {
        return parent;
    }
    public XMLMakeup getRoot(){
        XMLMakeup r = this;
        while(null !=r){
            if(null == r.getParent())break;
            r = r.getParent();
        }
        return r;
    }
    public void setParent(XMLMakeup parent) {
        this.parent = parent;
    }
    public void addChildProperty(String key,String value) throws Exception {
        children.add(XMLUtil.getDataFromString("<property name=\""+key+"\">"+value+"</property>"));
    }
    public void addChild(XMLMakeup xmlMakeup){
        children.add(xmlMakeup);
    }

    public XMLMakeup[] getChild(String title){
        if(null != this.children){
            XMLMakeup temp;
            LinkedList list = new LinkedList();
            for(int i=0;i<this.getChildren().size();i++){
                temp = (XMLMakeup)this.getChildren().get(i);
                if(null != temp && temp.getName().equalsIgnoreCase(title)){
                    list.add(temp); 
                }
            }
            return (XMLMakeup[])list.toArray(new XMLMakeup[0]);
        }
        return null;
    }
    public XMLMakeup getFirstChildById(String Id){
        if(null != this.children && StringUtils.isNotBlank(Id)){
            XMLMakeup temp;
            for(int i=0;i<this.getChildren().size();i++){
                temp = (XMLMakeup)this.getChildren().get(i);
                if(null != temp && Id.equalsIgnoreCase(temp.getId())){
                    return temp;
                }
            }
        }
        return null;
    }
    public List<Map> getChildrenPropertiesByTag(String title){
        if(null != this.children){
            XMLMakeup temp;
            LinkedList list = new LinkedList();
            for(int i=0;i<this.getChildren().size();i++){
                temp = (XMLMakeup)this.getChildren().get(i);
                if(null != temp && temp.getName().equalsIgnoreCase(title)){
                    list.add(temp.getProperties());
                }
            }
            return list;
        }
        return null;
    }
    public XMLMakeup getFirstChildrenEndWithName(String name){
        return getFirstChildrenEndWithName(this,name);
    }
    public List<XMLMakeup> findByEndWithNameAndEndWithParentName(String title,String parenttitle){
        List<XMLMakeup> ret = new ArrayList();
        findByEndWithNameAndEndWithParentName(this,title,parenttitle,ret);
        return ret;
    }

    public static void findByEndWithNameAndEndWithParentName(XMLMakeup root,String title,String parenttitle,List<XMLMakeup> result){
        if(root.getName().endsWith(title) && null != root.getParent() && root.getParent().getName().endsWith(parenttitle)){
            result.add(root);
        }
        if(root.getChildren().size()>0){
            for(XMLMakeup c:root.getChildren()){
                findByEndWithNameAndEndWithParentName(c,title,parenttitle,result);
            }
        }
    }
    public static XMLMakeup getFirstChildrenEndWithName(XMLMakeup root,String name){
        if(root.getName().endsWith(name)){
            return root;
        }

        for(int i=0;i<root.getChildren().size();i++) {
            XMLMakeup x = getFirstChildrenEndWithName(root.getChildren().get(i),name);
            if(null != x){
                return x;
            }
        }


        return null;
    }
    public List<XMLMakeup> getChildrenEndWithName(String name){
        List ret = new ArrayList();
        getChildrenEndWithName(this,name,ret);
        return ret;
    }
    public List<XMLMakeup> getChildrenEndWithNameAndExistProperty(String name,String proName){
        List ret = new ArrayList();
        getChildrenEndWithNameAndExistProperty(this, name, proName, ret);
        return ret;
    }
    public static void getChildrenEndWithNameAndExistProperty(XMLMakeup root,String name,String proName,List ret){
        if(null != root && root.getName().endsWith(name) && StringUtils.isNotBlank(root.getProperties().getProperty(proName))){
            ret.add(root);
        }
        for(int i=0;i<root.getChildren().size();i++){
            getChildrenEndWithNameAndExistProperty(root.getChildren().get(i),name,proName,ret);
        }

    }
    public static void getChildrenEndWithName(XMLMakeup root,String name,List ret){
        if(root.getName().endsWith(name)){
            ret.add(root);
        }
        for(int i=0;i<root.getChildren().size();i++){
            getChildrenEndWithName(root.getChildren().get(i),name,ret);
        }
    }
    public boolean isProperty(String propertyName){
        return properties.containsKey(propertyName);
    }

    public XMLMakeup[] find(String title){
        List li = new ArrayList();
        cyclefind(this,new String[]{title},li);
        return (XMLMakeup[])li.toArray(new XMLMakeup[0]);
    }
    public XMLMakeup[] finds(String[] titles){
        List li = new ArrayList();
        cyclefind(this,titles,li);
        return (XMLMakeup[])li.toArray(new XMLMakeup[0]);
    }
    private void cyclefind(XMLMakeup root,String[] titles,List li){
        if(ArrayUtils.isIgnoreCaseInStringArray(titles,root.getName())){
            li.add(root);
        }
        if(null != root.getChildren() && root.getChildren().size()>0){
            XMLMakeup temp;
            for(int i=0;i<root.getChildren().size();i++){
                temp = (XMLMakeup)root.getChildren().get(i);
                cyclefind(temp,titles,li);
            }
        }
    }

    private void cyclefindByProperty(XMLMakeup root,String key,String value,List li){
        String v = (String)root.getProperties().get(key);
        if(!StringUtils.isBlank(v) && v.equalsIgnoreCase(value)){
            li.add(root);
        }
        if(null != root.getChildren() && root.getChildren().size()>0){
            XMLMakeup temp;
            for(int i=0;i<root.getChildren().size();i++){
                temp = (XMLMakeup)root.getChildren().get(i);
                cyclefindByProperty(temp,key,value,li);
            }
        }
    }
    
    private void cyclecontinsByProperty(XMLMakeup root,String key,List li){
        String v = (String)root.getProperties().get(key);
        if(!StringUtils.isBlank(v)){
            li.add(root);
        }
        if(null != root.getChildren() && root.getChildren().size()>0){
            XMLMakeup temp;
            for(int i=0;i<root.getChildren().size();i++){
                temp = (XMLMakeup)root.getChildren().get(i);
                cyclecontinsByProperty(temp,key,li);
            }
        }
    }
    private boolean  cyclecontinsKey(String key){
        String k = this.getId();
        if(StringUtils.isNotBlank(k)){
            if(k.equals(key)){
                return true;
            }
            if(null != getChildren() && getChildren().size()>0){
                XMLMakeup temp;
                for(int i=0;i<getChildren().size();i++){
                    temp = (XMLMakeup)getChildren().get(i);
                    boolean  b = temp.cyclecontinsKey(key);
                    if(b)
                        return b;
                }
            }
        }
        return false;
    }
    private void cyclefindByTitleProperty(XMLMakeup root,String title,String key,String value,List li){
    	if(root.getName().equalsIgnoreCase(title) && value.equalsIgnoreCase(root.getProperties().getProperty(key))){
    		li.add(root);
    	}
    	if(null != root.getChildren() && root.getChildren().size()>0){
            XMLMakeup temp;
            for(int i=0;i<root.getChildren().size();i++){
                temp = (XMLMakeup)root.getChildren().get(i);
                cyclefindByTitleProperty(temp,title,key,value,li);
            }
        }
    }

    public  XMLMakeup findFirstByTitle(String title){
        return findFirstByTitle(this,title);
    }
    private XMLMakeup findFirstByTitle(XMLMakeup root,String title){
        if(root.getName().equalsIgnoreCase(title))
            return root;
        else if(null != root.getChildren() && root.getChildren().size()>0){
            for(int i=0;i<root.getChildren().size();i++){
                XMLMakeup ret = findFirstByTitle(root.getChildren().get(i),title);
                if(null != ret)
                    return ret;
            }
        }
        return null;
    }
    public  XMLMakeup findFirstTextByTitleProperty(String title,String propertyKey,String propertyValue){
        return findFirstTextByTitleProperty(this,title,propertyKey,propertyValue);
    }
    private XMLMakeup findFirstTextByTitleProperty(XMLMakeup root,String title,String propertyKey,String propertyValue){
        if(root.getName().equalsIgnoreCase(title) && propertyValue.equals(root.getProperties().getProperty(propertyKey)))
            return root;
        else if(null != root.getChildren() && root.getChildren().size()>0){
            for(int i=0;i<root.getChildren().size();i++){
                XMLMakeup ret = findFirstTextByTitleProperty(root.getChildren().get(i), title, propertyKey, propertyValue);
                if(null != ret)
                    return ret;
            }
        }
        return null;
    }

    public XMLMakeup[] getByProperty(String key,String value){
        if(!StringUtils.isBlank(key) && !StringUtils.isBlank(value)){
            List li = new ArrayList();
            cyclefindByProperty(this,key,value,li);
            if(li.size()>0)
            return (XMLMakeup[])li.toArray(new XMLMakeup[0]);
        }
        return null;
    }


    public boolean existKey(String key){
        if(dynkeys.contains(key))
            return true;
        else{
            boolean  ret= cyclecontinsKey(key);
            if(ret){
                dynkeys.add(key);
            }
            return ret;
        }
    }

    public String[] getChildrenPropertiesValue(String proName){
        List<String> ls = new ArrayList<String>();
        cycleFindProperty(this,proName,ls);
        return ls.toArray(new String[0]);
    }
    public String getFirstCurChildText(String tagName,String childPropertyKey,String childPropertyValue){
        for(XMLMakeup c:children){
            if(c.getName().equals(tagName) && childPropertyValue.equals(c.getProperties().getProperty(childPropertyKey)))
                return c.getText();
        }
        return null;
    }
    public String getFirstCurChildText(String tagName){
        for(XMLMakeup c:children){
            if(c.getName().equals(tagName))
                return c.getText();
        }
        return null;
    }
    public String getFirstCurChildKeyValue(String tagName,String key){
        for(XMLMakeup c:children){
            if(c.getName().equals(tagName))
                return c.getProperties().getProperty(key);
        }
        return null;
    }
    public Map getChildrenKeyValue(String tagName){
        Map map = new LinkedHashMap();
        for(XMLMakeup c:children){
            if(c.getName().equals(tagName)){
                String k = c.getProperties().getProperty("key");
                String v = c.getProperties().getProperty("value");
                if(StringUtils.isNotBlank(k) &&StringUtils.isNotBlank(v))
                    map.put(k,v);
            }
        }
        if(map.size()>0)
            return map;
        return null;
    }
    void cycleFindProperty(XMLMakeup x,String proName,List<String> ls){
        if(x.getChildren().size()>0){
            for(XMLMakeup c:children){
                if(null !=c.getProperties().getProperty(proName) && !ls.contains(c.getProperties().getProperty(proName))){
                    ls.add(c.getProperties().getProperty(proName));
                    if(c.getChildren().size()>0)
                         cycleFindProperty(x,proName,ls);
                }
            }
        }
    }
    
    public XMLMakeup[] getByTagProperty(String title,String key,String value){
    	if(!StringUtils.isBlank(title) && !StringUtils.isBlank(key) && !StringUtils.isBlank(value)){
            List li = new ArrayList();
            cyclefindByTitleProperty(this,title,key,value,li);
            if(li.size()>0)
            return (XMLMakeup[])li.toArray(new XMLMakeup[0]);
        }
        return null;
    }
    
    public XMLMakeup[] getChildByType(String type){
    	return getByProperty("type",type);
    }
        
    public XMLMakeup[] contansProperty(String property){
    	List li = new ArrayList();
    	cyclecontinsByProperty(this,property,li);
    	if(li.size()>0)
            return (XMLMakeup[])li.toArray(new XMLMakeup[0]);
    	return null;
    }

    /**
     * ����������
     * @param xml
     */
    public boolean addChildByTitle(XMLMakeup xml){
        XMLMakeup[] fs = find(xml.getName());
        XMLMakeup parent=this;
        if(ObjectUtils.isNotNull(fs)){
            if(ObjectUtils.isNotNull(fs[0].getParent()))
            	parent = fs[0].getParent();
            		
        }
       parent.getChildren().add(xml);
        
        return true;
    }

    /**
     * ɾ��ĳ����xml
     * @param xmlId
     * @return
     */
    public boolean removeChildById(String xmlId){
        if(ObjectUtils.isNotNull(xmlId)){
            XMLMakeup[] te = getByProperty("id",xmlId);
            if(ObjectUtils.isNotNull(te) && ObjectUtils.isNotNull(te[0].getParent())){
                te[0].getParent().getChildren().remove(te[0]);
                return true;
            }
        }
        return false;
    }

    /**
     * ����ĳ����xml
     * @param xml
     * @return
     */
    public boolean updateChildByTitleAndId(XMLMakeup xml){
        if(removeChildById(xml.getProperties().getProperty("id"))){
            return addChildByTitle(xml);
        }
        return false;
    }
    
    public void getStringBuffer(XMLMakeup xml,StringBuffer sb){
    	sb.append("<").append(xml.getName());
    	if(ObjectUtils.isNotNull(xml.getProperties())){
    		Enumeration es = xml.getProperties().keys();
    		while(es.hasMoreElements()){
    			String key = (String)es.nextElement();
                String s = StringUtils.replace(xml.getProperties().getProperty(key),"\"","\\\"");
    			sb.append(" ").append(key).append("=").append("\"").append(s).append("\"");
    		}
    	}
    	if(ObjectUtils.isNotNull(xml.getChildren()) && xml.getChildren().size()>0){
        	sb.append(">");
        	for(int i=0;i<xml.getChildren().size();i++){
        		//sb.append("\n");
        		getStringBuffer((XMLMakeup)xml.getChildren().get(i),sb);
        	}
        	//sb.append("\n");
    		sb.append("</").append(xml.getName()).append(">");
    	}else if(ObjectUtils.isNotNull(xml.getText())){
        	sb.append(">");
        	sb.append(xml.getText()).append("</").append(xml.getName()).append(">");	
    	}else{
    		sb.append("/>");
    	}    	
    }

    
    public String toString(boolean hasHeader){
        StringBuffer sb = new StringBuffer();
        if(hasHeader)
    	    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    	getStringBuffer(this,sb);
    	return sb.toString();
    }
    public Map toMap()throws Exception{
        if(null != children && children.size()>0){
            HashMap map = new HashMap();
            for(XMLMakeup x:children){
                if(x.children.size()==0){
                    String value =x.getText(),key=x.getId();
                    if(x.getProperties().containsKey("key")){
                        key=x.getProperties().getProperty("key");
                    }
                    if(x.getProperties().containsKey("value")){
                        value = x.getProperties().getProperty("value");
                    }
                    if(x.getProperties().size()==1 && !x.getProperties().containsKey("key") && StringUtils.isBlank(x.getText())){
                        Iterator its = x.getProperties().keySet().iterator();
                        while(its.hasNext()){
                            key = (String)its.next();
                            value = (String)x.getProperties().get(key);
                            break;
                        }
                    }
                    if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)){
                        map.put(key,value);
                    }
                }else{
                    Map m = x.toMap();
                    if(null !=m && m.size()>0){
                        map.put(x.getId(),m);
                    }
                }
            }
            return map;
        }
        throw new Exception("not support this structure now");
    }
    public String toFormatString()throws Exception{
        return StringUtils.formatXml(toString(false));
    }

    public String toString(){
        return toString(false);
    }

    public XMLMakeup clone(){
        try{
            return XMLUtil.getDataFromString(toString());
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void copyFrom(XMLMakeup xml){
        setName(xml.getName());
        setText(xml.getText());
        properties.clear();
        Enumeration ens = xml.getProperties().keys();
        while(ens.hasMoreElements()){
            Object key = ens.nextElement();
            properties.put(key,xml.getProperties().get(key));
        }
        if(xml.getChildren().size()>0){
            for(int i=0;i<xml.getChildren().size();i++){
                XMLMakeup cx = ((XMLMakeup)xml.getChildren().get(i)).clone();
                cx.setParent(this);
                getChildren().add(cx);
            }
        }
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof XMLMakeup && StringUtils.isNotBlank(properties.getProperty("seq")) && StringUtils.isNotBlank(((XMLMakeup)o).getProperties().getProperty("seq")) ){
            return Double.parseDouble(properties.getProperty("seq"))>Double.parseDouble(((XMLMakeup)o).getProperties().getProperty("seq"))?1:0;
        }
        return 0;
    }

    /**
     * 根据路径设置属性，路径用.分割 是针对xmlmakup对象，属性是key=value 字符串。
     * @param pathMapProperties key=value [childid=ProMap]
     */
    public void setPropertiesByPathMap(Map pathMapProperties){
        if(null !=pathMapProperties){
            Iterator<String> its = pathMapProperties.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                Object v = pathMapProperties.get(k);
                String cur=k;
                if(k.contains(".")){
                    cur = k.substring(0,k.indexOf("."));
                }

                boolean isc = false;
                if(v instanceof Map) {
                    List<XMLMakeup> ls = getChildren();
                    if (null != ls && ls.size() > 0) {
                        for (XMLMakeup x : ls) {
                            if (cur.equals(x.getId())) {

                                x.setPropertiesByPathMap((Map)v);
                                isc = true;
                                break;
                            }
                        }
                    }
                }
                if(!isc && v instanceof String) {
                    if(getProperties().containsKey(k)){
                        String c = (String)getProperties().get(k);
                        if(c.startsWith("{") && ((String) v).startsWith("{")){
                            Map o = StringUtils.convert2MapJSONObject(c);
                            Map n = StringUtils.convert2MapJSONObject((String)v);
                            o.putAll(n);
                            getProperties().put(k,ObjectUtils.convertMap2String(o));
                        }else {
                            getProperties().remove(k);
                            getProperties().put(k, v);
                        }
                    }else {
                        getProperties().put(k, v);
                    }
                }

            }

        }
    }

    /**
     * 删除路径下的属性key,path针对xmlmakeup结构，key是子对象id，属性key组成，value是属性value中的json结构的路径
     * @param paths
     * input
     * [input,output]
     * {input:'alarm',output:'alarm'}
     * {input:'alarm.notification',output:'alarm.check'}
     * {a:{input:'data.username'}}
     * {a:[input,output]}
     * {a:input}
     * @return
     */
    public boolean removePropertiesByPath(Object paths){
        boolean is=false;
        if(null != paths){
            if(paths instanceof String){
                if(getProperties().containsKey(paths)){
                    getProperties().remove(paths);
                }
            }else if(paths instanceof Map){
                Iterator its = ((Map)paths).keySet().iterator();
                while(its.hasNext()){
                    String k = (String)its.next();
                    Object v = ((Map)paths).get(k);
                    if(v instanceof String){
                        if(getProperties().containsKey(k)) {
                            String n = getRemoveProperty(getProperties().getProperty(k), (String)v);
                            if (null != n) {
                                if("{}".equals(n)){
                                    getProperties().remove(k);
                                }else {
                                    getProperties().put(k, n);
                                }
                                is=true;
                            }
                        }else{
                            List<XMLMakeup> ls = getChildren();
                            if(null != ls && ls.size()>0){
                                for(XMLMakeup x:ls){
                                    if(k.equals(x.getId())){
                                        x.removePropertiesByPath(v);
                                    }
                                }
                            }
                        }
                    }else if(v instanceof List){
                        for(int i=0;i<((List)v).size();i++){
                            if(getProperties().containsKey(((List)v).get(i))){
                                getProperties().remove(((List)v).get(i));
                            }
                        }
                    }else if(v instanceof Map){
                        List<XMLMakeup> ls = getChildren();
                        if(null != ls && ls.size()>0){
                            for(XMLMakeup x:ls){
                                if(k.equals(x.getId())){
                                    x.removePropertiesByPath(v);
                                }
                            }
                        }
                    }
                }
            }
        }

        return is;
    }

    String getRemoveProperty(String c,String path){
        if(c.startsWith("{")){
            Map m = StringUtils.convert2MapJSONObject(c);
            if(null != m){
                ObjectUtils.removeValueByPath(m,path);
                return ObjectUtils.convertMap2String(m);
            }
        }
        return null;
    }

    public boolean isEnable(){
        if(StringUtils.isNotBlank(properties.getProperty("isenable"))){
            return StringUtils.isTrue(properties.getProperty("isenable"));
        }
        return true;
    }

}
