package com.rpc.netty.client.command;

import com.rpc.netty.client.utils.SerializationUtil;

import java.nio.ByteBuffer;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 10:49 2022/3/27
 * @Modified By:
 */
public class RpcCommand {

    private int serialNo;

    private String path;

    private transient byte[] body;

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    /**
     * 序列化 header
     * @return
     */
    public ByteBuffer encodeHeader() {
        return encodeHeader(this.body != null ? this.body.length : 0);
    }

    /**
     * 序列化 header
     * @return
     */
    public ByteBuffer encodeHeader(final int bodyLength) {
        // 1> 存储Header长度的位置
        int length = 4;

        // 2> 将 Header 序列化
        byte[] headerData = SerializationUtil.encode(this);
        // 加上 Header 内容序列化后的长度
        length += headerData.length;

        // 3> 加上 Body 内容的长度
        length += bodyLength;

        ByteBuffer result = ByteBuffer.allocateDirect(4 + length - bodyLength);

        //设置总长度
        result.putInt(length);
        //设置 header 长度
        result.putInt(headerData.length);

        //设置 header 内容
        result.put(headerData);

        result.flip();

        return result;
    }


    public static RpcCommand decode(final ByteBuffer byteBuffer) {
        int length = byteBuffer.limit();
        int headerDataLength = byteBuffer.getInt();

        byte[] headerData = new byte[headerDataLength];
        byteBuffer.get(headerData);

        RpcCommand cmd = SerializationUtil.decode(headerData, RpcCommand.class);

        int bodyLength = length - 4 - headerDataLength;
        byte[] bodyData = null;
        if (bodyLength > 0) {
            bodyData = new byte[bodyLength];
            byteBuffer.get(bodyData);
        }
        cmd.body = bodyData;

        return cmd;
    }

    public static final RpcCommand build(int serialNo, String path, byte[] body) {
        RpcCommand command = new RpcCommand();
        command.serialNo = serialNo;
        command.path = path;
        command.body = body;
        return command;
    }

    @Override
    public String toString() {
        return "RpcCommand{" +
                "serialNo=" + serialNo +
                ", body=" + new String(body) +
                '}';
    }
}
