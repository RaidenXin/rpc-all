package com.rpc.netty.client.test.model;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 21:46 2022/4/7
 * @Modified By:
 */
public class ResponseData {
    private Integer id;
    private String name;

    public ResponseData(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ResponseData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
