package com.dolby.daxappui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.dolby.dax.DolbyAudioEffect;

public class FragPower extends Fragment implements View.OnClickListener {
    private ImageView imgOn;
    private Context mContext;
    private IDsFragObserver mFObserver = null;

    @Override // android.support.v4.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mFObserver = (IDsFragObserver) context;
            this.mContext = context;
        } catch (ClassCastException unused) {
            throw new ClassCastException(context.toString() + " must implement IDsFragObserver");
        }
    }

    @Override // android.support.v4.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // android.support.v4.app.Fragment, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.imgOn.setLayoutDirection(configuration.getLayoutDirection());
    }

    @Override // android.support.v4.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        boolean z;
        View inflate = layoutInflater.inflate(R.layout.fragpower, viewGroup, false);
        this.imgOn = (ImageView) inflate.findViewById(R.id.powerButtonOn);
        this.imgOn.setOnClickListener(this);
        this.imgOn.setSoundEffectsEnabled(false);
        IDsFragObserver iDsFragObserver = this.mFObserver;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver == null ? null : iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect != null) {
            try {
                z = dolbyAudioEffect.getDsOn();
            } catch (Exception e) {
                Log.e("FragPower", "onCreateView(): Error getting DsOn", e);
                z = true;
            }
        } else {
            Log.e("FragPower", "onCreateView(): Dolby audio effect is null!");
            z = true;
        }
        if (z) {
            this.imgOn.setImageDrawable(getResources().getDrawable(R.drawable.btn_power_on_titlebar, this.mContext.getTheme()));
        } else {
            this.imgOn.setImageDrawable(getResources().getDrawable(R.drawable.btn_power_off_titlebar, this.mContext.getTheme()));
        }
        return inflate;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        int id = view.getId();
        IDsFragObserver iDsFragObserver = this.mFObserver;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver == null ? null : iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect == null) {
            Log.e("FragPower", "onClick(): Dolby audio effect is null!");
            return;
        }
        if (R.id.powerButtonOn == id) {
            try {
                boolean z = !dolbyAudioEffect.getDsOn();
                if (dolbyAudioEffect.hasControl()) {
                    dolbyAudioEffect.setDsOn(z);
                    // The engine changes synchronously, so mirror it immediately instead of
                    // waiting for a DMS broadcast or the next Activity onResume().
                    this.mFObserver.dsPowerChanged(z);
                } else {
                    Log.w("FragPower", "Dolby audio effect is out of control in FragPower");
                }
            } catch (Exception e) {
                Log.e("FragPower", "onClick(): Error toggling DsOn", e);
            }
        }
    }
}
