package io.agentscope.poc.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.TextBlock;
import io.agentscope.poc.util.AppLogger;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * 日志记录 Hook。
 *
 * <p>在每次工具调用完成后（PostActingEvent），将调用入参与返回结果写入日志，
 * 便于问题排查与全链路追踪。
 */
public class LoggingHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PostActingEvent e) {
            String toolName = e.getToolUse().getName();

            // 工具调用入参
            Object input = e.getToolUse().getInput();
            String inputStr = input != null ? input.toString() : "";
            AppLogger.logToolCall(toolName, inputStr);

            // 工具调用结果
            String resultText = e.getToolResult().getOutput().stream()
                    .filter(block -> block instanceof TextBlock)
                    .map(block -> ((TextBlock) block).getText())
                    .collect(Collectors.joining(" | "));
            AppLogger.logToolResult(toolName, resultText);
        }
        return Mono.just(event);
    }
}
