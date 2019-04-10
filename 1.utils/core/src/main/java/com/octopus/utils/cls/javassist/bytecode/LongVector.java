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

package com.octopus.utils.cls.javassist.bytecode;

final class LongVector {
    static final int ASIZE = 128;
    static final int ABITS = 7;  // ASIZE = 2^ABITS
    static final int VSIZE = 8;
    private com.octopus.utils.cls.javassist.bytecode.ConstInfo[][] objects;
    private int elements;

    public LongVector() {
        objects = new com.octopus.utils.cls.javassist.bytecode.ConstInfo[VSIZE][];
        elements = 0;
    }

    public LongVector(int initialSize) {
        int vsize = ((initialSize >> ABITS) & ~(VSIZE - 1)) + VSIZE;
        objects = new com.octopus.utils.cls.javassist.bytecode.ConstInfo[vsize][];
        elements = 0;
    }

    public int size() { return elements; }

    public int capacity() { return objects.length * ASIZE; }

    public com.octopus.utils.cls.javassist.bytecode.ConstInfo elementAt(int i) {
        if (i < 0 || elements <= i)
            return null;

        return objects[i >> ABITS][i & (ASIZE - 1)];
    }

    public void addElement(com.octopus.utils.cls.javassist.bytecode.ConstInfo value) {
        int nth = elements >> ABITS;
        int offset = elements & (ASIZE - 1);
        int len = objects.length;
        if (nth >= len) { 
            com.octopus.utils.cls.javassist.bytecode.ConstInfo[][] newObj = new com.octopus.utils.cls.javassist.bytecode.ConstInfo[len + VSIZE][];
            System.arraycopy(objects, 0, newObj, 0, len);
            objects = newObj;
        }

        if (objects[nth] == null)
            objects[nth] = new com.octopus.utils.cls.javassist.bytecode.ConstInfo[ASIZE];

        objects[nth][offset] = value;
        elements++;
    }
}
