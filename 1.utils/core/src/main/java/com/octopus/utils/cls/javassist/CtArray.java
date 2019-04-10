/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package com.octopus.utils.cls.javassist;

/**
 * Array types.
 */
final class CtArray extends com.octopus.utils.cls.javassist.CtClass {
    protected com.octopus.utils.cls.javassist.ClassPool pool;

    // the name of array type ends with "[]".
    CtArray(String name, com.octopus.utils.cls.javassist.ClassPool cp) {
        super(name);
        pool = cp;
    }

    public com.octopus.utils.cls.javassist.ClassPool getClassPool() {
        return pool;
    }

    public boolean isArray() {
        return true;
    }

    private com.octopus.utils.cls.javassist.CtClass[] interfaces = null;

    public int getModifiers() {
        int mod = com.octopus.utils.cls.javassist.Modifier.FINAL;
        try {
            mod |= getComponentType().getModifiers()
                   & (com.octopus.utils.cls.javassist.Modifier.PROTECTED | com.octopus.utils.cls.javassist.Modifier.PUBLIC | Modifier.PRIVATE);
        }
        catch (com.octopus.utils.cls.javassist.NotFoundException e) {}
        return mod;
    }

    public com.octopus.utils.cls.javassist.CtClass[] getInterfaces() throws com.octopus.utils.cls.javassist.NotFoundException {
        if (interfaces == null) {
            Class[] intfs = Object[].class.getInterfaces();
            // java.lang.Cloneable and java.io.Serializable.
            // If the JVM is CLDC, intfs is empty.
            interfaces = new com.octopus.utils.cls.javassist.CtClass[intfs.length];
            for (int i = 0; i < intfs.length; i++)
                interfaces[i] = pool.get(intfs[i].getName());
        }

        return interfaces;
    }

    public boolean subtypeOf(com.octopus.utils.cls.javassist.CtClass clazz) throws com.octopus.utils.cls.javassist.NotFoundException {
        if (super.subtypeOf(clazz))
            return true;

        String cname = clazz.getName();
        if (cname.equals(javaLangObject))
            return true;

        com.octopus.utils.cls.javassist.CtClass[] intfs = getInterfaces();
        for (int i = 0; i < intfs.length; i++)
            if (intfs[i].subtypeOf(clazz))
                return true;

        return clazz.isArray()
            && getComponentType().subtypeOf(clazz.getComponentType());
    }

    public com.octopus.utils.cls.javassist.CtClass getComponentType() throws com.octopus.utils.cls.javassist.NotFoundException {
        String name = getName();
        return pool.get(name.substring(0, name.length() - 2));
    }

    public CtClass getSuperclass() throws com.octopus.utils.cls.javassist.NotFoundException {
        return pool.get(javaLangObject);
    }

    public com.octopus.utils.cls.javassist.CtMethod[] getMethods() {
        try {
            return getSuperclass().getMethods();
        }
        catch (com.octopus.utils.cls.javassist.NotFoundException e) {
            return super.getMethods();
        }
    }

    public CtMethod getMethod(String name, String desc)
        throws com.octopus.utils.cls.javassist.NotFoundException
    {
        return getSuperclass().getMethod(name, desc);
    }

    public CtConstructor[] getConstructors() {
        try {
            return getSuperclass().getConstructors();
        }
        catch (NotFoundException e) {
            return super.getConstructors();
        }
    }
}
