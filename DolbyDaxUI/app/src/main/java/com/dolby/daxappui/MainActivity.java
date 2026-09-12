package com.dolby.daxappui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.text.SpannableStringBuilder;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.dolby.dax.DolbyAudioEffect;
import com.dolby.daxappui.DAXApplication;
import com.dolby.daxappui.exploreDolby.FragExploreDolbyAtmos;
import com.dolby.daxservice.DaxService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, IDsFragObserver {
    private static AudioManager mAudioManager;
    private static Method methodGetDevicesForStream;
    private int mDevice;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Handler mHandler;
    private RelativeLayout mHdmiLayout;
    private RelativeLayout mPowerOffLayout;
    private String mProductVersion;
    private boolean mTabletLayout;
    private DolbyAudioEffect mDolbyAudio = null;
    private AlertDialog mResetConfirmationDialog;
    private AlertDialog mAppearanceDialog;
    private BroadcastReceiver mDolbyIntentReceiver = new BroadcastReceiver() { // from class: com.dolby.daxappui.MainActivity.1
@Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.d("MainActivity", "mDolbyIntentReceiver, action = " + action);
                if (action.equals("com.dolby.intent.action.DAP_PARAMS_UPDATE")) {
                    String stringExtra = intent.getStringExtra("event name");
                    char c = 65535;
                    switch (stringExtra.hashCode()) {
                        case -1146943738:
                            if (stringExtra.equals("profile_change")) {
                                c = 1;
                                break;
                            }
                            break;
                        case 173585614:
                            if (stringExtra.equals("ds_state_change")) {
                                c = 3;
                                break;
                            }
                            break;
                        case 893024970:
                            if (stringExtra.equals("reset_profile_setting")) {
                                c = 2;
                                break;
                            }
                            break;
                        case 1577092565:
                            if (stringExtra.equals("profile_setting_change")) {
                                c = 0;
                                break;
                            }
                            break;
                    }
                    if (c == 0 || c == 1) {
                        int intExtra = intent.getIntExtra("Integer Value", 0);
                        if (MainActivity.this.mDolbyAudio != null) {
                            try {
                                if (MainActivity.this.mDolbyAudio.getProfile() != intExtra) {
                                    return;
                                }
                            } catch (Exception e) {
                                Log.w("MainActivity", "Error getting profile in mDolbyIntentReceiver", e);
                                MainActivity.this.releaseDolbyAudio();
                            }
                        }
                        if (intExtra >= 0 && intExtra < 4) {
                            MainActivity.this.profileSettingsChanged(intExtra);
                            return;
                        }
                        Log.d("MainActivity", "profile index is out of 0~3");
                        return;
                    }
                    if (c != 2) {
                        if (c != 3) {
                            return;
                        }
                        MainActivity.this.dsPowerChanged(intent.getIntExtra("Integer Value", 0) > 0);
                        return;
                    } else {
                        int intExtra2 = intent.getIntExtra("Integer Value", 0);
                        if (intExtra2 >= 0 && intExtra2 < 4) {
                            MainActivity.this.resetProfile(intExtra2);
                            return;
                        }
                        Log.d("MainActivity", "profile index is out of 0~3");
                        return;
                    }
                }
                if (action.equals("audio_server_restarted")) {
                    if (MainActivity.this.mDolbyAudio != null) {
                        try {
                            MainActivity.this.mDolbyAudio.release();
                        } catch (Exception ignored) {}
                        MainActivity.this.mDolbyAudio = null;
                    }
                    try {
                        MainActivity.this.mDolbyAudio = new DolbyAudioEffect(0, 0);
                        Log.d("MainActivity", "Dax effect recreate successfully");
                    } catch (Exception e) {
                        Log.e("MainActivity", "Failed to recreate Dax effect on audio_server_restarted", e);
                        MainActivity.this.mDolbyAudio = null;
                    }
                    if (MainActivity.this.getSupportFragmentManager().getBackStackEntryCount() == 0) {
                        MainActivity.this.setInitUIState();
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Exception found in MainActivity::onReceive()");
                e.printStackTrace();
            }
        }
    };
    private final BroadcastReceiver mDeviceReceiver = new BroadcastReceiver() { // from class: com.dolby.daxappui.MainActivity.2
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            Log.d("MainActivity", "mDeviceReceiver, action = " + action);
            boolean update = false;
            boolean deviceAlreadySelected = false;
            if ("android.intent.action.HEADSET_PLUG".equals(action)) {
                update = true;
                if (extras.getInt("state") != 0) {
                    mDevice = 8;
                    deviceAlreadySelected = true;
                }
            } else if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra("device");
                if (device != null) {
                    for (int index = 0; index < device.getInterfaceCount(); index++) {
                        if (device.getInterface(index).getInterfaceClass() == 1) {
                            mDevice = 16384;
                            update = true;
                        }
                    }
                }
                deviceAlreadySelected = true;
            } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
                if ((getActiveDevices() & 67125248) != 0) {
                    try { Thread.sleep(1000L); } catch (Exception e) { e.printStackTrace(); }
                    update = true;
                }
            } else if ("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
                int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", Integer.MIN_VALUE);
                if (state == 2) {
                    mDevice = 128;
                    update = true;
                    deviceAlreadySelected = true;
                } else if (state == 0 && (getActiveDevices() & 896) != 0) {
                    try { Thread.sleep(200L); } catch (Exception e) { e.printStackTrace(); }
                    update = true;
                }
            } else if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(action)) {
                if (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1) == 10
                        && (getActiveDevices() & 896) != 0) {
                    try { Thread.sleep(1000L); } catch (Exception e) { e.printStackTrace(); }
                    update = true;
                }
            } else if ("android.intent.action.HDMI_PLUGGED".equals(action)
                    || "android.media.action.HDMI_AUDIO_PLUG".equals(action)) {
                if (intent.getBooleanExtra("state", false)) {
                    if (mDrawerLayout != null) {
                        ViewGroup parent = (ViewGroup) mHdmiLayout.getParent();
                        if (parent != null) parent.removeView(mHdmiLayout);
                        mDrawerLayout.addView(mHdmiLayout);
                    }
                } else if (mHdmiLayout != null) {
                    ViewGroup parent = (ViewGroup) mHdmiLayout.getParent();
                    if (parent != null) parent.removeView(mHdmiLayout);
                }
                update = true;
                deviceAlreadySelected = true;
            }
            if (update) {
                if (!deviceAlreadySelected) mDevice = getActiveDevices();
                setGeqViewEnabled();
            }
        }
    };

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
public void profileSettingsChanged(int i) {
        FragMainContent fragMainContent;
        FragProfilePanel fragProfilePanel;
        // Never let a profile refresh re-select a tab while Dolby is off. The
        // engine still remembers its last active profile, but the UI contract is
        // that no profile pill is shown until Dolby is enabled again.
        if (this.mDolbyAudio != null) {
            try {
                if (!this.mDolbyAudio.getDsOn()) {
                    return;
                }
            } catch (Exception e) {
                Log.w("MainActivity", "Error checking getDsOn in profileSettingsChanged", e);
                releaseDolbyAudio();
            }
        }
        if (getSupportFragmentManager().getBackStackEntryCount() != 0 || (fragMainContent = (FragMainContent) getSupportFragmentManager().findFragmentById(R.id.containerView)) == null) {
            return;
        }
        FragProfilePresets fragProfilePresets = (FragProfilePresets) fragMainContent.getChildFragmentManager().findFragmentById(R.id.fragProfilePanel);
        if (fragProfilePresets != null) {
            fragProfilePresets.updateProfileSettings(i);
        }
        if (!this.mTabletLayout || (fragProfilePanel = (FragProfilePanel) fragMainContent.getChildFragmentManager().findFragmentById(R.id.fragProfilePanelTablet)) == null) {
            return;
        }
        fragProfilePanel.updateProfilePanel(i);
    }
