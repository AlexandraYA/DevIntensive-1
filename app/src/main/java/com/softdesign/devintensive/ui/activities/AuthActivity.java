package com.softdesign.devintensive.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.softdesign.devintensive.R;
import com.softdesign.devintensive.data.managers.DataManager;
import com.softdesign.devintensive.data.network.api.req.UserLoginReq;
import com.softdesign.devintensive.data.network.api.res.UserAuthRes;
import com.softdesign.devintensive.data.network.restmodels.BaseModel;
import com.softdesign.devintensive.data.network.restmodels.User;
import com.softdesign.devintensive.ui.adapters.PicassoTargetByName;
import com.softdesign.devintensive.utils.AppConfig;
import com.softdesign.devintensive.utils.ConstantManager;
import com.softdesign.devintensive.utils.ErrorUtils;
import com.softdesign.devintensive.utils.NetworkUtils;
import com.squareup.picasso.Picasso;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import java.text.MessageFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.softdesign.devintensive.utils.UiHelper.getScreenWidth;

public class AuthActivity extends BaseActivity {

    private static final String TAG = ConstantManager.TAG_PREFIX + "Auth Activity";

    @BindView(R.id.auth_CoordinatorL) CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.auth_email_editText) EditText mEditText_login_email;
    @BindView(R.id.auth_password_editText) EditText mEditText_login_password;
    @BindView(R.id.save_login_checkbox) CheckBox mCheckBox_saveLogin;
    @BindView(R.id.signIn_vk_icon) ImageView mImageView_vk;

    private DataManager mDataManager;
    private int mWrongPasswordCount;
    private Boolean mUserDataEmpty;
    private User mUser;

    //region onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        ButterKnife.bind(this);

        mDataManager = DataManager.getInstance();
        mUserDataEmpty = mDataManager.getPreferencesManager().isEmpty();
        if (!mUserDataEmpty) {
            if (mDataManager.getPreferencesManager().isLoginNameSavingEnabled()) {
                mCheckBox_saveLogin.setChecked(true);
                mEditText_login_email.setText(mDataManager.getPreferencesManager().loadLoginName());
                mEditText_login_email.setSelection(mEditText_login_email.length());
            }
            mUser = mDataManager.getPreferencesManager().loadAllUserData();
            if (NetworkUtils.isNetworkAvailable(this) && !mDataManager.getPreferencesManager().loadBuiltInAuthId().isEmpty()) {
                silentLogin(mDataManager.getPreferencesManager().loadBuiltInAuthId());
            }
        }
    }
    //endregion

    //region onClick
    @OnClick({R.id.login_button, R.id.forgot_pass_button, R.id.signIn_vk_icon})
    void submitAuthButton(View view) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showSnackBar(getString(R.string.error_no_network_connection));
            return;
        }
        switch (view.getId()) {
            case R.id.login_button:
                login();
                break;
            case R.id.forgot_pass_button:
                forgotPassword();
                break;
            case R.id.signIn_vk_icon:
                VKSdk.login(this, VKScope.PHOTOS, VKScope.NOTIFY);
                break;
        }
    }
    //endregion

    //region Activity Results
    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                showToast(getString(R.string.notify_auth_by_VK));
                mDataManager.getPreferencesManager().saveVKAuthorizationInfo(res);
            }

            @Override
            public void onError(VKError error) {
                showToast(error.toString());
                // Произошла ошибка авторизации (например, пользователь запретил авторизацию)
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    //endregion

    //region Login methods

    private void login() {
        showProgressDialog();
        Call<BaseModel<UserAuthRes>> call = mDataManager.loginUser(new UserLoginReq(
                mEditText_login_email.getText().toString(),
                mEditText_login_password.getText().toString()));
        call.enqueue(new Callback<BaseModel<UserAuthRes>>() {
            @Override
            public void onResponse(Call<BaseModel<UserAuthRes>> call, Response<BaseModel<UserAuthRes>> response) {
                if (response.isSuccessful()) {
                    mWrongPasswordCount = 0;
                    onLoginSuccess(response.body());
                } else {
                    hideProgressDialog();
                    switch (response.code()) {
                        case ConstantManager.HTTP_RESPONSE_NOT_FOUND:
                            wrongPasswordAnnounce();
                            if (!mUserDataEmpty) {
                                mWrongPasswordCount++;
                                actionDependsOnFailTriesCount(mWrongPasswordCount);
                            }
                            break;
                        default:
                            ErrorUtils.BackendHttpError error = ErrorUtils.parseHttpError(response);
                            showToast(error.getErrMessage());
                            break;
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseModel<UserAuthRes>> call, Throwable t) {
                // there is more than just a failing request (like: no internet connection)
                hideProgressDialog();
                if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                    showSnackBar(getString(R.string.error_no_network_connection));
                } else
                    showSnackBar(String.format("%s: %s", getString(R.string.error_unknown_auth_error), t.getMessage()));
            }
        });
    }

    private void silentLogin(@NonNull String userId) {

        showProgressDialog();

        Call<BaseModel<User>> call = mDataManager.getUserData(userId);

        call.enqueue(new Callback<BaseModel<User>>() {
            @Override
            public void onResponse(Call<BaseModel<User>> call,
                                   Response<BaseModel<User>> response) {
                if (response.isSuccessful()) {
                    updateUserInfo(response.body().getData());
                } else {
                    hideProgressDialog();
                }
            }

            @Override
            public void onFailure(Call<BaseModel<User>> call, Throwable t) {
                hideProgressDialog();
                showSnackBar(String.format("%s: %s", getString(R.string.error_unknown_auth_error), t.getMessage()));
                Log.e(TAG, "onFailure: " + String.format("%s: %s", getString(R.string.error_unknown_auth_error), t.getMessage()));
            }
        });
    }

    private void forgotPassword() {  //// TODO: 10.07.2016 переделать в отдельную форму
        Intent forgotPassIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.FORGOT_PASS_URL));
        startActivity(forgotPassIntent);
    }

    private void finishSignIn() {
        hideProgressDialog();
        showToast(getString(R.string.notify_auth_successful));
        startActivity(new Intent(AuthActivity.this, MainActivity.class));
        AuthActivity.this.finish();
    }

    //endregion

    //region Ui methods
    private void showSnackBar(String message) {
        Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_LONG).show();
    }

    private void actionDependsOnFailTriesCount(int failsCount) {
        if (failsCount == AppConfig.MAX_LOGIN_TRIES) {
            mDataManager.getPreferencesManager().totalLogout();
            mEditText_login_email.setText("");
            showSnackBar(getString(R.string.error_current_user_data_erased));
        } else if (failsCount < AppConfig.MAX_LOGIN_TRIES) {
            String s = MessageFormat.format("{0}: {1}",
                    getString(R.string.error_tries_before_erase),
                    AppConfig.MAX_LOGIN_TRIES - mWrongPasswordCount);
            showSnackBar(s);
        }
    }

    private void wrongPasswordAnnounce() {
        mEditText_login_password.setText("");
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(AppConfig.ERROR_VIBRATE_TIME);
        showToast(getString(R.string.error_wrong_credentials));
    }
    //endregion

    //region After Success Login
    private void onLoginSuccess(BaseModel<UserAuthRes> userModelRes) {
        if (mCheckBox_saveLogin.isChecked()) {
            mDataManager.getPreferencesManager().saveLoginName(mEditText_login_email.getText().toString());
        } else {
            mEditText_login_email.setText("");
        }
        mEditText_login_password.setText("");
        saveUserAuthData(userModelRes);
        updateUserInfo(userModelRes.getData().getUser());
    }

    private void saveUserAuthData(@NonNull BaseModel<UserAuthRes> userModelRes) {
        mDataManager.getPreferencesManager().saveBuiltInAuthInfo(
                userModelRes.getData().getUser().getId(),
                userModelRes.getData().getToken()
        );
    }

    private void updateUserInfo(User user) {
        if (mUser != null && mUser.getUpdated().equals(user.getUpdated())) {
            finishSignIn();
        } else {
            mDataManager.getPreferencesManager().saveAllUserData(user);
            if (mUser == null || !mUser.getPublicInfo().getUpdated().equals(user.getPublicInfo().getUpdated())) {
                updateUserPhoto(user);
            } else {
                finishSignIn();
            }
        }
    }

    private void updateUserPhoto(@NonNull User user) {

        String pathToPhoto = user.getPublicInfo().getPhoto();

        PicassoTargetByName photoTarget = new PicassoTargetByName("photo") {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                super.onBitmapLoaded(bitmap, from);
                finishSignIn();
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                hideProgressDialog();
                showToast(getString(R.string.error_connection_failed));
            }
        };
        mImageView_vk.setTag(photoTarget);

        int photoWidth = getScreenWidth();
        int photoHeight = (int) (photoWidth / ConstantManager.ASPECT_RATIO_3_2);

        Picasso.with(this)
                .load(Uri.parse(pathToPhoto))
                .resize(photoWidth,
                        photoHeight)
                .centerCrop()
                .into(photoTarget);

        mDataManager.getPreferencesManager().saveUserPhoto(Uri.fromFile(photoTarget.getFile()));
    }

    //endregion
}