package com.octopus.utils.cls.proxy;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.cls.JavaStringObject;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.cls.javassist.*;
import com.octopus.utils.cls.jcl.MultiClassLoader;
import com.octopus.utils.file.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.texen.util.FileUtil;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * User: Administrator
 * Date: 14-8-28
 * Time: 下午7:46
 */
public class GeneratorClass {
    static transient Log log = LogFactory.getLog(GeneratorClass.class);
    static ClassPool pool = ClassPool.getDefault();
    public static Map<String,byte[]> bytesCode = new HashMap<String, byte[]>();

    public static byte [] getByteCode(String className){
        return bytesCode.get(className);
    }
    static Method[] filterSame(Method[] ms){
        List<Method> ls = new ArrayList<Method>();
        for(Method cm:ms){
            try{
                if(ls.size()==0){
                    ls.add(cm);
                    continue;
                }
                boolean isExist=false;
                for(Method m:ls){
                    if(m.getName().equals(cm.getName()) && m.getParameterTypes().length==cm.getParameterTypes().length){
                        int i=0;
                        for(i=0;i<m.getParameterTypes().length;i++){
                            if(!m.getParameterTypes()[i].getName().equals(cm.getParameterTypes()[i].getName())){
                                break;
                            }
                        }
                        if(i==m.getParameterTypes().length){
                            isExist=true;
                            break;
                        }
                    }
                }
                if(!isExist){
                    ls.add(cm);
                }

            }catch (Exception e){

            }
        }
        return ls.toArray(new Method[0]);
    }
    static Class getClass(String className){
        try{
            return GeneratorClass.class.getClassLoader().loadClass(className);
        }catch (Exception e){
            return null;
        }
    }
    public static String getMethodParameterClass(Method m){
        Class[] cs = m.getParameterTypes();
        if(null != cs && cs.length>0){
            StringBuffer sb = new StringBuffer("new Class[]{");
            for(int i=0;i<cs.length;i++){
                if(i==0){
                    sb.append(cs[i].getName()).append(".class");
                }else{
                    sb.append(",").append(cs[i].getName()).append(".class");
                }
            }
            sb.append("}");
            return sb.toString();
        }else {
            return null;
        }
    }
    static String getSetMethodBosy(String name,String type){
       return "public void set"+ POJOUtil.firstCharUpcase(name)+"("+type+" "+name+"){this."+name+"="+name+";}";
    }
    static String getGetMethodBosy(String name,String type){
        return "public "+type+" get"+ POJOUtil.firstCharUpcase(name)+"(){return this."+name+";}";
    }

    /***
     *
     * @param className  类名称
     * @param fields    属性名称，属性类型
     * @return
     * @throws CannotCompileException
     * @throws NotFoundException
     * @throws IOException
     */
    public static synchronized Class generatorPOJOClass(String className,Map<String,String> fields) throws CannotCompileException, NotFoundException, IOException {
        pool = ClassPool.getDefault();
        ClassClassPath classPath = new ClassClassPath(GeneratorClass.class);
        pool.insertClassPath(classPath);

        Class c = getClass(className);
        if(c==null){
            CtClass cc = pool.makeClass(className);
            if(null != fields){
                Iterator<String> its = fields.keySet().iterator();
                while(its.hasNext()){
                    String name = its.next();
                    String type = fields.get(name);
                    CtField field=null;
                    try{
                        field = new CtField(pool.get(type),name,cc);
                    }catch (NotFoundException e){
                        throw new NotFoundException("create pojo error , find field ["+type+" "+name+"] from class["+className+"]",e);
                    }
                    cc.addField(field);
                    CtMethod method = CtMethod.make(getSetMethodBosy(name, type),cc);
                    cc.addMethod(method);
                    CtMethod method2 = CtMethod.make(getGetMethodBosy(name, type),cc);
                    cc.addMethod(method2);
                }
            }
            //cc.writeFile();

            return cc.toClass();
        }
        return c;
    }

