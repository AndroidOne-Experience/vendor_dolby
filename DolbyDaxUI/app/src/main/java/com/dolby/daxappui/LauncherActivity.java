package com.dolby.daxappui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Keeps the hideable launcher component out of the task containing the Dolby UI. */
public final class LauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This entry activity has no task affinity. NEW_TASK opens (or brings back)
        // the normal MainActivity task, whose root is never the disabled alias.
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
