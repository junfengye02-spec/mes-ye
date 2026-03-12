package com.mes.framework.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * <p>当前实现：本地文件系统，后续可切换为 MinIO/OSS</p>
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param directory 子目录（如 "workorder", "abnormal"）
     * @return 文件访问路径
     */
    String upload(MultipartFile file, String directory);

    /**
     * 删除文件
     *
     * @param fileUrl 文件路径
     */
    void delete(String fileUrl);
}
