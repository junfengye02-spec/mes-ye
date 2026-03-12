package com.mes.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通用业务编号生成器
 * <p>生成格式：{前缀}-{yyyyMMdd}-{4位序号}</p>
 * <p>例如：YC-20260206-0001, WO-20260206-0001</p>
 *
 * <p>注意：此实现基于内存计数，重启后序号会重置。
 * 在单体架构下配合数据库唯一索引使用足够安全。</p>
 */
public final class NumberGenerator {

    private NumberGenerator() {}

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * key = prefix + date, value = 当日计数器
     */
    private static final ConcurrentHashMap<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    /**
     * 生成业务编号
     *
     * @param prefix 前缀，如 "YC", "WO", "LL", "RK"
     * @return 格式化编号，如 YC-20260206-0001
     */
    public static String generate(String prefix) {
        String dateStr = LocalDate.now().format(DATE_FMT);
        String key = prefix + "-" + dateStr;
        AtomicInteger counter = COUNTERS.computeIfAbsent(key, k -> new AtomicInteger(0));
        int seq = counter.incrementAndGet();
        return key + "-" + String.format("%04d", seq);
    }

    /**
     * 基于数据库已有最大序号重置计数器（应用启动时可调用）
     *
     * @param prefix  前缀
     * @param lastSeq 数据库中当日最大序号
     */
    public static void resetCounter(String prefix, int lastSeq) {
        String dateStr = LocalDate.now().format(DATE_FMT);
        String key = prefix + "-" + dateStr;
        COUNTERS.put(key, new AtomicInteger(lastSeq));
    }
}
