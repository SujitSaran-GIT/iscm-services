package com.iscm.iam.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;

@Component
public class CacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringJoiner keyBuilder = new StringJoiner(":");

        // Add class name
        keyBuilder.add(target.getClass().getSimpleName());

        // Add method name
        keyBuilder.add(method.getName());

        // Add parameters
        for (Object param : params) {
            if (param != null) {
                if (param.getClass().isArray()) {
                    // Handle array parameters
                    Object[] array = (Object[]) param;
                    StringJoiner arrayJoiner = new StringJoiner(",");
                    for (Object arrayItem : array) {
                        arrayJoiner.add(arrayItem != null ? arrayItem.toString() : "null");
                    }
                    keyBuilder.add("[" + arrayJoiner + "]");
                } else {
                    keyBuilder.add(param.toString());
                }
            } else {
                keyBuilder.add("null");
            }
        }

        return keyBuilder.toString();
    }
}