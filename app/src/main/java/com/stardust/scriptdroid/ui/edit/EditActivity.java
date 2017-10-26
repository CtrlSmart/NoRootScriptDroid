package com.stardust.scriptdroid.ui.edit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jecelyin.editor.v2.common.Command;
import com.jecelyin.editor.v2.common.SaveListener;
import com.jecelyin.editor.v2.core.widget.TextView;
import com.jecelyin.editor.v2.ui.EditorDelegate;
import com.jecelyin.editor.v2.view.EditorView;
import com.jecelyin.editor.v2.view.menu.MenuDef;
import com.stardust.app.OnActivityResultDelegate;
import com.stardust.autojs.engine.JavaScriptEngine;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.autojs.script.JavaScriptFileSource;
import com.stardust.autojs.script.JsBeautifier;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.autojs.AutoJs;
import com.stardust.scriptdroid.script.ScriptFile;
import com.stardust.scriptdroid.script.Scripts;
import com.stardust.scriptdroid.tool.JsBeautifierFactory;
import com.stardust.scriptdroid.tool.MaterialDialogFactory;
import com.stardust.scriptdroid.ui.BaseActivity;
import com.stardust.scriptdroid.ui.edit.completion.InputMethodEnhanceBar;
import com.stardust.scriptdroid.ui.edit.editor920.Editor920Activity;
import com.stardust.scriptdroid.ui.edit.editor920.Editor920Utils;
import com.stardust.scriptdroid.ui.help.HelpCatalogueActivity;
import com.stardust.theme.ThemeColorManager;
import com.stardust.theme.dialog.ThemeColorMaterialDialogBuilder;
import com.stardust.util.SparseArrayEntries;
import com.stardust.widget.ToolbarMenuItem;

//import org.androidannotations.annotations.AfterViews;
//import org.androidannotations.annotations.Click;
//import org.androidannotations.annotations.EActivity;
//import org.androidannotations.annotations.ViewById;

import java.io.File;

/**
 * Created by Stardust on 2017/1/29.
 */
//@EActivity(R.layout.activity_edit)
public class EditActivity extends Editor920Activity implements OnActivityResultDelegate.DelegateHost {


    public static class InputMethodEnhanceBarBridge implements InputMethodEnhanceBar.EditTextBridge {

        private Editor920Activity mEditor920Activity;
        private TextView mTextView;

        public InputMethodEnhanceBarBridge(Editor920Activity editor920Activity, TextView textView) {
            mEditor920Activity = editor920Activity;
            mTextView = textView;
        }

        @Override
        public void appendText(CharSequence text) {
            mEditor920Activity.insertText(text);
        }

        @Override
        public void backspace(int count) {

        }

        @Override
        public TextView getEditText() {
            return mTextView;
        }


    }


    public static final String EXTRA_PATH = "Still Love Eating 17.4.5";
    private static final String EXTRA_NAME = "Still love you 17.6.29 But....(ಥ_ಥ)";

    public static void editFile(Context context, String path) {
        editFile(context, null, path);
    }

    public static void editFile(Context context, String name, String path) {
//        context.startActivity(new Intent(context, EditActivity_.class)
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                .putExtra(EXTRA_PATH, path)
//                .putExtra(EXTRA_NAME, name));
    }

    public static void editFile(Context context, ScriptFile file) {
        editFile(context, file.getSimplifiedName(), file.getPath());
    }

//    @ViewById(R.id.content_view)
    View mView;
    private String mName;
    private File mFile;
    private EditorDelegate mEditorDelegate;
    private SparseArray<ToolbarMenuItem> mMenuMap;
    private boolean mReadOnly = false;
    private OnActivityResultDelegate.Mediator mActivityResultMediator = new OnActivityResultDelegate.Mediator();
    private BroadcastReceiver mOnRunFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Scripts.ACTION_ON_EXECUTION_FINISHED)) {
                mScriptExecution = null;
                setMenuStatus(R.id.run, MenuDef.STATUS_NORMAL);
                String msg = intent.getStringExtra(Scripts.EXTRA_EXCEPTION_MESSAGE);
                if (msg != null) {
                    Snackbar.make(mView, getString(R.string.text_error) + ": " + msg, Snackbar.LENGTH_LONG).show();
                }
            }
        }
    };
    private JsBeautifier mJsBeautifier = JsBeautifierFactory.getJsBeautify();
    private ScriptExecution mScriptExecution;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        setTheme(R.style.EditorTheme);
        handleIntent(getIntent());
        registerReceiver(mOnRunFinishedReceiver, new IntentFilter(Scripts.ACTION_ON_EXECUTION_FINISHED));
    }

//    @AfterViews
    void setUpViews() {
        ThemeColorManager.addActivityStatusBar(this);
        setUpToolbar();
        initMenuItem();
        setUpEditor();
    }

    private void handleIntent(Intent intent) {
        String path = intent.getStringExtra(EXTRA_PATH);
        mName = intent.getStringExtra(EXTRA_NAME);
        mReadOnly = intent.getBooleanExtra("readOnly", false);
        boolean saveEnabled = intent.getBooleanExtra("saveEnabled", true);
        if (mReadOnly || !saveEnabled) {
            findViewById(R.id.save).setVisibility(View.GONE);
        }
        String content = intent.getStringExtra("content");
        if (content != null) {
            mEditorDelegate = new EditorDelegate(0, mName, content);
        } else {
            mFile = new File(path);
            if (mName == null) {
                mName = mFile.getName();
            }
            mEditorDelegate = new EditorDelegate(0, mFile, 0, "utf-8");
        }
    }

    private void setUpEditor() {
        final EditorView editorView = (EditorView) findViewById(R.id.editor);
        mEditorDelegate.setEditorView(editorView);
        if (mFile == null)
            Editor920Utils.setLang(mEditorDelegate, "JavaScript");
        editorView.getEditText().setReadOnly(mReadOnly);
        editorView.getEditText().setHorizontallyScrolling(true);
        setUpInputMethodEnhanceBar(editorView);

    }

    private void setUpInputMethodEnhanceBar(final EditorView editorView) {
        InputMethodEnhanceBar inputMethodEnhanceBar = (InputMethodEnhanceBar) findViewById(R.id.input_method_enhance_bar);
        if (mReadOnly) {
            inputMethodEnhanceBar.setVisibility(View.GONE);
        } else {
            inputMethodEnhanceBar.setEditTextBridge(new InputMethodEnhanceBarBridge(this, editorView.getEditText()));
        }
    }


    private void setUpToolbar() {
        BaseActivity.setToolbarAsBack(this, R.id.toolbar, mName);
    }

