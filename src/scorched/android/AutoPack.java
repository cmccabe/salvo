package scorched.android;

import java.lang.reflect.Field;

import android.os.Bundle;

/**
 * Handles automatic serialization / deserialization.
 */
public abstract class AutoPack
{
    /*================= Utility =================*/
    private static String fieldNameToKey(String prefix, String fieldName) {
        StringBuilder b = new StringBuilder(80);
        b.append("KEY_").append(prefix).append(fieldName);
        return b.toString();
    }

    /*================= Operations =================*/
    /** Add all elements of obj to bundle b.
     *
     * Their names will be just as they were in the class.
     */
    @SuppressWarnings("unchecked")
    public static void autoPack(Bundle map, String prefix, Object obj) {
        Class oc = obj.getClass();
        for (Field f : oc.getDeclaredFields()) {
            try {
                String name = fieldNameToKey(prefix, f.getName());
                Class fc = f.getType();
                if (fc.isEnum()) {
                    Enum myEnum = (Enum)f.get(obj);
                    map.putInt(name, myEnum.ordinal());
                }
                else if (fc == oc) {
                    // Ignore the "this" field I wish I could be absolutely
                    // certain that this was the class's "this" field.
                    // However, it could also be a recursive field. We want
                    // "obj" to contain only primitive members, so don't do
                    // that!
                }
                else if (fc == boolean.class)
                    map.putBoolean(name, f.getBoolean(obj));
                else if (fc == short.class)
                    map.putShort(name, f.getShort(obj));
                else if (fc == int.class)
                    map.putInt(name, f.getInt(obj));
                else if (fc == long.class)
                    map.putLong(name, f.getLong(obj));
                else if (fc == float.class)
                    map.putFloat(name, f.getFloat(obj));
                else if (fc == double.class)
                    map.putDouble(name, f.getDouble(obj));
                else if (fc == String.class)
                    map.putString(name, (String)f.get(obj));
                else {
                    throw new RuntimeException("autoPack doesn't know how " +
                        "to pack fields of type " + fc);
                }
            }
            catch (java.lang.IllegalAccessException e) {
                // This code is unreachable, as you can easily see.
                // However, the compiler's reasoning powers are limited,
                // so we have to go through the formalities here.
                throw new RuntimeException(e.toString());
            }
        }
    }

    /** Create a new object of class oc from the elements in bundle "map"
     *
     * Assumes that the elements were inserted with autoPack()
     */
    @SuppressWarnings("unchecked")
    public static Object autoUnpack(Bundle map, String prefix, Class oc) {
        Object ret;
        try {
            ret = oc.newInstance();
        }
        catch (Exception e) {
            // You should provide a default constructor for all classes that
            // you use with AutoPack.
            StringBuilder b = new StringBuilder(200);
            b.append("error calling newInstance:").append(e.toString());
            b.append(" ");
            b.append("Please check that you have provided a default constructor ");
            b.append("for the class you are using with AutoPack.");
            throw new RuntimeException(b.toString());
        }
        for (Field f : oc.getDeclaredFields()) {
            try {
                String name = fieldNameToKey(prefix, f.getName());
                Class fc = f.getType();
                if (fc.isEnum()) {
                    Object vals[] = fc.getEnumConstants();
                    f.set(ret, vals[map.getInt(name)]);
                }
                else if (fc == oc) {
                    // Ignore the "this" field. As described in autoPack().
                }
                else if (fc == boolean.class)
                    f.setBoolean(ret, map.getBoolean(name));
                else if (fc == short.class)
                    f.setShort(ret, map.getShort(name));
                else if (fc == int.class)
                    f.setInt(ret, map.getInt(name));
                else if (fc == long.class)
                    f.setLong(ret, map.getLong(name));
                else if (fc == float.class)
                    f.setFloat(ret, map.getFloat(name));
                else if (fc == double.class)
                    f.setDouble(ret, map.getDouble(name));
                else if (fc == String.class)
                    f.set(ret, map.getString(name));
                else {
                    throw new RuntimeException("autoUnpack doesn't know " +
                        "how to unpack fields of type " + fc);
                }
            }
            catch (java.lang.IllegalAccessException e) {
                // As in pack()
                throw new RuntimeException(e.toString());
            }
        }
        return ret;
    }
}
