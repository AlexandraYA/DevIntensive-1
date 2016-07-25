package com.softdesign.devintensive.ui.activities;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.redmadrobot.chronos.gui.activity.ChronosAppCompatActivity;
import com.softdesign.devintensive.R;
import com.softdesign.devintensive.data.managers.DataManager;
import com.softdesign.devintensive.data.operations.BaseChronosOperation;
import com.softdesign.devintensive.data.operations.DatabaseOperation;
import com.softdesign.devintensive.ui.callbacks.BaseActivityCallback;
import com.softdesign.devintensive.ui.fragments.DialogsFragment;
import com.softdesign.devintensive.utils.Const;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

@SuppressWarnings("unused")
public class BaseActivity extends ChronosAppCompatActivity implements BaseActivityCallback {

    public final String TAG = Const.TAG_PREFIX + getClass().getSimpleName();

    public static final DataManager DATA_MANAGER = DataManager.getInstance();
    public static final EventBus BUS = EventBus.getDefault();
    private ProgressDialog mProgressDialog;

    //region Activity's LifeCycle

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
    //endregion

    //region Announce
    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this, R.style.custom_dialog);
            mProgressDialog.setCancelable(false);
            mProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        mProgressDialog.show();
        mProgressDialog.setContentView(R.layout.progress_splash);
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public void showError(int messageId) {
        try {
            showDialogFragment(Const.DIALOG_SHOW_ERROR, getString(messageId));
            Log.e(TAG, getString(messageId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showError(int dialogId, int messageId) {
        try {
            showDialogFragment(dialogId, getString(messageId));
            Log.e(TAG, getString(messageId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showError(String message) {
        showDialogFragment(Const.DIALOG_SHOW_ERROR, message);
        Log.e(TAG, String.valueOf(message));
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void showDialogFragment(int dialogId) {
        DialogFragment newFragment = DialogsFragment.newInstance(dialogId);
        newFragment.show(getFragmentManager(), newFragment.getClass().toString() + dialogId);
    }

    public void showDialogFragment(int dialogId, String message) {
        DialogFragment newFragment = DialogsFragment.newInstance(dialogId, message);
        newFragment.show(getFragmentManager(), newFragment.getClass().toString() + dialogId);
    }
    //endregion

    //region UTIL (стырила у Сереги Куприна, на память)
    public <T extends View> T $(@IdRes int id) {
        //noinspection unchecked
        return (T) findViewById(id);
    }

    @Nullable
    public <T extends BaseActivity> T as(Class<T> clazz) {
        //noinspection unchecked
        return (T) this;
    }
    //endregion

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
    }

    public void logout(int mode) {
        Log.d(TAG, "logout: ");
        if (mode == 1) {
            runOperation(new DatabaseOperation(BaseChronosOperation.Action.CLEAR));
            DATA_MANAGER.getPreferencesManager().totalLogout();
        }
        startAuthActivity();
    }

    public void startAuthActivity() {
        Intent intent = new Intent(getApplicationContext(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void startMainActivity() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        this.finish();
    }

    public void startUserListActivity() {
        startActivity(new Intent(getApplicationContext(), UserListActivity.class));
    }

    public void openAppSettings() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }

    public void openAppSettingsForResult(int flag) {
        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        startActivityForResult(appSettingsIntent, flag);
    }

    @Override
    public void setupToolbar(Toolbar toolbar, @MenuRes int id) {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
            toolbar.inflateMenu(id);
        }
    }
}
