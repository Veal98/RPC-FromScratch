package com.cswiki.rpc.registry.zookeeper;


import com.cswiki.rpc.registry.ServiceDiscovery;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * 使用 ZooKeeper 实现服务发现功能
 */
public class ZookeeperServiceDiscovery implements ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperServiceDiscovery.class);

    // 注册/发现中心地址
    private String zkAddress;

    /**
     * 该构造方法提供给用户（用户通过配置文件指定 zkAddress 完成服务发现组件的注入）
     * @param zkAddress 注册/发现中心地址
     */
    public ZookeeperServiceDiscovery(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    /**
     * 服务发现
     * @param serviceName 服务名称（被暴露的实现类的接口名称）
     * @return
     */
    @Override
    public String discovery(String serviceName) {
        ZkClient zkClient = new ZkClient(zkAddress, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
        LOGGER.info("connect zookeeper");

        try {
            // 根据 serviceName 查找 service 节点
            String servicePath = Constant.ZK_REGISTRY_PATH + "/" + serviceName;
            if (!zkClient.exists(servicePath)) {
                throw new RuntimeException(String.format("can not find any service node on path: %s\", servicePath"));
            }
            // 查找 address 节点
            List<String> addressList = zkClient.getChildren(servicePath);
            if (CollectionUtils.isEmpty(addressList)) {
                throw new RuntimeException(String.format("can not find any address node on path: %s", servicePath));
            }
            String address;
            int size = addressList.size();
            if (size == 1) {
                // 如果只有一个 address 节点，则直接获取该地址
                address = addressList.get(0);
                LOGGER.info("get only address node: {}", address);
            }
            else {
                // 若存在多个address 节点，则通过负载均衡策略获取一个地址（这里就选择了最简单的随机获取）
                address = addressList.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.info("get random address node: {}", address);
            }
            // 读取 address 节点的内容（服务地址）
            String addressPath = servicePath + "/" + address;
            return zkClient.readData(addressPath);
        } finally {
            zkClient.close();
        }
    }

}