    public static String getClassPath(ClassLoader loader) throws Exception {

        List tmpClassLoader = new ArrayList();
        List urls = new ArrayList();
        StringBuilder sb = new StringBuilder();
        while ((loader != null) &&
                (!tmpClassLoader.contains(loader)))
        {
            if ((loader instanceof URLClassLoader)) {
                URLClassLoader uL = (URLClassLoader)loader;
                URL[] path = uL.getURLs();
                if (path != null) {
                    for (int i = 0; i < path.length; i++) {
                        sb.append(File.pathSeparator).append(path[i].getPath());
                        urls.add(path[i].getFile());
                    }
                }

            }
            if( MultiClassLoader.class.isAssignableFrom(loader.getClass()) ){
                URL[] path =(URL[])loader.getClass().getMethod("getURLs", null).invoke(loader, null);
                if(path!=null){
                    for (int i = 0; i < path.length; i++) {
                        sb.append(File.pathSeparator).append(path[i].getPath());
                        urls.add(path[i].getFile());
                    }
                }
            }
            tmpClassLoader.add(loader);
            loader = loader.getParent();
        }

        loader = GeneratorClass.class.getClassLoader();
        while ((loader != null) &&
                (!tmpClassLoader.contains(loader)))
        {
            if ((loader instanceof URLClassLoader)) {
                URLClassLoader uL = (URLClassLoader)loader;
                URL[] path = uL.getURLs();
                if (path != null) {
                    for (int i = 0; i < path.length; i++) {
                        sb.append(File.pathSeparator).append(path[i].getPath());
                        urls.add(path[i].getFile());
                    }
                }

            }
            if( MultiClassLoader.class.isAssignableFrom(loader.getClass()) ){
                URL[] path =(URL[])loader.getClass().getMethod("getURLs", null).invoke(loader, null);
                if(path!=null){
                    for (int i = 0; i < path.length; i++) {
                        sb.append(File.pathSeparator).append(path[i].getPath());
                        urls.add(path[i].getFile());
                    }
                }
            }
            tmpClassLoader.add(loader);
            loader = loader.getParent();
        }

        return sb.toString();

    }

