package com.example.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ValidateCodeCache {

    private static final Logger log = LoggerFactory.getLogger(ValidateCodeCache.class);

    private static List<CodeCache> codeCache = new ArrayList<>();

    /**
     * 设置验证码到缓存
     */
    public static void setCache(String key, String code) {
        CodeCache cache = new CodeCache();
        cache.setKey(key);
        cache.setCode(code);
        cache.setTimestamp(System.currentTimeMillis());
        codeCache.add(cache);
        log.info("当前的验证码缓存: {}", codeCache);
    }

    /**
     * 验证
     */
    public static boolean validateCode(String key, String code) {
        return codeCache.stream().anyMatch(cache -> cache.getKey().equals(key) && cache.getCode().equalsIgnoreCase(code));
    }

}