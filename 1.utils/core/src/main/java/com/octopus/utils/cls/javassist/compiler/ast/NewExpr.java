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

package com.octopus.utils.cls.javassist.compiler.ast;

import com.octopus.utils.cls.javassist.compiler.CompileError;
import com.octopus.utils.cls.javassist.compiler.TokenId;

/**
 * New Expression.
 */
public class NewExpr extends com.octopus.utils.cls.javassist.compiler.ast.ASTList implements TokenId {
    protected boolean newArray;
    protected int arrayType;

    public NewExpr(com.octopus.utils.cls.javassist.compiler.ast.ASTList className, com.octopus.utils.cls.javassist.compiler.ast.ASTList args) {
        super(className, new com.octopus.utils.cls.javassist.compiler.ast.ASTList(args));
        newArray = false;
        arrayType = CLASS;
    }

    public NewExpr(int type, com.octopus.utils.cls.javassist.compiler.ast.ASTList arraySize, com.octopus.utils.cls.javassist.compiler.ast.ArrayInit init) {
        super(null, new com.octopus.utils.cls.javassist.compiler.ast.ASTList(arraySize));
        newArray = true;
        arrayType = type;
        if (init != null)
            append(this, init);
    }

    public static NewExpr makeObjectArray(com.octopus.utils.cls.javassist.compiler.ast.ASTList className,
                                          com.octopus.utils.cls.javassist.compiler.ast.ASTList arraySize, com.octopus.utils.cls.javassist.compiler.ast.ArrayInit init) {
        NewExpr e = new NewExpr(className, arraySize);
        e.newArray = true;
        if (init != null)
            append(e, init);

        return e;
    }

    public boolean isArray() { return newArray; }

    /* TokenId.CLASS, TokenId.INT, ...
     */
    public int getArrayType() { return arrayType; }

    public com.octopus.utils.cls.javassist.compiler.ast.ASTList getClassName() { return (com.octopus.utils.cls.javassist.compiler.ast.ASTList)getLeft(); }

    public com.octopus.utils.cls.javassist.compiler.ast.ASTList getArguments() { return (com.octopus.utils.cls.javassist.compiler.ast.ASTList)getRight().getLeft(); }

    public com.octopus.utils.cls.javassist.compiler.ast.ASTList getArraySize() { return getArguments(); }

    public com.octopus.utils.cls.javassist.compiler.ast.ArrayInit getInitializer() {
        ASTree t = getRight().getRight();
        if (t == null)
            return null;
        else
            return (ArrayInit)t.getLeft();
    }

    public void accept(Visitor v) throws CompileError { v.atNewExpr(this); }

    protected String getTag() {
        return newArray ? "new[]" : "new";
    }
}
