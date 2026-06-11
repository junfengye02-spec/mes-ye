package com.mes.process.service.support;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用工艺参数检索条件
 */
@Data
@Builder
public class ProcessParameterSearch {

    private String codeKeyword;
    private String nameKeyword;
    private String status;

    @Builder.Default
    private List<String> searchKeywords = new ArrayList<>();
}
