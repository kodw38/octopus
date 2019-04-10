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

package com.octopus.utils.cls.javassist.bytecode.stackmap;

import com.octopus.utils.cls.javassist.bytecode.StackMapTable;

public interface TypeTag {
    String TOP_TYPE = "*top*";
    com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData TOP = new com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData.BasicType(TOP_TYPE, StackMapTable.TOP);
    com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData INTEGER = new com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData.BasicType("int", StackMapTable.INTEGER);
    com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData FLOAT = new com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData.BasicType("float", StackMapTable.FLOAT);
    com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData DOUBLE = new com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData.BasicType("double", StackMapTable.DOUBLE);
    com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData LONG = new com.octopus.utils.cls.javassist.bytecode.stackmap.TypeData.BasicType("long", StackMapTable.LONG);

    // and NULL, THIS, OBJECT, UNINIT
}
