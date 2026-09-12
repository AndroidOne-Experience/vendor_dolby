package com.dolby.daxappui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import com.dolby.dax.DolbyAudioEffect;
import com.dolby.dax.DsParams;
import com.dolby.daxappui.EqualizerAdapter;

public class FragProfilePanel extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, EqualizerAdapter.IPresetListener, SeekBar.OnSeekBarChangeListener {
    private Context mContext;
    private EqualizerAdapter mEqualizerAdapter;
    private String[] mIeqName;
    private FrameLayout mMask;
    private String mProductVersion;
    private Switch mbeSwitch;
    private Switch mdeSwitch;
    private Switch msvSwitch;
    private Switch mvlSwitch;
    public int mNum = -1;
    private IDsFragObserver mFObserver = null;
    private boolean mbeState = false;
    private boolean mdeState = false;
    private boolean mvlState = false;
    private boolean msvState = false;

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public static FragProfilePanel newInstance(int i) {
        FragProfilePanel fragProfilePanel = new FragProfilePanel();
        Bundle bundle = new Bundle();
        bundle.putInt("num", i);
        fragProfilePanel.setArguments(bundle);
        return fragProfilePanel;
    }

    @Override // com.dolby.daxappui.EqualizerAdapter.IPresetListener
    public void onPresetChanged(int i, boolean z) {
        if (z && isProfileInteractionLocked()) {
            return;
        }
        TextView textView;
        if (getView() == null || (textView = (TextView) getView().findViewById(R.id.ieqName)) == null) {
            return;
        }
        textView.setText(this.mIeqName[i]);
        ImageView imageView = (ImageView) getView().findViewById(R.id.icon_off);
        LinearLayout linearLayout = (LinearLayout) getView().findViewById(R.id.equalizerListOffLayout);
        if (i == 3) {
            linearLayout.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_blue_shadow));
            imageView.setImageResource(R.drawable.ic_none_on_ieq);
        } else {
            linearLayout.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_shadow));
            imageView.setImageResource(R.drawable.ic_none_off_ieq);
        }
        if (z) {
            DsClientSettings.INSTANCE.setIeqPreset(this.mFObserver, i);
            this.mFObserver.profileModificationChanged(this.mNum);
        }
    }

    public void setGeqViewEnabled() {
        IDsFragObserver iDsFragObserver = this.mFObserver;
        if (iDsFragObserver != null) {
            boolean graphicEqualizerOn = DsClientSettings.INSTANCE.getGraphicEqualizerOn(iDsFragObserver, iDsFragObserver.getActivePort());
            View view = getView();
            if (graphicEqualizerOn) {
                if (view != null) {
                    ViewGroup viewGroup = (ViewGroup) this.mMask.getParent();
                    if (viewGroup != null) {
                        viewGroup.removeView(this.mMask);
                    }
                    TextView textView = (TextView) view.findViewById(R.id.geqText);
                    if (textView != null) {
                        textView.setTextColor(getResources().getColor(R.color.colorText, getContext().getTheme()));
                    }
                }
            } else if (view != null) {
                GeqViewLayout geqViewLayout = (GeqViewLayout) view.findViewById(R.id.GeqViewLayout);
                TextView textView2 = (TextView) view.findViewById(R.id.geqText);
                if (((ViewGroup) this.mMask.getParent()) == null) {
                    geqViewLayout.addView(this.mMask);
                }
                if (textView2 != null) {
                    textView2.setTextColor(getResources().getColor(R.color.colorGeqDisableText, getContext().getTheme()));
                }
            }
            if (view != null) {
                ((GeqView) view.findViewById(R.id.geqView)).invalidate();
                updateGeqLockButton(view);
            }
        }
    }

    @Override // android.support.v4.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mNum = getArguments() != null ? getArguments().getInt("num") : 1;
    }
