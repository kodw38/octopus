<!--
   need fun : Logic , DataClient, cache[isp_dictionary_field], var
-->
<init key="defobjects" init="{}" xmlid="Logic">
    <!-- isp_dictionary_field -->
    <do key="fields" action="DataClient" input="{op:'query',table:'isp_dictionary_field',conds:{STATE:'1'}}">
        <for collection="${fields}">
            <do key="fs" action="cache" input="{cache:'isp_dictionary_field',op:'add',key:'${fields}.item.FIELD_CODE',value:'${fields}.item'}"/>
        </for>
    </do>

    <do key="m" action="DataClient" input="{op:'getDefTables'}">
        <for collection="${m}">
            <for collection="${m}.value.tableFields">
                <do key="r" action="var" input="{value:{
                            declare:{clazz:'java.util.HashMap'}
                            , data:{
                            TYPE_NAME:'com.octopus.defobjs.(${m}.key)',
                            PROPERTY_NAME:'(${m}.value.tableFields.item.field.fieldCode)',
                            PROPERTY_TYPE:'(${m}.value.tableFields.item.field.realFieldType)'
                            }
                            }}">
                    <do key="temcache" action="cache" input="{op:'addList',cache:'temp_cache',key:'cache_def_objs',value:'${r}'}"/>
                </do>
            </for>
        </for>
        <do key="def_objs" action="cache" input="{cache:'temp_cache',op:'get',key:'cache_def_objs'}"/>
        <do key="rmtemcache" action="cache" input="{op:'del',cache:'temp_cache',key:'cache_def_objs'}"/>
    </do>
    <!-- 初始化数据对象和服务对象 -->
    <do key="Dictionary" clazz="com.octopus.isp.actions.ISPDictionary" input="{extendpojo:'${def_objs}'}">
        <servicesload key="fromOutDesc" auto="before" xmlid="Logic">
            <do key="getDescFiles" action="utils" input="{op:'getAllFilesPath',path:'(${env}.outApiPath)',endwith:'json'}">
                <for collection="${getDescFiles}">
                    <do key="srvtxt" action="utils" input="{op:'getFileStringContent',data:'${getDescFiles}.value'}">
                        <do key="srvbody" action="desc" input="{op:'getInvokeStructure',txt:'${srvtxt}'}" desc="get service body from desc txt">
                            <do key="addsrv" action="Dictionary" input="{op:'addService',data:'${srvbody}'}" desc="add service by body txt"/>
                        </do>
                    </do>
                </for>
            </do>
        </servicesload>
        <dataquerys>
            <!--
                            <dataquery key="fields" input="{op:'query',table:'isp_dictionary_field',fields:['field_id','field_name','field_code','field_type','remark','field_len','field_num'],conds:{STATE:'1'}}"/>
            -->
            <dataquery key="pojos" input="{op:'query',table:'isp_dictionary_pojo',fields:['type_name','property_name','property_type','remark'],conds:{STATE:'1'}}"/>
            <dataquery key="services" input="{op:'query',table:'isp_dictionary_service',fields:['no_num','catalog','name','parameter_type','busi_class','op_class','parameter_limit','body_type','body','return_type','remark','remark_error','remark_dependence','remark_scene'],conds:{STATE:'1'}}" />
        </dataquerys>
        <cfg key="pojos">
            <property TYPE_NAME="com.ai.system.LoginBean" PROPERTY_NAME="UserName" PROPERTY_TYPE="java.lang.String" STATE="1" REMARK=""/>
            <property TYPE_NAME="com.ai.system.LoginBean" PROPERTY_NAME="UserPwd" PROPERTY_TYPE="java.lang.String" STATE="1" REMARK=""/>
        </cfg>
        <!--<cfg key="services">
            <property NO_NUM="1" BUSI_NAME="SystemFrame" CATALOG="com.isp.dictionary" NAME="Login" BUSI_CLASS="System" OP_CLASS="LGN" PARAMETER_TYPE="com.ai.system.LoginBean" PARAMETER_LIMIT=""
                      BODY_TYPE="xml" BODY="&lt;action key=&quot;Login&quot; clazz=&quot;com.octopus.octopus.actions.UserLogin&quot;&gt;
                                &lt;/action&gt;"
                      RETURN_TYPE="java.lang.Boolean" STATE="1"/>
            <property NO_NUM="2" CATALOG="com.isp.dictionary" NAME="searchFields" BUSI_CLASS="System" OP_CLASS="BUS"
                      PARAMETER_TYPE="" PARAMETER_LIMIT=""
                      BODY_TYPE="xml" BODY="&lt;action key=&quot;searchFields&quot; result=&quot;${fkeys}&quot; xmlid=&quot;Logic&quot;&gt;
                                 &lt;do key=&quot;fkeys&quot; action=&quot;cache&quot; input=&quot;{cache:'isp_dictionary_field',op:'search',fields:['FIELD_CODE','FIELD_NAME','REMARK']}&quot;/&gt;
                                &lt;/action&gt;"
                      RETURN_TYPE="" STATE="1"/>
            <property NO_NUM="2" CATALOG="com.isp.dictionary" NAME="getAllServices" BUSI_CLASS="System" OP_CLASS="BUS"
                      PARAMETER_TYPE="" PARAMETER_LIMIT=""
                      BODY_TYPE="xml" BODY="&lt;action key=&quot;getAllServices&quot; result=&quot;${actions}&quot; xmlid=&quot;Logic&quot;&gt;
                                 &lt;do key=&quot;actions&quot; action=&quot;system&quot; input=&quot;{op:'getActions'}&quot;/&gt;
                                &lt;/action&gt;"
                      RETURN_TYPE="" STATE="1"/>
            <property NO_NUM="3" CATALOG="com.octopus.OSE" NAME="testV7" BUSI_CLASS="System" OP_CLASS="BUS"
                      PARAMETER_TYPE="" PARAMETER_LIMIT=""
                      BODY_TYPE="xml" BODY="&lt;action key=&quot;testV7&quot; result=&quot;${fkeys}&quot; xmlid=&quot;Logic&quot;&gt;
                                 &lt;do key=&quot;custinfo&quot; action=&quot;httpClient&quot; input=&quot;{method:'POST',url:'(${env}.verisPath)',addRequestHeaders:{Content-Type:'application/x-www-form-urlencoded',Accept:'application/json'},inputstream:'servicecode=(${input_data}.v7)&amp;tenant=10&amp;WEB_HUB_PARAMS=(${input_data}.data)'}&quot; output=&quot;{format:'format({return:\'${return}.responseOutputStream\',charset:\'UTF-8\',clazz:\'java.util.HashMap\'})'}&quot; /&gt;

                                 &lt;do key=&quot;sendms&quot; action=&quot;utils&quot; input=&quot;{op:'encodeURL',data:'${createCust}'}&quot; /&gt;
                                 &lt;print msg=&quot;{msg:'${sendms}'}&quot;/&gt;
                                 &lt;do key=&quot;fkeys&quot; action=&quot;httpClient&quot; input=&quot;{method:'POST',url:'(${env}.verisPath)',addRequestHeaders:{Content-Type:'application/x-www-form-urlencoded',Accept:'application/json'},inputstream:'servicecode=createCustomer&amp;tenant=10&amp;WEB_HUB_PARAMS=(${sendms})'}&quot; output=&quot;{format:'format({return:\'${return}.responseOutputStream\',charset:\'UTF-8\'})'}&quot; /&gt;

                                &lt;/action&gt;"
                      RETURN_TYPE="" STATE="1"/>
        </cfg>-->
        <!-- 加载数据库中定义的action， 系统启动和dictionary表变更触发 -->
        <do key="LoadDefineActions" auto="after" clazz="com.octopus.isp.listeners.LoadActions" input="{op:'init',services:'${return}'}"/>
    </do>
</init>