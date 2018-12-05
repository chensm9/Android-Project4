package com.example.admin.personalproject4;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private String path;
    public final IBinder binder = new MyBinder();
    public class MyBinder extends Binder {
        @Override
        protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
            switch (code) {
                //service solve
                case 0:  play(); break;
                case 1:  stop(); break;
                case 2: reply.writeInt(isPlaying()); break;
                case 3: reply.writeInt(getCurrenPostion()); break;
                case 4: reply.writeInt(getDuration()); break;
                case 5: seekTo(data.readInt()); break;
                case 6: reply.writeString(getPath()); break;
                case 7: setPath(data.readString()); break;
                default: break;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    public MusicService() {
        try {
            initPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override // 被启动时回调该方法
    public int onStartCommand(Intent intent, int flags, int
            startId) {
        return Service.START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override// 被关闭之前回调该方法
    public void onDestroy() {
        super.onDestroy();
    }

    // 播放、暂停
    public void play() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    // 是否播放中
    public int isPlaying() {
        if (mediaPlayer.isPlaying())
            return 1;
        else
            return 0;
    }

    // 停止播放
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare();
                mediaPlayer.seekTo(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //返回歌曲的长度，单位为毫秒
    public int getDuration(){
        return mediaPlayer.getDuration();
    }

    //返回歌曲目前的进度，单位为毫秒
    public int getCurrenPostion(){
        return mediaPlayer.getCurrentPosition();
    }

    //设置歌曲播放的进度，单位为毫秒
    public void seekTo(int mesc){
        mediaPlayer.seekTo(mesc);
    }

    public void setPath(String path) {
        this.path = path;
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPath() {
        return path;
    }

    private void initPath() throws IOException {
        // 第一次运行应用程序时，加载数据库到data/data/当前包的名称/database/<db_name>
        File dir = new File("data/data/com.example.admin.personalproject4/data");

        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }

        String filename = "山高水长.mp3";
        File file = new File(dir, filename);
        InputStream inputStream = null;
        OutputStream outputStream = null;

        //通过IO流的方式，将assets目录下的数据库文件，写入到SD卡中。
        if (!file.exists()) {
            try {
                file.createNewFile();
                inputStream = this.getClass().getClassLoader().getResourceAsStream("assets/" + filename);
                outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int len ;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer,0,len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        path = file.getPath();
        setPath(path);
    }
}