@Override // android.support.v4.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mFObserver = (IDsFragObserver) context;
            this.mbeState = DsClientSettings.INSTANCE.getBassEnhancerOn(this.mFObserver);
            this.mvlState = DsClientSettings.INSTANCE.getVolumeLevelerOn(this.mFObserver);
            this.msvState = DsClientSettings.INSTANCE.getSpeakerVirtualizerOn(this.mFObserver);
            this.mProductVersion = DAXApplication.getInstance().getProductVersion();
            this.mIeqName = new String[4];
            this.mIeqName[0] = context.getString(R.string.bright);
            this.mIeqName[1] = context.getString(R.string.balanced);
            this.mIeqName[2] = context.getString(R.string.warm);
            this.mIeqName[3] = context.getString(R.string.no_effect);
            this.mContext = context;
            if (this.mbeSwitch != null) {
                this.mbeSwitch.setChecked(this.mbeState);
                this.mvlSwitch.setChecked(this.mvlState);
                this.msvSwitch.setChecked(this.msvState);
            }
            SharedPreferences sharedPreferences = this.mContext.getSharedPreferences("dax_dea_default", 0);
            SharedPreferences.Editor edit = sharedPreferences.edit();
            if (sharedPreferences.getBoolean("firstrun", true)) {
                sharedPreferences.edit().putBoolean("firstrun", false).apply();
                if (this.mFObserver.getDolbyAudioEffect() == null) {
                    Log.e("FragProfilePanel", "mAudioEffect is null when save default dea at firstrun");
                    return;
                }
                try {
                    edit.putInt("DialogEnhancerAmountForMovie", this.mFObserver.getDolbyAudioEffect().getDapParameter(1, DsParams.DialogEnhancementAmount.toInt())[0]);
                    edit.putInt("DialogEnhancerAmountForCustom", this.mFObserver.getDolbyAudioEffect().getDapParameter(3, DsParams.DialogEnhancementAmount.toInt())[0]);
                    edit.apply();
                } catch (Exception e) {
                    Log.e("FragProfilePanel", "Failed to save default dea at firstrun", e);
                }
            }
        } catch (ClassCastException unused) {
            throw new ClassCastException(context.toString() + " must implement IDsFragObserver");
        }
    }
