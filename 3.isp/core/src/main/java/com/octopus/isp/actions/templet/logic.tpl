{
common_define_properties:{
  worktime: {
     @value:{
       crons:[{
        @type:'String'
       ,@desc:'seconds[0~59 ,-*/] minutes[0~59 ,-*/] hours[0~23 ,-*/] dayofmonth[1~31 ,-*?/LWC] month[1~12 ,-*/] dayofweek[1~7 ,-*?/LC#] year[1970~2099 ,-*/]'
       }]
       ,isconcurrent:{
         @type:'boolean'
        ,@enum:[true,false]
        ,@desc:'true: allow reload same time task if pre task still does not finish, false: else not'
        ,@depend:'crons'
       }
       ,delay:{
         @type:'int'
        ,@desc:'delay running the task , only running one time'
       }
     }
    ,@enum:[{crons:[''],isconcurrent:},{delay:}]
    ,@desc:'main use cron expression to running this task as refer time. is_asyn=true(asynchronous execution) ,is_exe=true(can execute) ,exe_type=init(running at object initial)'
  }

, init : {
     @value:'{}'
    ,@desc:'only run this task one time at application initial, no parameter.'
  }

, trigger : {
     @value:{src:{@type:'String',@desc:'Service Name of implement addTrigger method'},cond:''}
    ,@desc:'if src Object implement addTrigger method , and will check the cond , will do this Action with trigger when check cond return true.'
  }

, retry : {
    @value:{times:{@type:'int',@desc:'try times number'},interval:{@type:'int', @desc:'each retry interval time unit second'}}
   ,@desc:'when throw Exception, it will retry'
  }

, globalsingle : {
     @value:'{}'
    ,@desc:'like synchronized. synchronized by xmlId in all instance services . this function depends Redis and redis client name is RedisClient,no parameter.'
  }

, concurrence : {
     @value:{threadnum:,size:{@type:int,@desc:'total task number, it normal usd with as first item in for',@depend:'threadnum'},iswait:'true/false'}
    ,@desc:'it normal usd  as first item property in for, concurrent deal with for items'
  }

, interruptnotification : {
     @value:true
    ,@desc:'depend kafka,kafka client name is kafkaClient'
  }

, isAsyn : {
     @enum:[true,false]
    ,@desc:'true:this task is asynchronous'
  }

, isTrade : {
     @enum:[true,false]
    ,@desc:'all children and this task will belong in one transaction'
  }

}

,do_element:{
do:{ properties:{key:'', action:'', input:{ check:{check:'${express}',error:{@type:[string,map],@desc:'if string ,value is throw message; if map ',@value:['',{msg:'throw message',break:'go to point'}] } }, alarm:{}, notification:{}  },config:{},output:{ check:{check:'${express}',error:{@type:[string,map],@desc:'if string ,value is throw message; if map ',@value:['',{msg:'throw message',break:'go to point'}] } }, alarm:{}, notification:{} }}}
, if:{ properties:{cond:{@value:'${express}',@desc:'a boolean expression'} } , children:{else:{}}, @desc:'if logic judge'}
, for:{ properties:{ collection:{ @enum:['${env}','${int}'],@desc:'variable of environment or a number'},json:{len:{@value:'${int}',@desc:'batch number for each time to do'},threadnum:{@value:'${int}',@desc:'concurrent running thread number'},depend:'collection'}}}
, while: { properties:{cond:'$Express'} }
, print: { properties:{msg:{@desc:'fix string message or json structure data'},trace:{@desc:'will print xmlId of parent trace'},all:{@desc:'will print all data of data container '}} }
, result: { properties:{value:{@enum:[${env},${express}]}} }
, error: { properties:{msg:{@desc:'fix string message or json structure data'},code:'',@desc:'throw a ISPException exception'} }
, timestart: { properties:{key:'',msg:'',@desc:'log begin date point of one task, there is must exist key.'} }
, timeprint: { properties:{key:'',msg:'',@desc:'calculate cost at the end of one task, there is must exist key.'} }
}

,string_methods:{
   getallparameters:{@desc:''}
   , base64_encode:{}
   , geterrortrace:{}
   , substrnotag:{} }
   , isnotnull:{}
   , startwith:{}
   , numformat:{}
   , classtype:{}
   , notexist:{}
   , getvalue:{}
   , contains:{}
   , indexof:{}
   , decrypt:{}
   , encrypt:{}
   , isrnull:{}
   , substr:{}
   , isnull:{}
   , ifnull:{}
   , tojson:{}
   , todate:{}
   , remain:{}
   , format:{}
   , varcal:{}
   , getvar:{}
   , exist:{}
   , times:{}
   , case:{}
   , len:{}
   , #:{}
}
,data_structures:{
   mapping:{src:'$var',declare:{clazz:'',structure:[{type:''}],@desc:'target data structure'},mapping:{@type:'map',@desc:'key is new structure path,value is data path in src obj'} }
}

}