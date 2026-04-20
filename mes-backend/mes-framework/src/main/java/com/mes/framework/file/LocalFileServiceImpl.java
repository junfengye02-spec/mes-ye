package com.mes.framework.file;

import com.mes.common.exception.BusinessException;
import com.mes.framework.tenant.TenantContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 本地文件存储实现
 */
@Slf4j
@Service
public class LocalFileServiceImpl implements FileService {

    /**
     * 目录名白名单规则：只允许字母数字、下划线、短横线、以及 / 分隔的多级子目录。
     * 任何 .. / 反斜杠 / 冒号 / 起始 / 结尾斜杠都会被视为非法。
     */
    private static final Pattern SAFE_DIRECTORY = Pattern.compile("^[A-Za-z0-9_\\-]+(?:/[A-Za-z0-9_\\-]+)*$");

    /**
     * 默认扩展名白名单：文档 / 图片 / 压缩包 / 常见办公附件。
     * 可通过 mes.file.allowed-extensions 覆盖，多个用逗号分隔；均使用小写。
     */
    private static final Set<String> DEFAULT_ALLOWED_EXTS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "txt", "csv", "log",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z", "tar", "gz"
    );

    @Value("${mes.file.upload-path:./uploads}")
    private String uploadPath;

    @Value("${mes.file.access-prefix:/files}")
    private String accessPrefix;

    @Value("${mes.file.max-size-bytes:52428800}")
    private long maxSizeBytes;

    @Value("${mes.file.allowed-extensions:}")
    private String allowedExtensionsProp;

    /**
     * 运行时用的实际白名单，启动时解析一次。
     */
    private Set<String> allowedExtensions = DEFAULT_ALLOWED_EXTS;

    /**
     * 规范化后的上传根路径（绝对路径），用于路径穿越 {@code startsWith} 校验。
     */
    private Path uploadRoot;

    @PostConstruct
    void initUploadRoot() {
        try {
            Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            this.uploadRoot = root;
        } catch (IOException e) {
            throw new IllegalStateException("初始化上传目录失败: " + uploadPath, e);
        }

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
        log.info("文件上传初始化完成, root={}, maxSize={}B, allowedExts={}",
                uploadRoot, maxSizeBytes, allowedExtensions);
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

        try {
            // 按 tenant + 业务目录 + 日期 分层：
            //   uploads/tenant-{id}/workorder/2026/02/06/xxxx.png
            // 租户上下文缺失（平台任务）时写入 uploads/platform/... ，便于审计定位。
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path targetDir = uploadRoot.resolve(tenantSegment).resolve(safeDirectory).resolve(datePath).normalize();

            ensureWithinRoot(targetDir);
            Files.createDirectories(targetDir);

            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path targetFile = targetDir.resolve(fileName).normalize();
            ensureWithinRoot(targetFile);

            file.transferTo(targetFile.toFile());

            String relativePath = tenantSegment + "/" + safeDirectory + "/" + datePath + "/" + fileName;
            log.info("文件上传成功: {}", relativePath);
            return accessPrefix + "/" + relativePath;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            String prefix = accessPrefix.endsWith("/") ? accessPrefix : accessPrefix + "/";
            if (!fileUrl.startsWith(prefix)) {
                log.warn("忽略非法文件删除请求（前缀不匹配）: {}", fileUrl);
                return;
            }
            String relativePath = fileUrl.substring(prefix.length());
            if (relativePath.isBlank() || relativePath.contains("..") || relativePath.contains("\\")) {
                log.warn("忽略非法文件删除请求（路径含非法字符）: {}", fileUrl);
                return;
            }
            // 租户归属校验：非平台上下文 → 文件路径必须以 tenant-{currentTenantId}/ 开头
            if (!isTenantPathAuthorized(relativePath)) {
                log.warn("拒绝跨租户删除文件请求: tenantId={}, path={}",
                        TenantContextHolder.getTenantId(), relativePath);
                throw new BusinessException("无权删除该文件（跨租户）");
            }
            Path filePath = uploadRoot.resolve(relativePath).normalize();
            ensureWithinRoot(filePath);
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", relativePath);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", fileUrl, e);
        }
    }

    private boolean isTenantPathAuthorized(String relativePath) {
        Long tid = TenantContextHolder.getTenantId();
        if (tid == null || TenantContextHolder.PLATFORM_TENANT_ID.equals(tid)) {
            // 平台超管允许删任何文件
            return true;
        }
        String expectedPrefix = "tenant-" + tid + "/";
        return relativePath.startsWith(expectedPrefix);
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
        // 仅保留最后一个分隔符后的基础名，阻断 ../a.txt 这类构造
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

    private void ensureWithinRoot(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(uploadRoot)) {
            throw new BusinessException("非法的上传路径: " + candidate);
        }
    }
}
