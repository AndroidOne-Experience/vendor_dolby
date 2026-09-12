package com.dolby.daxappui.soundsettings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.dolby.daxappui.MainActivity;

public class DaxSoundSettingsEntryPoint extends Activity {
    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        startActivity(new Intent(this, (Class<?>) MainActivity.class));
        finish();
    }
}
