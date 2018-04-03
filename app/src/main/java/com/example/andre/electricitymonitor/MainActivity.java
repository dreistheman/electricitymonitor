package com.example.andre.electricitymonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import static com.example.andre.electricitymonitor.R.color.green;

public class MainActivity extends Activity {
    TextView txtStatus;
    RelativeLayout layoutParent;
    IntentFilter intentFilter;
    ElectricityListener electricityListener = null;
    static boolean isReceiverRegistered = false;
    static AlertDialog warningDialog;
    Uri alarm;
    Ringtone r;
    static MediaPlayer mediaPlayer;
    Button btnToggle;
    static NotificationManager notifManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtStatus = findViewById(R.id.txtStatus);
        layoutParent = findViewById(R.id.layoutParent);
        intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        btnToggle = findViewById(R.id.btnToggle);

        alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        r = RingtoneManager.getRingtone(MainActivity.this, alarm);
        mediaPlayer = MediaPlayer.create(getApplicationContext(), alarm);
        mediaPlayer.setLooping(true);

        warningDialog = noElectricityDialog();
        notifManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        electricityListener = new ElectricityListener();
        //registerReceiver(electricityListener, intentFilter);
        //isReceiverRegistered = true;

        setInitialFunctionOnOff();

//        if(mediaPlayer.isPlaying()){
//            mediaPlayer.stop();
//        }
        notifManager.cancelAll();



    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    /*
    @Override
    protected void onResume() {
        super.onResume();
        if(!isReceiverRegistered){
            if(electricityListener == null){
                electricityListener = new ElectricityListener();
            }
            registerReceiver(electricityListener, new IntentFilter(intentFilter));
            isReceiverRegistered = true;
        }

    }
    */

    /*
    @Override
    protected void onPause() {
        super.onPause();
        if(isReceiverRegistered){
            unregisterReceiver(electricityListener);
            electricityListener = null;
            isReceiverRegistered = false;
        }


    }
    */

    public void onClickToggleButton(View view){
        //Log.i("myTag", btnToggle.getText().toString());
        if(isReceiverRegistered){

            btnToggle.setText(R.string.activate);
            deactivateListener();
            Toast.makeText(this,"Deactivated!", Toast.LENGTH_SHORT).show();
            notifManager.cancelAll();

        }else{

            btnToggle.setText(R.string.deactivate);
            activateListener();
            Toast.makeText(this,"Activated!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setInitialFunctionOnOff(){
        if(isReceiverRegistered){
            btnToggle.setText(R.string.deactivate);

        }else{
            btnToggle.setText(R.string.activate);

        }
    }

    private void activateListener(){
        registerReceiver(electricityListener, intentFilter);
        isReceiverRegistered = true;

    }
    private void deactivateListener(){
        if(mediaPlayer!=null){
            if(mediaPlayer.isPlaying()){

                mediaPlayer.stop();
                mediaPlayer.reset();
                //mediaPlayer.release();
                mediaPlayer = null;

            }
        }

        unregisterReceiver(electricityListener);
        isReceiverRegistered = false;



    }


    private void electricityRunning(boolean isCharging){
        if(isCharging){
            //warningDialog.dismiss();
            txtStatus.setText("Electricity is flowing");
            layoutParent.setBackgroundColor(getResources().getColor(R.color.green));



        }else{
            //warningDialog.show();
            txtStatus.setText("NO ELECTRICITY!!");
            layoutParent.setBackgroundColor(getResources().getColor(R.color.red));
            createNotification();
        }

    }

    private AlertDialog noElectricityDialog(){
        //Ringtone r = null;
        //alarm:

        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        dialog.setView(inflater.inflate(R.layout.popup_noelectricity, null));
        dialog.setTitle("WARNING");
        dialog.setPositiveButton("OK", new Dialog.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(mediaPlayer.isPlaying()){

                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer.reset();
                }

                dialogInterface.dismiss();
            }
        });
        return dialog.create();
    }


    public void createNotification(){
        int requestCode = 1; //id of pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, requestCode, new Intent(this, MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent = new Intent(this, StopSoundReceiver.class);
        PendingIntent closeNotifIntent = PendingIntent.getBroadcast(this,requestCode+1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Notification notif = builder.setContentTitle("Warning!")
                .setContentText("NO ELECTRICITY!")
                .setTicker("Warning: NO ELECTRICITY!")
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(pIntent)
                .addAction(R.drawable.stop,"Stop",closeNotifIntent)
                .build();

        NotificationManager notifManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(requestCode, notif);
    }

    private MediaPlayer createMediaPlayer( ){
        mediaPlayer = MediaPlayer.create(this, alarm);
        mediaPlayer.setLooping(true);
        return mediaPlayer;
    }

    class ElectricityListener extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            //Log.i("myTag", isCharging? "Status: charging":"Status: not charging");
            if(isCharging){
                if(mediaPlayer==null){
                    createMediaPlayer().start();
                }
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer = null;

                }
                notifManager.cancelAll();
                electricityRunning(true);
            }else{
                if(mediaPlayer==null){
                    createMediaPlayer().start();
                }else{
                    mediaPlayer.start();
                }

                electricityRunning(false);
            }
        }
    }

    public static class StopSoundReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            if(mediaPlayer!=null){
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer = null;
            }
            notifManager.cancelAll();
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);

        }
    }

}
