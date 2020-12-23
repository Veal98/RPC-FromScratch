package com.cswiki.rpc.sample.server;

import com.cswiki.rpc.sample.api.HelloService;
import com.cswiki.rpc.server.RpcService;

@RpcService(interfaceName = HelloService.class)
public class HelloServiceImpl1 implements HelloService {

    @Override
    public String hello(String name) {
        return name + " Hello from " + "HelloServiceImpl1" ;
    }
}
