package com.softdesign.devintensive.ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.softdesign.devintensive.R;
import com.softdesign.devintensive.ui.callbacks.BaseActivityCallback;
import com.softdesign.devintensive.ui.callbacks.MainActivityCallback;
import com.softdesign.devintensive.utils.Const;

public class DialogsFragment extends DialogFragment {

    private MainActivityCallback mMainActivityCallback;
    private BaseActivityCallback mBaseActivityCallback;

    public static DialogsFragment newInstance(int type) {
        DialogsFragment dialogsFragment = new DialogsFragment();
        Bundle args = new Bundle();
        args.putInt(Const.DIALOG_FRAGMENT_KEY, type);
        dialogsFragment.setArguments(args);
        return dialogsFragment;
    }

    public static DialogsFragment newInstance(int type, String content) {
        DialogsFragment dialogsFragment = new DialogsFragment();
        Bundle args = new Bundle();
        args.putInt(Const.DIALOG_FRAGMENT_KEY, type);
        args.putString(Const.DIALOG_CONTENT_KEY, content);
        dialogsFragment.setArguments(args);
        return dialogsFragment;
    }

    //region :::::::::::::::::::::::::::::::::::::::::: Life cycle
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof MainActivityCallback) {
            mMainActivityCallback = (MainActivityCallback) activity;
        }
        if (activity instanceof BaseActivityCallback) {
            mBaseActivityCallback = (BaseActivityCallback) activity;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int type = getArguments().getInt(Const.DIALOG_FRAGMENT_KEY);
        switch (type) {
            case Const.DIALOG_LOAD_PROFILE_PHOTO:
                if (mMainActivityCallback != null) return loadPhotoDialog();
                else errorAlertDialog(getString(R.string.error));
            case Const.DIALOG_LOAD_PROFILE_AVATAR:
                if (mMainActivityCallback != null) return loadAvatarDialog();
                else errorAlertDialog(getString(R.string.error));
            case Const.DIALOG_SHOW_ERROR:
                return errorAlertDialog(getArguments().getString(Const.DIALOG_CONTENT_KEY));
            case Const.DIALOG_SHOW_ERROR_RETURN_TO_MAIN:
                return errorAlertExitToMain(getArguments().getString(Const.DIALOG_CONTENT_KEY));
            case Const.DIALOG_SHOW_ERROR_RETURN_TO_AUTH:
                return errorAlertExitToAuth(getArguments().getString(Const.DIALOG_CONTENT_KEY));
            default:
                return errorAlertDialog(getString(R.string.error));
        }
    }
    //endregion ::::::::::::::::::::::::::::::::::::::::::

    //region :::::::::::::::::::::::::::::::::::::::::: Dialogs
    private Dialog loadPhotoDialog() {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.header_change_profile_photo))
                .setItems(R.array.profile_placeHolder_loadPhotoDialog, this::choosePhoto).create();
    }

    private Dialog loadAvatarDialog() {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.header_change_avatar))
                .setItems(R.array.profile_placeHolder_loadPhotoDialog, this::chooseAvatar).create();
    }

    private Dialog errorAlertDialog(String error) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(error)
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    dialog.cancel();
                }).create();
    }

    private Dialog errorAlertExitToMain(String error) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(error)
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (mBaseActivityCallback != null) mBaseActivityCallback.startMainActivity();
                    else dialog.cancel();
                }).create();
    }

    private Dialog errorAlertExitToAuth(String error) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(error)
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (mBaseActivityCallback != null) mBaseActivityCallback.startAuthActivity();
                    else dialog.cancel();
                }).create();
    }
    //endregion ::::::::::::::::::::::::::::::::::::::::::

    //region :::::::::::::::::::::::::::::::::::::::::: Utils
    private void choosePhoto(DialogInterface dialogInterface, int i) {
        switch (i) {
            case 0:
                mMainActivityCallback.takeSnapshotFromCamera(Const.REQUEST_PHOTO_FROM_CAMERA);
                break;
            case 1:
                mMainActivityCallback.loadImageFromGallery(Const.REQUEST_PHOTO_FROM_GALLERY);
                break;
            case 2:
                dialogInterface.cancel();
                break;
        }
    }

    private void chooseAvatar(DialogInterface dialogInterface, int i) {
        switch (i) {
            case 0:
                mMainActivityCallback.takeSnapshotFromCamera(Const.REQUEST_AVATAR_FROM_CAMERA);
                break;
            case 1:
                mMainActivityCallback.loadImageFromGallery(Const.REQUEST_AVATAR_FROM_GALLERY);
                break;
            case 2:
                dialogInterface.cancel();
                break;
        }
    }
    //endregion ::::::::::::::::::::::::::::::::::::::::::
}
