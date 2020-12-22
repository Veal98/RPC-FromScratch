package com.cswiki.rpc.client;

import com.cswiki.rpc.common.entity.RpcRequest;
import com.cswiki.rpc.common.entity.RpcResponse;
import com.cswiki.rpc.registry.ServiceDiscovery;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
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

    /**
     * 该构造函数用于提供给用户通过配置文件注入服务地址
     * @param serviceAddress
     */
    public RpcProxy(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    /**
     * 该构造函数用于提供给用户通过配置文件注入服务发现组件
     * @param serviceDiscovery
     */
    public RpcProxy(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * 该方法用于对 send 方法进行增强，屏蔽远程方法调用的细节
     *
     * 使用 JDK 动态代理机制 创建客户端请求服务的动态代理对象
     * 适用于一个接口对应一个实现类的情况
     * @param interfaceClass 服务的接口类型
     * @param <T>
     * @return
     *
     * 使用示例：
     *  RpcProxy rpcProxy = context.getBean(RpcProxy.class);
     *  HelloService helloServiceImpl2 = rpcProxy.create(HelloService.class, "helloServiceImpl2");
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass) {
        return create(interfaceClass, "");
    }

    /**
     * 该方法用于对 send 方法进行增强，屏蔽远程方法调用的细节
     *
     * 创建客户端请求服务的动态代理对象，通过代理对象来传输网络请求
     * 适用于一个接口对应多个实现类的情况（此时实现类需指定版本）
     * @param interfaceClass 服务的接口类型
     * @param serviceVersion 服务（实现类）的版本
     * @param <T>
     * @return 返回服务的实例
     *
     * 使用示例：
     *  RpcProxy rpcProxy = context.getBean(RpcProxy.class);
     *  HelloService helloService2 = rpcProxy.create(HelloService.class, "sample.hello2");
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<?> interfaceClass, final String serviceVersion) {

        // 使用 CGLIB 动态代理机制创建代理对象
        Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(interfaceClass.getClassLoader());
        enhancer.setSuperclass(interfaceClass);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                // 创建 RPC 请求对象并设置请求属性
                RpcRequest request = new RpcRequest();
                request.setRequestId(UUID.randomUUID().toString());
                request.setInterfaceName(method.getDeclaringClass().getName());
                request.setServiceVersion(serviceVersion);
                request.setMethodName(method.getName());
                request.setParameterTypes(method.getParameterTypes());
                request.setParameters(args);
                if (serviceDiscovery != null) {
                    // 获取服务名称（被暴露的实现类的接口名称）和版本号
                    String serviceName = interfaceClass.getName();
                    if(serviceVersion != null){
                        String service_Version = serviceVersion.trim();
                        if(!StringUtils.isEmpty(service_Version)){
                            serviceName += "-" + service_Version;
                        }
                    }
                    // 获取服务地址
                    serviceAddress = serviceDiscovery.discover(serviceName);
                    LOGGER.info("discover service: {} => {}", serviceName, serviceAddress);
                }
                if(serviceAddress != null){
                    serviceAddress = serviceAddress.trim();
                    if(StringUtils.isEmpty(serviceAddress)){
                        throw new RuntimeException("server address is empty");
                    }
                }

                // 从服务地址中解析主机名与端口号
                String[] array = StringUtils.split(serviceAddress, ":");
                String host = array[0];
                int port = Integer.parseInt(array[1]);
                // 创建 RPC 客户端对象
                RpcClient client = new RpcClient(host, port);
                long time = System.currentTimeMillis(); // 当前事件
                // 通过动态代理对象发送 RPC 请求
                RpcResponse response = client.send(request);
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
        });
        return (T) enhancer.create();


        // 使用 JDK 动态代理机制创建代理对象
        /*return (T) Proxy.newProxyInstance(
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
                        if (serviceDiscovery != null) {
                            // 获取服务名称（被暴露的实现类的接口名称）和版本号
                            String serviceName = interfaceClass.getName();
                            if(serviceVersion != null){
                                String service_Version = serviceVersion.trim();
                                if(!StringUtils.isEmpty(service_Version)){
                                    serviceName += "-" + service_Version;
                                }
                            }
                            // 获取服务地址
                            serviceAddress = serviceDiscovery.discover(serviceName);
                            LOGGER.info("discover service: {} => {}", serviceName, serviceAddress);
                        }
                        if(serviceAddress != null){
                            serviceAddress = serviceAddress.trim();
                            if(StringUtils.isEmpty(serviceAddress)){
                                throw new RuntimeException("server address is empty");
                            }
                        }

                        // 从服务地址中解析主机名与端口号
                        String[] array = StringUtils.split(serviceAddress, ":");
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);
                        // 创建 RPC 客户端对象
                        RpcClient client = new RpcClient(host, port);
                        long time = System.currentTimeMillis(); // 当前事件
                        // 通过动态代理对象发送 RPC 请求
                        RpcResponse response = client.send(request);
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
        );*/
    }
}

