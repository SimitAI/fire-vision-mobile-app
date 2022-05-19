package com.example.firevisionnotificationapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;


import java.sql.Array;
import java.sql.Struct;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    TextView dataText;
    ImageView dataImage;
    ArrayList<String> notificationFires = new ArrayList<String>();
    ArrayList<String> notificationHumans = new ArrayList<String>();;
    ArrayList<String> notificationTimeList = new ArrayList<>();
    private OkHttpClient client;
    public static boolean messageChanged = false;
    NotificationCompat.Builder builder;

    final class EchoWebSocketListener extends WebSocketListener {
        private  static final int NORMAL_CLOSURE_STATUS = 1000;
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull okhttp3.Response response) {
            System.out.println("connected");
            //webSocket.close(NORMAL_CLOSURE_STATUS,"sleep");
            //onTextChange("Connected!");
        }


        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            JSONObject jsonObject;
            System.out.println("received :" + text);

            try {
                jsonObject = new JSONObject(text);
                System.out.println("received" + text);
                messageChanged = true;
                if (!jsonObject.getString("fire").isEmpty()){
                String fireText = getEmoji(0X1F525) + " Fire Detected | Lat: " + jsonObject.getInt("latitude") + " Long: "  + jsonObject.getInt("longitude") +"\n" + getEmoji(0X1F551) + " " +  new SimpleDateFormat("MMM d, hh:mm aaa").format(new Date());
                    notificationFires.add(fireText);
                }
                createFireListView(notificationFires);
                if (!jsonObject.getString("person").isEmpty()){
                    String humanText = getEmoji(0X1F64B) + " Human Detected | Lat: " + jsonObject.getInt("latitude") + " Long: "  + jsonObject.getInt("longitude") + "\n" + getEmoji(0X1F551) + " " + new SimpleDateFormat("MMM d, hh:mm aaa").format(new Date());
                    notificationHumans.add(humanText);
                }
                createHumanListView(notificationHumans);



                if (notificationFires.size() == 4){
                    notificationFires.remove(0);
                }

                if (notificationHumans.size() == 4){
                    notificationHumans.remove(0);
                }
                System.out.println("OnMessage: " + notificationFires.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }
            finally {
                createNotification(text);
            }


        }




        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            onTextChange("closing" + code + "/" + reason);
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable okhttp3.Response response) {
            onTextChange("error: " + t.getMessage());
            okhttp3.Request request = new okhttp3.Request.Builder().url("ws://54.209.157.254:8000/warning").build();
            EchoWebSocketListener listener = new EchoWebSocketListener();
            WebSocket ws =  client.newWebSocket(request,listener);

        }

    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        client = new OkHttpClient();
        builder = new NotificationCompat.Builder(MainActivity.this, "New Fire");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = this.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.topBar));

        start();

        if (getIntent().hasExtra("fire")){
            System.out.println("There is array");
            ArrayList<String> fire = getIntent().getStringArrayListExtra("fire");
            System.out.println(fire);
            notificationFires = fire;
            System.out.println("sksk" + notificationFires.toString());
            createFireListView(fire);
        }
        if(getIntent().hasExtra("person")){
            System.out.println("There is array");
            ArrayList<String> human = getIntent().getStringArrayListExtra("person");
            System.out.println(human);
            notificationHumans = human;
            System.out.println("sksk" + notificationHumans.toString());
            createHumanListView(human);

        }



        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("New Fire", "Fire Alert", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager =  getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public void  createNotification(String text){
        Intent intent = new Intent(this, MainActivity.class);
        System.out.println(notificationFires.toString());

        intent.putStringArrayListExtra("fire",notificationFires);
        intent.putStringArrayListExtra("person",notificationHumans);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        System.out.println("new notification");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "New Fire");
        builder.setContentTitle("New Fire Alert!");
        //builder.setContentText(new SimpleDateFormat("MMM d, hh:mm aaa").format(new Date()).toString());
        onTextChange(text);
        builder.setSmallIcon(R.mipmap.fire_vision_logo);
        builder.setAutoCancel(true);
        builder.setContentIntent(pendingIntent);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(MainActivity.this);
        managerCompat.notify(1, builder.build());

    }
    private void onTextChange(String text) {//runOnUiThread(() -> {
        //dataText.setText(text);
        //dataImage.setImageResource(R.mipmap.fire_gif_round);
    //});
    }

    private void onImageChange(byte[] data){
       // runOnUiThread(() -> {
            //Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            //dataImage.setImageBitmap(bitmap);
       // });
    }
    private void start(){
        okhttp3.Request request = new okhttp3.Request.Builder().url("ws://54.209.157.254:8000/warning").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws =  client.newWebSocket(request,listener);
    }

    public void onClose(View view){
        dataText.setText("No New Fire is Detected! \n Forests are Protected!");
        dataImage.setImageResource(R.mipmap.green_tick_round);
        messageChanged = false;
        finish();
    }
    public void createFireListView(ArrayList<String> fire){ runOnUiThread(new Runnable() {
        @Override
        public void run() {
            System.out.print("sssaaaaaa:::" +  fire.toString());
            HashMap<String,String> hashedInfo = new HashMap<>();

            //notificationFires = fire;
            ArrayList<String> reverseFireList = (ArrayList<String>) fire.clone();
            Collections.reverse(reverseFireList);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                    R.layout.activity_listview, reverseFireList);
            ListView listView = (ListView) findViewById(R.id.fire_list);
            listView.setAdapter(adapter);
        }
    });
    }
    public void createHumanListView(ArrayList<String> human){ runOnUiThread(new Runnable() {
        @Override
        public void run() {
            System.out.print("sssaaaaaa:::" +  human.toString());
            //notificationFires = fire;
            ArrayList<String> reverseHumanList = (ArrayList<String>) human.clone();
            Collections.reverse(reverseHumanList);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                    R.layout.activity_listview, reverseHumanList);
            ListView listView = (ListView) findViewById(R.id.human_list);
            listView.setAdapter(adapter);
        }
    });
    }
    private String getEmoji(int unicode ){
        return new String(Character.toChars(unicode));
    }
}