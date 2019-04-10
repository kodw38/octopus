package com.octopus.tools.dataclient.impl;

import com.octopus.tools.dataclient.IDataEngine;
import com.octopus.tools.dataclient.IDataRouter;
import com.octopus.tools.dataclient.IDictionary;
import com.octopus.tools.dataclient.ds.field.FieldContainer;
import com.octopus.tools.dataclient.ds.field.FieldDef;
import com.octopus.tools.dataclient.ds.field.TableDef;
import com.octopus.tools.dataclient.ds.field.TableDefContainer;
import com.octopus.tools.dataclient.ds.store.TableValue;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * User: Administrator
 * Date: 14-10-23
 * Time: 上午11:15
 */
public class Dictionary extends XMLObject implements IDictionary {
    static transient Log log = LogFactory.getLog(Dictionary.class);
    public Dictionary(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);

        IDataRouter router = (IDataRouter)getPropertyObject("router");
        String dc = xml.getProperties().getProperty("datasource");
        IDataEngine[] engine = router.getRouter(dc,null);
        if(null == engine)throw new Exception("not find dataEngine by dataSource="+dc);
        TableValue fv = engine[0].queryByTableName(xml.getChild("fields")[0].getText(), null);
        FieldDef[] fs = fv.getTableDef().getFieldDefs();
        List<Object[]> fds = fv.getRecordValues();
        if(null != fds){
            for(Object[] ds:fds){
                FieldDef f = new FieldDef();
                int isadd=0;
                for(int i=0;i<fs.length;i++){
                    if(fs[i].getFieldCode().equalsIgnoreCase("FIELD_NAME"))
                        f.setFieldName(String.valueOf(ds[i]));
                    if(fs[i].getFieldCode().equalsIgnoreCase("FIELD_CODE"))
                        f.setFieldCode(String.valueOf(ds[i]));
                    if(fs[i].getFieldCode().equalsIgnoreCase("FIELD_TYPE"))
                        f.setFieldType((String) ds[i]);
                    if(fs[i].getFieldCode().equalsIgnoreCase("STATE"))
                        isadd= (Integer)ds[i];
                }
                if(isadd>0)
                    FieldContainer.addFieldDef(f);
            }
        }

        TableValue tv = engine[0].queryByTableName(xml.getChild("tables")[0].getText(), null);
        FieldDef[] ts = tv.getTableDef().getFieldDefs();
        List<Object[]> tds = tv.getRecordValues();
        Map<String,List<FieldDef>> tables = new HashMap<String,List<FieldDef>>();
        if(null != tds){
            for(Object[] ds:tds){
                int isadd=0;
                String datasource=null,datapath=null,fieldcode=null;
                for(int i=0;i<ts.length;i++){
                    /*if(ts[i].getFieldCode().equalsIgnoreCase("DATA_SOURCE"))
                        datasource=String.valueOf(ds[i]);*/
                    if(ts[i].getFieldCode().equalsIgnoreCase("TABLE_NAME"))
                        datapath=String.valueOf(ds[i]);
                    if(ts[i].getFieldCode().equalsIgnoreCase("FIELD_CODE"))
                        fieldcode=(String)ds[i];
                    if(ts[i].getFieldCode().equalsIgnoreCase("STATE"))
                        isadd= (Integer)ds[i];
                }
                if(isadd>0){
                    if(!tables.containsKey(datasource+"^"+datapath)) tables.put(datasource+"^"+datapath,new LinkedList<FieldDef>());
                    FieldDef fdd = FieldContainer.getField(fieldcode);
                    if(null != fdd)
                        tables.get(datasource+"^"+datapath).add(fdd);
                    else
                        log.error("not find fielddef["+fieldcode+"].");
                }
            }
        }

