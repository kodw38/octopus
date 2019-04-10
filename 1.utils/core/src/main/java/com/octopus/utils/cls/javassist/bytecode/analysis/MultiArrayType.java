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
package com.octopus.utils.cls.javassist.bytecode.analysis;

import com.octopus.utils.cls.javassist.ClassPool;
import com.octopus.utils.cls.javassist.CtClass;
import com.octopus.utils.cls.javassist.NotFoundException;

/**
 * Represents an array of {@link com.octopus.utils.cls.javassist.bytecode.analysis.MultiType} instances.
 *
 * @author Jason T. Greene
 */
public class MultiArrayType extends com.octopus.utils.cls.javassist.bytecode.analysis.Type {
    private com.octopus.utils.cls.javassist.bytecode.analysis.MultiType component;
    private int dims;

    public MultiArrayType(com.octopus.utils.cls.javassist.bytecode.analysis.MultiType component, int dims) {
        super(null);
        this.component = component;
        this.dims = dims;
    }

    public CtClass getCtClass() {
        CtClass clazz = component.getCtClass();
        if (clazz == null)
            return null;

        ClassPool pool = clazz.getClassPool();
        if (pool == null)
            pool = ClassPool.getDefault();

        String name = arrayName(clazz.getName(), dims);

        try {
            return pool.get(name);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    boolean popChanged() {
        return component.popChanged();
    }

    public int getDimensions() {
        return dims;
    }

    public com.octopus.utils.cls.javassist.bytecode.analysis.Type getComponent() {
       return dims == 1 ? (com.octopus.utils.cls.javassist.bytecode.analysis.Type)component : new MultiArrayType(component, dims - 1);
    }

    public int getSize() {
        return 1;
    }

    public boolean isArray() {
        return true;
    }

    public boolean isAssignableFrom(com.octopus.utils.cls.javassist.bytecode.analysis.Type type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isReference() {
       return true;
    }

    public boolean isAssignableTo(com.octopus.utils.cls.javassist.bytecode.analysis.Type type) {
        if (eq(type.getCtClass(), com.octopus.utils.cls.javassist.bytecode.analysis.Type.OBJECT.getCtClass()))
            return true;

        if (eq(type.getCtClass(), com.octopus.utils.cls.javassist.bytecode.analysis.Type.CLONEABLE.getCtClass()))
            return true;

        if (eq(type.getCtClass(), com.octopus.utils.cls.javassist.bytecode.analysis.Type.SERIALIZABLE.getCtClass()))
            return true;

        if (! type.isArray())
            return false;

        com.octopus.utils.cls.javassist.bytecode.analysis.Type typeRoot = getRootComponent(type);
        int typeDims = type.getDimensions();

        if (typeDims > dims)
            return false;

        if (typeDims < dims) {
            if (eq(typeRoot.getCtClass(), com.octopus.utils.cls.javassist.bytecode.analysis.Type.OBJECT.getCtClass()))
                return true;

            if (eq(typeRoot.getCtClass(), com.octopus.utils.cls.javassist.bytecode.analysis.Type.CLONEABLE.getCtClass()))
                return true;

            if (eq(typeRoot.getCtClass(), Type.SERIALIZABLE.getCtClass()))
                return true;

            return false;
        }

        return component.isAssignableTo(typeRoot);
    }

    public boolean equals(Object o) {
        if (! (o instanceof MultiArrayType))
            return false;
        MultiArrayType multi = (MultiArrayType)o;

        return component.equals(multi.component) && dims == multi.dims;
    }

    public String toString() {
        // follows the same detailed formating scheme as component
        return arrayName(component.toString(), dims);
    }
}
