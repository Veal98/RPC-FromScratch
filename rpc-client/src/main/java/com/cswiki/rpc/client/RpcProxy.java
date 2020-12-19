package com.cswiki.rpc.client;

import com.cswiki.rpc.common.entity.RpcRequest;
import com.cswiki.rpc.common.entity.RpcResponse;
import com.cswiki.rpc.registry.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * RPC 动态代理（用于创建 RPC 服务代理）
 */
public class RpcProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxy.class);

    private String serviceAddress;

    private ServiceDiscovery serviceDiscovery;

    public RpcProxy(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public RpcProxy(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass) {
        return create(interfaceClass, "");
    }

    /**
     * 使用 JDK 动态代理机制 创建客户端请求服务的动态代理对象
     * @param interfaceClass
     * @param serviceVersion
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass, final String serviceVersion) {
        // 创建动态代理对象
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // 创建 RPC 请求对象并设置请求属性
                        RpcRequest request = new RpcRequest();
                        request.setRequestId(UUID.randomUUID().toString());
                        request.setInterfaceName(method.getDeclaringClass().getName());
                        request.setServiceVersion(serviceVersion);
                        request.setMethodName(method.getName());
                        request.setParameterTypes(method.getParameterTypes());
                        request.setParameters(args);
                        // 获取 RPC 服务地址
                        if (serviceDiscovery != null) {
                            String serviceName = interfaceClass.getName();
                            if(serviceVersion != null){
                                String service_Version = serviceVersion.trim();
                                if(!StringUtils.isEmpty(service_Version)){
                                    serviceName += "-" + service_Version;
                                }
                            }
                            serviceAddress = serviceDiscovery.discover(serviceName);
                            LOGGER.info("discover service: {} => {}", serviceName, serviceAddress);
                        }
                        if(serviceAddress != null){
                            serviceAddress = serviceAddress.trim();
                            if(StringUtils.isEmpty(serviceAddress)){
                                throw new RuntimeException("server address is empty");
                            }
                        }
                        // 从 RPC 服务地址中解析主机名与端口号
                        String[] array = StringUtils.split(serviceAddress, ":");
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);
                        // 创建 RPC 客户端对象并发送 RPC 请求
                        RpcClient client = new RpcClient(host, port);
                        long time = System.currentTimeMillis(); // 当前事件
                        RpcResponse response = client.send(request); // 发送 RPC 请求
                        LOGGER.info("time: {}ms", System.currentTimeMillis() - time);
                        if (response == null) {
                            throw new RuntimeException("response is null");
                        }
                        // 返回 RPC 响应结果
                        if (response.hasException()) {
                            throw response.getException();
                        } else {
                            return response.getResult();
                        }
                    }
                }
        );
    }
}

