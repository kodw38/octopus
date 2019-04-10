<?xml version="1.0" encoding="utf-8"?>
<slaver key="MysqlSlaver"  clazz="com.octopus.tools.synchro.canal.SimMysqlSlaver">
    <!-- id:longtype  -->
    <instance id="1" name="srv" prot="8110" desc="">
        <property name="canal.zkServers"></property>
        <!--MetaMode[MEMORY,ZOOKEEPER,MIXED] current only support MEMORY -->
        <property name="canal.MetaMode">MEMORY</property>
        <!--HaMode[HEARTBEAT,MEDIA] current only support HEARTBEAT -->
        <property name="canal.HaMode">HEARTBEAT</property>
        <!--MetaMode[MEMORY,ZOOKEEPER,MIXED,META,MEMORY_META_FAILBACK] current only support MEMORY -->
        <property name="canal.IndexMode">MEMORY</property>
        <!--MetaMode[MEMORY,FILE,MIXED] current only support MEMORY -->
        <property name="canal.StorageMode">MEMORY</property>
        <!--SourcingType[MYSQL,ORACLE] current only support MYSQL -->
        <property name="canal.SourcingType">MYSQL</property>
        <property name="canal.MemoryStorageBufferSize">32768</property>

        <property name="canal.instance.mysql.slaveId">1234</property>
        <property name="canal.instance.master.address">127.0.0.1:3306</property>
        <property name="canal.instance.master.journal.name"></property>
        <property name="canal.instance.master.position"></property>
        <property name="canal.instance.master.timestamp"></property>
        <property name="canal.instance.standby.address"></property>
        <property name="canal.instance.standby.journal.name"></property>
        <property name="canal.instance.standby.position"></property>
        <property name="canal.instance.standby.timestamp"></property>
        <property name="canal.instance.dbUsername">root</property>
        <property name="canal.instance.dbPassword"></property>
        <!--
                    当丢失logposition时用时间从数据库binlog获取丢失的数据
                    <property name="canal.instance.master.timestamp">1465099963000</property>
                -->
        <!--
            有canal.logpositionmanager配置，下面两项不用配置
            <property name="canal.instance.master.journal.name">mysql-bin.000508</property>
            <property name="canal.instance.master.position">219</property>
        -->
        <!--<property name="canal.logpositionmanager"  intervalsecond="600" value="com.octopus.tools.synchro.canal.impl.LogPositionManager">
            <reader action="DataClient" input="{ds:'mysql',op:'query',table:'isp_binlog_point',conds:{SLAVE_NAME:'mysql'}}"/>
            <writer action="DataClient" input="{ds:'mysql',op:'upadd',table:'isp_binlog_point',keyfields:['SLAVE_NAME']}"/>
        </property>-->
        <property name="canal.instance.defaultDatabaseName">mysql</property>
        <property name="canal.instance.connectionCharset">UTF-8</property>
        <!-- canal.instance.filter.regex = .*\\..* -->
        <property name="canal.instance.filter.regex">mysql.isp_dictionary_service</property>
        <property name="canal.instance.filter.black.regex"></property>
    </instance>
    <instance id="2" name="dictionary" prot="8111" desc="">
        <property name="canal.zkServers"></property>
        <!--MetaMode[MEMORY,ZOOKEEPER,MIXED] current only support MEMORY -->
        <property name="canal.MetaMode">MEMORY</property>
        <!--HaMode[HEARTBEAT,MEDIA] current only support HEARTBEAT -->
        <property name="canal.HaMode">HEARTBEAT</property>
        <!--MetaMode[MEMORY,ZOOKEEPER,MIXED,META,MEMORY_META_FAILBACK] current only support MEMORY -->
        <property name="canal.IndexMode">MEMORY</property>
        <!--MetaMode[MEMORY,FILE,MIXED] current only support MEMORY -->
        <property name="canal.StorageMode">MEMORY</property>
        <!--SourcingType[MYSQL,ORACLE] current only support MYSQL -->
        <property name="canal.SourcingType">MYSQL</property>
        <property name="canal.MemoryStorageBufferSize">32768</property>

        <property name="canal.instance.mysql.slaveId">1234</property>
        <property name="canal.instance.master.address">127.0.0.1:3306</property>
        <property name="canal.instance.master.journal.name"></property>
        <property name="canal.instance.master.position"></property>
        <property name="canal.instance.master.timestamp"></property>
        <property name="canal.instance.standby.address"></property>
        <property name="canal.instance.standby.journal.name"></property>
        <property name="canal.instance.standby.position"></property>
        <property name="canal.instance.standby.timestamp"></property>
        <property name="canal.instance.dbUsername">root</property>
        <property name="canal.instance.dbPassword"></property>
        <!--
                    当丢失logposition时用时间从数据库binlog获取丢失的数据
                    <property name="canal.instance.master.timestamp">1465099963000</property>
                -->
        <!--
            有canal.logpositionmanager配置，下面两项不用配置
            <property name="canal.instance.master.journal.name">mysql-bin.000508</property>
            <property name="canal.instance.master.position">219</property>
        -->
        <!--<property name="canal.logpositionmanager"  intervalsecond="600" value="com.octopus.tools.synchro.canal.impl.LogPositionManager">
            <reader action="DataClient" input="{ds:'mysql',op:'query',table:'isp_binlog_point',conds:{SLAVE_NAME:'mysql'}}"/>
            <writer action="DataClient" input="{ds:'mysql',op:'upadd',table:'isp_binlog_point',keyfields:['SLAVE_NAME']}"/>
        </property>-->
        <property name="canal.instance.defaultDatabaseName">mysql</property>
        <property name="canal.instance.connectionCharset">UTF-8</property>
        <!-- canal.instance.filter.regex = .*\\..* -->
        <property name="canal.instance.filter.regex">mysql.isp_dictionary_field</property>
        <property name="canal.instance.filter.black.regex"></property>
    </instance>

    <listener id="1" auto="after" isAsyn="true" desc="服务信息发生变化，更新运行中的服务"  clazz="com.octopus.tools.synchro.canal.SynchroMysqlQueryKeys2RedisServer">
        <handler key="SynchroServiceHandle" clazz="com.octopus.isp.actions.impl.ServiceUpdateHandler"/>
        <property name="canal.zkServers"></property>
        <property name="canal.ip">127.0.0.1</property>
        <property name="canal.port">8110</property>
        <property name="canal.name">srv</property>
        <property name="canal.instance.filter.regex">mysql.isp_dictionary_service</property>
        <property name="client.id">1001</property>
    </listener>
    <!--<listener key="SynchroTableQueryKeys2Redis" desc="Angle 数据发生变化重新装载到Redis" isasyn="true" clazz="com.octopus.tools.synchro.canal.SynchroMysqlQueryKeys2RedisServer">
        <handler key="SynchroHandle" clazz="com.octopus.tools.dataclient.dataquery.CanalRealTimHandler" input="{redis:'AngleQuery'}"/>
        <property name="canal.zkServers"></property>
        <property name="canal.ip">127.0.0.1</property>
        <property name="canal.port">1110</property>
        <property name="canal.name">test</property>
        <property name="canal.instance.filter.regex">mysql.ISP_DICTIONARY_FIELD</property>
    </listener>-->
    <listener id="2" auto="after" isAsyn="true" desc="数据字典发生变化，重新装载数据字典到Redis"  clazz="com.octopus.tools.synchro.canal.SynchroMysqlQueryKeys2RedisServer">
        <handler key="fieldHandle" clazz="com.octopus.tools.synchro.canal.XMLLogicHandle">
            <if cond="#{(${op})=INSERT | (${op})=UPDATE}">
                <do key="sv" action="setvalue" input="{obj:'${data}',path:'OP',value:'${op}'}"/>
                <print msg="1111111111"/>
                <do key="mapstr" action="utils" input="{op:'map2string',data:'${data}'}"/>
                <do key="sendzk" action="ZkServer" input="{check:'isnotnull(${mapstr})',op:'send',path:'/isp_dictionary_field',data:'${mapstr}'}"/>
            </if>
            <if cond="#{(${op})=DELETE}">
                <do key="sv" action="setvalue" input="{obj:'${olddata}',path:'OP',value:'${op}'}"/>
                <do key="sv" action="setvalue" input="{obj:'${olddata}',path:'OP#Type',value:'varchar'}"/>
                <do key="mapstr" action="utils" input="{op:'map2string',data:'${olddata}'}"/>
                <do key="sendzk" action="ZkServer" input="{check:'isnotnull(${mapstr})',op:'send',path:'/isp_dictionary_field',data:'${mapstr}'}"/>
            </if>
        </handler>
        <property name="canal.zkServers"></property>
        <property name="canal.ip">127.0.0.1</property>
        <property name="canal.port">8111</property>
        <property name="canal.name">dictionary</property>
        <property name="canal.instance.filter.regex" desc="注意大小写和实际表明称一样">mysql.isp_dictionary_field</property>
        <property name="client.id">1002</property>
    </listener>

</slaver>
