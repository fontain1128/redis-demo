package com.example.redis.queue;


public class Message<T> {

    private String msgId;

    private T data;

    public Message(String msgId, T data) {
        this.msgId = msgId;
        this.data = data;
    }
}
