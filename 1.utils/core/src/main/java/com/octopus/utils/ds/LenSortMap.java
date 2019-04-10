package com.octopus.utils.ds;

import java.text.Collator;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * User: wfgao_000
 * Date: 15-4-16
 * Time: 下午4:30
 */
public class LenSortMap extends TreeMap {
        public static int SORT_DESC = 1;
        public static int SORT_ABS = 0;

        @SuppressWarnings("unchecked")
        public LenSortMap(final int type){
            super(new Comparator<Object>(){
                Collator collator = Collator.getInstance();
                public int compare(Object o1,Object o2){
                    int len1 = o1.toString().length();
                    int len2 = o2.toString().length();
                    if(o1.equals(o2)){
                        return 0;
                    }else{
                        if(type==SORT_DESC){
                            return (len1>=len2?-1:(1));
                        }else{
                            return (len1>=len2?1:(-1));
                        }
                    }

                }
            });
        }

}
