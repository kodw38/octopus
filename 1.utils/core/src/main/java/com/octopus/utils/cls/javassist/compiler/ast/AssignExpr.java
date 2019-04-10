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

/**
 * Assignment expression.
 */
public class AssignExpr extends com.octopus.utils.cls.javassist.compiler.ast.Expr {
    /* operator must be either of:
     * =, %=, &=, *=, +=, -=, /=, ^=, |=, <<=, >>=, >>>=
     */

    private AssignExpr(int op, com.octopus.utils.cls.javassist.compiler.ast.ASTree _head, com.octopus.utils.cls.javassist.compiler.ast.ASTList _tail) {
        super(op, _head, _tail);
    }

    public static AssignExpr makeAssign(int op, com.octopus.utils.cls.javassist.compiler.ast.ASTree oprand1,
                                        ASTree oprand2) {
        return new AssignExpr(op, oprand1, new ASTList(oprand2));
    }

    public void accept(Visitor v) throws CompileError {
        v.atAssignExpr(this);
    }
}
