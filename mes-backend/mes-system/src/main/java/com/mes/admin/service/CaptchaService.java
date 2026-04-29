package com.mes.admin.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.codec.Base64;
import com.mes.admin.domain.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 图形验证码生成与校验服务（P1-14）
 *
 * <h3>Redis 键模型</h3>
 * <ul>
 *   <li>{@code auth:captcha:answer:{captchaKey}} — 验证码答案（小写），TTL 5 分钟</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    public static final String KEY_ANSWER_PREFIX = "auth:captcha:answer:";

    /** 验证码 TTL */
    public static final Duration TTL = Duration.ofMinutes(5);

    /** 图形尺寸：宽 */
    private static final int WIDTH = 130;
    /** 图形尺寸：高 */
    private static final int HEIGHT = 48;
    /** 字符个数 */
    private static final int CHAR_COUNT = 4;
    /** 干扰线条数 */
    private static final int LINE_COUNT = 8;

    private final StringRedisTemplate redisTemplate;

    /**
     * 生成验证码，返回 base64 图片与 key
     */
    public CaptchaVO generate() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, CHAR_COUNT, LINE_COUNT);
        String answer = captcha.getCode().toLowerCase();
        String key = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(KEY_ANSWER_PREFIX + key, answer, TTL);
        } catch (Exception e) {
            log.warn("[Captcha] 答案写入 Redis 失败: {}", e.getMessage());
        }
        String base64 = "data:image/png;base64," + Base64.encode(captcha.getImageBytes());
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaKey(key);
        vo.setImageBase64(base64);
        vo.setExpireSeconds(TTL.toSeconds());
        return vo;
    }

    /**
     * 校验验证码，校验成功后 answer 立即删除（one-shot，防重放）
     *
     * @return true 校验通过；false 不通过或已过期
     */
    public boolean verify(String captchaKey, String userInput) {
        if (captchaKey == null || captchaKey.isBlank() || userInput == null || userInput.isBlank()) {
            return false;
        }
        String redisKey = KEY_ANSWER_PREFIX + captchaKey;
        try {
            String answer = redisTemplate.opsForValue().get(redisKey);
            if (answer == null) return false;
            boolean match = answer.equalsIgnoreCase(userInput.trim());
            redisTemplate.delete(redisKey);
            return match;
        } catch (Exception e) {
            log.warn("[Captcha] 校验时 Redis 异常: {}", e.getMessage());
            return false;
        }
    }
}
