/*
 * Copyright 2012-2022 Steven C. Saliman
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.  See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.saliman.gradle.plugin.properties;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import sun.misc.Unsafe;

/**
 * This class contains some very ugly utilities for setting and clearing environment variables for
 * unit tests.  This sort of thing should never see the light of day in a production application,
 * but we need to be able to set different types of environment variables to make sure the
 * properties plugin does the right thing.
 * <p>
 * This code is probably incredibly flaky, and it only works if it is a Java file.  It breaks if it
 * is run as a Groovy file.
 */
public class SetEnv {
    private static Unsafe UNSAFE;

    // Get the "Unsafe" singleton from deep within the bowels of Java internal classes.
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the writeable map that actually holds the environment variables so the other methods in
     * this class can set or unset them.
     *
     * @return a map of environment variables
     * @throws NoSuchFieldException if the internal field that has the map can't be loaded.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getWritableEnvMap() throws NoSuchFieldException {
        Map<String, String> unwritable = System.getenv();
        Object o = UNSAFE.getObject(unwritable, UNSAFE.objectFieldOffset(unwritable.getClass().getDeclaredField("m")));
        return (Map<String, String>)o;
    }

    /**
     * Set environment variables to include those in the given map.
     *
     * @param newenv the map of new variables to include.
     */
    public static void setEnv(Map<String, String> newenv) {
        try {
            Map<String, String> writable = getWritableEnvMap();
            writable.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for ( Class cl : classes ) {
                    if ( "java.util.Collections$UnmodifiableMap".equals(cl.getName()) ) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }


    /**
     * Remove the specified variables from the current map of environment variables.
     *
     * @param vars the names of the variables to remove.
     */
    public static void unsetEnv(List<String> vars) {
        try {
            Map<String, String> writable = getWritableEnvMap();
            for ( String v: vars ) {
                writable.remove(v);
            }

        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for ( Class cl : classes ) {
                    if ( "java.util.Collections$UnmodifiableMap".equals(cl.getName()) ) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        for ( String v : vars ) {
                            map.remove(v);
                        }
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
