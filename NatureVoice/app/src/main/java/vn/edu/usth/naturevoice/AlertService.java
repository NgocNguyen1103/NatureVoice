package vn.edu.usth.naturevoice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class AlertService extends Service {

    private static final String TAG = "AlertService";
    private Socket mSocket;
    private ArrayList<Plant> plantList;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AlertService started");

        // Initialize the socket connection
        mSocket = SocketSingleton.getInstance();
        if (!SocketSingleton.isConnected()) {
            mSocket.connect();
        }

        // Attach the alert listener
        mSocket.on("alert", onAlert);
    }

    private final Emitter.Listener onAlert = args -> {
        if (args.length > 0) {
            try {
                JSONObject data = (JSONObject) args[0];
                int id = data.getInt("id");
                String type = data.getString("type");
                String message = data.getString("message");

                Log.d(TAG, "Alert received: " + id + " - " + type + " - " + message);

                // Update the plant list and send broadcast to UI
                if (plantList != null) {
                    for (Plant plant : plantList) {
                        if (plant.getPlantId() == id) {
                            plant.setNoti_type(type);
                            plant.setNoti_message(message);
                        }
                    }
                }

                // Send broadcast to update UI in HomeFragment
                Intent intent = new Intent("vn.edu.usth.naturevoice.UPDATE_UI");
                intent.putExtra("id", id);
                intent.putExtra("type", type);
                intent.putExtra("message", message);
                sendBroadcast(intent);

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing alert data", e);
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AlertService is running");
        return START_STICKY; // Keep the service running
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AlertService stopped");

        // Remove the alert listener and disconnect the socket
        if (mSocket != null) {
            mSocket.off("alert", onAlert);
            mSocket.disconnect();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // This service is not bound
    }

    public void setPlantList(ArrayList<Plant> plantList) {
        this.plantList = plantList;
    }
}