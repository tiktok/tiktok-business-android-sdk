/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/
package com.tiktok.appevents;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TTSafeReadObjectUtil {
    /**
     * Safely read the object and do some security checks
     */

    public static TTAppEventPersist safeReadTTAppEventPersist(InputStream in) throws IOException, ClassNotFoundException {
        List<Class<?>> safeClasses = new ArrayList<>();
        safeClasses.add(TTAppEventPersist.class);
        safeClasses.add(ArrayList.class);
        safeClasses.add(TTAppEvent.class);
        safeClasses.add(Enum.class);
        safeClasses.add(String.class);
        safeClasses.add(Date.class);
        safeClasses.add(Long.class);
        safeClasses.add(TTUserInfo.class);
        safeClasses.add(TTAppEvent.TTAppEventType.class);
        return safeReadObjects(safeClasses, Long.MAX_VALUE, Long.MAX_VALUE, in);
    }

    public static TTCrashHandler.TTCrashReport safeReadTTCrashHandler(InputStream in) throws IOException, ClassNotFoundException {
        List<Class<?>> safeClasses = new ArrayList<>();
        safeClasses.add(TTCrashHandler.TTCrashReport.class);
        safeClasses.add(TTCrashHandler.TTCrashReport.Monitor.class);
        safeClasses.add(String.class);
        safeClasses.add(Long.class);
        safeClasses.add(Integer.class);
        safeClasses.add(ArrayList.class);
        return safeReadObjects(safeClasses, Long.MAX_VALUE, Long.MAX_VALUE, in);
    }

    public static <T> T safeReadObjects(List<Class<?>> safeClasses, long maxObjects, long maxBytes, InputStream in) throws IOException, ClassNotFoundException {
        InputStream lis = new FilterInputStream(in) {
            private long length = 0;

            public int read() throws IOException {
                int count = super.read();
                if (count != -1) {
                    length++;
                    checkLength();
                }
                return count;
            }

            public int read(byte[] b, int off, int readLength) throws IOException {
                int count = super.read(b, off, readLength);
                if (count > 0) {
                    length += count;
                    checkLength();
                }
                return count;
            }

            private void checkLength() {
                if (length > maxBytes) {
                    throw new SecurityException("too many bytes from stream. Limit is " + maxBytes);
                }
            }
        };
        ObjectInputStream ois = new ObjectInputStream(lis) {
            private int objCount = 0;
            boolean enableResolve = enableResolveObject(true);

            protected Object resolveObject(Object obj) throws IOException {
                if (objCount++ > maxObjects)
                    throw new SecurityException("too many objects from stream. Limit is " + maxObjects);
                Object object = super.resolveObject(obj);
                return object;
            }

            protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException {
                Class<?> clazz = super.resolveClass(osc);
                if (clazz.isArray() || clazz.equals(String.class) || Number.class.isAssignableFrom(clazz) || safeClasses.contains(clazz))
                    return clazz;
                throw new SecurityException("deserialize unauthorized " + clazz);
            }
        };
        T t = (T) ois.readObject();
        try{
            in.close();
            lis.close();
            ois.close();
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
        return t;
    }
}
