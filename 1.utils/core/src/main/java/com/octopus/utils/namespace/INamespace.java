package com.octopus.utils.namespace;

/**
 * User: Administrator
 * Date: 14-9-2
 * Time: 下午1:50
 */
public interface INamespace {
    //更新外部格式
    public void updateStyleValue(String style);
    //更新外部格式某个段的值
    public void updateStyleValue(int segmentIndex,String segmentStyle);
    //获取外部格式某个段的值
    public String getSegmentStyleValue(int segmentIndex);
    //克隆当前对象，并更新该对象中的Namespace
    public Object colon();
    //演进该对象，并更新该对象中的Namespace
    public Object evolution();
    //回滚该对象为上一个Namespace
    public void	rollback();
    //获取Namespace的String
    public String getString();

}
