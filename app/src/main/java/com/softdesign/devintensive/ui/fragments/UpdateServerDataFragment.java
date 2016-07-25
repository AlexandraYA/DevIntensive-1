package com.softdesign.devintensive.ui.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.softdesign.devintensive.data.network.api.req.EditProfileReq;
import com.softdesign.devintensive.data.network.api.res.EditProfileRes;
import com.softdesign.devintensive.data.network.api.res.UserPhotoRes;
import com.softdesign.devintensive.data.network.restmodels.BaseModel;
import com.softdesign.devintensive.data.network.restmodels.User;
import com.softdesign.devintensive.data.operations.FullUserDataOperation;
import com.softdesign.devintensive.data.storage.viewmodels.ProfileViewModel;
import com.softdesign.devintensive.ui.callbacks.BaseTaskCallbacks;
import com.softdesign.devintensive.utils.Const;
import com.softdesign.devintensive.utils.DevIntensiveApplication;

import java.io.File;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

import static com.softdesign.devintensive.utils.UiHelper.filePathFromUri;
import static com.softdesign.devintensive.utils.UiHelper.getObjectFromJson;

public class UpdateServerDataFragment extends BaseNetworkFragment {

    private static final String TAG = Const.TAG_PREFIX + "UploadInfoToServer";

    private UploadToServerCallbacks mCallbacks;
    private BaseModel<?> mResult;

    public interface UploadToServerCallbacks extends BaseTaskCallbacks {
        void onRequestFinished(BaseModel<?> result);
    }

    //region Life cycle
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCallbacks != null && mStatus == Status.FINISHED) {
            if (!mCancelled && mResult != null) {
                mCallbacks.onRequestFinished(mResult);
                mResult = null;
            } else {
                mCallbacks.onRequestFailed(mError);
                mError = null;
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof UploadToServerCallbacks) {
            mCallbacks = (UploadToServerCallbacks) activity;
        } else {
            throw new IllegalStateException("Parent activity must implement UploadToServerCallbacks");
        }
    }
    //endregion

    //region Request status
    @Override
    @SuppressWarnings("unchecked")
    public void onRequestComplete(Response response) {
        mStatus = Status.FINISHED;
        BaseModel bm = (BaseModel) response.body();

        if (bm.getData().getClass().isAssignableFrom(UserPhotoRes.class)) {
            synchronized (this) {
                BaseModel<UserPhotoRes> res = (BaseModel<UserPhotoRes>) response.body();
                runOperation(new FullUserDataOperation(res.getData()));
                if (mCallbacks != null) {
                    mCallbacks.onRequestFinished();
                } else {
                    mResult = res;
                }
            }
        } else if (bm.getData().getClass().isAssignableFrom(EditProfileRes.class)) {
            synchronized (this) {
                BaseModel<EditProfileRes> res = (BaseModel<EditProfileRes>) response.body();
                runOperation(new FullUserDataOperation(res.getData().getUser()));
                if (mCallbacks != null) {
                    mCallbacks.onRequestFinished();
                } else {
                    mResult = res;
                }
            }
        }
    }

    @Override
    public void onRequestStarted() {
        mResult = null;
        super.onRequestStarted();
    }
    //endregion

    //region Requests
    public void uploadUserPhoto(final Uri uri_SelectedImage) {

        if (uri_SelectedImage == null || !isExecutePossible() ||
                DATA_MANAGER.getPreferencesManager().loadUserPhoto().equals(uri_SelectedImage))
            return;

        Log.d(TAG, "uploadUserPhoto: ");

        synchronized (this) {
            onRequestStarted();

            File file = new File(filePathFromUri(uri_SelectedImage));

            final RequestBody requestFile =
                    RequestBody.create(Const.MEDIATYPE_MULTIPART_FORM_DATA, file);

            MultipartBody.Part body =
                    MultipartBody.Part.createFormData("photo", file.getName(), requestFile);

            DATA_MANAGER.uploadUserPhoto(DATA_MANAGER.getPreferencesManager().loadBuiltInAuthId(), body)
                    .enqueue(new NetworkCallback<>());
        }
    }

    public void uploadUserAvatar(final String uri_SelectedImage) {

        if (uri_SelectedImage == null || !isExecutePossible() ||
                DATA_MANAGER.getPreferencesManager().loadUserAvatar().equals(uri_SelectedImage))
            return;

        Log.d(TAG, "uploadUserPhoto: ");
        synchronized (this) {
            onRequestStarted();

            File file = new File(filePathFromUri(Uri.parse(uri_SelectedImage)));

            final RequestBody requestFile =
                    RequestBody.create(Const.MEDIATYPE_MULTIPART_FORM_DATA, file);

            MultipartBody.Part body =
                    MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);

            DATA_MANAGER.uploadUserAvatar(DATA_MANAGER.getPreferencesManager().loadBuiltInAuthId(), body)
                    .enqueue(new NetworkCallback<>());
        }
    }

    public void uploadUserData(ProfileViewModel model) {

        if (model == null || !isExecutePossible() || !isUserDataChanged(model)) return;

        Log.d(TAG, "uploadUserData: ");
        synchronized (this) {
            onRequestStarted();

            DATA_MANAGER.uploadUserInfo(new EditProfileReq(model).createReqBody()).enqueue(new NetworkCallback<>());
        }
    }

    private boolean isUserDataChanged(ProfileViewModel model){

        User savedUser;
        String jsonSavedUser = DevIntensiveApplication.getSharedPreferences().getString(Const.USER_JSON_OBJ, null);
        if (jsonSavedUser != null) savedUser = (User) getObjectFromJson(jsonSavedUser, User.class);
        else return true;

        return model.compareUserData(savedUser);
    }
    //endregion
}