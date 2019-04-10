package com.octopus.tools.dataclient.impl.engines;

import com.octopus.tools.dataclient.IDataEngine;
import com.octopus.tools.dataclient.ds.DelCnd;
import com.octopus.tools.dataclient.ds.QueryCondition;
import com.octopus.tools.dataclient.ds.UpdateData;
import com.octopus.tools.dataclient.ds.store.TableValue;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-24
 * Time: 下午5:55
 */
public class FileEngine extends XMLObject implements IDataEngine {
    IDCS dcs;
    ITyper typer;

    public FileEngine(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
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
    public Object add(String opId, Map[] data, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object delete(DelCnd delData, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean add(TableValue[] tableValues,Object env) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TableValue query(QueryCondition cnd, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TableValue queryByTableName(String tableName, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean update(UpdateData updateData) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object update(String opId, Map cnd, Map data, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object rollbackAdd(String opId, Map[] data, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object rollbackDelete(String opId, Map cnd, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object rollbackUpdate(String opId, Map cnd, Map data, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object query(String opId, Map cnd, int startIndex, int endIndex, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getMetaData(String opId, Map cnd, Object env) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
