package com.zane.p2pclient.comman.receive;

import android.util.Log;

import com.google.gson.Gson;
import com.zane.p2pclient.comman.Config;
import com.zane.p2pclient.comman.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.subjects.AsyncSubject;

/**
 * 循环接收数据
 * Created by Zane on 2017/6/17.
 * Email: zanebot96@gmail.com
 * Blog: zane96.github.io
 */

public class UDPMessageReciver extends Thread implements IMessageReceiver{

    private DatagramSocket socket;
    private int byteLength;
    private Flowable<Message> responseFlowable;
    private AsyncSubject<String> subject;
    private Gson gson;
    private OnReceiverListener listener;

    public void setOnReceiverFailedListener(OnReceiverListener listener) {
        this.listener = listener;
    }

    public UDPMessageReciver(DatagramSocket socket, int byteLength) {
        this.socket = socket;
        this.byteLength = byteLength;
        gson = new Gson();
        subject = AsyncSubject.create();
        responseFlowable = subject.toFlowable(BackpressureStrategy.LATEST).map(new Function<String, Message>() {
            @Override
            public Message apply(@NonNull String data) throws Exception {
                return gson.fromJson(data, Message.class);
            }
        });
    }

    public Flowable<Message> getFlowable() {
        return responseFlowable;
    }

    public void finish() {
        interrupt();
    }

    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[byteLength], byteLength);
                socket.receive(packet);
                byte[] responseData = packet.getData();
                subject.onNext(new String(responseData));

                //Log应该写一个拦截器
                Log.i("response", "response IP: " + packet.getAddress().toString() + " port: " + packet.getPort());
            } catch (IOException e) {
                Log.i("response", "response failed: " + e.getMessage());
                finish();
                if (listener != null) {
                    listener.onFailed();
                }
            }
        }
    }
}