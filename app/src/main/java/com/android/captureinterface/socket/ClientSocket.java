package com.android.captureinterface.socket;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

import com.android.captureinterface.R;
import com.android.captureinterface.utils.currentClickUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;

// 客户端
public class ClientSocket {
    BufferedReader in = null;
    PrintWriter out = null;

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
            String packageName = in.readLine();
            currentClickUtil.setClickPackageName(packageName);
            File filePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
            String clickFilePath = filePath + File.separator + "界面信息收集" + File.separator + packageName;
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
            String dateStr = dateformat.format(System.currentTimeMillis());
            if(currentClickUtil.getInterfaceNum() == 1) {
                newDirectory(clickFilePath, dateStr);
                currentClickUtil.setClickFilePath(clickFilePath + "/"  + dateStr);
            }
            String serverMsg = in.readLine();

            String fileName = "SDK" + "_" + "TreeView(" + currentClickUtil.getInterfaceNum() +").json";
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


    /**
     * 创建文件夹
     */
    public void newDirectory(String _path, String dirName) {
        File file = new File(_path + "/" + dirName);
        try {
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
