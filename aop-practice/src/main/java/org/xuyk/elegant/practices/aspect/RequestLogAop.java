package org.xuyk.elegant.practices.aspect;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.xuyk.elegant.practices.annotation.RequestLog;
import org.xuyk.elegant.practices.pojo.RequestLogInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: Xuyk
 * @Description: @RequestAop 注解方法切面
 * @Date: 2020/9/24
 */
@Component
@Aspect // 标识这是一个切面
@Slf4j
public class RequestLogAop {

    @Autowired
    private ApplicationContext context;

    /**
     * 被 @requestLog 所注解的切点
     * @param requestLog
     */
    @Pointcut("@annotation(requestLog)")
    public void requestLogPointcut(RequestLog requestLog){}

    /**
     * 环绕增强
     * @param pjp
     * @param requestLog
     */
    @Around(value = "requestLogPointcut(requestLog)", argNames = "pjp,requestLog")
    public Object around(ProceedingJoinPoint pjp, RequestLog requestLog) throws Throwable {

        // 生产环境不记录请求参数
        Environment environment = context.getEnvironment();
        String currentEnv = environment.getActiveProfiles()[0];
        if("prod".equals(currentEnv)){
            return pjp.proceed();
        }

        // 请求信息
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();

        // 使用包装类目的为了减少多请求情况下 日志串行的问题
        RequestLogInfo info = RequestLogInfo.builder()
                .requestUri(request.getRequestURI())
                .apiDesc(requestLog.desc())
                .httpMethod(request.getMethod())
                .classMethod(pjp.getSignature().getDeclaringTypeName() + "." +pjp.getSignature().getName())
                .requestIp(request.getRemoteHost())
                .requestParams(JSONUtil.toJsonStr(pjp.getArgs()))
                .build();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
//        log.info("【请求链接】:{}",request.getRequestURI());
//        log.info("【接口描述】:{}",requestLog.desc());
//        log.info("【请求类型】:{}",request.getMethod());
//        log.info("【请求方法】:{}.{}",pjp.getSignature().getDeclaringTypeName(),pjp.getSignature().getName());
//        log.info("【请求IP】:{},{}:{}",request.getRemoteAddr(),request.getRemoteHost(),request.getRemotePort());
//        log.info("【请求参数】:{}", JSONUtil.toJsonStr(pjp.getArgs()));

        // 执行原方法逻辑
        Object result = pjp.proceed();

        stopWatch.stop();

        info.setCostTimeMillis(stopWatch.getTotalTimeMillis());
        info.setResult(JSONUtil.toJsonStr(result));
        log.info("【requestLog】:{}",JSONUtil.toJsonStr(info));

//        log.info("【接口花费时间统计】:{} 秒",stopWatch.getTotalTimeSeconds());
//        log.info("【接口花费时间调度】:{}",stopWatch.prettyPrint());
//        log.info("【请求返回结果】:{}",JSONUtil.toJsonStr(result));
        return result;
    }

}
