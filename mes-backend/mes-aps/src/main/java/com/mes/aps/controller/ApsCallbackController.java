package com.mes.aps.controller;

import com.mes.aps.domain.vo.ApsScheduleCallbackVO;
import com.mes.aps.service.IApsCallbackService;
import com.mes.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "APS回调接收", description = "接收APS排程结果回调")
@RestController
@RequestMapping("/aps/callback")
@RequiredArgsConstructor
public class ApsCallbackController {

    private final IApsCallbackService callbackService;

    @Operation(summary = "接收排程结果")
    @PostMapping("/schedule-result")
    public R<Void> receiveScheduleResult(@RequestBody ApsScheduleCallbackVO callback) {
        callbackService.handleScheduleResult(callback);
        return R.ok();
    }

    @Operation(summary = "接收请求拒绝")
    @PostMapping("/request-rejected")
    public R<Void> receiveRejection(@RequestBody ApsScheduleCallbackVO callback) {
        callbackService.handleRequestRejected(callback);
        return R.ok();
    }
}
