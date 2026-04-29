package com.mes.common.utils;

import java.security.SecureRandom;

/**
 * 随机强密码生成工具（P0-07 安全整改）
 *
 * <p>用于替代之前硬编码的 "123456" 默认密码：
 * <ul>
 *   <li>管理员新建用户时若未指定密码，由此工具生成随机强密码</li>
 *   <li>管理员重置密码时若未指定新密码，由此工具生成随机强密码并一次性返回给管理员</li>
 * </ul>
 * </p>
 *
 * <p>生成规则（长度 12 位，强度满足常见合规要求）：
 * <ul>
 *   <li>至少 1 个大写字母</li>
 *   <li>至少 1 个小写字母</li>
 *   <li>至少 1 个数字</li>
 *   <li>至少 1 个特殊字符</li>
 *   <li>使用 SecureRandom 保证随机性</li>
 * </ul>
 * </p>
 *
 * @author mcp7
 */
public final class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String SPECIAL = "@#$%&*!";
    private static final String ALL = UPPER + LOWER + DIGIT + SPECIAL;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_LENGTH = 12;

    private PasswordGenerator() {
    }

    /**
     * 生成默认长度（12 位）的随机强密码。
     *
     * @return 随机密码明文
     */
    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    /**
     * 生成指定长度的随机强密码。
     *
     * @param length 密码长度，至少 8 位
     * @return 随机密码明文
     * @throws IllegalArgumentException 长度小于 8 抛出
     */
    public static String generate(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("密码长度不能小于 8 位");
        }
        // 先保证 4 类字符各有一个
        char[] chars = new char[length];
        chars[0] = UPPER.charAt(RANDOM.nextInt(UPPER.length()));
        chars[1] = LOWER.charAt(RANDOM.nextInt(LOWER.length()));
        chars[2] = DIGIT.charAt(RANDOM.nextInt(DIGIT.length()));
        chars[3] = SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length()));
        // 剩余位用全集填充
        for (int i = 4; i < length; i++) {
            chars[i] = ALL.charAt(RANDOM.nextInt(ALL.length()));
        }
        // Fisher-Yates 洗牌打乱前 4 位的可猜性
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
