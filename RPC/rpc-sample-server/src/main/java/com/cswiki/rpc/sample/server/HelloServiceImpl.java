package com.cswiki.rpc.sample.server;

import com.cswiki.rpc.sample.api.HelloService;
import com.cswiki.rpc.server.RpcService;

/**
 * HelloService 接口实现类 1（暴露该服务）
 */
@RpcService(HelloService.class) // 指定暴露服务的接口类型
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }
}
