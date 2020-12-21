package com.cswiki.rpc.server;

import com.cswiki.rpc.common.codec.RpcDecoder;
import com.cswiki.rpc.common.codec.RpcEncoder;
import com.cswiki.rpc.common.entity.RpcRequest;
import com.cswiki.rpc.common.entity.RpcResponse;
import com.cswiki.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;


import java.util.HashMap;
import java.util.Map;


/**
 * Netty / RPC 服务器，用于发布 RPC 服务
 */
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    // 服务地址（比如服务被暴露在 Netty 的 8000 端口，服务地址就是 127.0.0.1:8000）
    private String serviceAddress;

    // 注册服务组件（Zookeeper）
    private ServiceRegistry  serviceRegistry;

    // 存放 服务名称（被暴露的实现类的接口名称） 与 服务对象（被暴露的实现类） 之间的映射关系
    private Map<String, Object> handlerMap = new HashMap<>();


    /**
     * 下面的这两个构造函数用于提供给用户在 Spring 配置文件中通过构造函数进行注入
     * @param serviceAddress
     */
    public RpcServer(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public RpcServer(String serviceAddress, ServiceRegistry serviceRegistry) {
        this.serviceAddress = serviceAddress;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Spring 容器在加载的时候会自动调用一次 setApplicationContext, 并将上下文 ApplicationContext 传递给这个方法
     * 该方法的作用就是获取带有 @RpcSerivce 注解的类的 value (被暴露的实现类的接口名称) 和 version (被暴露的实现类的版本号，默认为 “”)
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 扫描所有带有 @RpcSerivce 注解的类
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if(MapUtils.isNotEmpty(serviceBeanMap)){
            for(Object serviceBean : serviceBeanMap.values()){
                // 获取类上的注解 @RpcService
                RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);
                // 获取注解 @RpcService 的 value（即被暴露的实现类的接口名称）
                String serviceName = rpcService.value().getName();
                // 获取注解 @RpcService 的 version（即被暴露的实现类的版本号，默认为 “”）
                String serviceVersion = rpcService.version();
                // 判断服务版本号是否非空
                if(serviceVersion != null){
                    serviceVersion = serviceVersion.trim();
                    if(!StringUtils.isEmpty(serviceVersion)){
                        serviceName += "-" + serviceVersion;
                    }
                }
                // 将服务名称-版本号 与 服务对象 存入 handlerMap
                handlerMap.put(serviceName, serviceBean);
            }
        }
    }

    /**
     * 在初始化 Bean 的时候会自动执行该方法
     * 该方法的目标就是启动 Netty 服务器进行服务端和客户端的通信，接收并处理客户端发来的请求,
     * 并且还要将服务名称和服务地址注册进 Zookeeper（注册中心）
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 创建并初始化 Netty 服务端 Bootstrap 对象
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast(new RpcDecoder(RpcRequest.class)); // 解码器，解码 RPC 请求
                    pipeline.addLast(new RpcEncoder(RpcResponse.class)); // 编码器，编码 RPC 响应
                    pipeline.addLast(new RpcServerHandler(handlerMap)); // 处理 RPC 请求
                }
            });
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            // TPC 协议的心跳机制
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            // 获取服务地址与端口号
            String[] addressArray = StringUtils.split(serviceAddress, ":");
            String ip = addressArray[0];
            int port = Integer.parseInt(addressArray[1]);
            // 启动 RPC 服务器
            ChannelFuture future = bootstrap.bind(ip, port).sync();
            // 将被暴露的服务(实现类)名称及其服务地址注册进 Zookeeper（注册与发现中心）
            if (serviceRegistry != null) {
                for (String interfaceName : handlerMap.keySet()) { // Map.KeySet() 获取 Map 的全部 Key 值
                    serviceRegistry.register(interfaceName, serviceAddress);
                    LOGGER.info("register service: {} => {}", interfaceName, serviceAddress);
                }
            }
            LOGGER.info("server started on port {}", port);
            // 关闭 RPC 服务器
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }
}
