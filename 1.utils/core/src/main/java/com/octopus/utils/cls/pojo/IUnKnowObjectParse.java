package com.octopus.utils.cls.pojo;

/**
 * User: wangfeng2
 * Date: 14-4-16
 * Time: 上午10:28
 */
public interface IUnKnowObjectParse {
    /**
     * 把不能自动简化的特殊类通过特殊处理简化，返回该类的内部简化结构
     * @param c
     * @return
     * @throws Exception
     */
    public void parse(Class c, PropertyInfo propertyInfo)throws Exception;

    /**
     * 根据简化数据，实例化原数据对象
     * @param propertyInfo
     * @return
     * @throws Exception
     */
    public Object backParse(PropertyInfo propertyInfo)throws Exception;
}
