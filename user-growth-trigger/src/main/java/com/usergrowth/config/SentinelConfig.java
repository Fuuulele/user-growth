package com.usergrowth.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelConfig {

    /**
     * 注册 Sentinel 注解 AOP 切面
     * 使 @SentinelResource 注解生效
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    /**
     * 初始化限流规则
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 规则1：同步兑换接口，QPS 限制为 10
        FlowRule exchangeRule = new FlowRule();
        exchangeRule.setResource("exchange");
        exchangeRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        exchangeRule.setCount(10);
        rules.add(exchangeRule);

        // 规则2：异步兑换接口，QPS 限制为 20
        FlowRule exchangeAsyncRule = new FlowRule();
        exchangeAsyncRule.setResource("exchangeAsync");
        exchangeAsyncRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        exchangeAsyncRule.setCount(20);
        rules.add(exchangeAsyncRule);

        // 规则3：完成任务上报，QPS 限制为 50
        FlowRule completeTaskRule = new FlowRule();
        completeTaskRule.setResource("completeTask");
        completeTaskRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        completeTaskRule.setCount(50);
        rules.add(completeTaskRule);

        FlowRuleManager.loadRules(rules);
    }

    /**
     * 初始化熔断规则
     */
    @PostConstruct
    public void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 兑换接口熔断：异常比例超过 50% 且请求数 >= 5，熔断 10 秒
        DegradeRule exchangeDegrade = new DegradeRule();
        exchangeDegrade.setResource("exchange");
        exchangeDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        exchangeDegrade.setCount(0.5);   // 异常比例 50%
        exchangeDegrade.setMinRequestAmount(5);  // 最小请求数
        exchangeDegrade.setStatIntervalMs(10000); // 统计窗口 10s
        exchangeDegrade.setTimeWindow(10);       // 熔断时长 10s
        rules.add(exchangeDegrade);

        // 任务完成接口熔断：异常比例超过 50%，熔断 5 秒
        DegradeRule completeTaskDegrade = new DegradeRule();
        completeTaskDegrade.setResource("completeTask");
        completeTaskDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        completeTaskDegrade.setCount(0.5);
        completeTaskDegrade.setMinRequestAmount(5);
        completeTaskDegrade.setStatIntervalMs(10000);
        completeTaskDegrade.setTimeWindow(5);
        rules.add(completeTaskDegrade);

        DegradeRuleManager.loadRules(rules);
    }
}