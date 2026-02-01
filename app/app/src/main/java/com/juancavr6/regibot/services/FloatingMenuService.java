package com.juancavr6.regibot.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;

import com.juancavr6.regibot.NavigationActivity;
import com.juancavr6.regibot.R;
import com.juancavr6.regibot.ui.fragment.HomeFragment;

public class FloatingMenuService extends Service implements View.OnClickListener{

    private static final String CHANNEL_ID = "FloatingMenuServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    private final IBinder mBinder = new LocalBinder();
    Callbacks fragment;

    private boolean isRunning = false;
    private boolean isPokemonGoForeground = false;
    private boolean isForegroundStarted = false;

    private WindowManager mWindowManager;
    private View myFloatingView;

    private ProgressBar loader;
    private ImageView mainIcon;
    private CardView mainButton;
    private CardView destroyButton;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                // Pause the looper when screen turns off, but keep service running
                if (isRunning) {
                    Intent pauseIntent = new Intent(getApplicationContext(), ActionService.class);
                    pauseIntent.putExtra("action", "pause");
                    getApplication().startService(pauseIntent);
                    isRunning = false;
                    updateMainButton();
                }
            }
        }
    };

    private final BroadcastReceiver pokemonGoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActionService.ACTION_POKEMONGO_FOREGROUND.equals(intent.getAction())) {
                boolean isForeground = intent.getBooleanExtra(ActionService.EXTRA_IS_FOREGROUND, false);
                onPokemonGoForegroundChanged(isForeground);
            }
        }
    };

    public FloatingMenuService() {


    }



    @Override
    public IBinder onBind(Intent intent) {return mBinder;}
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, screenFilter);

        IntentFilter pokemonGoFilter = new IntentFilter(ActionService.ACTION_POKEMONGO_FOREGROUND);
        registerReceiver(pokemonGoReceiver, pokemonGoFilter, Context.RECEIVER_NOT_EXPORTED);

        // Check current Pokemon Go state in case it's already in foreground
        if (ActionService.isPokemonGoInForeground()) {
            isPokemonGoForeground = true;
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Floating menu service notification");
        channel.setShowBadge(false);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, NavigationActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service immediately
        if (!isForegroundStarted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
            isForegroundStarted = true;
        }

        if( loader != null && intent != null && intent.getBooleanExtra("loadedNotification", false)) {
            loader.setVisibility(View.GONE);
            mainButton.setVisibility(View.VISIBLE);
            destroyButton.setVisibility(View.VISIBLE);

            fragment.updateClient(true);
            Toast.makeText(this, getString(R.string.displayText_ready), Toast.LENGTH_SHORT).show();

        }
        else{
            //getting the widget layout from xml using layout inflater
            myFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);


            int layout_parms = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;


            //setting the layout parameters
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layout_parms,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP;

            //getting windows services and adding the floating view to it
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(myFloatingView, params);

            //adding an touchlistener to make drag movement of the floating widget
            myFloatingView.findViewById(R.id.thisIsAnID).setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;

                        case MotionEvent.ACTION_UP:

                            return true;

                        case MotionEvent.ACTION_MOVE:
                            //this code is helping the widget to move around the screen with fingers
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            mWindowManager.updateViewLayout(myFloatingView, params);
                            return true;
                    }
                    return false;
                }
            });

            //getting the widgets reference from xml layout
            loader = myFloatingView.findViewById(R.id.loader);
            mainIcon = myFloatingView.findViewById(R.id.mainIcon);
            mainButton =  myFloatingView.findViewById(R.id.main);
            mainButton.setOnClickListener(this);
            destroyButton = myFloatingView.findViewById(R.id.destroy);
            destroyButton.setOnClickListener(this);

            // Show or hide based on current Pokemon Go state
            if (isPokemonGoForeground) {
                myFloatingView.setVisibility(View.VISIBLE);
            } else {
                myFloatingView.setVisibility(View.GONE);
            }
        }

           return START_STICKY;
    }

    public class LocalBinder extends Binder {
        public FloatingMenuService getServiceInstance(){
            return FloatingMenuService.this;
        }
    }

    public void registerClient(HomeFragment fragment){
        this.fragment = fragment;
    }

    @Override
    public void onClick(View v) {

        Intent intent = new Intent(getApplicationContext(), ActionService.class);
        int id = v.getId();
        if (id == R.id.main) {
            if(isRunning){
                intent.putExtra("action", "pause");
            }else{
                intent.putExtra("action", "resume");
            }
            isRunning = !isRunning;
            updateMainButton();
            getApplication().startService(intent);

        } else if (id == R.id.destroy) {
            isRunning = false;
            fragment.updateClient(false);
            getApplication().startService(intent);
        }
    }


    @Override
    public void onDestroy() {

        Intent intent = new Intent(getApplicationContext(), ActionService.class);
        intent.putExtra("action", "destroy");
        getApplication().startService(intent);

        super.onDestroy();
        if (myFloatingView != null) {
            mWindowManager.removeView(myFloatingView);
        }
        unregisterReceiver(screenReceiver);
        unregisterReceiver(pokemonGoReceiver);
    }

    private void onPokemonGoForegroundChanged(boolean isForeground) {
        isPokemonGoForeground = isForeground;

        if (isForeground) {
            showFloatingMenu();
        } else {
            hideFloatingMenu();
            // Pause the looper if it was running
            if (isRunning) {
                isRunning = false;
                updateMainButton();
            }
        }
    }

    private void showFloatingMenu() {
        if (myFloatingView != null) {
            myFloatingView.setVisibility(View.VISIBLE);
        }
    }

    private void hideFloatingMenu() {
        if (myFloatingView != null) {
            myFloatingView.setVisibility(View.GONE);
        }
    }

    private void updateMainButton() {
        if(!isRunning){
            mainButton.setCardBackgroundColor(Color.parseColor("#979797"));
            mainIcon.setImageResource(android.R.drawable.ic_media_play);
        }else{
            mainButton.setCardBackgroundColor(Color.parseColor("#C4C4C4"));
            mainIcon.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    public interface Callbacks{
        void updateClient(boolean data);
    }


}