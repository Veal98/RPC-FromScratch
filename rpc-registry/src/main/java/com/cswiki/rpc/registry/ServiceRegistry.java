package com.cswiki.rpc.registry;

/**
 * 服务注册接口
 */
public interface ServiceRegistry {

    /**
     * 注册服务名称与服务地址
     *
     * @param serviceName    服务名称（方法名称）
     * @param serviceAddress 服务地址（方法地址）
     */
    void register(String serviceName, String serviceAddress);
}
