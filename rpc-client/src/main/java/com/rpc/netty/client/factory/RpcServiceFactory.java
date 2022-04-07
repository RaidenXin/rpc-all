package com.rpc.netty.client.factory;

import com.alibaba.fastjson.JSON;
import com.rpc.netty.client.common.SpecialSymbols;
import com.rpc.netty.client.core.RpcService;
import com.rpc.netty.client.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
public final class RpcServiceFactory {

    private static final int DEFAULT_PROT = 8080;

    public static final RpcService[] newInstance(String urls){
        if (log.isInfoEnabled()){
            log.info("Loaded into the mq manage urls:" + urls);
        }
        String[] hostAndProt;
        //如果这里是用 ; 号分割的
        if (urls.indexOf(SpecialSymbols.SEMICOLONS) > 0){
            hostAndProt = StringUtils.split(urls, SpecialSymbols.SEMICOLONS);
        }else if (urls.indexOf(SpecialSymbols.COMMA) > 0){
            hostAndProt = StringUtils.split(urls, SpecialSymbols.COMMA);
        }else if (urls.indexOf(SpecialSymbols.SEPARATOR) > 0){
            hostAndProt = StringUtils.split(urls, SpecialSymbols.SEPARATOR);
        }else {
            hostAndProt = new String[]{urls};
        }
        RpcService[] serviceInfos = Stream.of(hostAndProt).map(h -> {
            //判断是否存在协议
            if (h.indexOf("://") > 0){
                //如果存在 是 地址
                throw new IllegalArgumentException("{mq-manage.url} Is not correct ！url:" + h);
            }else {
                //如果不存在 是 host + prot
                if (h.indexOf(SpecialSymbols.COLON) > 0) {
                    final String[] split = StringUtils.split(h, SpecialSymbols.COLON);
                    return new RpcService(split[0], Integer.valueOf(split[1]));
                } else {
                    return new RpcService(h, DEFAULT_PROT);
                }
            }
        }).toArray(RpcService[]::new);
        if (log.isDebugEnabled()){
            log.debug("Loaded into the mq manage services:" + JSON.toJSONString(serviceInfos));
        }
        return serviceInfos;
    }
}