    public static synchronized void compile(ClassLoader loader,String compilePath) throws Exception {
        StringBuilder sbClassPath = new StringBuilder();
        sbClassPath.append(getClassPath(loader)).append(java.io.File.pathSeparatorChar).append(compilePath);

        List<File> li = FileUtils.getAllFile(compilePath, new String[]{".java"});
        String classpath = sbClassPath.toString();
        if(null != li && li.size()>0){
            for(File f:li){
                String[] args = new String[] {"-encoding","UTF-8","-g",
                        "-classpath", classpath, "-d",
                        compilePath,f.getPath()
                };
                com.sun.tools.javac.Main.compile(args);
            }
        }
    }
    public static synchronized Class generatorClass2(String className,String clazzbody,String compildPath){
        String fname=null;
        try {
            JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
            String classpath = getClassPath(GeneratorClass.class.getClassLoader());

            String name = className;
            if(className.contains(".")){
                name = className.substring(className.lastIndexOf(".")+1);
            }
            JavaFileObject fileObject = new JavaStringObject(name, clazzbody);
            if(StringUtils.isBlank(compildPath)) {
                throw new Exception("not input compildPath:"+compildPath);
            }
            FileUtil.mkdir(compildPath);
            log.info("compildPath:"+compildPath);
            List<String> optionList = new ArrayList<String>();
            optionList.add("-g:lines,vars,source");
            optionList.addAll(Arrays.asList("-classpath",classpath));
            optionList.addAll(Arrays.asList("-d",compildPath));

            fname="" + compildPath  + className.replace(".", "/") + ".class";

            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, null, null,optionList, null, Arrays.asList(fileObject));
            boolean success = task.call();
            if (!success) {
                log.error("compile "+fname+" fault.");
                return null;
            }else {
                log.info("compile "+fname+" successful.");
                FileInputStream classfile = new FileInputStream(fname);
                BufferedInputStream buf = new BufferedInputStream(classfile);
                byte[] bys = new byte[buf.available()];
                buf.read(bys);
                Class cc = ClassUtils.defineClass(GeneratorClass.class.getClassLoader(),bys,className);
                log.debug("has load class:["+className+"]");
                return cc;
            }
        } catch (IOException e1) {
            log.error("not find class file:"+clazzbody+"\n"+fname, e1);
        } catch (CannotCompileException e1) {
            log.error("compile exception", e1);
        } catch (NotFoundException e1) {
            log.error("not find exception:",e1);
        } catch (Exception e1) {
            log.error("get classpath error:",e1);
        }
        return null;
    }

    public static synchronized Class generatorClass(String className,String clazzbody,String compildPath){
        if(bytesCode.containsKey(className)){
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(GeneratorClass.class));
        CtClass cc=null;
        try{
            //cc = pool.get(className);
            return GeneratorClass.class.getClassLoader().loadClass(className);
        }catch (Exception e){
            String fname=null;
            try {
                log.debug("start generator class:"+className + " to "+compildPath);
                JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
                String classpath = compildPath.replaceAll("\\\\","/")+getClassPath(GeneratorClass.class.getClassLoader());
                if(log.isDebugEnabled()){
                    log.debug("classpath:"+classpath);
                }
                String name = className;
                String pkg ="";
                if(className.contains(".")){
                    name = className.substring(className.lastIndexOf(".")+1);
                    pkg = className.substring(0,className.lastIndexOf("."));
                }

                JavaFileObject fileObject = new JavaStringObject(name, clazzbody);
                if(StringUtils.isBlank(compildPath)) {
                    throw new Exception("not input compildPath:"+compildPath);
                }
                FileUtil.mkdir(compildPath);
                log.debug("compildPath:"+compildPath);
                List<String> optionList = new ArrayList<String>();
                optionList.add("-g:lines,vars,source");
                optionList.add("-Xlint:unchecked");
                optionList.addAll(Arrays.asList("-classpath",classpath));
                optionList.addAll(Arrays.asList("-d",compildPath));

                fname="" + compildPath  + className.replace(".", "/") + ".class";

/*
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
                StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(diagnostics, null, null);
                //Iterable<? extends JavaFileObject> compilationUnits = fileManager.get(Arrays.asList(fileObject));
*/
                JavaCompiler.CompilationTask task = javaCompiler.getTask(null, null, null,optionList, null, Arrays.asList(fileObject));
                boolean success = task.call();
                if (!success) {
                    log.error("compile "+fname+" fault.");
                    return null;
                }else {
                    log.debug("compile "+fname+" successful.");
                    FileInputStream classfile = new FileInputStream(fname);
                    cc = pool.makeClass(classfile);
                    //cc.writeFile();
                    bytesCode.put(className, cc.toBytecode());
                    log.debug("has load class:[" + className + "]");
                    Class c =  cc.toClass();
                    if(c.getPackage()==null){
                        if(StringUtils.isNotBlank(pkg))
                        pool.makePackage(cc.getClass().getClassLoader(),pkg);
                        //System.out.println();
                    }
                    return c;
                }
            } catch (IOException e1) {
                log.error("not find class file:"+clazzbody+"\n"+fname, e1);
            } catch (CannotCompileException e1) {
                log.error("not find class file:"+clazzbody+"\n"+fname, e1);
            } catch (NotFoundException e1) {
                log.error("not find class file:"+clazzbody+"\n"+fname, e1);
            } catch (Exception e1) {
                log.error("not find class file:"+clazzbody+"\n"+className, e1);
            }

        }

        return null;
    }


    /**
     *
     * @param className  要生成的类名称,如 com.octopus.AA
     * @param fields<String,String> 属性名称，属性类型
     * @return
     * @throws IOException
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    public static synchronized Class generatorClass(String className,Map<String,String> fields,Map<String,String> methodBodys ) throws IOException, CannotCompileException, NotFoundException,Exception {
        pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(GeneratorClass.class));
        CtClass cc=null;
        try{
            cc = pool.get(className);
        }catch (Exception e){}
        boolean chg=false;
        boolean isexist=false;
        if(null == cc){
            cc = pool.makeClass(className);
            chg=true;
        }else{
            cc = pool.get(className);
            cc.defrost();
            isexist=true;
        }
        if(null != fields){
            Iterator<String> its = fields.keySet().iterator();
            while(its.hasNext()){
                String name = its.next();
                try{
                    cc.getField(name);
                }catch (Exception e){
                    String type = null;
                    type = fields.get(name);
                    String generic=null;
                    if(type.contains("<")){
                        List<String>gs= StringUtils.getTagsNoMark(type,"<",">");
                        if(null != gs && gs.size()>0){
                            generic=gs.get(0);
                        }
                        type = type.substring(0,type.indexOf("<"));
                    }
                    CtField field = new CtField(pool.get(type),name,cc);
                    if(null != generic) {
                        field.setGenericSignature(generic);
                    }
                    cc.addField(field);
                    chg=true;
                }
            }
        }
        if(null != methodBodys){
            Iterator<String> its = methodBodys.keySet().iterator();
            while(its.hasNext()){
                String methodName = its.next();
                try{
                    boolean in=false;
                    CtMethod[] ms= cc.getMethods();
                    for(CtMember m:ms){
                        if(m.getName().equals(methodName)){
                            in=true;
                            break;
                        }
                    }
                    if(!in){
                        CtMethod method = CtMethod.make(methodBodys.get(methodName),cc);
                        cc.addMethod(method);
                        chg=true;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }

        }
        if(chg){
            try{
                //cc.writeFile();
                bytesCode.put(className,cc.toBytecode());
                return cc.toClass();
            }catch (Exception e){
                throw new Exception(className,e);
            }
        }else{
            return null;
        }
    }

    public static synchronized Class generatorClass(Class c,Method[] ms,String proxyClassName,Class[] constructorClass){
        try{
            if(ArrayUtils.isNotEmpty(ms)){
                pool = ClassPool.getDefault();
                pool.insertClassPath(new ClassClassPath(c));
                pool.insertClassPath(new ClassClassPath(IMethodAddition.class));
                CtClass fieldType=pool.get(IMethodAddition[].class.getName());
                CtClass handlerType=pool.get(IProxyHandler.class.getName());
                CtClass sup=pool.get(c.getName());
                CtClass cc = pool.makeClass(proxyClassName);
                cc.setSuperclass(sup);
                CtField field = new CtField(fieldType,"additions",cc);
                CtField handler = new CtField(handlerType,"handler",cc);
                cc.addField(field);
                cc.addField(handler);

                if(null != constructorClass){
                    CtClass[] cs = new CtClass[constructorClass.length];
                    for(int i=0;i<cs.length;i++){
                        cs[i] = pool.get(constructorClass[i].getName());
                    }

                    CtConstructor cons = new CtConstructor(cs,cc);
                    cons.setBody("{super("+getValues(constructorClass.length)+");}");
                    cc.addConstructor(cons);
                }

                ms = filterSame(ms);
                for(Method m:ms){
                    if(Modifier.isPublic(m.getModifiers())){
                        addClass(m.getReturnType());
                        addClass(m.getExceptionTypes()) ;
                        addClass(m.getParameterTypes());
                        boolean  isException=false;
                        Class[] es = m.getExceptionTypes();
                        if(ArrayUtils.isNotEmpty(es)){
                            isException=true;
                        }
                        //super method
                        StringBuffer sub = new StringBuffer(getMethodDeclare(m,"super_"+m.getName()));
                        sub.append("{");
                        String par="";
                        if(null == m.getParameterTypes() || m.getParameterTypes().length==0){
                            par="";
                        }else{
                            par = getValues(m.getParameterTypes().length);
                        }
                        if(m.getReturnType().getName().equals("void")){
                            sub.append(" super."+m.getName()+"("+par+");");
                        }else{
                            sub.append(" return super."+m.getName()+"("+par+");");
                        }
                        sub.append("}");
                        if(log.isDebugEnabled()) {
                            log.debug("make method:" + sub);
                        }
                        CtMethod submethod = CtMethod.make(sub.toString(),cc);
                        cc.addMethod(submethod);

                        //proxy method
                        StringBuffer sb = new StringBuffer(getMethodDeclare(m,m.getName()));
                        sb.append("{try{");
                        if(m.getReturnType().getName().equals("void")){
                            sb.append(" handler.handle(additions,this,\""+m.getName()+"\","+getMethodParameterClass(m)+",$args);");
                        }else{
                            if(m.getReturnType().isPrimitive()){
                                sb.append(" return com.octopus.utils.cls.ClassUtils.convertObject2Primitive(("+com.octopus.utils.cls.ClassUtils.getPrimitiveObjectNameByPrimitiveName(m.getReturnType().getName())+")handler.handle(additions,this,\""+m.getName()+"\","+getMethodParameterClass(m)+",$args));");
                            }else{
                                String stype="";
                                if(m.getReturnType().isArray()){
                                    stype=((Class)m.getReturnType().getComponentType()).getName()+"[] ";
                                }else{
                                    stype=m.getReturnType().getName();
                                }
                                sb.append(" return ("+stype+")handler.handle(additions,this,\""+m.getName()+"\","+getMethodParameterClass(m)+",$args);");
                            }
                        }

                        boolean ex=false;
                        if(isException){
                            for(Class e:es){
                                if(e==Exception.class){
                                    ex=true;
                                }
                                sb.append("}catch ("+e.getName()+" e){");
                                sb.append("throw e;");
                            }
                        }
                        if(!ex){
                            sb.append("}catch(Exception e){");
                            if(!m.getReturnType().getName().equals("void")){
                                if(m.getReturnType().isPrimitive()){
                                    sb.append(" return "+ClassUtils.getClassTypeDefaultValue(m.getReturnType().getName())+";");
                                }else{
                                    sb.append(" return null;");
                                }
                            }
                            sb.append("}");
                        }
                        sb.append("}");
                        sb.append("}");

                        /*sb.append("{").append("boolean isSuccess=true,isInvoke=true;Throwable xe=null;");
                        String rettype="";
                        if(!m.getReturnType().getName().equals("void")){
                            if(m.getReturnType().isArray()){
                                rettype = m.getReturnType().getComponentType().getName()+"[]";
                                sb.append(rettype+" result=null;");
                            }else{
                                rettype = m.getReturnType().getName();
                                sb.append(rettype+" result="+ ClassUtils.getClassTypeDefaultValue(m.getReturnType().getName())+";");
                            }
                        }
                        if(isException)
                            sb.append("try{");
                        sb.append("try{");
                        sb.append("Object temp=null;");
                        sb.append("for(int i=0;i<this.additions.length;i++)temp = ((com.octopus.utils.cls.proxy.IMethodAddition)additions[i]).beforeAction(this,\""+m.getName()+"\",$args);");
                        sb.append("if(null != temp){isInvoke=false;}");
                        if(!m.getReturnType().getName().equals("void")){
                            if(m.getReturnType().isPrimitive()){
                                sb.append("result=com.octopus.utils.cls.ClassUtils.convertObject2Primitive(temp);");
                            }else{
                                sb.append("result=("+rettype+")temp;");
                            }
                        }
                        sb.append("}catch(Exception e){isInvoke=false;}");
                        sb.append("if(isInvoke){");
                        if(!m.getReturnType().getName().equals("void")){
                            if(ArrayUtils.isNotEmpty(m.getParameterTypes()))
                                sb.append("result = super."+m.getName()+"("+getValues(m.getParameterTypes().length)+");");
                            else
                                sb.append("result = super."+m.getName()+"();");
                        }else{
                            if(ArrayUtils.isNotEmpty(m.getParameterTypes()))
                                sb.append("super."+m.getName()+"("+getValues(m.getParameterTypes().length)+");");
                            else
                                sb.append("super."+m.getName()+"();");
                        }
                        sb.append("}");
                        if(isException){
                            for(Class e:es){
                                sb.append("}catch ("+e.getName()+" e){");
                                sb.append("xe=e;").append("isSuccess=false;").append("throw e;");
                            }
                            sb.append("}finally {");
                        }
                        if(!m.getReturnType().getName().equals("void")){
                            if(m.getReturnType().isPrimitive()){
                                String type = ClassUtils.getPrimitiveObjectClass(m.getReturnType()).getName();
                                sb.append(type+" retO=null;");
                                sb.append("retO=com.octopus.utils.cls.ClassUtils.convertPrimitive2Object(result);");
                                sb.append("for(int i=0;i<this.additions.length;i++)retO = ("+type+")((com.octopus.utils.cls.proxy.IMethodAddition)additions[i]).afterAction(this,\"" + m.getName() + "\",$args,isInvoke,isSuccess,xe,retO);");
                            }else
                                sb.append("for(int i=0;i<this.additions.length;i++)result=("+rettype+")((com.octopus.utils.cls.proxy.IMethodAddition)additions[i]).afterAction(this,\"" + m.getName() + "\",$args,isInvoke,isSuccess,xe,result);");
                        }else{
                            sb.append("for(int i=0;i<this.additions.length;i++)((com.octopus.utils.cls.proxy.IMethodAddition)additions[i]).afterAction(this,\"" + m.getName() + "\",$args,isInvoke,isSuccess,xe,null);");
                        }

                        if(!m.getReturnType().getName().equals("void")){
                            sb.append("for(int i=0;i<this.additions.length;i++){");
                            if(!m.getReturnType().getName().equals("void") && m.getReturnType().isPrimitive()){
                                sb.append("retO= ("+ClassUtils.getPrimitiveObjectClass(m.getReturnType()).getName()+")((com.octopus.utils.cls.proxy.IMethodAddition)additions[i]).dealResult(this,\""+m.getName()+"\",$args,isInvoke,retO);");
                            }else{
                                sb.append("result = ("+rettype+")((com.octopus.utils.cls.proxy.IMethodAddition)additions[i]).dealResult(this,\""+m.getName()+"\",$args,isInvoke,result);");
                            }
                            if(!m.getReturnType().getName().equals("void") && m.getReturnType().isPrimitive()){
                                sb.append("result=com.octopus.utils.cls.ClassUtils.convertObject2Primitive(retO);");
                            }

                            sb.append("}");
                            sb.append("return result;");
                        }
                        sb.append("}");
                        if(isException)
                            sb.append("}");*/

                        try{
                            if(log.isDebugEnabled()) {
                                log.debug("["+c.getName()+"] append proxy method :\n"+sb.toString());
                            }
                            CtMethod method = CtMethod.make(sb.toString(),cc);
                            cc.addMethod(method);
                        }catch (Exception e){
                            log.error("dynamical compile method exception:"+c.getName()+"#"+m,e);
                        }
                    }
                }
                //cc.writeFile();
                Class gc= cc.toClass();
                log.info("generator proxy class "+proxyClassName +" successful");
                return gc;
            }
        }catch (Exception e){
            log.error("dynamical compile class exception:"+c.getName(),e);
        }
        return null;
    }
    public static boolean appendMethod(String c, String[] methodName, String appendBefore, String appendError, String appendFinally,String savePath) {
        return appendMethodInClassPath(null,true, c, methodName, appendBefore, appendError, appendFinally, savePath);
    }

    public static boolean appendMethodInClassPath(String cPath,boolean istoClass,String c, String[] methodName, String appendBefore, String appendError, String appendFinally,String savePath){
        try {
            ClassPool pool = ClassPool.getDefault();
            if(StringUtils.isBlank(cPath)) {
                ClassClassPath classPath = new ClassClassPath(GeneratorClass.class);
                pool.insertClassPath(classPath);
            }else{
                pool.insertClassPath(cPath);
            }

            CtClass cc = pool.get(c);

            //CtMethod m = cc.getDeclaredMethod(methodName,new CtClass[]{pool.get("java.lang.String")});
            for(String mn:methodName) {
                CtMethod m = cc.getDeclaredMethod(mn);
                if (m.getMethodInfo().isMethod()) {
                    //m.addLocalVariable("xx_m",pool.get("java.util.Map"));
                    //m.insertBefore("System.out.println(\"0000000000000000000000000\");");
                    if (org.apache.commons.lang.StringUtils.isNotBlank(appendBefore)) {
                        m.insertBefore("{" + appendBefore + "}");
                    }

                    //m.insertAfter("System.out.println(\"11111111111111111111111\");",true);
                    if (org.apache.commons.lang.StringUtils.isNotBlank(appendFinally)) {
                        m.insertAfter("{" + appendFinally + "}");
                    }

                    if (org.apache.commons.lang.StringUtils.isNotBlank(appendError)) {
                        CtClass etype = pool.get("java.lang.Throwable");
                        m.addCatch("{ " + appendError + " throw $e; }", etype);
                    }

                }
            }
            //cc.toClass();
            //cc.writeFile();
            //cc.defrost();
            if(StringUtils.isNotBlank(savePath)) {
               cc.writeFile(savePath);
            }
            if(istoClass) {
                cc.toClass();
            }
            cc.detach();
            return true;
        }catch(Throwable e){
            e.printStackTrace();
            return false;
        }
    }

    static String getMethodDeclare(Method m,String methodName){
        StringBuffer sb = new StringBuffer("public ");
        String returntype="";
        if(m.getReturnType().isArray()){
            returntype = m.getReturnType().getComponentType().getName()+"[]";
        }else{
            returntype = m.getReturnType().getName();
        }
        sb.append(returntype).append(" ").append(methodName).append("(");
        Class[] pts = m.getParameterTypes();
        if(ArrayUtils.isNotEmpty(pts)){
            boolean isf=true;
            int c=0;
            for(Class p:pts){
                if(isf){
                    isf=false;
                }else{
                    sb.append(",");
                }
                if(p.isArray()){
                    sb.append(p.getComponentType().getName()).append("[] ").append("arg"+c);
                }else{
                    sb.append(p.getName()).append(" ").append("arg"+c);
                }
                c++;
            }
        }
        sb.append(")");
        Class[] ecs = m.getExceptionTypes();
        boolean isf=true;
        if(ArrayUtils.isNotEmpty(ecs)){
            sb.append("throws ");
            for(Class e:ecs){
                if(isf){
                    isf=false;
                }else{
                    sb.append(",");
                }
                sb.append(e.getName());
            }
        }
        return sb.toString();
    }

    static void addClass(Class c){
        pool.insertClassPath(new ClassClassPath(c));
    }
    static void addClass(Class[] cs){
        for(Class c:cs)
            pool.insertClassPath(new ClassClassPath(c));
    }

    static String getValues(int c){
        StringBuffer sb = new StringBuffer();
        boolean isf=true;
        for(int i=1;i<=c;i++){
            if(isf){
                isf=false;
            }else{
                sb.append(",");
            }
            sb.append("$").append(i);
        }
        return sb.toString();
    }
}
