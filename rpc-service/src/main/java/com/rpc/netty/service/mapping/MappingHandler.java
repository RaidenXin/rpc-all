package com.rpc.netty.service.mapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 16:48 2022/4/5
 * @Modified By:
 */
public final class MappingHandler {


    private String path;

    private Object instance;

    private Method method;

    private Class<?> paramType;

    private MappingHandler(){}

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<?> getParamType() {
        return paramType;
    }

    public void setParamType(Class<?> paramType) {
        this.paramType = paramType;
    }

    public <T> T invoke(Object... params) throws InvocationTargetException, IllegalAccessException {
        return (T) method.invoke(instance, params);
    }

    public <T> T invoke() throws InvocationTargetException, IllegalAccessException {
        return (T) method.invoke(instance);
    }

    public static final MappingHandler build(String path,Object instance,Method method,Class<?> paramType){
        if (path == null || path.isEmpty()){
            throw new IllegalArgumentException("path can not be null!");
        }
        if (instance == null){
            throw new IllegalArgumentException("instance can not be null!");
        }
        if (method == null){
            throw new IllegalArgumentException("method can not be null!");
        }
        MappingHandler handler = new MappingHandler();
        handler.path = path;
        handler.instance = instance;
        handler.method = method;
        handler.paramType = paramType;
        return handler;
    }
}
