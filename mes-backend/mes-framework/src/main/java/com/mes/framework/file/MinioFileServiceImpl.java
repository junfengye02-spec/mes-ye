package com.mes.framework.file;

import com.mes.common.exception.BusinessException;
import com.mes.framework.tenant.TenantContextHolder;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * MinIO / S3 兼容对象存储实现
 * <p>
 * 文件名规则：{@code {tenantSegment}/{directory}/{yyyy/MM/dd}/{uuid}.{ext}}，与本地实现保持一致，
 * 方便从 Local 切换到 MinIO 时 uploads 目录可以直接 {@code mc mirror} 到桶里。
 * </p>
 * <p>
 * 上传返回的字符串采用 {@code minio://{bucket}/{objectKey}} 形式，方便数据库统一存储与跨实现迁移。
 * 通过 {@link #getUrl(String, int)} 可换取带签名的临时访问 URL（默认 1 小时）。
 * </p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "mes.file", name = "storage-type", havingValue = "minio")
@EnableConfigurationProperties(MinioProperties.class)
public class MinioFileServiceImpl implements FileService {

    /**
     * 目录合法性与 LocalFileServiceImpl 保持一致，防止路径穿越
     */
    private static final Pattern SAFE_DIRECTORY = Pattern.compile("^[A-Za-z0-9_\\-]+(?:/[A-Za-z0-9_\\-]+)*$");

    /**
     * 默认扩展名白名单：与 LocalFileServiceImpl 完全一致
     */
    private static final Set<String> DEFAULT_ALLOWED_EXTS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "txt", "csv", "log",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z", "tar", "gz"
    );

    /**
     * 上传返回的逻辑前缀
     */
    private static final String URL_SCHEME = "minio://";

    @Value("${mes.file.max-size-bytes:52428800}")
    private long maxSizeBytes;

    @Value("${mes.file.allowed-extensions:}")
    private String allowedExtensionsProp;

    private final MinioProperties properties;

    private MinioClient client;

    private Set<String> allowedExtensions = DEFAULT_ALLOWED_EXTS;

    public MinioFileServiceImpl(MinioProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            throw new IllegalStateException("mes.file.minio.endpoint 未配置");
        }
        if (properties.getAccessKey() == null || properties.getSecretKey() == null) {
            throw new IllegalStateException("mes.file.minio.access-key / secret-key 未配置");
        }

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey());
        if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
            builder.region(properties.getRegion());
        }
        this.client = builder.build();

        if (allowedExtensionsProp != null && !allowedExtensionsProp.isBlank()) {
            Set<String> custom = new HashSet<>();
            for (String ext : allowedExtensionsProp.split(",")) {
                String trimmed = ext.trim().toLowerCase(Locale.ROOT);
                if (trimmed.startsWith(".")) {
                    trimmed = trimmed.substring(1);
                }
                if (!trimmed.isEmpty()) {
                    custom.add(trimmed);
                }
            }
            if (!custom.isEmpty()) {
                this.allowedExtensions = Collections.unmodifiableSet(custom);
            }
        }

        if (properties.isAutoCreateBucket()) {
            ensureBucketExists();
        }
        log.info("MinIO 文件服务初始化完成, endpoint={}, bucket={}, maxSize={}B, allowedExts={}",
                properties.getEndpoint(), properties.getBucket(), maxSizeBytes, allowedExtensions);
    }

    private void ensureBucketExists() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
                log.info("MinIO bucket 自动创建成功: {}", properties.getBucket());
            }
        } catch (Exception e) {
            // 启动时创建失败不阻断应用启动，仅告警；生产通常预建好 bucket
            log.warn("MinIO bucket 检查/创建失败（将继续启动）: {}", e.getMessage());
        }
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (maxSizeBytes > 0 && file.getSize() > maxSizeBytes) {
            throw new BusinessException("上传文件大小超出限制: " + file.getSize() + " > " + maxSizeBytes);
        }

        String safeDirectory = sanitizeDirectory(directory);
        String ext = extractAndValidateExtension(file.getOriginalFilename());
        String tenantSegment = tenantPathSegment();
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectKey = String.join("/",
                tenantSegment,
                safeDirectory,
                datePath,
                UUID.randomUUID().toString().replace("-", "") + ext);

        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .build());
        } catch (Exception e) {
            log.error("MinIO 上传失败, object={}", objectKey, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        String logicalUrl = URL_SCHEME + properties.getBucket() + "/" + objectKey;
        log.info("MinIO 上传成功: {}", logicalUrl);
        return logicalUrl;
    }

    @Override
    public InputStream download(String fileUrl) {
        String objectKey = parseObjectKey(fileUrl);
        authorizeTenant(objectKey);
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO 下载失败: {}", fileUrl, e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public String getUrl(String fileUrl, int expireInSeconds) {
        String objectKey = parseObjectKey(fileUrl);
        authorizeTenant(objectKey);
        int expiry = expireInSeconds > 0 ? expireInSeconds : properties.getPresignedExpirySeconds();
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(expiry, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.warn("生成预签名 URL 失败: {}", fileUrl, e);
            throw new BusinessException("生成访问 URL 失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        String objectKey;
        try {
            objectKey = parseObjectKey(fileUrl);
        } catch (BusinessException ignore) {
            log.warn("忽略非法删除请求: {}", fileUrl);
            return;
        }
        authorizeTenant(objectKey);
        try {
            // 先校验存在再删除，避免误删后无法审计
            client.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
            log.info("MinIO 删除成功: {}", objectKey);
        } catch (ErrorResponseException e) {
            log.warn("MinIO 对象不存在或已删除: {}", objectKey);
        } catch (Exception e) {
            log.warn("MinIO 删除失败: {}", fileUrl, e);
        }
    }

    /**
     * 解析逻辑 URL 为 objectKey
     * 支持：
     * <ul>
     *   <li>完整逻辑 URL：{@code minio://mes/tenant-1/.../abc.png}</li>
     *   <li>纯 objectKey：{@code tenant-1/.../abc.png}</li>
     * </ul>
     */
    private String parseObjectKey(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BusinessException("文件路径不能为空");
        }
        String key;
        if (fileUrl.startsWith(URL_SCHEME)) {
            String rest = fileUrl.substring(URL_SCHEME.length());
            int slashIdx = rest.indexOf('/');
            if (slashIdx < 0) {
                throw new BusinessException("非法的文件路径: " + fileUrl);
            }
            String bucket = rest.substring(0, slashIdx);
            if (!bucket.equals(properties.getBucket())) {
                throw new BusinessException("文件不属于当前 bucket: " + bucket);
            }
            key = rest.substring(slashIdx + 1);
        } else {
            key = fileUrl;
        }
        if (key.isBlank() || key.contains("..") || key.contains("\\")) {
            throw new BusinessException("非法的文件路径: " + fileUrl);
        }
        return key;
    }

    /**
     * 租户授权校验：租户只能访问自己 tenant-{id}/ 前缀下的文件，平台租户不限
     */
    private void authorizeTenant(String objectKey) {
        Long tid = TenantContextHolder.getTenantId();
        if (tid == null || TenantContextHolder.PLATFORM_TENANT_ID.equals(tid)) {
            return;
        }
        String expected = "tenant-" + tid + "/";
        if (!objectKey.startsWith(expected)) {
            log.warn("拒绝跨租户文件访问: tenantId={}, objectKey={}", tid, objectKey);
            throw new BusinessException("无权访问该文件（跨租户）");
        }
    }

    private String tenantPathSegment() {
        Long tid = TenantContextHolder.getTenantId();
        if (tid == null) {
            return "platform";
        }
        return "tenant-" + tid;
    }

    private String sanitizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "common";
        }
        String normalized = directory.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty() || !SAFE_DIRECTORY.matcher(normalized).matches()) {
            throw new BusinessException("非法的上传子目录: " + directory);
        }
        if (Arrays.asList(normalized.split("/")).contains("..")) {
            throw new BusinessException("非法的上传子目录: " + directory);
        }
        return normalized;
    }

    private String extractAndValidateExtension(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("上传文件名不能为空");
        }
        String baseName = originalName.replace('\\', '/');
        int slash = baseName.lastIndexOf('/');
        if (slash >= 0) {
            baseName = baseName.substring(slash + 1);
        }
        if (baseName.isBlank() || baseName.contains("\u0000")) {
            throw new BusinessException("非法的上传文件名: " + originalName);
        }
        int dot = baseName.lastIndexOf('.');
        if (dot < 0 || dot == baseName.length() - 1) {
            throw new BusinessException("文件缺少扩展名: " + originalName);
        }
        String extNoDot = baseName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extNoDot)) {
            throw new BusinessException("不支持的文件类型: ." + extNoDot);
        }
        return "." + extNoDot;
    }
}
