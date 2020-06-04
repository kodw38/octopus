上下文
  服务下载顺序，后面的优先
    1. 指定加载desc的jar文件，可以指定多个jar文件
    2. 指定zk目录，可以多个
    3. 指定的class，解析成desc
    4. 加载本地服务目录

webstart\cxf 等launcher在ready之后，finishedtask中执行，且要等到应用ready后再启动
ZkClientObject 同步zk数据，在系统启动后会同步本地服务到zk，再拉取所有服务信息到本地。由于zk存在创建paht和setdata可侵入性。导致其他实例还没来得及监听path，data就已经设立了，其他实例不能同步到这个data。所以setEphemeralData方法延迟30毫秒，设置数据，并且每10分钟重新全量同步数据一次。

cxf webservice接口时，系统会在cxf启动前把desc服务生成class类，每个desc的入参出参生成到这个desc的类路径+服务名称目录下，在加载到cxf容器中，指定端口。

语法
    @{}
xmlobject执行顺序
    1. 实例化类，初始化属性
    2. 设置refer关联属性
    3. 执行doObjectsInitial
    4. 执行doApplicationInitial,先init后afterInit
    5. 执行doApplicationReady
    6. 执行doFinishedEvent
    如果要等系统ready才能执行某些内容，可以使用waitReady()方法。

xml_object:
  properties:
    init:
       - {}  -- do it dur object's doInitial after create all objects
       - {ripe:'objectsInitial'}  -- do it dur application initial after 'init:{}'
       - {ripe:'applicationInitial'}   -- do it dur application ready after 'objectsInitial'
       - {ripe:'applicationReady'}   -- do it after application ready after 'applicationInitial'

config元素
   envents:{thisObjectMethod:eventSourceObject#method,...}  --当eventSourceObject对象的method方法调用后，会异步调用本对象的thisObjectMethod方法。参考zkClientObject的config配置
