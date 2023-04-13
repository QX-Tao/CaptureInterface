package com.android.captureinterface.socket;

import com.android.captureinterface.utils.currentClickUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

// 客户端
public class ClientSocket {
    BufferedReader in = null;
    PrintWriter out = null;

    //private static final int PORT = 9000;
    // private static final String HOSTNAME = "127.0.0.1";
    private Socket serverSocket;

    public ClientSocket(String HOSTNAME, int PORT){
        // 连接服务端
        try {
            serverSocket = new Socket(HOSTNAME,PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




    public void send(String Msg, CountDownLatch countDownLatch) {

        try {
            // 向服务端发送消息
            out = new PrintWriter(serverSocket.getOutputStream(), true);
            out.println(Msg);
            // 从服务端接收消息并打印
            in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            String serverMsg = in.readLine();

            String fileName = "SDK" + "_" + "TreeView.json";
            String strFilePath = currentClickUtil.getClickFilePath() + File.separator + fileName;
            File saveFile = new File(strFilePath);
            RandomAccessFile raf = new RandomAccessFile(saveFile, "rwd");
            raf.seek(saveFile.length());
            raf.write(serverMsg.getBytes());
            raf.close();

            countDownLatch.countDown();

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                in.close();
                out.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    // TODO: 阻塞等待SDK发来的信息

}
