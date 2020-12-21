package com.cswiki.rpc.sample.server;


import com.cswiki.rpc.sample.api.HelloService;
import com.cswiki.rpc.server.RpcService;

/**
 * HelloService 接口实现类 2（暴露该服务，需要指明 version）
 */
@RpcService(value = HelloService.class, version = "helloServiceImpl2") // 指定暴露服务的接口类型和版本
public class HelloServiceImpl2 implements HelloService {
    @Override
    public String hello(String name) {
        return "Hello! " + name + ", I am helloServiceImpl2";
    }
}
