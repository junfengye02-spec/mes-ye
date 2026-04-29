package com.mes.framework.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO / S3 对象存储配置
 * <p>
 * 仅当 {@code mes.file.storage-type=minio} 时生效。
 * 配置前缀：{@code mes.file.minio.*}。
 * </p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mes.file.minio")
public class MinioProperties {

    /**
     * MinIO / S3 服务端点，例如：
     * <ul>
     *   <li>自建 MinIO：{@code http://minio:9000}</li>
     *   <li>阿里云 OSS（S3 兼容）：{@code https://oss-cn-hangzhou.aliyuncs.com}</li>
     *   <li>AWS S3：{@code https://s3.amazonaws.com}</li>
     * </ul>
     */
    private String endpoint;

    /**
     * 访问 Key（生产必须通过环境变量注入，不要硬编码）
     */
    private String accessKey;

    /**
     * 访问 Secret（生产必须通过环境变量注入）
     */
    private String secretKey;

    /**
     * 默认桶名（不存在时启动自动创建）
     */
    private String bucket = "mes";

    /**
     * 区域，S3 兼容场景一般可不填；OSS 推荐填入
     */
    private String region;

    /**
     * 预签名 URL 默认过期时间（秒），默认 1 小时
     */
    private int presignedExpirySeconds = 3600;

    /**
     * 是否在启动时尝试创建 bucket（默认 true，生产建议在部署脚本中预建 bucket 并关闭本开关）
     */
    private boolean autoCreateBucket = true;
}
