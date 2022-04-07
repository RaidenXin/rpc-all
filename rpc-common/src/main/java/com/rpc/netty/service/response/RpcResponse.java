package com.rpc.netty.service.response;
/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 14:32 2022/4/5
 * @Modified By:
 */
public final class RpcResponse<T> {
    
    public static final String SUCCESS_CODE = "0";

    public static final String UNKNOWN_CODE = "500";

    private transient T result;

    private String code;

    private String message;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean success() {
        return SUCCESS_CODE.equals(this.code);
    }

    /**
     * 成功
     * @param result
     * @param <T>
     * @return
     */
    public static <T> RpcResponse<T> success(T result){
        RpcResponse response = new RpcResponse();
        response.code = SUCCESS_CODE;
        response.result = result;
        return response;
    }

    /**
     * 失败
     * @param code
     * @param message
     * @return
     */
    public static RpcResponse fail(String code, String message){
        RpcResponse response = new RpcResponse();
        response.code = code;
        response.message = message;
        return response;
    }

    public static RpcResponse fail(String message){
        RpcResponse response = new RpcResponse();
        response.code = UNKNOWN_CODE;
        response.message = message;
        return response;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "result=" + result +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
