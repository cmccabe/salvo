package scorched.android;

import java.lang.reflect.Field;

import android.os.Bundle;

/**
 * Automatically packs class data into Android Bundles (also unpacks).
 *
 * Most classes have a set of core data that they want to preserve across
 * changes in screen orientation, etc.
 * Android requires you to write this data to a Bundle in
 * onSaveInstanceState(), and be ready to re-create the object from this
 * bundle in onCreate().
 *
 * It is painful and error-prone to write these serialization /
 * deserialization functions by hand.
 * AutoPack provides a better way.
 *
 * Simply move your class's "essential" data into a static inner class. All
 * the members of this class should be public, and it should contain only
 * "simple" datatypes that can be handled natively by the Bundle methods.
 * Then, you can call autoPack() in onSaveInstanceState, and autoUnpack in
 * onCreate(), and rest assured that your data will be properly recreated.
 *
 * One small additional detail: if you are saving multiple instances of a
 * class (for example, if you have 5 Players who all want to save their
 * data), you must provide a "prefix" string argument to separate out each
 * instance.
 */
public abstract class AutoPack
{
    /*================= Constant =================*/
    public static final String EMPTY_STRING = "";

    /*================= Utility =================*/
    public static String fieldNameToKey(String prefix, String fieldName) {
        StringBuilder b = new StringBuilder(80);
        b.append("KEY_").append(prefix).append(fieldName);
        return b.toString();
    }

    /*================= Operations =================*/
    /** Add all elements of obj to bundle "map".
     *
     * Their names will be just as they were in the class, except that
     * "prefix" will be prepended.
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
                else if (fc.isArray()) {
                    Class ec = fc.getComponentType();
                    if (ec == boolean.class)
                        map.putBooleanArray(name, (boolean[])f.get(obj));
                    else if (ec == short.class)
                        map.putShortArray(name, (short[])f.get(obj));
                    else if (ec == int.class)
                        map.putIntArray(name, (int[])f.get(obj));
                    else if (ec == long.class)
                        map.putLongArray(name, (long[])f.get(obj));
                    else if (ec == float.class)
                        map.putFloatArray(name, (float[])f.get(obj));
                    else if (ec == double.class)
                        map.putDoubleArray(name, (double[])f.get(obj));
                    else if (ec == String.class)
                        map.putStringArray(name, (String[])f.get(obj));
                    else {
                        throw new RuntimeException("autoPack doesn't know " +
                            "how to pack fields of type array of " + ec);
                    }
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
                // This code should be unreachable.
                // If you ever reach it, make sure that all of your class'
                // members are public.
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
            b.append("Please check that you have provided a default ");
            b.append("constructor for the class you are ");
            b.append("using with AutoPack.");
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
                else if (fc.isArray()) {
                    Class ec = fc.getComponentType();
                    if (ec == boolean.class)
                        f.set(ret, map.getBooleanArray(name));
                    else if (ec == short.class)
                        f.set(ret, map.getShortArray(name));
                    else if (ec == int.class)
                        f.set(ret, map.getIntArray(name));
                    else if (ec == long.class)
                        f.set(ret, map.getLongArray(name));
                    else if (ec == float.class)
                        f.set(ret, map.getFloatArray(name));
                    else if (ec == double.class)
                        f.set(ret, map.getDoubleArray(name));
                    else if (ec == String.class)
                        f.set(ret, map.getStringArray(name));
                    else {
                        throw new RuntimeException("autoUnpack doesn't " +
                            "know how to unpack fields of type " +
                            "array of " + ec);
                    }
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
                // This code should be unreachable.
                // If you ever reach it, make sure that all of your class's
                // members are public.
                throw new RuntimeException(e.toString());
            }
        }
        return ret;
    }
}
