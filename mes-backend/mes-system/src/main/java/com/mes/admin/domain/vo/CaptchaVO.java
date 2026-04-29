package com.mes.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图形验证码返回体（P1-14）
 */
@Data
@Schema(description = "图形验证码")
public class CaptchaVO {

    @Schema(description = "验证码 key（登录时随 captchaCode 一起回传）")
    private String captchaKey;

    @Schema(description = "Base64 编码的 PNG 图片（含 data:image/png;base64, 前缀）")
    private String imageBase64;

    @Schema(description = "验证码有效秒数", example = "300")
    private long expireSeconds;
}
