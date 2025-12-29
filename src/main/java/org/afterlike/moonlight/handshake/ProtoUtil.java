package org.afterlike.moonlight.handshake;

import com.google.gson.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public final class ProtoUtil {

    private ProtoUtil() {
    }

    public static JsonObject toJson(Object message) {
        JsonObject out = new JsonObject();

        try {
            Method getAllFields = message.getClass().getMethod("getAllFields");
            Map<?, ?> fields = (Map<?, ?>) getAllFields.invoke(message);

            for (Map.Entry<?, ?> entry : fields.entrySet()) {
                Object field = entry.getKey();
                Object value = entry.getValue();

                String name = invokeString(field, "getName");

                out.add(name, convertValue(value));
            }

        } catch (Exception e) {
            out.addProperty("error", e.toString());
        }

        return out;
    }

    private static JsonElement convertValue(Object value) throws Exception {
        if (value == null) return JsonNull.INSTANCE;

        if (value instanceof List) {
            JsonArray arr = new JsonArray();
            for (Object o : (List<?>) value) {
                arr.add(convertValue(o));
            }
            return arr;
        }

        if (value instanceof Map) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                obj.add(
                        String.valueOf(e.getKey()),
                        convertValue(e.getValue())
                );
            }
            return obj;
        }

        Class<?> cls = value.getClass();

        if (cls.isEnum()) {
            return new JsonPrimitive(((Enum<?>) value).name());
        }

        if (hasMethod(cls, "getAllFields")) {
            return toJson(value);
        }

        if (hasMethod(cls, "toStringUtf8")) {
            return new JsonPrimitive(
                    (String) cls.getMethod("toStringUtf8").invoke(value)
            );
        }

        if (value instanceof Number) return new JsonPrimitive((Number) value);
        if (value instanceof Boolean) return new JsonPrimitive((Boolean) value);
        if (value instanceof String) return new JsonPrimitive((String) value);

        return new JsonPrimitive(value.toString());
    }

    private static boolean hasMethod(Class<?> cls, String name) {
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(name)) return true;
        }
        return false;
    }

    private static String invokeString(Object target, String method)
            throws Exception {
        return (String) target.getClass().getMethod(method).invoke(target);
    }
}
