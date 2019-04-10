XMLObject实现根据xml配置文件初始化系统的功能。
配置例句:
    1.<t  clazz="com.octopus.tools.i18n.impl.I18N" x1="" classloader="" x2="">xxx</t>
      1.1 xml中带有class的属性值必须继承XMLObject类
          1.1.1 如果xxx中还有clazz，并且标签名字=class中的属性名字，则该类中的属性不需要初始化，也不需要设置=null.
          1.1.2 在构造方法中不能直接使用定义的子对象。应该这时候子对象还没有构建。
          1.1.3 属性x1,x2如果在类中没有定义，则放入默认的xmlExtProperties对象中。
          1.1.4 xxx中还有对象配置，且没有在类中定义的，则默认根据标签名称放入xmlExtFields对象中。
          1.1.5 可以用classloader属性制定装在该类的classloader。


    2.<t  xmlid="mm">xxx</t>
      2.1 xml中带有xmlid的属性值,是在def中定义的类，这个类只要定义在该配置文件或父级配置文件中即可，必须继承XMLObject类。

    3.<t xmlid="x" xml="classpathXmlfile"/>
      可以把其他xml文件作为一个对象实例化在本配置文档中。

    4.多个相同元素
      4.1 map对象
        <ts>
            <t key="" xmlid=""/>
        </ts>
      4.1 List对象
        <ts>
           <t seq="" xmlid=""/>
        </ts>
        同一层相同的tag:
            如果有"seq"属性时对应解析类中的tag+"s" List属性
            如果有"key"属性时对应解析类中的tag+"s" Map属性
        有类的属性作为xml的child配置。
        数据属性作为xml的properties配置。

    5. text属性
      <t key="">xxx</t>
      属性中没有xmlid和clazz属性，xxx不是xml形式的内容，将把xxx所谓属性处理。放入定义好的父级类中，或放入父级类的xmlExtProperties;如果有key属性，则当做map存放。

    6. 对象引用
       6.1 <t/>
       6.2 <t key=""/>
       6.3 <t seq="'/>
       应用前面已经实例化的对象时，引用方式以上三种。


内部元素:
    1. 类别名
    <defs>
        <def xmlid="Flows"  clazz="com.octopus.utils.flow.impl.FlowMgr"/>
    </defs>
    可以定义需要多次实例化的类。在其他xml元素中用xmlid应用该类。

代理类:
    0. <handler clazz="" level="" iswaitebefore="" isnextinvoke="" targetids="id1#method1,id1#method2,id2"></handler>
       0.2 clazz
          clazz代理类必须实现extends XMLObject implements IMethodAddition。被代理的类必须实现某个或多个接口，代理类只代理接口的方法。
       0.3 level
          当前IMethodAddition代理的等级,Int值,值越小等级越高。默认handler将按高等级的iswaitebefore，isnextinvoke值处理逻辑。默认0
       0.4 iswaitebefore
          IMethodAddition的before方法是多线程异步执行的， iswaitebefore=true,false,来设置before是否等待before都执行完了，才执行invoke.对于需要对before结果处理的需要设置为true.默认false
       0.5 isnextinvoke
          true,false.设置是否调用被代理的方法。默认true

    1. <handler clazz=""></handler>
       代理所有对象类。默认实现。

    2. <handler clazz="" targetxmlids="xmlid1,xmlid2"></handler>
       代理所有对象中含有xmlid=xmlid1,xmlid2的类。

       <handler clazz="" targetxmlids="xmlid1#method1#method2,xmlid1#method3,xmlid2"></handler>
       代理所有对象中含有xmlid=xmlid1的接口方法method1,method2做代理，xmlid2的所有接口方法做代理。

    3. <handler clazz="" targetids="id1,id2"></handler>
       代理所有对象中含有id=id1,id2的类。

       <handler clazz="" targetids="id1#method1,id1#method2,id2"></handler>
       代理所有对象中含有id=id1的接口方法method1,method2做代理，id2的所有接口方法做代理。


    4. 默认XMLObjectHandler的功能
      <handler clazz="IMethodAddition" level="" iswaitebefore="" isnextinvoke="" targetxmlids="or targetids=">
          <befores exetype="asyn/asynwait" isjump="默认false">
              <before></before>     <!-- before可以配置flow的实现 -->
          </befores>
          <afters exetype="asyn/asynwait">
              <after></after>
          </afters>
          <results>
              <result></result>
          </results>
      </handler>


      4.1 befores
          1). isasyn=true 表示before的任务将异步执行，提交后继续执行代理的方法。
          2). isjump=true 表示before的最后一个任务返回的结果不为null，则不执行代理的方法，返回null则执行代理方法；为isjump=true时isasyn=false。
      4.2 afters
          1). isasyn=true 表示before的任务将异步执行，提交后继续执行results的方法。
      4.3 results
          results中的方法都是串行执行。返回最后一个的处理结果。

    代理类中的任务必须实现IXMLDoObject接口。