public int getActiveDevices() {
        try {
            return ((Integer) methodGetDevicesForStream.invoke(mAudioManager, 3)).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    void setGeqViewEnabled() {
        FragMainContent fragMainContent;
        FragProfilePanel fragProfilePanel;
        DolbyAudioEffect dolbyAudioEffect;
        if (getSupportFragmentManager().getBackStackEntryCount() != 0 || (fragMainContent = (FragMainContent) getSupportFragmentManager().findFragmentById(R.id.containerView)) == null) {
            return;
        }
        FragProfilePresets fragProfilePresets = (FragProfilePresets) fragMainContent.getChildFragmentManager().findFragmentById(R.id.fragProfilePanel);
        if (fragProfilePresets != null && (dolbyAudioEffect = getDolbyAudioEffect()) != null) {
            try {
                fragProfilePresets.setGeqViewEnabled(dolbyAudioEffect.getProfile());
            } catch (Exception e) {
                Log.w("MainActivity", "Error getting profile in setGeqViewEnabled", e);
                releaseDolbyAudio();
            }
        }
        if (!this.mTabletLayout || (fragProfilePanel = (FragProfilePanel) fragMainContent.getChildFragmentManager().findFragmentById(R.id.fragProfilePanelTablet)) == null) {
            return;
        }
        fragProfilePanel.setGeqViewEnabled();
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        ViewGroup viewGroup;
        ActionBar supportActionBar;
        DAXApplication.applyAppearanceMode(this,
                getSharedPreferences(DAXApplication.APPEARANCE_PREFERENCES, MODE_PRIVATE)
                        .getInt(DAXApplication.APPEARANCE_MODE, DAXApplication.APPEARANCE_DARK));
        super.onCreate(bundle);
        this.mProductVersion = DAXApplication.getInstance().getProductVersion();
        String str = this.mProductVersion;
        if (str == null) {
            Log.e("MainActivity", "Could not get the product version");
            return;
        }
        String substring = str.substring(0, 3);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.dolby.intent.action.DAP_PARAMS_UPDATE");
        intentFilter.addAction("audio_server_restarted");
        registerReceiver(this.mDolbyIntentReceiver, intentFilter,
                DaxService.PERMISSION, null);
        try {
            this.mDolbyAudio = new DolbyAudioEffect(0, 0);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to create DolbyAudioEffect in onCreate", e);
            this.mDolbyAudio = null;
        }
        try {
            Constructor<?>[] declaredConstructors = AudioManager.class.getDeclaredConstructors();
            int length = declaredConstructors.length;
            Constructor<?> constructor = null;
            for (int i = 0; i < length; i++) {
                constructor = declaredConstructors[i];
                if (constructor.getGenericParameterTypes().length == 0) {
                    break;
                }
            }
            constructor.setAccessible(true);
            mAudioManager = (AudioManager) constructor.newInstance(new Object[0]);
            methodGetDevicesForStream = mAudioManager.getClass().getDeclaredMethod("getDevicesForStream", Integer.TYPE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.HEADSET_PLUG");
        intentFilter2.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter2.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        intentFilter2.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter2.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        if (28 <= Build.VERSION.SDK_INT) {
            intentFilter2.addAction("android.media.action.HDMI_AUDIO_PLUG");
        } else {
            try {
                Field field = Class.forName("android.view.WindowManagerPolicy").getField("ACTION_HDMI_PLUGGED");
                if (field.getType() == String.class) {
                    intentFilter2.addAction((String) field.get(null));
                }
            } catch (ClassNotFoundException e2) {
                throw new RuntimeException(e2);
            } catch (Exception e3) {
                e3.printStackTrace();
            }
        }
        registerReceiver(this.mDeviceReceiver, intentFilter2);
        this.mTabletLayout = getResources().getBoolean(R.bool.tabletLayout);
        // Let the window determine orientation so short split-screen windows
        // are laid out at their actual size instead of portrait compatibility bounds.
        setContentView(R.layout.activity_main);
        updateSystemBarAppearance();
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorStatusBar, getTheme()));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorNavBackground, getTheme()));
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        this.mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.mDrawerToggle = new ActionBarDrawerToggle(this, this.mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        this.mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() { // from class: com.dolby.daxappui.-$$Lambda$MainActivity$HSGO6IFSkcelc_691wXyeHK-KLk
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.lambda$onCreate$0$MainActivity(view);
            }
        });
        this.mDrawerLayout.addDrawerListener(this.mDrawerToggle);
        this.mDrawerToggle.syncState();
        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);
        FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
        beginTransaction.replace(R.id.containerView, new FragMainContent());
        beginTransaction.commit();
        this.mPowerOffLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.power_off, (ViewGroup) null).findViewById(R.id.poweroffLayout);
        this.mPowerOffLayout.setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
        if (this.mDrawerLayout != null) {
            ViewGroup viewGroup3 = (ViewGroup) this.mPowerOffLayout.getParent();
            if (viewGroup3 != null) {
                viewGroup3.removeView(this.mPowerOffLayout);
            }
            this.mDrawerLayout.addView(this.mPowerOffLayout);
        }
        if (this.mTabletLayout && (supportActionBar = getSupportActionBar()) != null) {
            supportActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorTitleBar, getTheme())));
        }
        TextView textView = (TextView) findViewById(R.id.powerOffText);
        if (substring.equals("DS1")) {
            textView.setText(getResources().getString(R.string.ds1_power_off_text));
        } else {
            textView.setText(getResources().getString(R.string.power_off_text));
        }
        this.mHandler = new Handler();
        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.mainLayout);
        linearLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.dolby.daxappui.MainActivity.3
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                linearLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                View logoView = MainActivity.this.findViewById(R.id.dsLogoText);
                if (logoView instanceof TextView) {
                    TextView textView2 = (TextView) logoView;
                    if (MainActivity.this.mProductVersion.substring(0, 3).equals("DS1")) {
                        navigationView.getMenu().findItem(R.id.nav_exploredolby).setVisible(false);
                        textView2.setText(MainActivity.this.getResources().getString(R.string.app_name_ds1));
                    } else {
                        textView2.setText(MainActivity.this.getResources().getString(R.string.app_title));
                    }
                }
            }
        });
        this.mHdmiLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.hdmi_page, (ViewGroup) null).findViewById(R.id.hdmi_layout);
        this.mHdmiLayout.setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
        if (this.mHdmiLayout != null && (viewGroup = (ViewGroup) this.mHdmiLayout.getParent()) != null) {
            viewGroup.removeView(this.mHdmiLayout);
        }
        navigationView.getMenu();
        final MenuItem launcherItem = navigationView.getMenu().findItem(R.id.nav_launcher_icon);
        if (launcherItem != null) {
            final ComponentName aliasComponent = new ComponentName(this, "com.dolby.daxappui.MainActivityLauncherAlias");
            final View actionView = MenuItemCompat.getActionView(launcherItem);
            final Switch switchView = (actionView != null) ? (Switch) actionView.findViewById(R.id.nav_switch_launcher_icon) : null;
            if (switchView != null) {
                int enabledState = getPackageManager().getComponentEnabledSetting(aliasComponent);
                boolean isHidden = (enabledState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                switchView.setChecked(isHidden);
                switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        int newState = isChecked
                                ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                        getPackageManager().setComponentEnabledSetting(aliasComponent, newState, PackageManager.DONT_KILL_APP);
                    }
                });
            }
        }
    }

    public /* synthetic */ void lambda$onCreate$0$MainActivity(View view) {
        onBackPressed();
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        Log.i("MainActivity", "onPause()");
        super.onPause();
        DolbyAudioEffect dolbyAudioEffect = this.mDolbyAudio;
        if (dolbyAudioEffect != null) {
            try {
                dolbyAudioEffect.release();
            } catch (Exception ignored) {}
            this.mDolbyAudio = null;
        }
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        Log.i("MainActivity", "onResume()");
        super.onResume();
        int appearanceMode = getSharedPreferences(DAXApplication.APPEARANCE_PREFERENCES, MODE_PRIVATE)
                .getInt(DAXApplication.APPEARANCE_MODE, DAXApplication.APPEARANCE_DARK);
        if (appearanceMode == DAXApplication.APPEARANCE_SYSTEM) {
            boolean currentlyDark = (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            if (currentlyDark != DAXApplication.systemUsesDarkTheme(this)) {
                DAXApplication.applyAppearanceMode(this, appearanceMode);
                recreate();
                return;
            }
        }
        if (this.mDolbyAudio == null) {
            try {
                this.mDolbyAudio = new DolbyAudioEffect(0, 0);
                if (!this.mDolbyAudio.hasControl()) {
                    Log.w("MainActivity", "Dolby audio effect is out of control");
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Failed to create DolbyAudioEffect in onResume", e);
                this.mDolbyAudio = null;
            }
        }
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            setInitUIState();
        }
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.app.Activity
    public void onStart() {
        super.onStart();
        this.mDevice = getActiveDevices();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        DolbyAudioEffect dolbyAudioEffect;
        Log.i("MainActivity", "onWindowFocusChanged(" + z + ")");
        super.onWindowFocusChanged(z);
        // An app-owned dialog has its own window, so showing it temporarily removes
        // focus from the activity. Keep the effect alive while the reset dialog is
        // visible; Continue needs this same controlled effect to perform the reset.
        if (!z && ((this.mResetConfirmationDialog != null && this.mResetConfirmationDialog.isShowing())
                || (this.mAppearanceDialog != null && this.mAppearanceDialog.isShowing()))) {
            return;
        }
        if (!z || this.mDolbyAudio != null) {
            if (z || (dolbyAudioEffect = this.mDolbyAudio) == null) {
                return;
            }
            try {
                dolbyAudioEffect.release();
            } catch (Exception ignored) {}
            this.mDolbyAudio = null;
            return;
        }
        try {
            this.mDolbyAudio = new DolbyAudioEffect(0, 0);
            if (!this.mDolbyAudio.hasControl()) {
                Log.w("MainActivity", "Dolby audio effect is out of control");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to create DolbyAudioEffect in onWindowFocusChanged", e);
            this.mDolbyAudio = null;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            setInitUIState();
        }
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        if (this.mResetConfirmationDialog != null) {
            this.mResetConfirmationDialog.dismiss();
            this.mResetConfirmationDialog = null;
        }
        if (this.mAppearanceDialog != null) {
            this.mAppearanceDialog.dismiss();
            this.mAppearanceDialog = null;
        }
        DolbyAudioEffect dolbyAudioEffect = this.mDolbyAudio;
        if (dolbyAudioEffect != null) {
            try {
                dolbyAudioEffect.release();
            } catch (Exception ignored) {}
            this.mDolbyAudio = null;
        }
        unregisterReceiver(this.mDolbyIntentReceiver);
        unregisterReceiver(this.mDeviceReceiver);
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    public void onBackPressed() {
        if (this.mDrawerLayout.isDrawerOpen(8388611)) {
            this.mDrawerLayout.closeDrawer(8388611);
            return;
        }
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        if (supportFragmentManager.getBackStackEntryCount() == 0) {
            super.onBackPressed();
            return;
        }
        supportFragmentManager.executePendingTransactions();
        supportFragmentManager.popBackStack();
        ((RelativeLayout) findViewById(R.id.dolbyToolbar)).setVisibility(View.VISIBLE);
        View logoView = findViewById(R.id.dsLogoText);
        if (logoView != null) {
            logoView.setVisibility(View.VISIBLE);
        }
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            if (this.mTabletLayout) {
                supportActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorTitleBar, getTheme())));
            } else {
                supportActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorTitleBar, getTheme())));
            }
            supportActionBar.setDisplayShowTitleEnabled(false);
        }
        supportFragmentManager.executePendingTransactions();
        if (supportFragmentManager.getBackStackEntryCount() == 0) {
            this.mDrawerToggle.setDrawerIndicatorEnabled(true);
            this.mDrawerLayout.setDrawerLockMode(0);
            setInitUIState();
        }
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Menu menu = ((NavigationView) findViewById(R.id.nav_view)).getMenu();
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).isChecked()) {
                bundle.putInt("currentNavItem", i);
            }
        }
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // android.support.design.widget.NavigationView.OnNavigationItemSelectedListener
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_exploredolby) {
            FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
            beginTransaction.replace(R.id.containerView, new FragExploreDolbyAtmos());
            beginTransaction.addToBackStack(null);
            beginTransaction.commit();
        } else if (itemId == R.id.nav_launcher_icon) {
            MenuItem launcherItem = ((NavigationView) findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_launcher_icon);
            if (launcherItem != null) {
                View actionView = MenuItemCompat.getActionView(launcherItem);
                Switch switchView = (actionView != null) ? (Switch) actionView.findViewById(R.id.nav_switch_launcher_icon) : null;
                if (switchView != null) {
                    switchView.toggle();
                }
            }
            return true;
        } else if (itemId == R.id.nav_appearance) {
            menuItem.setChecked(false);
            this.mDrawerLayout.closeDrawer(8388611);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.showAppearanceDialog();
                }
            }, 250L);
            return true;
        } else if (itemId == R.id.nav_reset) {
            menuItem.setChecked(false);
            this.mDrawerLayout.closeDrawer(8388611);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.showResetConfirmationDialog();
                }
            }, 250L);
            return true;
        }
        if (itemId == R.id.nav_exploredolby) {
            ((RelativeLayout) findViewById(R.id.dolbyToolbar)).setVisibility(View.GONE);
            View logoView2 = findViewById(R.id.dsLogoText);
            if (logoView2 != null) {
                logoView2.setVisibility(View.GONE);
            }
            ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null) {
                supportActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorActionBar, getTheme())));
                supportActionBar.setDisplayShowTitleEnabled(true);
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(menuItem.getTitle());
                spannableStringBuilder.setSpan(new TypefaceSpan("fonts/SourceSansPro-Regular.otf"), 0, spannableStringBuilder.length(), 33);
                supportActionBar.setTitle(spannableStringBuilder);
            }
            this.mDrawerToggle.setDrawerIndicatorEnabled(false);
            this.mDrawerLayout.setDrawerLockMode(1);
        }
        this.mHandler.postDelayed(new Runnable() { // from class: com.dolby.daxappui.MainActivity.4
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.mDrawerLayout.closeDrawer(8388611);
            }
        }, 250L);
        return true;
    }

    private void showResetConfirmationDialog() {
        if (this.mResetConfirmationDialog != null && this.mResetConfirmationDialog.isShowing()) {
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reset_confirmation, null);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        this.mResetConfirmationDialog = dialog;

        dialogView.findViewById(R.id.resetContinueButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DsClientSettings.INSTANCE.resetAllProfileSpecificSettings(MainActivity.this);
                GeqView.clearAllInteractionLocks(MainActivity.this);
                DolbyAudioEffect effect = MainActivity.this.getDolbyAudioEffect();
                if (effect != null) {
                    try {
                        int activeProfile = effect.getProfile();
                        if (activeProfile >= 0 && activeProfile < 4) {
                            MainActivity.this.profileSettingsChanged(activeProfile);
                        }
                    } catch (Exception e) {
                        Log.w("MainActivity", "Error resetting profiles", e);
                        MainActivity.this.releaseDolbyAudio();
                    }
                }
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(dialogInterface -> mResetConfirmationDialog = null);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.72f;
            window.setAttributes(attributes);
            window.setWindowAnimations(R.style.ResetDialogAnimation);
            int horizontalMargin = Math.round(24 * getResources().getDisplayMetrics().density);
            int maxWidth = Math.round(420 * getResources().getDisplayMetrics().density);
            int availableWidth = getResources().getDisplayMetrics().widthPixels - (horizontalMargin * 2);
            window.setLayout(Math.min(availableWidth, maxWidth), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showAppearanceDialog() {
        if (this.mAppearanceDialog != null && this.mAppearanceDialog.isShowing()) {
            return;
        }

        final int[] modes = {
                DAXApplication.APPEARANCE_LIGHT,
                DAXApplication.APPEARANCE_DARK,
                DAXApplication.APPEARANCE_SYSTEM
        };
        int savedMode = getSharedPreferences(DAXApplication.APPEARANCE_PREFERENCES, MODE_PRIVATE)
                .getInt(DAXApplication.APPEARANCE_MODE, DAXApplication.APPEARANCE_DARK);
        int checkedItem = savedMode == DAXApplication.APPEARANCE_LIGHT ? 0
                : savedMode == DAXApplication.APPEARANCE_SYSTEM ? 2 : 1;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_appearance, null);
        final View[] options = {
                dialogView.findViewById(R.id.appearanceOptionLight),
                dialogView.findViewById(R.id.appearanceOptionDark),
                dialogView.findViewById(R.id.appearanceOptionSystem)
        };
        final ImageView[] checks = {
                dialogView.findViewById(R.id.appearanceCheckLight),
                dialogView.findViewById(R.id.appearanceCheckDark),
                dialogView.findViewById(R.id.appearanceCheckSystem)
        };

        updateAppearanceSelection(options, checks, checkedItem);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        this.mAppearanceDialog = dialog;

        for (int i = 0; i < options.length; i++) {
            final int which = i;
            options[i].setOnClickListener(view -> {
                updateAppearanceSelection(options, checks, which);
                int selectedMode = modes[which];
                getSharedPreferences(DAXApplication.APPEARANCE_PREFERENCES, MODE_PRIVATE)
                        .edit()
                        .putInt(DAXApplication.APPEARANCE_MODE, selectedMode)
                        .apply();
                dialog.dismiss();
                DAXApplication.applyAppearanceMode(MainActivity.this, selectedMode);
                recreate();
            });
        }

        dialogView.findViewById(R.id.appearanceCancelButton)
                .setOnClickListener(view -> dialog.dismiss());
        dialog.setOnDismissListener(dialogInterface -> mAppearanceDialog = null);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.64f;
            window.setAttributes(attributes);
            window.setWindowAnimations(R.style.ResetDialogAnimation);
            int horizontalMargin = Math.round(24 * getResources().getDisplayMetrics().density);
            int maxWidth = Math.round(420 * getResources().getDisplayMetrics().density);
            int availableWidth = getResources().getDisplayMetrics().widthPixels - (horizontalMargin * 2);
            window.setLayout(Math.min(availableWidth, maxWidth), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void updateAppearanceSelection(View[] options, ImageView[] checks, int selectedIndex) {
        for (int i = 0; i < options.length; i++) {
            boolean selected = i == selectedIndex;
            options[i].setSelected(selected);
            checks[i].setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void updateSystemBarAppearance() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        boolean lightTheme = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        int visibility = getWindow().getDecorView().getSystemUiVisibility();
        if (lightTheme) {
            visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                visibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }

    private void setInitUIState() {
        DolbyAudioEffect dolbyAudioEffect;
        if (getSupportFragmentManager().getBackStackEntryCount() != 0 || (dolbyAudioEffect = getDolbyAudioEffect()) == null) {
            return;
        }
        try {
            dsPowerChanged(dolbyAudioEffect.getDsOn());
            int numOfProfiles = dolbyAudioEffect.getNumOfProfiles();
            int profile = dolbyAudioEffect.getProfile();
            if (4 <= profile && profile < numOfProfiles) {
                profile = 0;
            }
            chooseProfile(profile);
            profileSettingsChanged(profile);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to setInitUIState", e);
        }
    }

    public void dsPowerChanged(boolean z) {
        FragMainContent fragMainContent;
        FragMainContent fragMainContent2;
        ViewGroup viewGroup;
        TabLayout tabLayout = (TabLayout) findViewById(R.id.profiletable);
        ViewPager viewPager = (ViewPager) findViewById(R.id.profileViewpager);
        ListView listView = (ListView) findViewById(R.id.presetsListView);
        ImageView imageView = (ImageView) findViewById(R.id.powerButtonOn);
        if (z) {
            RelativeLayout relativeLayout = this.mPowerOffLayout;
            if (relativeLayout != null && (viewGroup = (ViewGroup) relativeLayout.getParent()) != null) {
                viewGroup.removeView(this.mPowerOffLayout);
            }
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.btn_power_on_titlebar, getTheme()));
            ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null) {
                if (this.mTabletLayout) {
                    supportActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorTitleBar, getTheme())));
                } else {
                    supportActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorTitleBar, getTheme())));
                }
                supportActionBar.setDisplayShowHomeEnabled(true);
                supportActionBar.setDisplayHomeAsUpEnabled(true);
                supportActionBar.setHomeButtonEnabled(true);
            }
            if (tabLayout != null) {
                LinearLayout linearLayout = (LinearLayout) tabLayout.getChildAt(0);
                tabLayout.setBackgroundColor(getResources().getColor(R.color.colorTitleBar, getTheme()));
                linearLayout.setEnabled(true);
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    linearLayout.getChildAt(i).setClickable(true);
                }
                tabLayout.setTabTextColors(getResources().getColor(R.color.colorProfileTextOff, getTheme()), getResources().getColor(R.color.colorSelectedTabText, getTheme()));
                tabLayout.setTabIconTint(ContextCompat.getColorStateList(this, R.color.profile_tab_icon_tint_on));
                tabLayout.setSelectedTabIndicatorHeight(0);

                // Support TabLayout can keep a stale icon drawable state after the
                // Dolby-off tint list is replaced. Re-sync the visual selection to
                // the profile that is actually active in the Dolby engine.
                int activeProfile = tabLayout.getSelectedTabPosition();
                if (this.mDolbyAudio != null) {
                    try {
                        int engineProfile = this.mDolbyAudio.getProfile();
                        if (engineProfile >= 0 && engineProfile < tabLayout.getTabCount()) {
                            activeProfile = engineProfile;
                        }
                    } catch (RuntimeException e) {
                        Log.w("MainActivity", "Unable to read active profile while refreshing tabs", e);
                    }
                }
                final int profileToRefresh = activeProfile;
                if (profileToRefresh >= 0 && profileToRefresh < tabLayout.getTabCount()) {
                    TabLayout.Tab activeTab = tabLayout.getTabAt(profileToRefresh);
                    if (activeTab != null && tabLayout.getSelectedTabPosition() != profileToRefresh) {
                        activeTab.select();
                    }
                }
                refreshProfileTabVisualState(tabLayout, profileToRefresh, true);
                tabLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshProfileTabVisualState(tabLayout, profileToRefresh, true);
                    }
                });
            }
            if (viewPager != null) {
                viewPager.setVisibility(0);
            }
            if (listView != null) {
                listView.setEnabled(true);
            }
            if (getSupportFragmentManager().getBackStackEntryCount() == 0 && (fragMainContent2 = (FragMainContent) getSupportFragmentManager().findFragmentById(R.id.containerView)) != null && fragMainContent2.getView() != null && this.mTabletLayout) {
                FragProfilePanel fragProfilePanel = (FragProfilePanel) fragMainContent2.getChildFragmentManager().findFragmentById(R.id.fragProfilePanelTablet);
                if (fragProfilePanel != null && fragProfilePanel.getView() != null) {
                    fragProfilePanel.getView().setVisibility(0);
                }
                ((RelativeLayout) fragMainContent2.getView().findViewById(R.id.titleBar)).setBackgroundColor(getResources().getColor(R.color.colorTitleBar, getTheme()));
            }
            this.mDrawerLayout.setDrawerLockMode(0);
            this.mDrawerToggle.setDrawerIndicatorEnabled(true);
            this.mDrawerToggle.getDrawerArrowDrawable().setColor(
                    getResources().getColor(R.color.colorNavText, getTheme()));
            this.mDrawerToggle.syncState();
            return;
        }
        this.mDrawerLayout.setDrawerLockMode(1);
        if (getSupportFragmentManager().getBackStackEntryCount() == 0 && (fragMainContent = (FragMainContent) getSupportFragmentManager().findFragmentById(R.id.containerView)) != null && fragMainContent.getView() != null && this.mTabletLayout) {
            FragProfilePanel fragProfilePanel2 = (FragProfilePanel) fragMainContent.getChildFragmentManager().findFragmentById(R.id.fragProfilePanelTablet);
            if (fragProfilePanel2 != null && fragProfilePanel2.getView() != null) {
                fragProfilePanel2.getView().setVisibility(4);
            }
            ((RelativeLayout) fragMainContent.getView().findViewById(R.id.titleBar)).setBackground(new ColorDrawable(getResources().getColor(R.color.dup_0x7f05002d, getTheme())));
        }
        if (listView != null) {
            listView.setEnabled(false);
        }
        if (viewPager != null) {
            viewPager.setVisibility(4);
        }
        if (tabLayout != null) {
            tabLayout.setBackgroundColor(getResources().getColor(R.color.colorMainBackground, getTheme()));
            LinearLayout linearLayout2 = (LinearLayout) tabLayout.getChildAt(0);
            linearLayout2.setEnabled(false);
            for (int i2 = 0; i2 < linearLayout2.getChildCount(); i2++) {
                linearLayout2.getChildAt(i2).setClickable(false);
            }
            tabLayout.setTabTextColors(getResources().getColor(R.color.colorProfileTextOff, getTheme()), getResources().getColor(R.color.colorProfileTextOff, getTheme()));
            tabLayout.setTabIconTint(ContextCompat.getColorStateList(this, R.color.profile_tab_icon_tint_off));
            tabLayout.setSelectedTabIndicatorHeight(0);
            // Clear any cached selected drawable state while Dolby is disabled.
            refreshProfileTabVisualState(tabLayout, -1, false);
            // TabLayout can restore its internally selected child during a pending
            // layout pass (notably on a cold app launch). Clear it once more after
            // layout so the remembered engine profile never leaves a stale pill.
            tabLayout.post(new Runnable() {
                @Override
                public void run() {
                    refreshProfileTabVisualState(tabLayout, -1, false);
                }
            });
        }
        ActionBar supportActionBar2 = getSupportActionBar();
        if (supportActionBar2 != null) {
            supportActionBar2.setDisplayHomeAsUpEnabled(false);
            supportActionBar2.setDisplayShowHomeEnabled(true);
            supportActionBar2.setHomeButtonEnabled(false);
            this.mDrawerToggle.getDrawerArrowDrawable().setColor(
                    getResources().getColor(R.color.colorProfileTextOff, getTheme()));
            this.mDrawerToggle.syncState();
            if (this.mTabletLayout) {
                supportActionBar2.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.dup_0x7f05002d, getTheme())));
            } else {
                supportActionBar2.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorNavBackground, getTheme())));
            }
        }
        if (this.mDrawerLayout != null) {
            ViewGroup viewGroup2 = (ViewGroup) this.mPowerOffLayout.getParent();
            if (viewGroup2 != null) {
                viewGroup2.removeView(this.mPowerOffLayout);
            }
            this.mDrawerLayout.addView(this.mPowerOffLayout);
        }
        imageView.setImageDrawable(getResources().getDrawable(R.drawable.btn_power_off_titlebar, getTheme()));
    }

    /**
     * Keeps the profile tab text/icon drawable state in sync with the Dolby engine.
     * Support Design 28 TabLayout may retain a stale icon tint when its tint list is
     * changed while the tab strip is disabled, so each child state is refreshed.
     */
    private void refreshProfileTabVisualState(TabLayout tabLayout, int selectedProfile, boolean dolbyEnabled) {
        if (tabLayout == null || tabLayout.getChildCount() == 0 || !(tabLayout.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout strip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < strip.getChildCount(); i++) {
            View tabView = strip.getChildAt(i);
            boolean selected = dolbyEnabled && i == selectedProfile;
            tabView.setSelected(selected);
            tabView.refreshDrawableState();

            if (tabView instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) tabView;
                for (int j = 0; j < group.getChildCount(); j++) {
                    View child = group.getChildAt(j);
                    child.setSelected(selected);
                    child.refreshDrawableState();
                    child.jumpDrawablesToCurrentState();
                    child.invalidate();
                }
            }
            tabView.jumpDrawablesToCurrentState();
            tabView.invalidate();
        }
        tabLayout.refreshDrawableState();
        tabLayout.invalidate();
    }


    @Override
    public void profileModificationChanged(int profile) {
        if (profile < 0 || profile >= 4 || getSupportFragmentManager().getBackStackEntryCount() != 0) {
            return;
        }
        FragMainContent main = (FragMainContent) getSupportFragmentManager().findFragmentById(R.id.containerView);
        if (main == null) {
            return;
        }
        FragProfilePresets presets = (FragProfilePresets) main.getChildFragmentManager()
                .findFragmentById(R.id.fragProfilePanel);
        if (presets != null) {
            presets.updateProfileModificationState(profile);
        }
        if (this.mTabletLayout) {
            FragProfilePanel panel = (FragProfilePanel) main.getChildFragmentManager()
                    .findFragmentById(R.id.fragProfilePanelTablet);
            if (panel != null) {
                panel.updateResetButtonState(profile);
            }
        }
    }

    public synchronized void releaseDolbyAudio() {
        if (this.mDolbyAudio != null) {
            try {
                this.mDolbyAudio.release();
            } catch (Exception ignored) {}
            this.mDolbyAudio = null;
        }
    }

    @Override // com.dolby.daxappui.IDsFragObserver
    public DolbyAudioEffect getDolbyAudioEffect() {
        if (this.mDolbyAudio == null) {
            try {
                this.mDolbyAudio = new DolbyAudioEffect(0, 0);
            } catch (Exception e) {
                Log.w("MainActivity", "Cannot initialize DolbyAudioEffect in getDolbyAudioEffect", e);
                this.mDolbyAudio = null;
            }
        }
        return this.mDolbyAudio;
    }

    @Override // com.dolby.daxappui.IDsFragObserver
    public void chooseProfile(int i) {
        DolbyAudioEffect dolbyAudioEffect = getDolbyAudioEffect();
        if (dolbyAudioEffect != null) {
            try {
                if (dolbyAudioEffect.hasControl()) {
                    if (dolbyAudioEffect.getProfile() != i) {
                        dolbyAudioEffect.setProfile(i);
                        return;
                    }
                    return;
                }
                Log.w("MainActivity", "Dolby audio effect is out of control in chooseProfile");
            } catch (Exception e) {
                Log.e("MainActivity", "Error in chooseProfile", e);
            }
        }
    }

    @Override // com.dolby.daxappui.IDsFragObserver
    public int getActivePort() {
        int i = this.mDevice;
        if (i == 2) {
            return Constants.DEFAULT_SPEAKER_PORT;
        }
        if (i == 4 || i == 8) {
            return Constants.DEFAULT_HEADPHONE_PORT;
        }
        if (i == 128) {
            return Constants.DEFAULT_BLUETOOTH_PORT;
        }
        if (i == 1024) {
            return Constants.DEFAULT_HDMI_PORT;
        }
        if (i == 16384) {
            return Constants.DEFAULT_USB_PORT;
        }
        if (i == 32768) {
            return Constants.DEFAULT_MIRACAST_PORT;
        }
        return Constants.DEFAULT_SPEAKER_PORT;
    }

    @Override // com.dolby.daxappui.IDsFragObserver
    public void resetProfile(int i) {
        profileSettingsChanged(i);
    }
}
