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

import com.octopus.utils.cls.javassist.CtMethod.ConstParameter;
import com.octopus.utils.cls.javassist.bytecode.Bytecode;
import com.octopus.utils.cls.javassist.bytecode.ClassFile;
import com.octopus.utils.cls.javassist.bytecode.Descriptor;

class CtNewWrappedConstructor extends com.octopus.utils.cls.javassist.CtNewWrappedMethod {
    private static final int PASS_NONE = com.octopus.utils.cls.javassist.CtNewConstructor.PASS_NONE;
    // private static final int PASS_ARRAY = CtNewConstructor.PASS_ARRAY;
    private static final int PASS_PARAMS = CtNewConstructor.PASS_PARAMS;

    public static com.octopus.utils.cls.javassist.CtConstructor wrapped(com.octopus.utils.cls.javassist.CtClass[] parameterTypes,
                                        com.octopus.utils.cls.javassist.CtClass[] exceptionTypes,
                                        int howToCallSuper,
                                        com.octopus.utils.cls.javassist.CtMethod body,
                                        ConstParameter constParam,
                                        com.octopus.utils.cls.javassist.CtClass declaring)
        throws com.octopus.utils.cls.javassist.CannotCompileException
    {
        try {
            com.octopus.utils.cls.javassist.CtConstructor cons = new CtConstructor(parameterTypes, declaring);
            cons.setExceptionTypes(exceptionTypes);
            Bytecode code = makeBody(declaring, declaring.getClassFile2(),
                                     howToCallSuper, body,
                                     parameterTypes, constParam);
            cons.getMethodInfo2().setCodeAttribute(code.toCodeAttribute());
            // a stack map table is not needed.
            return cons;
        }
        catch (NotFoundException e) {
            throw new com.octopus.utils.cls.javassist.CannotCompileException(e);
        }
    }

    protected static Bytecode makeBody(com.octopus.utils.cls.javassist.CtClass declaring, ClassFile classfile,
                                       int howToCallSuper,
                                       CtMethod wrappedBody,
                                       com.octopus.utils.cls.javassist.CtClass[] parameters,
                                       ConstParameter cparam)
        throws CannotCompileException
    {
        int stacksize, stacksize2;

        int superclazz = classfile.getSuperclassId();
        Bytecode code = new Bytecode(classfile.getConstPool(), 0, 0);
        code.setMaxLocals(false, parameters, 0);
        code.addAload(0);
        if (howToCallSuper == PASS_NONE) {
            stacksize = 1;
            code.addInvokespecial(superclazz, "<init>", "()V");
        }
        else if (howToCallSuper == PASS_PARAMS) {
            stacksize = code.addLoadParameters(parameters, 1) + 1;
            code.addInvokespecial(superclazz, "<init>",
                                  Descriptor.ofConstructor(parameters));
        }
        else {
            stacksize = compileParameterList(code, parameters, 1);
            String desc;
            if (cparam == null) {
                stacksize2 = 2;
                desc = ConstParameter.defaultConstDescriptor();
            }
            else {
                stacksize2 = cparam.compile(code) + 2;
                desc = cparam.constDescriptor();
            }

            if (stacksize < stacksize2)
                stacksize = stacksize2;

            code.addInvokespecial(superclazz, "<init>", desc);
        }

        if (wrappedBody == null)
            code.add(Bytecode.RETURN);
        else {
            stacksize2 = makeBody0(declaring, classfile, wrappedBody,
                                   false, parameters, CtClass.voidType,
                                   cparam, code);
            if (stacksize < stacksize2)
                stacksize = stacksize2;
        }

        code.setMaxStack(stacksize);
        return code;
    }
}
