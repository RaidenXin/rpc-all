package com.rpc.netty.service.annotation;

import java.lang.annotation.*;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 16:50 2022/4/5
 * @Modified By:
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcRequestMapping {

    String path();
}