//    @Click(R.id.run)
    void runAndSaveFileIFNeeded() {
        if (!mReadOnly && mEditorDelegate.isChanged()) {
            saveFile(false, new SaveListener() {
                @Override
                public void onSaved() {
                    run();
                }
            });
        } else {
            run();
        }
    }

    private void saveFile(boolean toast, SaveListener listener) {
        Command command = new Command(Command.CommandEnum.SAVE);
        command.args = new Bundle();
        command.args.putBoolean("is_cluster", !toast);
        command.object = listener;
        mEditorDelegate.doCommand(command);
    }

    private void run() {
        Snackbar.make(mView, R.string.text_start_running, Snackbar.LENGTH_SHORT).show();
        setMenuStatus(R.id.run, MenuDef.STATUS_DISABLED);
        mScriptExecution = Scripts.runWithBroadcastSender(new JavaScriptFileSource(mName, mFile), mFile.getParent());
    }


//    @Click(R.id.undo)
    void undo() {
        Command command = new Command(Command.CommandEnum.UNDO);
        mEditorDelegate.doCommand(command);
    }

//    @Click(R.id.redo)
    void redo() {
        Command command = new Command(Command.CommandEnum.REDO);
        mEditorDelegate.doCommand(command);
    }


//    @Click(R.id.save)
    void saveFile() {
        saveFile(false, null);
    }

    private void initMenuItem() {
        mMenuMap = new SparseArrayEntries<ToolbarMenuItem>()
                .entry(com.jecelyin.editor.v2.R.id.m_redo, (ToolbarMenuItem) findViewById(R.id.redo))
                .entry(com.jecelyin.editor.v2.R.id.m_undo, (ToolbarMenuItem) findViewById(R.id.undo))
                .entry(com.jecelyin.editor.v2.R.id.m_save, (ToolbarMenuItem) findViewById(R.id.save))
                .entry(R.id.run, (ToolbarMenuItem) findViewById(R.id.run))
                .sparseArray();
    }

    public void setMenuStatus(int menuResId, int status) {
        ToolbarMenuItem menuItem = mMenuMap.get(menuResId);
        if (menuItem == null)
            return;
        boolean disabled = status == MenuDef.STATUS_DISABLED;
        menuItem.setEnabled(!disabled);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_console:
//                showConsole();
//                return true;
//            case R.id.action_log:
//                showLog();
//                return true;
//            case R.id.action_help:
//                HelpCatalogueActivity.showMainCatalogue(this);
//                return true;
//            case R.id.action_beautify:
//                beautifyCode();
//                return true;
//            case R.id.action_open_by_other_apps:
//                openByOtherApps();
//                return true;
//            case R.id.action_force_stop:
//                forceStop();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLog() {
        AutoJs.getInstance().getScriptEngineService().getGlobalConsole().show();
    }

    private void showConsole() {
        if (mScriptExecution != null) {
            ((JavaScriptEngine) mScriptExecution.getEngine()).getRuntime().console.show();
        }
    }

    private void forceStop() {
        if (mScriptExecution != null) {
            mScriptExecution.getEngine().forceStop();
        }
    }

    private void openByOtherApps() {
        if (mFile != null)
            Scripts.openByOtherApps(mFile);
    }

    private void beautifyCode() {
        final MaterialDialog dialog = MaterialDialogFactory.showProgress(this);
        mJsBeautifier.beautify(mEditorDelegate.getText(), new JsBeautifier.Callback() {
            @Override
            public void onSuccess(final String beautifiedCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mEditorDelegate.mEditText.setText(beautifiedCode);
                        dialog.dismiss();
                    }
                });
            }

            @Override
            public void onException(final Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Toast.makeText(EditActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void finish() {
        if (!mReadOnly && mEditorDelegate.isChanged()) {
            showExitConfirmDialog();
        } else {
            super.finish();
        }
    }


    private void showExitConfirmDialog() {
        new ThemeColorMaterialDialogBuilder(this)
                .title(R.string.text_alert)
                .content(R.string.edit_exit_without_save_warn)
                .positiveText(R.string.text_cancel)
                .negativeText(R.string.text_save_and_exit)
                .neutralText(R.string.text_exit_directly)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        saveFile(true, null);
                        EditActivity.super.finish();
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        EditActivity.super.finish();
                    }
                })
                .show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mActivityResultMediator.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public OnActivityResultDelegate.Mediator getOnActivityResultDelegateMediator() {
        return mActivityResultMediator;
    }

    @Override
    public void doCommand(Command command) {
        mEditorDelegate.doCommand(command);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mOnRunFinishedReceiver);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (RuntimeException e) {
            // FIXME: 2017/3/20
            e.printStackTrace();
        }
    }
}
