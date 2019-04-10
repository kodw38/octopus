package com.octopus.tools.deploy.property;

import com.octopus.tools.deploy.IPropertiesGetter;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.file.impl.excel.ExcelReader;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * User: Administrator
 * Date: 15-1-15
 * Time: 上午11:00
 */
public class ExcelPropertiesGetter implements IPropertiesGetter{
    @Override
    public Map<String, List<Properties>> getCommandProperties(String configExcel) {
        try{

            ExcelReader ex = new ExcelReader(new FileInputStream(configExcel));
            List<Map<String,String>> machines = ex.getSheepData("Server Environment");
            List<Map<String,String>>  users = ex.getSheepData("Machine User");
            List<Map<String,String>> media = ex.getSheepData("Media Info");
            List<Map<String,String>> db = ex.getSheepData("DataBase Info");
            List<Map<String,String>> sql = ex.getSheepData("DB SQL");
            List<List<Map<String,String>>> datas = new ArrayList<List<Map<String, String>>>();
            datas.add(machines);
            datas.add(users);
            datas.add(media);
            datas.add(db);
            datas.add(sql);
            Map<String,String> relation = new HashMap<String, String>();
            //把excel中的记录根据第一行字段名称组合关系
            Map<String,List<Properties>> ret = ObjectUtils.pickLinesByRelation(datas,new int[][]{{0,1,2},{0,1,3,4}},new String[]{"CommandNames"},"CommandNames");
            Map<String,List<Properties>> result = new HashMap<String,List<Properties>>();
            Iterator<String> its = ret.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                List<Properties> ls = ret.get(k);
                String[] ks = k.split(",");
                for(String tk:ks){
                    String usertype="";
                    String cmd = tk;
                    if(tk.contains(":")){
                        usertype=tk.substring(0,tk.indexOf(":")).toUpperCase();
                        cmd = tk.substring(tk.indexOf(":")+1);
                    }

                    if(!result.containsKey(cmd)){
                        result.put(cmd,new ArrayList<Properties>());
                    }
                    for(Properties p:ls){
                        Properties tmp = new Properties();
                        tmp.putAll(p);
                        if(StringUtils.isNotBlank(usertype)){
                            tmp.put("^UserType",usertype);
                        }
                        tmp.setProperty("CommandNames",cmd);
                        result.get(cmd).add(tmp);
                    }

                }
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args){
        try{
            ExcelPropertiesGetter g = new ExcelPropertiesGetter();
            String configExcel = "";
            Map<String,List<Properties>> m = g.getCommandProperties(configExcel);
            List<Properties> cms = m.get("installWeb");
            if(null != cms){
                for(Properties p:cms){
                    System.out.println(p.toString());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
