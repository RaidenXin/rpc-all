package com.rpc.netty.client.utils;

import com.alibaba.fastjson.JSON;
import com.rpc.netty.service.response.RpcResponse;

import java.nio.charset.Charset;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 10:56 2022/3/27
 * @Modified By:
 */
public final class SerializationUtil {

    private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    //标识长度的字符长度
    private final static int CHARACTER_LENGTH_OF_SIZE = 8;
    //补0格式化
    private final static String FORMAT = "%0" + CHARACTER_LENGTH_OF_SIZE + "d";

    public static byte[] encode(final Object obj) {
        final String json = JSON.toJSONString(obj, false);
        if (json != null) {
            return json.getBytes(CHARSET_UTF8);
        }
        return null;
    }

    public static <T> T decode(final byte[] data, Class<T> classOfT) {
        final String json = new String(data, CHARSET_UTF8);
        return JSON.parseObject(json, classOfT);
    }

    public static <T> RpcResponse<T> decodeResponse(final byte[] data, Class<T> classOfT) {
        final String json = new String(data, CHARSET_UTF8);
        int endIndex = CHARACTER_LENGTH_OF_SIZE + Integer.parseInt(json.substring(0, CHARACTER_LENGTH_OF_SIZE));
        String responseJson = json.substring(CHARACTER_LENGTH_OF_SIZE, endIndex);
        String resultJson = json.substring(endIndex);
        RpcResponse<T> response = JSON.parseObject(responseJson, RpcResponse.class);
        T result = JSON.parseObject(resultJson, classOfT);
        response.setResult(result);
        return response;
    }


    public static byte[] encodeResponse(RpcResponse<?> response){
        Object result = response.getResult();
        StringBuilder builder = new StringBuilder();
        String responseJson = JSON.toJSONString(response);
        String length = String.format(FORMAT, responseJson.length());
        return builder.append(length).
                append(responseJson).
                append(JSON.toJSONString(result)).
                toString().
                getBytes(CHARSET_UTF8);
    }
}
