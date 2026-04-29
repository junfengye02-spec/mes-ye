package com.mes.framework.web;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.mes.common.result.R;
import com.mes.framework.file.FileService;
import com.mes.framework.sentinel.MesRateLimit;
import com.mes.framework.sentinel.SentinelBlockHandlers;
import com.mes.framework.sentinel.SentinelResources;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传 Controller
 */
@Tag(name = "文件管理", description = "文件上传/删除接口")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件", description = "每租户 5 QPS 限流，防止小文件洪水")
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('system:file:upload')")
    @SentinelResource(value = SentinelResources.FILE_UPLOAD,
            blockHandler = "handleR", blockHandlerClass = SentinelBlockHandlers.class)
    @MesRateLimit(resource = SentinelResources.FILE_UPLOAD, key = MesRateLimit.Key.TENANT, count = 5)
    public R<Map<String, String>> upload(
            @Parameter(description = "上传文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "子目录") @RequestParam(value = "directory", defaultValue = "common") String directory) {
        String fileUrl = fileService.upload(file, directory);
        return R.ok(Map.of(
                "fileUrl", fileUrl,
                "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
                "fileSize", String.valueOf(file.getSize())
        ));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping
    @PreAuthorize("hasAuthority('system:file:delete')")
    public R<Void> delete(@Parameter(description = "文件URL") @RequestParam("fileUrl") String fileUrl) {
        fileService.delete(fileUrl);
        return R.ok();
    }
}
