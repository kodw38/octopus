package com.octopus.utils.ds;

import java.text.Collator;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by robai on 2017/9/11.
 */
public class LongKeySortMap extends TreeMap {
    public static int SORT_DESC = 1;
    public static int SORT_ABS = 0;
    public LongKeySortMap(final String type){
        super(new Comparator<Long>(){
            Collator collator = Collator.getInstance();
            public int compare(Long o1,Long o2){
                if(o1.equals(o2)){
                    return 0;
                }else{
                    if("DESC".equalsIgnoreCase(type)){
                        return (o1>=o2?-1:(1));
                    }else{
                        return (o1>=o2?1:(-1));
                    }
                }

            }
        });
    }
    public LongKeySortMap(final Integer type){
        super(new Comparator<Long>(){
            Collator collator = Collator.getInstance();
            public int compare(Long o1,Long o2){
                if(o1.equals(o2)){
                    return 0;
                }else{
                    if(type==SORT_DESC){
                        return (o1>=o2?-1:(1));
                    }else{
                        return (o1>=o2?1:(-1));
                    }
                }

            }
        });
    }

}
