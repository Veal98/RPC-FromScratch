package com.cswiki.rpc.sample.server;

import com.cswiki.rpc.sample.api.HelloService;
import com.cswiki.rpc.server.RpcService;

@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }
}