        if(tables.size()>0){
            Iterator its = tables.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                String[] dn = k.split("\\^");
                TableDef td = new TableDef();
                td.setDataSource(dn[0]);
                td.setName(dn[1]);
                td.setFieldDefs(tables.get(k).toArray(new FieldDef[0]));
                TableDefContainer.addTableDef(td);
            }
        }

        /*FieldDef[] fs = new FieldDef[2];
        fs[0] = new FieldDef();
        fs[0].setFieldCode("STACK_CODE");
        fs[0].setFieldName("名称");
        fs[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        fs[1] = new FieldDef();
        fs[1].setFieldCode("STACK_PRICE");
        fs[1].setFieldType(FieldType.FIELD_TYPE_DOUBLE);

        FieldDef[] ps = new FieldDef[3];
        ps[0] = new FieldDef();
        ps[0].setFieldCode("ISP_CELL_PROPERTY_KEY");
        ps[0].setFieldName("属性名称");
        ps[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        ps[1] = new FieldDef();
        ps[1].setFieldCode("ISP_CELL_PROPERTY_VALUE");
        ps[1].setFieldName("属性值");
        ps[1].setFieldType(FieldType.FIELD_TYPE_STRING);
        ps[2] = new FieldDef();
        ps[2].setFieldCode("ISP_CELL_PROPERTY_GROUP");
        ps[2].setFieldName("属性分类");
        ps[2].setFieldType(FieldType.FIELD_TYPE_STRING);

        FieldDef[] gs = new FieldDef[6];
        gs[0] = new FieldDef();
        gs[0].setFieldCode("USER_CODE");
        gs[0].setFieldName("");
        gs[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        gs[1] = new FieldDef();
        gs[1].setFieldCode("USER_TXPWD");
        gs[1].setFieldName("");
        gs[1].setFieldType(FieldType.FIELD_TYPE_STRING);
        gs[2] = new FieldDef();
        gs[2].setFieldCode("USER_TRANPWD");
        gs[2].setFieldName("");
        gs[2].setFieldType(FieldType.FIELD_TYPE_STRING);
        gs[3] = new FieldDef();
        gs[3].setFieldCode("TRANS_SYSTE");
        gs[3].setFieldName("");
        gs[3].setFieldType(FieldType.FIELD_TYPE_STRING);
        gs[4] = new FieldDef();
        gs[4].setFieldCode("USER_TRANPWD2");
        gs[4].setFieldName("");
        gs[4].setFieldType(FieldType.FIELD_TYPE_STRING);
        gs[5] = new FieldDef();
        gs[5].setFieldCode("ISSIM");
        gs[5].setFieldName("");
        gs[5].setFieldType(FieldType.FIELD_TYPE_INT);

        FieldDef[] uc = new FieldDef[4];
        uc[0] = new FieldDef();
        uc[0].setFieldCode("USER_CODE");
        uc[0].setFieldName("");
        uc[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        uc[1] = new FieldDef();
        uc[1].setFieldCode("S_TYPE");
        uc[1].setFieldName("");
        uc[1].setFieldType(FieldType.FIELD_TYPE_STRING);
        uc[2] = new FieldDef();
        uc[2].setFieldCode("ACCT_CODE");
        uc[2].setFieldName("");
        uc[2].setFieldType(FieldType.FIELD_TYPE_STRING);
        uc[3] = new FieldDef();
        uc[3].setFieldCode("EXCHANGE_TYPE");
        uc[3].setFieldName("");
        uc[3].setFieldType(FieldType.FIELD_TYPE_STRING);

        FieldDef[] si = new FieldDef[5];
        si[0] = new FieldDef();
        si[0].setFieldCode("S_NAME");
        si[0].setFieldName("");
        si[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        si[1] = new FieldDef();
        si[1].setFieldCode("S_CODE");
        si[1].setFieldName("");
        si[1].setFieldType(FieldType.FIELD_TYPE_STRING);
        si[2] = new FieldDef();
        si[2].setFieldCode("S_PY");
        si[2].setFieldName("");
        si[2].setFieldType(FieldType.FIELD_TYPE_STRING);
        si[3] = new FieldDef();
        si[3].setFieldCode("S_TYPE");
        si[3].setFieldName("");
        si[3].setFieldType(FieldType.FIELD_TYPE_STRING);
        si[4] = new FieldDef();
        si[4].setFieldCode("S_SUB_TYPE");
        si[4].setFieldName("");
        si[4].setFieldType(FieldType.FIELD_TYPE_STRING);

        FieldDef[] sd = new FieldDef[9];
        sd[0] = new FieldDef();
        sd[0].setFieldCode("S_CODE");
        sd[0].setFieldName("");
        sd[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        sd[1] = new FieldDef();
        sd[1].setFieldCode("D_ZRSP");
        sd[1].setFieldName("");
        sd[1].setFieldType(FieldType.FIELD_TYPE_DOUBLE);
        sd[2] = new FieldDef();
        sd[2].setFieldCode("D_JYJ");
        sd[2].setFieldName("");
        sd[2].setFieldType(FieldType.FIELD_TYPE_DOUBLE);
        sd[3] = new FieldDef();
        sd[3].setFieldCode("I_JYL");
        sd[3].setFieldName("");
        sd[3].setFieldType(FieldType.FIELD_TYPE_INT);
        sd[4] = new FieldDef();
        sd[4].setFieldCode("D_SALE_PRICE");
        sd[4].setFieldName("");
        sd[4].setFieldType(FieldType.FIELD_TYPE_DOUBLE);
        sd[5] = new FieldDef();
        sd[5].setFieldCode("D_BUY_PRICE");
        sd[5].setFieldName("");
        sd[5].setFieldType(FieldType.FIELD_TYPE_DOUBLE);
        sd[6] = new FieldDef();
        sd[6].setFieldCode("C_DATE");
        sd[6].setFieldName("");
        sd[6].setFieldType(FieldType.FIELD_TYPE_DATE);
        sd[7] = new FieldDef();
        sd[7].setFieldCode("I_BUY_NUM");
        sd[7].setFieldName("");
        sd[7].setFieldType(FieldType.FIELD_TYPE_INT);
        sd[8] = new FieldDef();
        sd[8].setFieldCode("I_SALE_NUM");
        sd[8].setFieldName("");
        sd[8].setFieldType(FieldType.FIELD_TYPE_INT);

        FieldDef[] st = new FieldDef[7];
        st[0] = new FieldDef();
        st[0].setFieldCode("S_TYPE");
        st[0].setFieldName("");
        st[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        st[1] = new FieldDef();
        st[1].setFieldCode("S_TYPE_CODE");
        st[1].setFieldName("");
        st[1].setFieldType(FieldType.FIELD_TYPE_STRING);
        st[2] = new FieldDef();
        st[2].setFieldCode("S_SUB_TYPE");
        st[2].setFieldName("");
        st[2].setFieldType(FieldType.FIELD_TYPE_STRING);
        st[3] = new FieldDef();
        st[3].setFieldCode("S_SUB_TYPE_CODE");
        st[3].setFieldName("");
        st[3].setFieldType(FieldType.FIELD_TYPE_STRING);
        st[4] = new FieldDef();
        st[4].setFieldCode("S_TYPE_DESC");
        st[4].setFieldName("");
        st[4].setFieldType(FieldType.FIELD_TYPE_STRING);
        st[5] = new FieldDef();
        st[5].setFieldCode("S_SUB_TYPE_DESC");
        st[5].setFieldName("");
        st[5].setFieldType(FieldType.FIELD_TYPE_STRING);
        st[6] = new FieldDef();
        st[6].setFieldCode("IS_ENABLE");
        st[6].setFieldName("");
        st[6].setFieldType(FieldType.FIELD_TYPE_STRING);

        FieldDef[] sr = new FieldDef[5];
        sr[0] = new FieldDef();
        sr[0].setFieldCode("USER_CODE");
        sr[0].setFieldName("");
        sr[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        sr[1] = new FieldDef();
        sr[1].setFieldCode("STOCK_RULE");
        sr[1].setFieldName("");
        sr[1].setFieldType(FieldType.FIELD_TYPE_STRING);
        sr[2] = new FieldDef();
        sr[2].setFieldCode("SUCCESS_RATE");
        sr[2].setFieldName("");
        sr[2].setFieldType(FieldType.FIELD_TYPE_DOUBLE);
        sr[3] = new FieldDef();
        sr[3].setFieldCode("STATUS");
        sr[3].setFieldName("");
        sr[3].setFieldType(FieldType.FIELD_TYPE_INT);
        sr[4] = new FieldDef();
        sr[4].setFieldCode("RULE_TYPE");
        sr[4].setFieldName("");
        sr[4].setFieldType(FieldType.FIELD_TYPE_STRING);

        FieldDef[] sl = new FieldDef[8];
        sl[0] = new FieldDef();
        sl[0].setFieldCode("USER_CODE");
        sl[0].setFieldName("");
        sl[0].setFieldType(FieldType.FIELD_TYPE_STRING);
        sl[1] = new FieldDef();
        sl[1].setFieldCode("S_CODE");
        sl[1].setFieldName("");
        sl[1].setFieldType(FieldType.FIELD_TYPE_STRING);
        sl[2] = new FieldDef();
        sl[2].setFieldCode("ISSIM");
        sl[2].setFieldName("");
        sl[2].setFieldType(FieldType.FIELD_TYPE_INT);
        sl[3] = new FieldDef();
        sl[3].setFieldCode("D_JYJ");
        sl[3].setFieldName("");
        sl[3].setFieldType(FieldType.FIELD_TYPE_DOUBLE);
        sl[4] = new FieldDef();
        sl[4].setFieldCode("I_JYL");
        sl[4].setFieldName("");
        sl[4].setFieldType(FieldType.FIELD_TYPE_INT);
        sl[5] = new FieldDef();
        sl[5].setFieldCode("C_DATE");
        sl[5].setFieldName("");
        sl[5].setFieldType(FieldType.FIELD_TYPE_DATE);
        sl[6] = new FieldDef();
        sl[6].setFieldCode("OP_STATUS");
        sl[6].setFieldName("");
        sl[6].setFieldType(FieldType.FIELD_TYPE_STRING);
        sl[7] = new FieldDef();
        sl[7].setFieldCode("STOCK_RULE");
        sl[7].setFieldName("");
        sl[7].setFieldType(FieldType.FIELD_TYPE_STRING);


        FieldContainer.addFieldDef(fs[0]);
        FieldContainer.addFieldDef(fs[1]);
        FieldContainer.addFieldDef(ps[0]);
        FieldContainer.addFieldDef(ps[1]);
        FieldContainer.addFieldDef(ps[2]);
        FieldContainer.addFieldDef(gs[0]);
        FieldContainer.addFieldDef(gs[1]);
        FieldContainer.addFieldDef(gs[2]);
        FieldContainer.addFieldDef(gs[3]);
        FieldContainer.addFieldDef(gs[4]);
        FieldContainer.addFieldDef(gs[5]);
        FieldContainer.addFieldDef(si[0]);
        FieldContainer.addFieldDef(si[1]);
        FieldContainer.addFieldDef(si[2]);
        FieldContainer.addFieldDef(si[3]);
        FieldContainer.addFieldDef(si[4]);
        FieldContainer.addFieldDef(st[0]);
        FieldContainer.addFieldDef(st[1]);
        FieldContainer.addFieldDef(st[2]);
        FieldContainer.addFieldDef(st[3]);
        FieldContainer.addFieldDef(st[4]);
        FieldContainer.addFieldDef(st[5]);
        FieldContainer.addFieldDef(st[6]);
        FieldContainer.addFieldDef(sd[0]);
        FieldContainer.addFieldDef(sd[1]);
        FieldContainer.addFieldDef(sd[2]);
        FieldContainer.addFieldDef(sd[3]);
        FieldContainer.addFieldDef(sd[4]);
        FieldContainer.addFieldDef(sd[5]);
        FieldContainer.addFieldDef(sd[6]);
        FieldContainer.addFieldDef(sd[7]);
        FieldContainer.addFieldDef(sd[8]);
        FieldContainer.addFieldDef(sr[0]);
        FieldContainer.addFieldDef(sr[1]);
        FieldContainer.addFieldDef(sr[2]);
        FieldContainer.addFieldDef(sr[3]);
        FieldContainer.addFieldDef(sr[4]);
        FieldContainer.addFieldDef(uc[0]);
        FieldContainer.addFieldDef(uc[1]);
        FieldContainer.addFieldDef(uc[2]);
        FieldContainer.addFieldDef(uc[3]);
        FieldContainer.addFieldDef(sl[0]);
        FieldContainer.addFieldDef(sl[1]);
        FieldContainer.addFieldDef(sl[2]);
        FieldContainer.addFieldDef(sl[3]);
        FieldContainer.addFieldDef(sl[4]);
        FieldContainer.addFieldDef(sl[5]);
        FieldContainer.addFieldDef(sl[6]);

        TableDef t2 = new TableDef();
        t2.setDataSource("gs");
        t2.setName("mysql/ISP_CELL_PROPERTY");
        t2.setFieldDefs(ps);
        TableDefContainer.addTableDef(t2);

        TableDef t3 = new TableDef();
        t3.setDataSource("gs");
        t3.setName("mysql/STOCK_USER");
        t3.setFieldDefs(gs);
        TableDefContainer.addTableDef(t3);

        TableDef t4 = new TableDef();
        t4.setDataSource("gs");
        t4.setName("mysql/STOCK_INFO");
        t4.setFieldDefs(si);
        TableDefContainer.addTableDef(t4);

        TableDef t5 = new TableDef();
        t5.setDataSource("gs");
        t5.setName("mysql/STOCK_TYPE");
        t5.setFieldDefs(st);
        TableDefContainer.addTableDef(t5);

        TableDef t6 = new TableDef();
        t6.setDataSource("gs");
        t6.setName("mysql/STOCK_DATA");
        t6.setFieldDefs(sd);
        TableDefContainer.addTableDef(t6);

        TableDef t7 = new TableDef();
        t7.setDataSource("gs");
        t7.setName("mysql/STOCK_RULE");
        t7.setFieldDefs(sr);
        TableDefContainer.addTableDef(t7);

        TableDef t8 = new TableDef();
        t8.setDataSource("gs");
        t8.setName("mysql/STOCK_USER_ACCT");
        t8.setFieldDefs(uc);
        TableDefContainer.addTableDef(t8);

        TableDef t9 = new TableDef();
        t9.setDataSource("gs");
        t9.setName("mysql/STOCK_LOG");
        t9.setFieldDefs(sl);
        TableDefContainer.addTableDef(t9);*/

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

    @Override
    public FieldDef[] getFieldDef(String likeFieldName) {
        return new FieldDef[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
