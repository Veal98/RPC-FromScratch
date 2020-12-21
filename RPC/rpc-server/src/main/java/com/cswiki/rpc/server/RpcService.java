package com.cswiki.rpc.server;

import org.springframework.stereotype.Component;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义服务暴露注解 @RpcService 供用户使用
 *
 * 使用示例：@RpcService(HelloService.class)
 */
@Target(ElementType.TYPE) // 表示 @RpcService 注解可放在 接口、类、枚举、注解 上
@Retention(RetentionPolicy.RUNTIME) // 表示 @RpcService 注解会在class字节码文件中存在，在运行时可以通过反射获取到
@Component // 表明 @RpcService 注解可被 Spring 扫描
public @interface RpcService {

    /**
     * 被暴露的实现类的接口类型 Class（服务名称）
     */
    Class<?> value();

    /**
     * 被暴露的实现类的版本号（服务版本号）
     */
    String version() default "";
}
