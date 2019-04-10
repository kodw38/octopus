package com.octopus.utils.cls.proxy;

import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-12-30
 * Time: 上午11:08
 * 要解决的问题:
 * 1.业务事务一致
 * 2.多数据源数据收集
 * 3.中断选择，结果再处理,等待中断的IMethodAddition只处理一个
 */
public class DefaultHandler implements IProxyHandler {
    transient static Log log = LogFactory.getLog(DefaultHandler.class);
    @Override
    public Object handle(IMethodAddition[] adds,Object impl, String m,Class[] parclasses, Object[] args) throws Exception {
        //判断该方法适用的additions
        IMethodAddition[] additions=null;
        if(null != adds){
            List<IMethodAddition> rel = new ArrayList<IMethodAddition>();
            for(IMethodAddition a:adds){
                if(null == a.getMethods() || a.getMethods().contains(m))
                    rel.add(a);
            }
            if(rel.size()>0){
                additions=rel.toArray(new IMethodAddition[0]);
            }else{
                additions=null;
            }
        }

        //默认是异步
        if(null != additions && additions.length>0){
            boolean isInvoke=true;
            int level=Integer.MAX_VALUE;
            Object ret=null;
            LinkedList<IMethodAddition> waitBeforeList = new LinkedList();
            LinkedList<IMethodAddition> nowaitBeforeList = new LinkedList();
            LinkedList<IMethodAddition> waitAfterList = new LinkedList();
            LinkedList<IMethodAddition> nowaitAfterList = new LinkedList();
            LinkedList<IMethodAddition> waitReturnList = new LinkedList();
            LinkedList<IMethodAddition> nowaitReturnList = new LinkedList();
            for(IMethodAddition addition:additions){
                if(addition.isWaiteBefore()){
                   waitBeforeList.add(addition);
                }else{
                    nowaitBeforeList.add(addition);
                }
                if(addition.isWaiteAfter()){
                    waitAfterList.add(addition);
                }else{
                    nowaitAfterList.add(addition);
                }
                if(addition.isWaiteResult()){
                    waitReturnList.add(addition);
                }else{
                    nowaitReturnList.add(addition);
                }

            }
            //before action
            Object[] befList=null;

            if(nowaitBeforeList.size()>0){
                ExecutorUtils.multiWorkSameParWithCachePool(nowaitBeforeList.toArray(), "beforeAction", new Class[]{Object.class, String.class, Object[].class}, new Object[]{impl, m, args});
            }
            if(waitBeforeList.size()>0){
                befList = ExecutorUtils.multiWorkSameParWaitingWithCachePool(waitBeforeList.toArray(), "beforeAction", new Class[]{Object.class, String.class, Object[].class}, new Object[]{impl, m, args});
                //judge invoke by addition's level
                if(null != befList) {
                    for (Object o : befList) {
                        if(o instanceof Exception){
                            throw (Exception)o;
                        }
                    }
                    for (int i = 0; i < waitBeforeList.size(); i++) {
                        if (waitBeforeList.get(i).getLevel() < level) {
                            level = waitBeforeList.get(i).getLevel();
                            if (!waitBeforeList.get(i).isNextInvoke()) {
                                isInvoke = false;
                                if (waitBeforeList.get(i).isWaiteBefore()) {
                                    ret = befList[i];
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            //invoke real function
            boolean isSuccess=true;
            Exception ex=null;
            if(isInvoke){
                try{
                    ret = impl.getClass().getMethod("super_" + m, parclasses).invoke(impl, args);

                }catch (Exception e){
                    isSuccess=false;
                    ex=e;
                    throw e;
                }finally {
                    //after action
                    if(nowaitAfterList.size()>0){
                        ExecutorUtils.multiWorkSameParWithCachePool(nowaitAfterList.toArray(), "afterAction", new Class[]{Object.class, String.class, Object[].class, boolean.class, boolean.class, Throwable.class, Object.class}, new Object[]{impl, m, args, isInvoke, isSuccess, ex, ret});
                    }
                    if(waitAfterList.size()>0) {
                        Object[] afList = null;
                        afList = ExecutorUtils.multiWorkSameParWaitingWithCachePool(waitAfterList.toArray(), "afterAction", new Class[]{Object.class, String.class, Object[].class, boolean.class, boolean.class, Throwable.class, Object.class}, new Object[]{impl, m, args, isInvoke, isSuccess, ex, ret});
                        for (int i = 0; i < waitAfterList.size(); i++) {
                            if (waitAfterList.get(i).getLevel() == level) {
                                if (null != afList[i]) {
                                    ret = afList[i];
                                    break;
                                }
                            }
                        }
                    }

                    if(nowaitReturnList.size()>0){
                        ExecutorUtils.multiWorkSameParWithCachePool(nowaitReturnList.toArray(), "resultAction", new Class[]{Object.class, String.class, Object[].class, Object.class}, new Object[]{impl, m, args, ret});
                    }
                    if(waitReturnList.size()>0) {
                        Object[] resultList = null;
                        resultList = ExecutorUtils.multiWorkSameParWaitingWithCachePool(waitReturnList.toArray(), "resultAction", new Class[]{Object.class, String.class, Object[].class, Object.class}, new Object[]{impl, m, args, ret});
                        for (int i = 0; i < waitReturnList.size(); i++) {
                            if (waitReturnList.get(i).getLevel() == level) {
                                if (null != resultList[i]) {
                                    ret = resultList[i];
                                    break;
                                }
                            }
                        }
                    }

                }
            }
            return ret;

        }else{
            return impl.getClass().getMethod("super_" + m, parclasses).invoke(impl, args);

        }
    }


}

