{
common_define_properties:[
  { worktime: { properties:{},is_asyn:'true/false',is_exe:'true/false',exe_type:'init/invoke' } }
, { init : {} }
, { trigger : {} }
, { retry : {} }
, { globalsingle : {} }
, { concurrence : {} }
, { worktime : {} }
, { interruptnotification : {} }

, { isAsyn : {} }
, { isTrade : {} }
]

,do_element:[
  { if:{ properties:{cond:'#ExpressValueFromEnv'} , children:[else] }}
, { for:{ properties:{collection:{type:['$var','int'],desc:'variable of environment or a number'},json:{len:{type:'int',desc:'batch number for each time to do'},threadnum:{type:'int',desc:'concurrent running thread number'},depend:'collection'}}}}
, { do = { properties:{key:'',action:'',input:{ properties:{ check:{check:'#ExpressValueFromEnv',error:{type:[string,map],desc:'if string ,value is throw message; if map ',value:['',{msg:'throw message',break:'go to point'}] } }, alarm:{}, notification:{} } },config:'',output:{ properties:{ check:{check:'#ExpressValueFromEnv',error:{type:[string,map],desc:'if string ,value is throw message; if map ',value:['',{msg:'throw message',break:'go to point'}] } }, alarm:{}, notification:{} }}}}}
, { while : {} }
, { result : {} }
, { print : {} }
, { error : {} }
, { timestart : {} }
, { timeprint : {} }
]

}