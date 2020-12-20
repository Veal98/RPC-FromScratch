package com.cswiki.rpc.registry.zookeeper;

import com.cswiki.rpc.registry.ServiceRegistry;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 使用 ZooKeeper 实现服务注册功能（使用 Zookeeper 客户端 Curator）
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry {

    // slf4j 日志
    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);

    private final ZkClient zkClient;

    public ZooKeeperServiceRegistry(String zkAddress) {
        // 创建 ZooKeeper 客户端
        zkClient = new ZkClient(zkAddress, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
        LOGGER.info("connect zookeeper");
    }

    /**
     * 服务注册
     * @param serviceName    服务名称（方法名称）
     * @param serviceAddress 服务地址（方法地址）
     */
    @Override
    public void register(String serviceName, String serviceAddress) {
        // 创建 registry 节点（持久，该节点下存放所有的服务节点）
        String registryPath = Constant.ZK_REGISTRY_PATH;
        if (!zkClient.exists(registryPath)){
            zkClient.createPersistent(registryPath);
            LOGGER.info("create registry node: {}", registryPath);
        }
        // 在 registry 节点下创建 service 节点（持久，存放服务名称）
        String servicePath = registryPath + "/" + serviceName;
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
            LOGGER.info("create service node: {}", servicePath);
        }
        // 创建 address 节点（临时,存放服务地址）
        String addressPath = servicePath + "/address-";
        String addressNode = zkClient.createEphemeralSequential(addressPath, serviceAddress);
        LOGGER.debug("create address node: {}", addressNode);
    }
}
