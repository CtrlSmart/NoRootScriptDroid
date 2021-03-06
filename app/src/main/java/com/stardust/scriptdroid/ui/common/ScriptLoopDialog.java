package com.stardust.scriptdroid.ui.common;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.scriptdroid.App;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.script.ScriptFile;
import com.stardust.scriptdroid.script.Scripts;

//import butterknife.BindView;
//import butterknife.ButterKnife;

/**
 * Created by Stardust on 2017/7/8.
 */

public class ScriptLoopDialog {

    private ScriptFile mScriptFile;
    private MaterialDialog mDialog;

//    @BindView(R.id.loop_times)
    TextInputEditText mLoopTimes;

//    @BindView(R.id.loop_interval)
    TextInputEditText mLoopInterval;

//    @BindView(R.id.loop_delay)
    TextInputEditText mLoopDelay;


    public ScriptLoopDialog(Context context, ScriptFile file) {
        mScriptFile = file;
        View view = View.inflate(context, R.layout.dialog_script_loop, null);
        mDialog = new MaterialDialog.Builder(context)
                .title(R.string.text_run_repeatedly)
                .customView(view, true)
                .positiveText(R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        startScriptRunningLoop();
                    }
                })
                .build();
//        ButterKnife.bind(this, view);
    }

    private void startScriptRunningLoop() {
        try {
            int loopTimes = Integer.parseInt(mLoopTimes.getText().toString());
            float loopInterval = Float.parseFloat(mLoopInterval.getText().toString());
            float loopDelay = Float.parseFloat(mLoopDelay.getText().toString());
            Scripts.runRepeatedly(mScriptFile, loopTimes, (long) (1000L * loopDelay), (long) (loopInterval * 1000L));
        } catch (NumberFormatException e) {
            App.getApp().getUiHandler().toast(R.string.text_number_format_error);
        }
    }

    public ScriptLoopDialog windowType(int windowType) {
        Window window = mDialog.getWindow();
        if (window != null) {
            window.setType(windowType);
        }
        return this;
    }

    public void show() {
        mDialog.show();
    }

}
