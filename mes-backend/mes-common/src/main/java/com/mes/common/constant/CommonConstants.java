package com.mes.common.constant;

/**
 * 公共常量
 */
public final class CommonConstants {

    private CommonConstants() {}

    /** 逻辑删除：正常 */
    public static final int NOT_DELETED = 0;
    /** 逻辑删除：已删除 */
    public static final int DELETED = 1;

    /** 启用 */
    public static final int ENABLED = 1;
    /** 停用 */
    public static final int DISABLED = 0;

    /** 默认页码 */
    public static final int DEFAULT_PAGE_NUM = 1;
    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 导入单次最大行数 */
    public static final int IMPORT_MAX_ROWS = 5000;
}
