
package io.github.jootoi.veikkaus.api.gui.StaticTools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Joonas Toimela
 */

public class SearchCollection {
    public static Object search(Collection c,String key, Object value) throws IllegalAccessException {
        for(Object o:c) {
            if(o==null){continue;}
            Class cls = o.getClass();
            Field[] fields = cls.getDeclaredFields();
            for(Field f:fields) {
                f.setAccessible(true);
                if(f.getName().equalsIgnoreCase(key)) {
                    if(f.get(o).equals(value)) {
                        return o;
                    }
                }
            }  
        }
        return null;
    }
    
    public static ArrayList searchAll(Collection c,String key, Object value) throws IllegalAccessException {
        ArrayList results = new ArrayList();
        for(Object o:c) {
            if(o==null){continue;}
            Class cls = o.getClass();
            Field[] fields = cls.getDeclaredFields();
            for(Field f:fields) {
            f.setAccessible(true);
                if(f.getName().equalsIgnoreCase(key)) {
                    if(f.get(o).equals(value)) {
                        results.add(o);
                    }
                }
            }  
        }
        return results;
    }
}