行为类
   1. XMLObject
      每一个XML都有一个XMLObject装载解析，XMLObject提供xml的解析，类实例创建，以上功能[属性，代理]的实现。
   2. XMLDoObject
      这个类继承了XMLObject，并提供了一个doSomeThing抽象方法，使得调用都从这个方法进入，达到调用自动化。
      XMLDoObject中的孩子节点如果带有auto属性，在其父对象执行时自动先执行这个子对象。
      为提高配置化程度,统一入参XMLParatmer需要通过配置转换为每个XMLDoObject指定位置可以取到的参数，同样XMLDoObject返回的值需要通过配置放置在XMLDoObject的指定位置.
   3. 行为类的参数有三个主要来源:
      1) 用于实例化的xml配置，由XMLObject的构造方法处理。在定义clazz的xml体中配置。
      2) 用于方法调用的JSON配置参数，这个参数在引用的xml中属性jsonpar的值配置，也是配置在xml文本中的。
      3) 环境和输入参数XMLParameter,这个参数是可以文本化的。

<parmap clazz=""/> XMLDoObject
   parmap 是为调用XMLDoObject把请求全局参数XMLParameter转换为XMLDoObject定义的参数的转换类。在配置文件中需要配置该类。

定义特殊属性
   定义属性,自定义属性只服务于XMLDoObject对象。
   属性值都是JSONObject的字符串格式,分为对象初始化时的属性和对象调用是的属性，每种属性一个对象只能有一个。
   参数影射:parmap="{os:{env.xx.xx:xx,...}}"
     把全局环境参数根据parmap中的影射路径关系，转换为jsonobject对象，供XMLDoObject使用.
   执行属性:
     对于继承XMLDoObject的类可以增加以下属性扩展功能，这些属性都是系统初始化完后就开始异步加载的,参数为null:
     初始时执行的属性:
       1. 执行时机
         1.1 时间. 对象初始华时启动,放在XMLObject实例化类之后处理.
          <xx worktime="crons:['0 29 11 ? * MON-SAT']" /xx>
           worktime
           1.1.1) 配置cronExpression表达式，如果配置了，根据表达式执行该对象;可以配置多个用","分割.
           1.1.2)
         1.2 次数
         1.3 环境条件依赖
     调用时的属性
       1. 执行线程
         1.1 同异步
         <xx exetype="syn/asyn"  /xx>
         syn同步，默认为同步;asyn异步。
         1.2 并行.
         <xx concurrence="{num:3,size:100,spl:'xx',iswait:yes}" jsonpar="{to:}" /xx>
           num:并发线程数
           size:一个线程处理记录数
           spl:分割的参数名称
           num和size只有一个生效。
           多线程处理参数分割类标签固定为 cncrprmspl 的XMLDoObject
         1.3 执行队列
         <xx queue="" /xx>
         1.4 事务一致
       2. 结果处理
        2.1 送队列


解释说明类.
   1. 为每个行为类提供使用说明，参数说明。
   2. 为系统环境提供说明。

功能:
    1.可以实时变更xml，变更xml后当前xml对象即孩子对象将重新装载。
    2.可以获取xml配置信息。
    3.可以获取指定路径的属性值。
    4.可以clone。
    5.可以向上获取对象。
    6.可以向上获取所有的对象。

版本：不提供多版本服务，xmlObject增加创建临时服务的功能，执行一次就销毁。
    1.为了满足过去的某个间断性流程性服务，执行了一半，但没有被执行完成，过程中修改了服务流程或者某个节点服务，要保证原来没有完成的数据继续走老服务流程，新数据走新的服务流程。
    2.或者定义一个服务的两个版本，根据请求数据指定使用哪个版本执行，最后合并保留某个版本的服务作为最终的服务。
    每个服务增加版本信息是非常复杂的，不容易控制，什么时候增加版本，消除版本，版本过多问题，不知道哪个版本还在使用。
    问题2可以定义不同的服务来实现。
    问题1，当碰到间断性服务时，需要中断当前服务，等待后续继续执行时，把当前服务复制一份和业务数据一起保存下来，下次继续执行时，把服务重新实例化（底层代码能力不能改变的前提下）继续执行。


