package com.mes.framework.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.mes.framework.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MyBatis Plus 配置
 * <p>分页插件 + 自动填充审计字段</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            private String getCurrentUsername() {
                return Optional.ofNullable(SecurityUtils.getCurrentUsername()).orElse("system");
            }

            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "createdBy", this::getCurrentUsername, String.class);
                this.strictInsertFill(metaObject, "updatedBy", this::getCurrentUsername, String.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime::now, LocalDateTime.class);
                this.strictUpdateFill(metaObject, "updatedBy", this::getCurrentUsername, String.class);
            }
        };
    }
}
