package com.dolby.daxappui.exploreDolby;

import com.dolby.daxappui.R;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class FragExploreDolbyAtmos extends Fragment {
    @Override // android.support.v4.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override // android.support.v4.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreate(bundle);
        View inflate = layoutInflater.inflate(R.layout.explore_dolby_atmos, viewGroup, false);
        ((RelativeLayout) inflate.findViewById(R.id.btnupgotodolbyatmos)).setOnClickListener(new View.OnClickListener() { // from class: com.dolby.daxappui.exploreDolby.-$$Lambda$FragExploreDolbyAtmos$RqQz2rH-RbGoQN7YJpcmN27Jm1s
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                FragExploreDolbyAtmos.this.lambda$onCreateView$0$FragExploreDolbyAtmos(view);
            }
        });
        return inflate;
    }

    public /* synthetic */ void lambda$onCreateView$0$FragExploreDolbyAtmos(View view) {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://www.dolby.com/us/en/technologies/dolby-atmos.html")));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }
}
