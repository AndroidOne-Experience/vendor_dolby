package com.dolby.daxappui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import com.dolby.dax.DolbyAudioEffect;
import com.dolby.daxservice.DaxService;

public class DaxTileService extends TileService {
    private static final String TAG = "DaxTileService";
    private static int mPriority = -1;
    private DolbyAudioEffect mDolbyAudio = null;
    private boolean isReceiverRegistered = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("audio_server_restarted".equals(action)) {
                releaseDolbyAudio();
            }
            updateDolbyTileUI();
        }
    };

    private synchronized DolbyAudioEffect getDolbyAudio() {
        if (this.mDolbyAudio == null) {
            try {
                this.mDolbyAudio = new DolbyAudioEffect(mPriority, 0);
            } catch (Exception e) {
                Log.w(TAG, "Cannot initialize DolbyAudioEffect", e);
                this.mDolbyAudio = null;
            }
        }
        return this.mDolbyAudio;
    }

    private synchronized void releaseDolbyAudio() {
        if (this.mDolbyAudio != null) {
            try {
                this.mDolbyAudio.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing DolbyAudioEffect", e);
            }
            this.mDolbyAudio = null;
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerReceiverIfNeeded() {
        if (!isReceiverRegistered) {
            try {
                IntentFilter filter = new IntentFilter();
                filter.addAction("audio_server_restarted");
                filter.addAction("com.dolby.intent.action.DAP_PARAMS_UPDATE");
                if (Build.VERSION.SDK_INT >= 33) {
                    registerReceiver(mReceiver, filter, DaxService.PERMISSION, null, Context.RECEIVER_EXPORTED);
                } else {
                    registerReceiver(mReceiver, filter, DaxService.PERMISSION, null);
                }
                isReceiverRegistered = true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to register tile receiver", e);
            }
        }
    }

    private void unregisterReceiverIfNeeded() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(mReceiver);
            } catch (Exception e) {
                Log.w(TAG, "Failed to unregister tile receiver", e);
            }
            isReceiverRegistered = false;
        }
    }

    private void updateDolbyTileUI() {
        Tile qsTile = getQsTile();
        if (qsTile == null) {
            return;
        }
        int dolbyState = getDolbyState();
        Log.d(TAG, "updateDolbyTileUI, daxState = " + dolbyState);
        String version = getDolbyVersion();
        String substring = (version != null && version.length() >= 3) ? version.substring(0, 3) : "DAX";
        switch (dolbyState) {
            case 0:
            case 2:
                qsTile.setState(Tile.STATE_INACTIVE);
                break;
            case 1:
                qsTile.setState(Tile.STATE_ACTIVE);
                break;
            default:
                qsTile.setState(Tile.STATE_INACTIVE);
                break;
        }
        if ("DS1".equals(substring)) {
            qsTile.setLabel(getString(R.string.app_name_ds1));
        } else {
            qsTile.setLabel(getString(R.string.app_name));
        }
        try {
            qsTile.updateTile();
        } catch (Exception e) {
            Log.w(TAG, "Failed to update QS tile", e);
        }
    }

    private int getDolbyState() {
        DolbyAudioEffect dolbyAudio = getDolbyAudio();
        if (dolbyAudio != null) {
            try {
                return dolbyAudio.getDsOn() ? 1 : 0;
            } catch (Exception e) {
                Log.w(TAG, "Failed to get Dolby state: " + e.getMessage());
                releaseDolbyAudio();
                dolbyAudio = getDolbyAudio();
                if (dolbyAudio != null) {
                    try {
                        return dolbyAudio.getDsOn() ? 1 : 0;
                    } catch (Exception ignored) {
                        releaseDolbyAudio();
                    }
                }
            }
        }
        return 2;
    }

    private void setDolbyState() {
        DolbyAudioEffect effect = getDolbyAudio();
        if (effect != null) {
            try {
                boolean z = !effect.getDsOn();
                if (!effect.hasControl()) {
                    releaseDolbyAudio();
                    mPriority = 0;
                    effect = getDolbyAudio();
                }
                if (effect != null) {
                    effect.setDsOn(z);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to set Dolby state: " + e.getMessage());
                releaseDolbyAudio();
            }
        } else {
            Log.w(TAG, "DolbyAudioEffect is null when setDolbyState called");
        }
        updateDolbyTileUI();
    }

    private String getDolbyVersion() {
        DolbyAudioEffect dolbyAudio = getDolbyAudio();
        if (dolbyAudio == null) {
            return "DAX3";
        }
        try {
            return dolbyAudio.getDsVersion();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get Dolby version: " + e.getMessage());
            releaseDolbyAudio();
            return "DAX3";
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        registerReceiverIfNeeded();
        updateDolbyTileUI();
        Log.d(TAG, "onStartListening");
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        unregisterReceiverIfNeeded();
        Log.d(TAG, "onStopListening");
    }

    @Override
    public void onClick() {
        super.onClick();
        setDolbyState();
        Log.d(TAG, "onClick");
    }

    @Override
    public void onTileAdded() {
        Log.d(TAG, "onTileAdded");
    }

    @Override
    public void onTileRemoved() {
        Log.d(TAG, "onTileRemoved");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getDolbyAudio();
        Log.d(TAG, "onCreate() executed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiverIfNeeded();
        releaseDolbyAudio();
        Log.d(TAG, "onDestroy() executed");
    }
}
