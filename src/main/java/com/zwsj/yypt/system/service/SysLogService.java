package com.zwsj.yypt.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.zwsj.yypt.system.domain.SysLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.scheduling.annotation.Async;

/**
 * @创建人 zhk
 * @创建时间 2019-08-09
 * @描述
 */
public interface SysLogService extends IService<SysLog> {
    @Async
    void saveLog(ProceedingJoinPoint point, SysLog log) throws JsonProcessingException;
}
