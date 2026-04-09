package com.exam.aspect;

import com.alibaba.fastjson2.JSON;
import com.exam.entity.OperationLog;
import com.exam.mapper.OperationLogMapper;
import com.exam.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final OperationLogMapper operationLogMapper;

    @Pointcut("execution(* com.exam.controller.*.*(..)) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void logPointcut() {}

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = point.proceed();
        long endTime = System.currentTimeMillis();

        try {
            saveLog(point, endTime - startTime);
        } catch (Exception e) {
            log.error("Failed to save operation log", e);
        }

        return result;
    }

    private void saveLog(ProceedingJoinPoint point, long time) {
        String className = point.getTarget().getClass().getName();
        String methodName = point.getSignature().getName();
        Object[] args = point.getArgs();

        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(UserContext.getUserId());
        operationLog.setUsername(UserContext.getUsername());
        operationLog.setOperation(extractOperation(methodName));
        operationLog.setMethod(className + "." + methodName);

        // Truncate params to avoid too long
        String params = JSON.toJSONString(args);
        if (params.length() > 2000) {
            params = params.substring(0, 2000) + "...";
        }
        operationLog.setParams(params);

        // Get IP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operationLog.setIp(getIpAddress(request));
        }

        operationLogMapper.insert(operationLog);
        log.debug("Operation logged: {} - {} ({}ms)", operationLog.getUsername(), operationLog.getOperation(), time);
    }

    private String extractOperation(String methodName) {
        if (methodName.startsWith("save") || methodName.startsWith("add") || methodName.startsWith("create")) {
            return "CREATE";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else if (methodName.startsWith("login")) {
            return "LOGIN";
        } else if (methodName.startsWith("register")) {
            return "REGISTER";
        } else if (methodName.startsWith("submit")) {
            return "SUBMIT";
        } else if (methodName.startsWith("publish")) {
            return "PUBLISH";
        } else if (methodName.startsWith("grade") || methodName.startsWith("ai")) {
            return "GRADE";
        }
        return "OPERATION";
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