@Override // android.support.v4.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        int i;
        int i2;
        View inflate = layoutInflater.inflate(R.layout.profile, (ViewGroup) null);
        String substring = this.mProductVersion.substring(0, 4);
        this.mMask = new FrameLayout(getContext());
        this.mMask.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        this.mMask.setBackgroundColor(getResources().getColor(R.color.colorMaskBackground, getContext().getTheme()));
        this.mMask.setClickable(true);
        boolean z = getResources().getBoolean(R.bool.tabletLayout);
        int i3 = this.mNum;
        if (z) {
            GridView gridView = (GridView) inflate.findViewById(R.id.equalizerListView);
            if (gridView != null) {
                LinearLayout linearLayout = (LinearLayout) inflate.findViewById(R.id.equalizerListOff);
                ImageView imageView = (ImageView) inflate.findViewById(R.id.icon_off);
                LinearLayout linearLayout2 = (LinearLayout) inflate.findViewById(R.id.equalizerListOffLayout);
                int ieqPreset = DsClientSettings.INSTANCE.getIeqPreset(this.mFObserver);
                ((TextView) inflate.findViewById(R.id.ieqName)).setText(this.mIeqName[ieqPreset]);
                if (ieqPreset != 3) {
                    linearLayout2.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_shadow));
                    imageView.setImageResource(R.drawable.ic_none_off_ieq);
                } else {
                    linearLayout2.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_blue_shadow));
                    imageView.setImageResource(R.drawable.ic_none_on_ieq);
                }
                linearLayout.setOnClickListener(this);
                this.mEqualizerAdapter = new EqualizerAdapter(this.mContext, this);
                gridView.setAdapter((ListAdapter) this.mEqualizerAdapter);
            }
            RelativeLayout relativeLayout = (RelativeLayout) inflate.findViewById(R.id.deView);
            if (substring.equals("DAX3")) {
                if (i3 == 1 || i3 == 3) {
                    SeekBar seekBar = (SeekBar) inflate.findViewById(R.id.deButton);
                    if (DsClientSettings.INSTANCE.getDialogEnhancerOn(this.mFObserver)) {
                        seekBar.setProgress(DsClientSettings.INSTANCE.getDialogEnhancerAmount(this.mFObserver));
                        i2 = 0;
                    } else {
                        i2 = 0;
                        seekBar.setProgress(0);
                    }
                    seekBar.setOnSeekBarChangeListener(this);
                    relativeLayout.setVisibility(i2);
                } else {
                    relativeLayout.setVisibility(8);
                }
            } else {
                relativeLayout.setVisibility(8);
            }
            GeqView geqView = (GeqView) inflate.findViewById(R.id.geqView);
            if (geqView != null && geqView.mSelectedBar == -1) {
                geqView.onUpdateGeqData();
            }
            View imageView2 = inflate.findViewById(R.id.profileResetButton);
            if (imageView2 != null) {
                if (DsClientSettings.INSTANCE.isProfileSpecificSettingsModified(this.mFObserver, i3)) {
                    imageView2.setVisibility(0);
                } else {
                    imageView2.setVisibility(4);
                }
                imageView2.setOnClickListener(this);
            }
        } else {
            RelativeLayout relativeLayout2 = (RelativeLayout) inflate.findViewById(R.id.ieqLayout);
            if (i3 == 2 || i3 == 3) {
                relativeLayout2.setVisibility(0);
                int ieqPreset2 = DsClientSettings.INSTANCE.getIeqPreset(this.mFObserver);
                ((TextView) inflate.findViewById(R.id.ieqName)).setText(this.mIeqName[ieqPreset2]);
                GridView gridView2 = (GridView) inflate.findViewById(R.id.equalizerListView);
                LinearLayout linearLayout3 = (LinearLayout) inflate.findViewById(R.id.equalizerListOff);
                ViewGroup.LayoutParams layoutParams = gridView2.getLayoutParams();
                int ieqViewWidth = (((int) DAXApplication.getIeqViewWidth(requireContext())) * 3) + (getResources().getDimensionPixelOffset(R.dimen.ieq_horizontal_spacing_normal) * 2);
                if (layoutParams == null) {
                    layoutParams = new ViewGroup.LayoutParams(ieqViewWidth, -2);
                } else {
                    layoutParams.width = ieqViewWidth;
                }
                gridView2.setLayoutParams(layoutParams);
                ViewGroup.LayoutParams layoutParams2 = linearLayout3.getLayoutParams();
                int ieqViewWidth2 = (int) DAXApplication.getIeqViewWidth(requireContext());
                if (layoutParams2 == null) {
                    layoutParams2 = new ViewGroup.LayoutParams(ieqViewWidth2, -2);
                } else {
                    layoutParams2.width = ieqViewWidth2;
                }
                linearLayout3.setLayoutParams(layoutParams2);
                ImageView imageView3 = (ImageView) inflate.findViewById(R.id.icon_off);
                LinearLayout linearLayout4 = (LinearLayout) inflate.findViewById(R.id.equalizerListOffLayout);
                if (ieqPreset2 == 3) {
                    linearLayout4.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_blue_shadow));
                    imageView3.setImageResource(R.drawable.ic_none_on_ieq);
                } else {
                    linearLayout4.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_shadow));
                    imageView3.setImageResource(R.drawable.ic_none_off_ieq);
                }
                linearLayout3.setOnClickListener(this);
                this.mEqualizerAdapter = new EqualizerAdapter(this.mContext, this);
                gridView2.setAdapter((ListAdapter) this.mEqualizerAdapter);
                this.mEqualizerAdapter.setIeqSelection(ieqPreset2, false);
            } else {
                relativeLayout2.setVisibility(8);
            }
            RelativeLayout relativeLayout3 = (RelativeLayout) inflate.findViewById(R.id.deView);
            if (substring.equals("DAX3")) {
                if (i3 == 1 || i3 == 3) {
                    relativeLayout3.setVisibility(0);
                    this.mdeState = DsClientSettings.INSTANCE.getDialogEnhancerOn(this.mFObserver);
                    SeekBar seekBar2 = (SeekBar) inflate.findViewById(R.id.deButton);
                    if (this.mdeState) {
                        seekBar2.setVisibility(0);
                        seekBar2.setProgress(DsClientSettings.INSTANCE.getDialogEnhancerAmount(this.mFObserver));
                    } else {
                        seekBar2.setVisibility(8);
                    }
                    seekBar2.setOnSeekBarChangeListener(this);
                } else {
                    relativeLayout3.setVisibility(8);
                }
            } else {
                relativeLayout3.setVisibility(8);
            }
            ((TextView) inflate.findViewById(R.id.profileDescription)).setVisibility(8);
            GeqView geqView2 = (GeqView) inflate.findViewById(R.id.geqView);
            if (geqView2 != null && geqView2.mSelectedBar == -1) {
                geqView2.onUpdateGeqData();
            }
            boolean graphicEqualizerOn = DsClientSettings.INSTANCE.getGraphicEqualizerOn(this.mFObserver, this.mFObserver.getActivePort());
            TextView textView = (TextView) inflate.findViewById(R.id.geqText);
            if (graphicEqualizerOn) {
                ViewGroup viewGroup2 = (ViewGroup) this.mMask.getParent();
                if (viewGroup2 != null) {
                    viewGroup2.removeView(this.mMask);
                }
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.colorText, getContext().getTheme()));
                }
            } else {
                GeqViewLayout relativeLayout4 = (GeqViewLayout) inflate.findViewById(R.id.GeqViewLayout);
                if (((ViewGroup) this.mMask.getParent()) == null) {
                    relativeLayout4.addView(this.mMask);
                }
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.colorSwitchDisableText, getContext().getTheme()));
                }
            }
            View imageView4 = inflate.findViewById(R.id.profileResetButton);
            if (imageView4 != null) {
                if (DsClientSettings.INSTANCE.isProfileSpecificSettingsModified(this.mFObserver, i3)) {
                    imageView4.setVisibility(0);
                } else {
                    imageView4.setVisibility(4);
                }
                imageView4.setOnClickListener(this);
            }
            inflate.setTag(Integer.valueOf(i3));
        }
        View geqLockButton = inflate.findViewById(R.id.geqLockButton);
        if (geqLockButton != null) {
            geqLockButton.setOnClickListener(this);
        }
        updateGeqLockButton(inflate);

        this.mvlSwitch = (Switch) inflate.findViewById(R.id.vlButton);
        this.mvlSwitch.setOnCheckedChangeListener(this);
        this.mvlSwitch.setChecked(this.mvlState);
        this.mbeSwitch = (Switch) inflate.findViewById(R.id.beButton);
        this.mbeSwitch.setOnCheckedChangeListener(this);
        this.mbeSwitch.setChecked(this.mbeState);
        this.msvSwitch = (Switch) inflate.findViewById(R.id.svButton);
        this.msvSwitch.setOnCheckedChangeListener(this);
        Switch r2 = this.msvSwitch; // Smali: msvSwitch is android.widget.Switch.
        r2.setChecked(this.msvState);
        r2.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
        this.mdeSwitch = (Switch) inflate.findViewById(R.id.deButtonSwitch);
        if (this.mdeSwitch != null) {
            this.mdeSwitch.setOnCheckedChangeListener(this);
            this.mdeSwitch.setChecked(this.mdeState);
        }
        return inflate;
    }

    private void updateGeqLockButton(View root) {
        if (root == null) {
            return;
        }
        GeqView geqView = (GeqView) root.findViewById(R.id.geqView);
        View lockButton = root.findViewById(R.id.geqLockButton);
        if (geqView == null || lockButton == null) {
            return;
        }
        geqView.setInteractionProfile(this.mNum);
        boolean locked = geqView.isInteractionLocked();
        lockButton.setSelected(locked);
        if (lockButton instanceof ImageView) {
            ((ImageView) lockButton).setImageResource(locked ? R.drawable.ic_eq_lock : R.drawable.ic_eq_unlock);
        }
        lockButton.setContentDescription(getString(locked ? R.string.unlock_equalizer : R.string.lock_equalizer));
        updateProfileLockControls(root, locked);
    }

    private void updateProfileLockControls(View root, boolean locked) {
        boolean controlsEnabled = !locked;

        View resetButton = root.findViewById(R.id.profileResetButton);
        if (resetButton != null) {
            resetButton.setEnabled(controlsEnabled);
            resetButton.setClickable(controlsEnabled);
            resetButton.setAlpha(locked ? 0.45f : 1.0f);
        }

        int[] protectedSwitchIds = {
                R.id.vlButton,
                R.id.beButton,
                R.id.svButton,
                R.id.deButtonSwitch
        };
        for (int id : protectedSwitchIds) {
            View protectedSwitch = root.findViewById(id);
            if (protectedSwitch != null) {
                protectedSwitch.setEnabled(controlsEnabled);
                protectedSwitch.setClickable(controlsEnabled);
                protectedSwitch.setAlpha(locked ? 0.55f : 1.0f);
            }
        }

        // Keep Intelligent Tone visually unchanged while the profile is locked.
        // Its header/current-preset text and Bright/Balanced/Warm/Off cards should
        // retain their normal colors; only interaction is blocked by the adapter
        // and the guarded callbacks below.
        if (this.mEqualizerAdapter != null) {
            this.mEqualizerAdapter.setInteractionLocked(locked);
        }

        // Dialogue Enhancer has two interactive controls: the enable switch and amount slider.
        View deSeekBar = root.findViewById(R.id.deButton);
        if (deSeekBar != null) {
            deSeekBar.setEnabled(controlsEnabled);
            deSeekBar.setClickable(controlsEnabled);
            // Match the other profile settings: only the interactive control is
            // visually disabled. Keep the Dialogue Enhancer title and summary
            // at their normal text colors while the profile is locked.
            deSeekBar.setAlpha(locked ? 0.55f : 1.0f);
        }
    }

    private boolean isProfileInteractionLocked() {
        View root = getView();
        if (root == null) {
            return false;
        }
        GeqView geqView = (GeqView) root.findViewById(R.id.geqView);
        if (geqView == null) {
            return false;
        }
        geqView.setInteractionProfile(this.mNum);
        return geqView.isInteractionLocked();
    }

    private void toggleGeqLock() {
        View root = getView();
        if (root == null) {
            return;
        }
        GeqView geqView = (GeqView) root.findViewById(R.id.geqView);
        if (geqView == null) {
            return;
        }
        geqView.setInteractionProfile(this.mNum);
        geqView.setInteractionLocked(!geqView.isInteractionLocked());
        updateGeqLockButton(root);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view != null && view.getId() == R.id.geqLockButton) {
            toggleGeqLock();
            return;
        }
        IDsFragObserver iDsFragObserver = this.mFObserver;
        DolbyAudioEffect dolbyAudioEffect = iDsFragObserver == null ? null : iDsFragObserver.getDolbyAudioEffect();
        if (dolbyAudioEffect != null) {
            int id = view.getId();
            if (R.id.profileResetButton == id) {
                GeqView geqView = getView() == null ? null : (GeqView) getView().findViewById(R.id.geqView);
                if (geqView != null) {
                    geqView.setInteractionProfile(this.mNum);
                    if (geqView.isInteractionLocked()) {
                        return;
                    }
                }
                int profile = dolbyAudioEffect.getProfile();
                DsClientSettings.INSTANCE.resetProfileSpecificSettings(this.mFObserver, profile);
                this.mFObserver.resetProfile(profile);
                return;
            } else {
                if (R.id.equalizerListOff == id) {
                    if (isProfileInteractionLocked()) {
                        return;
                    }
                    ImageView imageView = (ImageView) getView().findViewById(R.id.icon_off);
                    ((LinearLayout) getView().findViewById(R.id.equalizerListOffLayout)).setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_blue_shadow));
                    imageView.setImageResource(R.drawable.ic_none_on_ieq);
                    this.mEqualizerAdapter.setIeqSelection(3, true);
                    return;
                }
                return;
            }
        }
        Log.e("FragProfilePanel", "onClick(): Dolby audio effect is null!");
    }


    /**
     * Refresh only the derived reset affordance. This is intentionally lightweight so
     * sliders/EQ gestures can update it without rebuilding the whole profile panel.
     */
    public void updateResetButtonState(int profile) {
        View root = getView();
        if (root == null || this.mFObserver == null) {
            return;
        }
        View resetButton = root.findViewById(R.id.profileResetButton);
        if (resetButton == null) {
            return;
        }
        boolean modified = DsClientSettings.INSTANCE.isProfileSpecificSettingsModified(this.mFObserver, profile);
        resetButton.setVisibility(modified ? View.VISIBLE : View.INVISIBLE);
    }

    public void updateProfilePanel(int i) {
        this.mNum = i;
        View view = getView();
        if (view != null) {
            RelativeLayout relativeLayout = (RelativeLayout) view.findViewById(R.id.ieqLayout);
            if (i == 2 || i == 3) {
                relativeLayout.setVisibility(0);
                int ieqPreset = DsClientSettings.INSTANCE.getIeqPreset(this.mFObserver);
                ((TextView) view.findViewById(R.id.ieqName)).setText(this.mIeqName[ieqPreset]);
                ImageView imageView = (ImageView) view.findViewById(R.id.icon_off);
                LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.equalizerListOffLayout);
                if (ieqPreset == 3) {
                    linearLayout.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_blue_shadow));
                    imageView.setImageResource(R.drawable.ic_none_on_ieq);
                } else {
                    linearLayout.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_shadow));
                    imageView.setImageResource(R.drawable.ic_none_off_ieq);
                }
                EqualizerAdapter equalizerAdapter = this.mEqualizerAdapter;
                if (equalizerAdapter != null) {
                    equalizerAdapter.setIeqSelection(ieqPreset, false);
                }
            } else {
                relativeLayout.setVisibility(8);
            }
            RelativeLayout relativeLayout2 = (RelativeLayout) view.findViewById(R.id.deView);
            if (this.mProductVersion.substring(0, 4).equals("DAX3")) {
                if (i == 0 || i == 2) {
                    relativeLayout2.setVisibility(8);
                } else {
                    relativeLayout2.setVisibility(0);
                    this.mdeState = DsClientSettings.INSTANCE.getDialogEnhancerOn(this.mFObserver);
                    Switch deSwitch = (Switch) view.findViewById(R.id.deButtonSwitch);
                    if (deSwitch != null) {
                        deSwitch.setOnCheckedChangeListener(null);
                        deSwitch.setChecked(this.mdeState);
                        deSwitch.setOnCheckedChangeListener(this);
                    }
                    SeekBar seekBar = (SeekBar) view.findViewById(R.id.deButton);
                    if (seekBar != null) {
                        if (this.mdeState) {
                            seekBar.setVisibility(0);
                            seekBar.setProgress(DsClientSettings.INSTANCE.getDialogEnhancerAmount(this.mFObserver));
                        } else {
                            seekBar.setVisibility(8);
                        }
                    }
                }
            }
            TextView textView = (TextView) view.findViewById(R.id.profileDescription);
            if (textView != null) {
                if (i == 0) {
                    textView.setVisibility(0);
                } else {
                    textView.setVisibility(8);
                }
            }
            GeqView geqView = (GeqView) view.findViewById(R.id.geqView);
            if (geqView != null && geqView.mSelectedBar == -1) {
                geqView.onUpdateGeqData();
            }
            updateGeqLockButton(view);
            updateResetButtonState(i);
            if (this.mFObserver != null) {
                this.mbeState = DsClientSettings.INSTANCE.getBassEnhancerOn(this.mFObserver);
                if (this.mbeSwitch != null) {
                    this.mbeSwitch.setChecked(this.mbeState);
                    this.mbeSwitch.invalidate();
                    this.mvlState = DsClientSettings.INSTANCE.getVolumeLevelerOn(this.mFObserver);
                    if (this.mvlSwitch != null) {
                        this.mvlSwitch.setChecked(this.mvlState);
                        this.mvlSwitch.invalidate();
                        this.msvState = DsClientSettings.INSTANCE.getSpeakerVirtualizerOn(this.mFObserver);
                        if (this.msvSwitch != null) {
                            this.msvSwitch.setChecked(this.msvState);
                            this.msvSwitch.invalidate();
                        }
                        if (this.mdeSwitch != null) {
                            this.mdeSwitch.setChecked(this.mdeState);
                            this.mdeSwitch.invalidate();
                        }
                    }
                }
            }
            setGeqViewEnabled();
        }
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        if (compoundButton == null) {
            return;
        }
        int id = compoundButton.getId();
        if ((id == R.id.deButtonSwitch || id == R.id.beButton || id == R.id.vlButton || id == R.id.svButton)
                && isProfileInteractionLocked()) {
            return;
        }
        DsClientSettings dsClientSettings = DsClientSettings.INSTANCE;
        boolean changed = false;
        try {
            if (id == R.id.deButtonSwitch) {
                if (z != dsClientSettings.getDialogEnhancerOn(this.mFObserver)) {
                    dsClientSettings.setDialogEnhancerOn(this.mFObserver, z);
                    changed = true;
                }
                this.mdeState = z;
                View currentView = getView();
                if (currentView != null) {
                    SeekBar seekBar = (SeekBar) currentView.findViewById(R.id.deButton);
                    if (seekBar != null) {
                        if (z) {
                            seekBar.setVisibility(View.VISIBLE);
                            seekBar.setProgress(dsClientSettings.getDialogEnhancerAmount(this.mFObserver));
                            seekBar.setOnSeekBarChangeListener(this);
                        } else {
                            seekBar.setVisibility(View.GONE);
                        }
                    }
                }
            } else if (id != R.id.beButton) {
                if (id != R.id.vlButton) {
                    if (id == R.id.svButton) {
                        if (z != dsClientSettings.getSpeakerVirtualizerOn(this.mFObserver)) {
                            dsClientSettings.setSpeakerVirtualizerOn(this.mFObserver, z);
                            changed = true;
                        }
                    }
                } else if (z != dsClientSettings.getVolumeLevelerOn(this.mFObserver)) {
                    dsClientSettings.setVolumeLevelerOn(this.mFObserver, z);
                    changed = true;
                }
            } else if (z != dsClientSettings.getBassEnhancerOn(this.mFObserver)) {
                dsClientSettings.setBassEnhancerOn(this.mFObserver, z);
                changed = true;
            }
            if (changed && this.mFObserver != null) {
                this.mFObserver.profileModificationChanged(this.mNum);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // android.support.v4.app.Fragment
    public void onDestroyView() {
        ViewGroup viewGroup;
        super.onDestroyView();
        if (getView() == null || (viewGroup = (ViewGroup) getView().getParent()) == null) {
            return;
        }
        viewGroup.removeView(getView());
    }

    @Override // android.support.v4.app.Fragment
    public void onDestroy() {
        super.onDestroy();
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (z && isProfileInteractionLocked()) {
            return;
        }
        if (z) {
            boolean dialogEnhancerOn = DsClientSettings.INSTANCE.getDialogEnhancerOn(this.mFObserver);
            if (i == 0 && dialogEnhancerOn) {
                DsClientSettings.INSTANCE.setDialogEnhancerOn(this.mFObserver, false);
                SharedPreferences sharedPreferences = this.mContext.getSharedPreferences("dax_dea_default", 0);
                String str = null;
                int i2 = this.mNum;
                if (i2 == 1) {
                    str = "DialogEnhancerAmountForMovie";
                } else if (i2 == 3) {
                    str = "DialogEnhancerAmountForCustom";
                }
                if (str != null) {
                    DsClientSettings.INSTANCE.setDialogEnhancerAmount(this.mFObserver, sharedPreferences.getInt(str, 0));
                    this.mFObserver.profileModificationChanged(this.mNum);
                    return;
                }
                this.mFObserver.profileModificationChanged(this.mNum);
                return;
            }
            DsClientSettings.INSTANCE.setDialogEnhancerOn(this.mFObserver, true);
            DsClientSettings.INSTANCE.setDialogEnhancerAmount(this.mFObserver, i);
            this.mFObserver.profileModificationChanged(this.mNum);
        }
    }
}
