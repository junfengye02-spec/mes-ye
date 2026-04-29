package com.mes.framework.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务接口
 * <p>
 * 抽象屏蔽底层存储差异，支持的实现：
 * <ul>
 *   <li>{@link LocalFileServiceImpl} —— 本地磁盘（默认，开发/小规模场景）</li>
 *   <li>{@link MinioFileServiceImpl} —— MinIO / S3 兼容对象存储（生产推荐）</li>
 * </ul>
 * 通过配置项 {@code mes.file.storage-type=local|minio} 切换实现（默认 local）。
 * </p>
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file      上传的文件
     * @param directory 子目录（如 "workorder", "abnormal"）
     * @return 文件访问路径（本地：/files/xxx；MinIO：预签名 URL 或逻辑路径 minio://bucket/key）
     */
    String upload(MultipartFile file, String directory);

    /**
     * 删除文件
     *
     * @param fileUrl 文件路径（upload 返回的字符串）
     */
    void delete(String fileUrl);

    /**
     * 下载文件
     * <p>默认实现抛出 UnsupportedOperationException，具体实现类按需覆盖。</p>
     *
     * @param fileUrl 文件路径
     * @return 文件流（调用方负责关闭）
     */
    default InputStream download(String fileUrl) {
        throw new UnsupportedOperationException("当前存储实现不支持 download 操作");
    }

    /**
     * 获取可访问 URL（对 MinIO 来说是带签名的临时 URL；对本地来说就是原 accessPrefix 路径）
     *
     * @param fileUrl         文件路径
     * @param expireInSeconds 过期时间（秒），&le;0 时使用实现类默认
     * @return 可访问的 URL
     */
    default String getUrl(String fileUrl, int expireInSeconds) {
        return fileUrl;
    }
}
