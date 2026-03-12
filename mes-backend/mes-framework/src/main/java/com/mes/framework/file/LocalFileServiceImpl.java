package com.mes.framework.file;

import com.mes.common.exception.BusinessException;
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
import java.util.UUID;

/**
 * 本地文件存储实现
 */
@Slf4j
@Service
public class LocalFileServiceImpl implements FileService {

    @Value("${mes.file.upload-path:./uploads}")
    private String uploadPath;

    @Value("${mes.file.access-prefix:/files}")
    private String accessPrefix;

    @Override
    public String upload(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        try {
            // 按日期分目录：uploads/workorder/2026/02/06/
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path targetDir = Paths.get(uploadPath, directory, datePath);
            Files.createDirectories(targetDir);

            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : "";
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

            // 保存文件
            Path targetFile = targetDir.resolve(fileName);
            file.transferTo(targetFile.toFile());

            // 返回访问路径
            String relativePath = directory + "/" + datePath + "/" + fileName;
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
            String relativePath = fileUrl.replace(accessPrefix + "/", "");
            Path filePath = Paths.get(uploadPath, relativePath);
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", relativePath);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", fileUrl, e);
        }
    }
}
