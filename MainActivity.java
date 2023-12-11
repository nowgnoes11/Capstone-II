package com.example.noin;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextClock;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Date;
import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity {
    public final String CHANNEL_ID = "noin";
    public static final int NOTIFICATION_ID = 101;
    private String host_ip = "192.168.219.114";
    private int port = 12345;
    private DataInputStream inputStream;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                PERMISSION_GRANTED);

        WebView myWebView = findViewById(R.id.webView);
        WebSettings webSettings = myWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);


        myWebView.setWebViewClient(new WebViewClient());
        myWebView.setWebChromeClient(new WebChromeClient());
        myWebView.loadData("<html><head><style type='text/css'>body{margin:auto auto;text-align:center;}"
                + "imgtext{width:100%25;} div{overflow: hidden;}</style></head>"
                + "<body><div><img src='http://192.168.219.114:12345'/></div></body></head>", "text/html", "UTF-8");

        TextClock myClock = findViewById(R.id.textClock);
        myClock.setFormat12Hour("yyyy-MM-dd hh:mm:ss a");

        //connect();
    }

    public void connect() {
        Log.w("connect", "연결하는 중");

        Thread checkUpdate = new Thread() {
            public void run() {
                try {
                    socket = new Socket(host_ip, port);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Log.w("connect", "연결성공");
                try {
                    inputStream = new DataInputStream(socket.getInputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    int signal;
                    while (true) {
                        signal = (int)inputStream.read() - '0';
                        Log.w("connect", String.valueOf(signal));
                        displayNotification(signal);

                        signal -= 2;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        checkUpdate.start();
    }

    public void displayNotification(int signal) {
        createNotificationChannel();
        String motionTitle = "위급 상황 감지: 넘어짐";
        String speechTitle = "위급 상황 감지: 구조 요청";

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String getDate = sdf.format(date);
        String getTime = new SimpleDateFormat("HH:mm:ss").format(now);


        NotificationCompat.Builder builder;
        if (signal == 1) {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(motionTitle)
                    .setContentText(getDate + " " + getTime)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        } else {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(speechTitle)
                    .setContentText(getDate + " " + getTime)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_GRANTED);
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "넘어짐/구조요청 감지";
            String description = "위급 상황 감지 시 알림";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}