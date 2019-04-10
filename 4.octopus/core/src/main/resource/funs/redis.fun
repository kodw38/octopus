<action clazz="com.octopus.tools.dataclient.dataquery.redis.RedisClient">
    <cluster key="AngleQuery">
        <redis ip="127.0.0.1" port="6379" maxtotal="20" db="5" password="" timeoutmillis="60000" maxwaitmillis="600000"/>
    </cluster>
    <cluster key="trade_redis">
        <redis ip="127.0.0.1" port="6379" maxtotal="20" db="2" password="" timeoutmillis="60000" maxwaitmillis="600000"/>
    </cluster>
    <cluster key="Session">
        <redis ip="127.0.0.1" port="6379" maxtotal="20" db="6" password="" timeoutmillis="60000" maxwaitmillis="600000"/>
    </cluster>
    <cluster key="CountTimes">
        <redis ip="127.0.0.1" port="6379" maxtotal="20" db="7" password="" timeoutmillis="60000" maxwaitmillis="600000"/>
    </cluster>
    <cluster key="ISP_DICTIONARY_FIELD">
        <redis ip="127.0.0.1" port="6379" maxtotal="20" db="1" password="" timeoutmillis="60000" maxwaitmillis="600000"/>
    </cluster>
    <cluster key="TRACE">
    <redis ip="127.0.0.1" port="6379" maxtotal="20" db="3" password="" timeoutmillis="60000" maxwaitmillis="600000"/>
    </cluster>
</action>