package com.softdesign.devintensive.data.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.softdesign.devintensive.data.network.api.res.UserModelRes;
import com.softdesign.devintensive.utils.ConstantManager;
import com.softdesign.devintensive.utils.DevIntensiveApplication;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.softdesign.devintensive.utils.UiHelper.getJsonFromObject;
import static com.softdesign.devintensive.utils.UiHelper.getObjectFromJson;

/**
 * saves and loads Shared Preferences of this app
 */
public class PreferencesManager {

    public static final String TAG = ConstantManager.TAG_PREFIX + "PreferencesManager";
    private final SharedPreferences mSharedPreferences;
    private final Context mContext;

    public PreferencesManager() {
        mSharedPreferences = DevIntensiveApplication.getSharedPreferences();
        mContext = DevIntensiveApplication.getContext();
    }

    public Boolean isEmpty() {
        return mSharedPreferences.getAll().isEmpty();
    }

    //region User Data save & load

    public void saveAllUserData(UserModelRes res) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(ConstantManager.USER_JSON_OBJ, getJsonFromObject(res, UserModelRes.class));
        editor.apply();
    }

    public UserModelRes loadAllUserData() {
        String json = mSharedPreferences.getString(ConstantManager.USER_JSON_OBJ, null);
        if (json != null) return (UserModelRes) getObjectFromJson(json, UserModelRes.class);
        else return null;
    }

    //region Login name
    public Boolean isLoginNameSavingEnabled() {
        return mSharedPreferences.getBoolean(ConstantManager.SAVE_LOGIN, false);
    }

    public void saveLoginName(String login) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(ConstantManager.SAVE_LOGIN, true);
        editor.putString(ConstantManager.SAVED_LOGIN_NAME, login);
        editor.apply();
    }

    public String loadLoginName() {
        if (isLoginNameSavingEnabled())
            return mSharedPreferences.getString(ConstantManager.SAVED_LOGIN_NAME, "");
        else return "";
    }
    //endregion

    //region User Photo
    public void saveUserPhoto(Uri uri) {
        Log.d(TAG, "saveUserPhoto: " + uri);
        if (uri != null) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(ConstantManager.USER_PROFILE_PHOTO_URI, uri.toString());
            editor.apply();
        }
    }

    public Uri loadUserPhoto() {
        return Uri.parse(mSharedPreferences.getString(ConstantManager.USER_PROFILE_PHOTO_URI,
                ""));
    }
    //endregion

    //region User Avatar
    public void saveUserAvatar(String uri) {
        Log.d(TAG, "saveUserAvatar: " + uri);
        if (uri != null) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(ConstantManager.USER_PROFILE_AVATAR_URI, uri);
            editor.apply();
        }
    }

    public String loadUserAvatar() {
        return mSharedPreferences.getString(ConstantManager.USER_PROFILE_AVATAR_URI, "");
    }
    //endregion

    //endregion

    //region Our Primary Auth
    public void saveBuiltInAuthInfo(String id, String token) {
        if (id != null && token != null && !id.isEmpty() && !token.isEmpty()) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(ConstantManager.BUILTIN_ACCESS_ID, id);
            editor.putString(ConstantManager.BUILTIN_ACCESS_TOKEN, token);
            editor.apply();
        }
    }

    public String loadBuiltInAuthId() {
        return mSharedPreferences.getString(ConstantManager.BUILTIN_ACCESS_ID, "");
    }

    public String loadBuiltInAuthToken() {
        return mSharedPreferences.getString(ConstantManager.BUILTIN_ACCESS_TOKEN, "");
    }
    //endregion

    //region Vk Auth
    public void saveVKAuthorizationInfo(VKAccessToken res) {
        if (res != null) {
            saveAuthorizationSystem(ConstantManager.AUTH_VK);
            res.saveTokenToSharedPreferences(mContext, ConstantManager.VK_ACCESS_TOKEN);
        }
    }

    public VKAccessToken loadVKToken() {
        return VKAccessToken.tokenFromSharedPreferences(mContext, ConstantManager.VK_ACCESS_TOKEN);
    }
    //endregion

    //region Google Auth
    public void saveGoogleAuthorizationInfo(String accountName, String accountType, String token) {
        if (token != null && accountName != null && accountType != null) {
            saveAuthorizationSystem(ConstantManager.AUTH_GOOGLE);
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(ConstantManager.GOOGLE_ACCESS_ACC_NAME, accountName);
            editor.putString(ConstantManager.GOOGLE_ACCESS_ACC_TYPE, accountType);
            editor.putString(ConstantManager.GOOGLE_ACCESS_TOKEN, token);
            editor.apply();
        }
    }

    public List<String> loadGoogleAuthorizationInfo() {
        List<String> authDataList = new ArrayList<>();
        authDataList.add(mSharedPreferences.getString(ConstantManager.GOOGLE_ACCESS_ACC_NAME, ""));
        authDataList.add(mSharedPreferences.getString(ConstantManager.GOOGLE_ACCESS_ACC_TYPE, ""));
        authDataList.add(mSharedPreferences.getString(ConstantManager.GOOGLE_ACCESS_TOKEN, ""));
        return authDataList;
    }

    public void removeGoogleAuthorizationOnDestroy() {
        //removing google token on exit
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(ConstantManager.GOOGLE_ACCESS_TOKEN);
        editor.apply();
    }
    //endregion

    //region General auth methods
    public void saveAuthorizationSystem(String system) {
        if (system != null) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(ConstantManager.AUTHORIZATION_SYSTEM, system);
            editor.apply();
        }
    }

    public String getAuthorizationSystem() {
        return mSharedPreferences.getString(ConstantManager.AUTHORIZATION_SYSTEM, "");
    }

    public Boolean checkAuthorizationStatus() {   //// TODO: 08.07.2016 временно. Переделать на авторизацию сервером
        switch (getAuthorizationSystem()) {
            case ConstantManager.AUTH_VK:
                VKAccessToken vkToken = VKAccessToken.tokenFromSharedPreferences(mContext, ConstantManager.VK_ACCESS_TOKEN);
                return vkToken != null && !vkToken.isExpired();
            case ConstantManager.AUTH_FACEBOOK:
                AccessToken fbToken = AccessToken.getCurrentAccessToken();
                return fbToken != null && !fbToken.isExpired();
            case ConstantManager.AUTH_GOOGLE:
                //а хрен вам а не проверка на expired, мы Гугл, мы шлем вас снова отправить запрос авторизации.
                return !(loadGoogleAuthorizationInfo().get(2)).isEmpty();   //токен пуст, если после последней авторизации вызывался метод onDestroy()
            case ConstantManager.AUTH_BUILTIN:
                return true;
            default:
                return false;
        }
    }

    public void softLogout() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        List<String> exclusionKeys = new ArrayList<>();
        if (mSharedPreferences.getBoolean(ConstantManager.SAVE_LOGIN, false)) {
            exclusionKeys.add(ConstantManager.SAVE_LOGIN);
            exclusionKeys.add(ConstantManager.SAVED_LOGIN_NAME);
        }

        Map<String, ?> spMap = mSharedPreferences.getAll();
        for (String key : spMap.keySet()) {
            if (!exclusionKeys.contains(key)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    /**
     * totally removes all current users auth data
     */
    public void totalLogout() {
        //removing all received tokens and auth status
        VKSdk.logout();                         //vk logout
        LoginManager.getInstance().logOut();    //fb logout
        VKAccessToken.removeTokenAtKey(mContext, ConstantManager.VK_ACCESS_TOKEN);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.clear().apply();
    }
    //endregion
}