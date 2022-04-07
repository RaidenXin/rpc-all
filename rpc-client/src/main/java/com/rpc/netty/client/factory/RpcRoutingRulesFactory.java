package com.rpc.netty.client.factory;

import com.rpc.netty.client.rule.RpcRoutingRules;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 23:35 2022/4/6
 * @Modified By: 路由规则工厂
 */
public interface RpcRoutingRulesFactory {

    List<RpcRoutingRules> getBeans();
    /**
     * 创建工厂
     * 使用java Spi 获取 如果没有获取到实现 就很使用默认的
     * 自己实现可以 搭配 SpringContextUtil 实现冲Spring 容器中获取
     * @Component
     * public class SpringContextUtil implements ApplicationContextAware {
     *
     *      private static ApplicationContext applicationContext;
     *
     *      @Override
     *      public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
     *          SpringContextUtil.applicationContext = applicationContext;
     *      }
     *
     *      public static <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
     *          return applicationContext.getBeansOfType(type);
     *      }
     * }
     *
     * @return
     */
    static RpcRoutingRulesFactory createFactory(){
        ServiceLoader<RpcRoutingRulesFactory> handlers = ServiceLoader.load(RpcRoutingRulesFactory.class);
        Iterator<RpcRoutingRulesFactory> iterator = handlers.iterator();
        if (iterator.hasNext()){
            return iterator.next();
        }
        return new DefaultRpcRoutingRulesFactory();
    }
}
