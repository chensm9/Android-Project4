package com.example.admin.personalproject4;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;

import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private SeekBar seekBar;
    private TextView current;
    private CircleImageView circleImageView;
    private  TextView end;
    private ObjectAnimator mMusicAnimation;
//    private MusicService ms;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private IBinder mBinder;
    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = service;
            try {
                // 获取音乐当前path并设置相关显示
                initByMisicPath(getMusicPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 启动rxJava观察者和被观察者
            startMusicListen();
        }

        @Override
        public void onServiceDisconnected(ComponentName name){

        }
    };

    private static final int UPDATE_PROGRESS = 0;

//    //使用handler定时更新进度条
//    private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case -1:
//                    handler.removeCallbacks(mRunnable);
//                    break;
//                default:
//                    handler.postDelayed(mRunnable,100);
//            }
//        }
//    };
//
//    private Runnable mRunnable = new Runnable() {
//        @Override
//        public void run() {
//            try {
//                int currenPostion = ms.getCurrenPostion();
//                int duration = ms.getDuration();
//                seekBar.setProgress(currenPostion);
//                current.setText(formatTime(currenPostion));
//                Message msg = handler.obtainMessage();
//                msg.arg1 = 1;
//                if (formatTime(currenPostion).equals(formatTime(duration))) {
//                    mMusicAnimation.start();
//                    mMusicAnimation.pause();
//                    seekBar.setProgress(0);
//                    current.setText(formatTime(0));
//                    ImageView iv = findViewById(R.id.play);
//                    iv.setImageResource(R.mipmap.play);
//                    msg.arg1 = -1;
//                }
//                handler.sendMessage(msg);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    };

    // 使用rxJava更新进度条
    private void startMusicListen() {
        final io.reactivex.Observable<Integer> observable = io.reactivex.Observable.create(new ObservableOnSubscribe<Integer>() {

            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                while (true) {
                    try {
                        Thread.sleep(500); //每过500ms询问一下进度
                        e.onNext(getCurrenPostion());
                    } catch (InterruptedException exception) {
                        if (!e.isDisposed()) {
                            e.onError(exception);
                        }
                        break;
                    }
                }
                e.onComplete();
            }

        });
        DisposableObserver<Integer> disposableObserver = new DisposableObserver<Integer>() {

            @Override
            public void onNext(Integer value) {
                int currenPostion = value;
                int duration = getDuration();
                seekBar.setProgress(currenPostion);
                current.setText(formatTime(currenPostion));
                if (formatTime(currenPostion).equals(formatTime(duration))) {
                    mMusicAnimation.start();
                    mMusicAnimation.pause();
                    seekBar.setProgress(0);
                    current.setText(formatTime(0));
                    ImageView iv = findViewById(R.id.play);
                    iv.setImageResource(R.mipmap.play);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.d("BackgroundActivity", "onError=" + e);
            }

            @Override
            public void onComplete() {
                Log.d("BackgroundActivity", "onComplete");
            }
        };
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, sc, BIND_AUTO_CREATE);

        current = findViewById(R.id.tv_current);
        end = findViewById(R.id.tv_end);
        circleImageView = findViewById(R.id.circle_image);
        seekBar = findViewById(R.id.bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 进度条改变
                try {
                    if (fromUser) {
                        seekTo(progress);
                        int currenPostion = getCurrenPostion();
                        int duration = getDuration();
                        seekBar.setProgress(currenPostion);
                        current.setText(formatTime(currenPostion));
                        if (formatTime(currenPostion).equals(formatTime(duration))) {
                            mMusicAnimation.start();
                            mMusicAnimation.pause();
                            seekBar.setProgress(0);
                            current.setText(formatTime(0));
                            ImageView iv = findViewById(R.id.play);
                            iv.setImageResource(R.mipmap.play);
//                        Message msg = handler.obtainMessage();
//                        msg.arg1 = -1;
//                        handler.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始触摸进度条
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //停止触摸进度条
            }
        });

        mMusicAnimation = ObjectAnimator.ofFloat(circleImageView, "rotation", 0f,360f);
        mMusicAnimation.setDuration(30000);
        mMusicAnimation.setInterpolator(new LinearInterpolator());//not stop 
        mMusicAnimation.setRepeatCount(-1);//set repeat time forever
    }


    @Override
    protected void onResume() {
        super.onResume();
        //进入到界面后开始更新进度条
//        if (ms != null){
//            handler.sendEmptyMessage(UPDATE_PROGRESS);
//        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //退出应用后与service解除绑定
        unbindService(sc);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        //停止更新进度条的进度
//        handler.removeCallbacksAndMessages(null);
//        handler.obtainMessage(-1).sendToTarget();
    }

    // 转化时间为“mm:ss”格式
    private String formatTime(int length){
        Date date = new Date(length);
        //时间格式化工具
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        String totalTime = sdf.format(date);
        return totalTime;
    }

    public void playButtonOnclick(View view) {
        ImageView iv = findViewById(R.id.play);
//        mRunnable.run();
        PlayAndPause();
        if (isPlaying()) {
            iv.setImageResource(R.mipmap.pause);
            if (!mMusicAnimation.isStarted()) {
                mMusicAnimation.start();
            } else {
                mMusicAnimation.resume();
            }
        } else {
            iv.setImageResource(R.mipmap.play);
            mMusicAnimation.pause();
        }
    }

    public void stopButtonOnclick(View view) {
        try {
            stop();
            ImageView iv = findViewById(R.id.play);
            iv.setImageResource(R.mipmap.play);
            mMusicAnimation.end();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fileButtonOnclick(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*"); //选择音频
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }

    public void backButtonOnclick(View view) {
//        handler.removeCallbacks(mRunnable);
        unbindService(sc);
        try {
            MainActivity.this.finish();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void PlayAndPause() {
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            mBinder.transact(0, data, reply, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            mBinder.transact(1, data, reply, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying() {
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            mBinder.transact(2, data, reply, 0);
            if (reply.readInt() == 1)
                return true;
            else
                return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getCurrenPostion() {
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            mBinder.transact(3, data, reply, 0);
            return reply.readInt();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getDuration() {
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            mBinder.transact(4, data, reply, 0);
            return reply.readInt();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void seekTo(int mesc) {
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            data.writeInt(mesc);
            mBinder.transact(5, data, reply, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMusicPath() {
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            mBinder.transact(6, data, reply, 0);
            return reply.readString();
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setMusicPath(String path) {
        try {
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            data.writeString(path);
            mBinder.transact(7, data, reply, 0);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = getPath(this, uri);
            initByMisicPath(path);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 根据音乐的路径初始化界面和后台播放器
    public void initByMisicPath(String path) {
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(path);
            setMusicPath(path);
//            ((MusicService.MyBinder)mBinder).setMediaPlayer(mp);
            // 设置进度条的最大值
            seekBar.setMax(getDuration());
            // 设置进度条的进度
            seekBar.setProgress(getCurrenPostion());
            // 设置最大时间
            end.setText(formatTime(getDuration()));
            // 设置音乐名称,歌手名称,专辑图片
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);

            String song = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (song == null||song.equals(""))
                song = "未知歌曲";
            ((TextView)findViewById(R.id.song)).setText(song);

            String singer = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (singer == null||singer.equals(""))
                singer = "未知歌曲";
            ((TextView)findViewById(R.id.singer)).setText(singer);

            try {
                byte[] picture = mmr.getEmbeddedPicture();
                Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                circleImageView.setImageBitmap(bitmap);
            } catch (NullPointerException e) {
                e.printStackTrace();
                circleImageView.setImageResource(R.mipmap.img);
            }
            mMusicAnimation.start();
            mMusicAnimation.pause();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageView iv = findViewById(R.id.play);
        iv.setImageResource(R.mipmap.play);
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
