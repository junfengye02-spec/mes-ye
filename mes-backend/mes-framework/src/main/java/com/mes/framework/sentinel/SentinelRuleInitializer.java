package com.mes.framework.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 规则初始化器
 *
 * <p>启动时加载默认规则，覆盖 P2-26 要求的五个保护目标：
 * <ol>
 *   <li>/auth/login ——&gt; 每 IP 10 QPS（ParamFlowRule）</li>
 *   <li>/file/upload ——&gt; 每租户 5 QPS（ParamFlowRule）</li>
 *   <li>/workorder/work-order/page ——&gt; 单机 200 QPS（FlowRule）</li>
 *   <li>/dispatch/task/page ——&gt; 单机 200 QPS（FlowRule）</li>
 *   <li>/*&#47;export/** ——&gt; 每租户 1 QPS（ParamFlowRule）</li>
 * </ol>
 * </p>
 *
 * <p>生产环境如果接入 Nacos 数据源，{@code application-prod.yml} 中的
 * {@code spring.cloud.sentinel.datasource.*} 会覆盖本类加载的默认规则。
 * 这里保留的本地规则主要用于：开发环境、Nacos 不可用时的兜底。</p>
 */
@Slf4j
@Component
@ConditionalOnClass(FlowRuleManager.class)
@ConditionalOnProperty(prefix = "mes.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SentinelRuleInitializer {

    /**
     * 启动后加载规则。
     * 规则变更若走 Nacos，启动后由 DataSource 自动覆盖；否则保留本地默认。
     */
    @PostConstruct
    public void loadDefaultRules() {
        loadFlowRules();
        loadParamFlowRules();
        log.info("[Sentinel] 默认规则加载完成：FlowRule {} 条，ParamFlowRule {} 条",
                FlowRuleManager.getRules().size(),
                ParamFlowRuleManager.getRules().size());
    }

    /**
     * 加载单机 QPS 流控规则（无业务维度）
     */
    private void loadFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        rules.add(buildFlowRule(SentinelResources.WORKORDER_LIST, 200));
        rules.add(buildFlowRule(SentinelResources.DISPATCH_TASK_PAGE, 200));
        FlowRuleManager.loadRules(rules);
    }

    /**
     * 加载热点参数限流规则（按 IP / 租户维度）
     */
    private void loadParamFlowRules() {
        List<ParamFlowRule> rules = new ArrayList<>();
        rules.add(buildParamRule(SentinelResources.AUTH_LOGIN, 10));
        rules.add(buildParamRule(SentinelResources.FILE_UPLOAD, 5));
        rules.add(buildParamRule(SentinelResources.ANY_EXPORT, 1));
        ParamFlowRuleManager.loadRules(rules);
    }

    /**
     * 构造单机 QPS 流控规则
     *
     * @param resource Sentinel 资源名
     * @param qps      每秒最大通过请求数
     * @return 规则对象
     */
    private FlowRule buildFlowRule(String resource, int qps) {
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        rule.setLimitApp("default");
        rule.setStrategy(RuleConstant.STRATEGY_DIRECT);
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        return rule;
    }

    /**
     * 构造热点参数限流规则（paramIdx=0 的参数即 IP 或 租户 ID）
     *
     * @param resource Sentinel 资源名
     * @param qps      每个热点参数值的每秒最大请求数
     * @return 规则对象
     */
    private ParamFlowRule buildParamRule(String resource, int qps) {
        ParamFlowRule rule = new ParamFlowRule(resource);
        rule.setParamIdx(0);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        rule.setDurationInSec(1);
        return rule;
    }
}
