package com.cswiki.rpc.server;

import com.cswiki.rpc.common.entity.RpcRequest;
import com.cswiki.rpc.common.entity.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;


/**
 * Netty / RPC 服务端处理器（用于处理 RPC 请求）
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerHandler.class);

    // 存放 服务名称（被暴露的实现类的接口名称） 与 服务对象（被暴露的实现类） 之间的映射关系
    private final Map<String, Object> handlerMap;

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        // 创建并初始化 RPC 响应对象
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId()); // 指定该 response 对应的 request id
        try {
            Object result = handle(request); // 核心处理方法 ↓
            response.setResult(result);
        } catch (Exception e) {
            LOGGER.error("handle result failure", e);
            response.setException(e);
        }
        // 写入 RPC 响应对象并自动关闭连接
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 处理请求（获取客户端请求的方法和参数，通过反射进行调用）
     * @param request
     * @return
     * @throws Exception
     */
    private Object handle(RpcRequest request) throws Exception {
        String serviceName = request.getInterfaceName();
        String serviceVersion = request.getServiceVersion();
        if(serviceVersion != null){
            serviceVersion = serviceVersion.trim();
            if(!StringUtils.isEmpty(serviceVersion)){
                serviceName += "-" + serviceVersion;
            }
        }
        // 获取服务对象
        Object serviceBean = handlerMap.get(serviceName);
        if (serviceBean == null) {
            throw new RuntimeException(String.format("can not find service bean by key: %s", serviceName));
        }
        // 获取反射调用所需的参数
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        // 执行反射调用
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(serviceBean, parameters);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("server caught exception", cause);
        ctx.close();
    }
}