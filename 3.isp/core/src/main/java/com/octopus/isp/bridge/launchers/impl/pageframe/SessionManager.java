package com.octopus.isp.bridge.launchers.impl.pageframe;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.isp.ds.Session;
import com.octopus.tools.dataclient.dataquery.redis.RedisClient;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: wfgao_000
 * Date: 15-8-24
 * Time: 下午4:18
 */
public class SessionManager extends XMLDoObject {
    static transient Log log = LogFactory.getLog(SessionManager.class);
    String jvmRoute="";
    int inactiveInterval;
    int timeout=600;
    static String StartWith_Session="S_"; //存放用户数据的key
    static String StartWith_KeySession="KS_"; //存放用户名.密码
    //static String StartWith_AUTHSRV="SRV_"; //存放用户配置的授权信息
    static String StartWith_JSessionRelSessionID="JS_"; //存放id
    static Map<String,Session> sessions = new ConcurrentHashMap<String, Session>();

    String[] activeexpirekeys=null;

    private final Queue<SecureRandom> randoms = new ConcurrentLinkedQueue();

    public SessionManager(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String interval = xml.getFirstCurChildText("property","name","inactiveinterval");
        if(StringUtils.isNotBlank(interval)){
            inactiveInterval=Integer.parseInt(interval);
        }
        String jvmroute = xml.getFirstCurChildText("property","name","jvmroute");
        if(StringUtils.isNotBlank(jvmroute)){
            jvmRoute=jvmroute;
        }
        String stime = xml.getFirstCurChildText("property","name","sessionTimeout");
        if(StringUtils.isNotBlank(stime)){
            timeout=Integer.parseInt(stime);
        }
        String tt = xml.getFirstCurChildText("property","name","activeexpirekeys");
        if(StringUtils.isNotBlank(tt)){
            activeexpirekeys=StringUtils.split(tt,",");
        }
    }
    public void start(){
        ExecutorUtils.work(new Runnable(){
            @Override
            public void run() {
                RedisClient rc = (RedisClient)getObjectById("RedisClient");
                Jedis jedis= null;
                try{
                    synchronized (sessions) {
                        jedis = rc.getRedis("Session");
                        Iterator its = sessions.keySet().iterator();
                        while (its.hasNext()) {
                            Session session = (Session) its.next();
                            String key = jedis.get(StartWith_KeySession + session.getSessionId());
                            if (StringUtils.isBlank(key)) {
                                sessions.remove(session.getSessionId());
                            }
                        }
                    }

                }catch (Exception e){
                    log.error(e);
                }finally {
                    if(null !=jedis)
                        jedis.close();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        },"run",null,null);
    }

    class SessionJedisPubSub extends JedisPubSub{
        String dbid="";
        public SessionJedisPubSub(String dbid){
            this.dbid=dbid;
        }

        public void onPSubscribe(String pattern, int subscribedChannels) {
                 System.out.println("onPSubscribe "
                         + pattern + " " + subscribedChannels);
         }

         @Override
         public void onPMessage(String pattern, String channel, String message) {
             if(log.isDebugEnabled()) {
                 log.error("onPMessage pattern "
                         + pattern + " " + channel + " " + message);
             }
             if(channel.equals("__keyevent@"+dbid+"__:expired")||channel.equals("__keyevent@"+dbid+"__:del")){
                if(log.isDebugEnabled()) {
                    System.out.println("----:" + message);
                }
                 String key = message.substring(message.indexOf("_")+1);
                 sessions.remove(key);
             }
         }

    }
    private SecureRandom createSecureRandom(){
        SecureRandom result = null;
        if (result == null) {
            try
            {
                result = SecureRandom.getInstance("SHA1PRNG");
            }
            catch (NoSuchAlgorithmException e)
            {
                log.error("sessionIdGeneratorBase.randomAlgorithm", e);
            }
        }
        if (result == null) {
            result = new SecureRandom();
        }
        result.nextInt();
        return result;
    }
    protected void getRandomBytes(byte[] bytes)
    {
        SecureRandom random = (SecureRandom)this.randoms.poll();
        if (random == null) {
            random = createSecureRandom();
        }
        random.nextBytes(bytes);
        this.randoms.add(random);
    }
    String generateSessionId(String route)
    {
        byte[] random = new byte[16];
        int sessionIdLength = 16;
        StringBuilder buffer = new StringBuilder(2 * sessionIdLength + 20);

        int resultLenBytes = 0;
        for (; resultLenBytes < sessionIdLength;)
        {
            getRandomBytes(random);
            int j = 0;
            if ((j < random.length) && (resultLenBytes < sessionIdLength))
            {
                byte b1 = (byte)((random[j] & 0xF0) >> 4);
                byte b2 = (byte)(random[j] & 0xF);
                if (b1 < 10) {
                    buffer.append((char)(48 + b1));
                } else {
                    buffer.append((char)(65 + (b1 - 10)));
                }
                if (b2 < 10) {
                    buffer.append((char)(48 + b2));
                } else {
                    buffer.append((char)(65 + (b2 - 10)));
                }
                resultLenBytes++;j++;
            }
        }
        if ((route != null) && (route.length() > 0))
        {
            buffer.append('.').append(route);
        }
        else
        {
            if ((jvmRoute != null) && (jvmRoute.length() > 0)) {
                buffer.append('.').append(jvmRoute);
            }
        }
        return buffer.toString();
    }


    public Session getSessionByUser(String userName) throws Exception {
        RedisClient rc = (RedisClient)getObjectById("RedisClient");
        Jedis jedis= null;
        try{
            jedis=rc.getRedis("Session");
            String key = StartWith_KeySession+userName;
            String sessionId = jedis.get(key);
            Session s=null;
            if(StringUtils.isNotBlank(sessionId)){
                s = getSessionById(sessionId,null);
                if(null != s){
                    if(log.isDebugEnabled()){
                        log.debug("session getSessionByUser:"+s);
                    }
                    active(s,userName,(String)s.get("expire"),null,false);
                    return s;
                }else{
                    jedis.del(key);
                }
            }
            return s;
        }catch (Exception e){
            log.error(e);
        }finally {
            if(null !=jedis)
                jedis.close();
        }
        return null;
    }
    public List<Session> getSessions(){
        List li = new ArrayList();
        li.addAll(sessions.values());
        return li;
    }
    public void addSessionAttrs(Session session,Map<String,String> data)throws Exception{
        try{
            if(null != session && null != data) {
                session.putAll(data);
                if (log.isDebugEnabled()) {
                    log.debug("addSessionAttributes:" + session);
                }
                active(session, null, (String) session.get("expire"), null, true);
            }
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }
    public void addSessionAttributes(String sessionid,Map<String,String> data)throws Exception{
        RedisClient rc = (RedisClient)getObjectById("RedisClient");
        try{
            Session s = getSessionById(sessionid,null);
            addSessionAttrs(s,data);
        }catch (Exception e){
            log.error(e);
            throw e;
        }
    }
    /*public void addAuthServices(String sessionid,Map services){
        RedisClient rc = (RedisClient)getObjectById("RedisClient");
        try{
            Session s = getSessionById(sessionid,null);
            String name = s.getUserName();
            Jedis jedis= null;
            if(null != services) {
                try {
                    jedis = rc.getRedis("Session");
                    Iterator its = services.keySet().iterator();
                    while(its.hasNext()) {
                        String key = (String)its.next();
                        Object v = services.get(key);
                        if(null != v){
                            if(v instanceof Map){
                                v = ObjectUtils.convertMap2String((Map)v);
                            }else{
                                v = v.toString();
                            }
                        }else{
                            v="";
                        }
                        jedis.hset(StartWith_AUTHSRV + name,key,(String)v );
                    }
                    jedis.expire(StartWith_AUTHSRV + name,timeout);
                } finally {
                    if (null != jedis) {
                        jedis.close();
                    }
                }
            }
        }catch (Exception e){
            log.error(e);
            //throw e;
        }
    }*/

    Session active(Session s,String user,String expire,String jsessionId,boolean issetdata){
        RedisClient rc = (RedisClient)getObjectById("RedisClient");
        Jedis jedis= null;
        int sessionTimeout=0;
        if(StringUtils.isBlank(expire)){
            sessionTimeout=timeout;
        }else{
            sessionTimeout = Integer.parseInt(expire);
        }
        try{

            jedis=rc.getRedis("Session");

            String key = jedis.get(StartWith_KeySession+s.getSessionId());
            if(null ==key && StringUtils.isNotBlank(user)){
                key = user;
            }
            if(null != s.getSessionId()) {
                jedis.setex(StartWith_KeySession + key, sessionTimeout, s.getSessionId());
            }
            if(null != key) {
                jedis.setex(StartWith_KeySession + s.getSessionId(), sessionTimeout, key);
            }
            if(StringUtils.isBlank(jsessionId)){
                jsessionId = jedis.get(StartWith_JSessionRelSessionID+s.getSessionId());
            }
            if(StringUtils.isNotBlank(jsessionId)){
                jedis.setex(StartWith_JSessionRelSessionID+jsessionId,sessionTimeout,s.getSessionId());
                jedis.setex(StartWith_JSessionRelSessionID+s.getSessionId(),sessionTimeout,jsessionId);
            }
            log.debug("set session to redis:"+s);
            if(null != s) {
                String outSysID = (String) s.get("OUT_SYSTEM_ID");
                if (StringUtils.isNotBlank(outSysID) && StringUtils.isNotBlank(jsessionId)){
                    jedis.expire(StartWith_JSessionRelSessionID+outSysID+"-"+jsessionId,sessionTimeout);
                }
            }
            if(issetdata) {
                jedis.setex(StartWith_Session + s.getSessionId(), sessionTimeout, ObjectUtils.convertMap2String(s));
            }else{
                jedis.expire(StartWith_Session + s.getSessionId(),sessionTimeout);
            }
            String name = s.getUserName();

            //update srv timeout
            activekeys(jedis,name,sessionTimeout);

            return updateSession(s.getSessionId(),s);
        }catch (Exception e){
            log.error("cache Manager error",e);
        }finally {
            if(null !=jedis)
                jedis.close();
        }
        return null;
    }
    void activekeys(Jedis jedis,String name,int sessionTimeout){
        if(null != activeexpirekeys) {
            for(String s:activeexpirekeys) {
                if(StringUtils.isNotBlank(s)) {
                    if (StringUtils.isNotBlank(name) && jedis.exists(s + name)) {
                        jedis.expire(s + name, sessionTimeout);
                    }
                }
            }
        }
    }

    Session updateSession(String k,Session s){
        if(null != s) {
            Session old = sessions.get(k);
            if(log.isDebugEnabled()) {
                log.debug("update session:" + old);
            }
            if (null != old) {
                old.putAll(s);
                return old;
            } else {
                return s;
            }
        }
        return null;
    }

    public boolean deleteSession(String sessionId,String jsessionid){
        if(StringUtils.isNotBlank(sessionId)){
            RedisClient rc = (RedisClient)getObjectById("RedisClient");
            Jedis jedis= null;
            try{
                jedis=rc.getRedis("Session");
                if(StringUtils.isBlank(sessionId) && StringUtils.isNotBlank(jsessionid)){
                    sessionId = jedis.get(StartWith_JSessionRelSessionID+jsessionid);
                }
                if(StringUtils.isNotBlank(sessionId)){
                    String userkey = jedis.get(StartWith_KeySession+sessionId);
                    if(StringUtils.isNotBlank(userkey)){
                        jedis.del(StartWith_KeySession+userkey);
                    }
                    String jseesionID = jedis.get(StartWith_JSessionRelSessionID+sessionId);
                    if(StringUtils.isNotBlank(jseesionID))
                        jedis.del(StartWith_JSessionRelSessionID+jseesionID);
                    jedis.del(StartWith_JSessionRelSessionID+sessionId);
                    jedis.del(StartWith_KeySession+sessionId);
                    jedis.del(StartWith_Session+sessionId);
                    sessions.remove(sessionId);
                    return true;
                }else{
                    return false;
                }
            } catch (Exception e){
                log.error(e);
            }finally {
                if(null !=jedis)
                    jedis.close();
            }
        }
        return false;
    }
    public Session createSession(String userName,String pwd,String expire,String jsessionId){
        //RedisClient rc = (RedisClient)getObjectById("RedisClient");
        //Jedis jedis= null;
        try{
            //jedis=rc.getRedis("Session");
            Session s = createEmptySession();
            if(StringUtils.isBlank(expire)){
                expire=timeout+"";
            }
            s.put("expire",expire);
            active(s,userName,expire,jsessionId,true);
            if(log.isDebugEnabled()){
                log.debug("create session:"+s);
            }
            sessions.put(s.getSessionId(),s);
            return s;
        } catch (Exception e) {
            log.error(e);
        } finally {
            //if(null !=jedis)
                //jedis.close();
        }
        return null;
    }
    public Session getSessionById(String sessionId,String jsessionID) throws Exception {
        if(StringUtils.isBlank(sessionId) && StringUtils.isNotBlank(jsessionID)){
            if(sessions.containsKey(jsessionID)){
                if(log.isDebugEnabled()){
                    log.debug("session getSessionById:"+sessionId+","+jsessionID);
                }
                ExecutorUtils.work(new ActiveSession(sessionId,jsessionID));
                return sessions.get(jsessionID);
            }else{
                Session session = getSession(null,jsessionID);
                if(null != session) {
                    ExecutorUtils.work(new ActiveSession(sessionId, jsessionID));
                    sessions.put(jsessionID,session);
                }
                return session;
            }
        }
        if(StringUtils.isNotBlank(sessionId)) {
            if(sessions.containsKey(sessionId)) {
                if(log.isDebugEnabled()){
                    log.debug("session getSessionById:"+sessionId+","+jsessionID);
                }
                ExecutorUtils.work(new ActiveSession(sessionId,jsessionID));
                return sessions.get(sessionId);
            }else{
                Session session = getSession(sessionId,null);
                if(null != session) {
                    ExecutorUtils.work(new ActiveSession(sessionId, jsessionID));
                    sessions.put(sessionId,session);
                }
                return session;
            }
        }
        return getSession(sessionId,jsessionID);
    }
    class ActiveSession implements Runnable{
        String sessionId;
        String jsessionID;
        public ActiveSession(String sessionId,String jsessionID){
            this.sessionId=sessionId;
            this.jsessionID=jsessionID;
        }

        @Override
        public void run() {
            try {
                getSession(sessionId, jsessionID);
            }catch (Exception e){

            }
        }
    }
    Session getSession(String sessionId,String jsessionID) throws Exception {
        RedisClient rc = (RedisClient)getObjectById("RedisClient");
        log.debug("get session by sessionId:"+sessionId+" jsessionID:"+jsessionID);
        Jedis jedis= null;
        try{
            jedis=rc.getRedis("Session");
            if(StringUtils.isBlank(sessionId) && StringUtils.isNotBlank(jsessionID)){
                sessionId = jedis.get(StartWith_JSessionRelSessionID+jsessionID);
                if(StringUtils.isBlank(sessionId)){
                    sessionId=jsessionID;
                }
            }
            if(StringUtils.isNotBlank(sessionId)){
                String str = jedis.get(StartWith_Session+sessionId);
                if(StringUtils.isNotBlank(str)){
                    Map m = StringUtils.convert2MapJSONObject(str);
                    Session s = new Session();
                    s.putAll(m);
                    if(log.isDebugEnabled())
                        log.debug("get session:"+s+" by id:"+StartWith_Session+sessionId);
                    return active(s,null,(String)s.get("expire"),jsessionID,false);
                }
                sessions.remove(sessionId);
            }

            return null;
        }finally {
            if(null !=jedis)
            jedis.close();
        }
    }

    public Session getSessionByJSessionId(String jsessionID)throws Exception{
        RedisClient rc = (RedisClient)getObjectById("RedisClient");
        Jedis jedis= null;
        try{
            jedis=rc.getRedis("Session");
            if(StringUtils.isNotBlank(jsessionID)){
                String sessionId = jedis.get(StartWith_JSessionRelSessionID+jsessionID);
                if(StringUtils.isNotBlank(sessionId))
                    return getSessionById(sessionId);
            }
            return null;
        }finally {
            if(null != jedis)
                jedis.close();
        }
    }
    public Session getSessionById(String sessionId){
        return sessions.get(sessionId);
    }
    public Session createEmptySession(){
        String sessionId = generateSessionId(jvmRoute);
        Session session = new Session();
        session.setCreateDate(System.currentTimeMillis());
        session.setMaxInactiveInterval(inactiveInterval);
        session.setSessionId(sessionId);
        return session;
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            if("getLoginInfo".equals(input.get("op"))){

                Session s=  getSessionById(((RequestParameters)env).getSession().getSessionId(),((RequestParameters)env).getRequestCookies().get("JSESSIONID"));
                System.out.println("-----"+s);
                return s;
            }else if("getSession".equals(input.get("op"))){
                String sessionId = (String)input.get("sessionId");
                String purlSessionId = (String)input.get("purlSessionId");
                String un = (String)input.get("UserName");
                String token = (String)input.get("token");
                String pwd = (String)input.get("UserPwd");
                if(StringUtils.isNotBlank(purlSessionId)){
                    return getSession(purlSessionId,null);
                }if(StringUtils.isNotBlank(sessionId)){
                    return getSession(null,sessionId);
                }else {
                    return getSessionByUser(un);
                }
            }else if("createSession".equals(input.get("op"))){
                String un = (String)input.get("UserName");
                String pwd = (String)input.get("UserPwd");
                String sessionId = (String)input.get("sessionId");
                String expire = (String)input.get("expire");
                Map attrs = (Map)input.get("attrs");
                Session s = createSession(un,pwd,expire,sessionId);
                addSessionAttrs(s,attrs);
                ((RequestParameters)env).setSession(getSessionByUser(un));

            }else if("getSessionIdEndWith".equals(input.get("op"))){
                return jvmRoute;
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doInitial() throws Exception {
        ExecutorUtils.work(new Runnable(){

            @Override
            public void run() {
                try {
                    RedisClient rc = (RedisClient) getObjectById("RedisClient");
                    synchronized (sessions) {
                        Jedis jedis = rc.getRedis("Session");
                        String dbid = (String) rc.getXML().getByTagProperty("cluster", "key", "Session")[0].getChildren().get(0).getProperties().get("db");
                        //log.error("JedisPubSub patterns:" + "__key*__:*");
                        //need redis config redis.conf notify-keyspace-events Eg
                        jedis.psubscribe(new SessionJedisPubSub(dbid), "__key*__:*");
                    }
                }catch (Exception e){
                    log.error("",e);
                }
            }
        });

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public static void main(String[] args){
        try{
            SessionManager sm = new SessionManager(null,null,null);
            String s= sm.generateSessionId("");
            System.out.println(s);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
