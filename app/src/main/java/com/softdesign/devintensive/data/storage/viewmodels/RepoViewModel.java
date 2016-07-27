package com.softdesign.devintensive.data.storage.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;

import com.softdesign.devintensive.BR;

public class RepoViewModel extends BaseObservable implements Parcelable {

    private String mRepoUri;
    private boolean isEnabled;
    private boolean isCanBeEdit;

    //region Parcel
    protected RepoViewModel(Parcel in) {
        mRepoUri = in.readString();
        isEnabled = in.readByte() != 0x00;
        isCanBeEdit = in.readByte() != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mRepoUri);
        dest.writeByte((byte) (isEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (isCanBeEdit ? 0x01 : 0x00));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<RepoViewModel> CREATOR = new Parcelable.Creator<RepoViewModel>() {
        @Override
        public RepoViewModel createFromParcel(Parcel in) {
            return new RepoViewModel(in);
        }

        @Override
        public RepoViewModel[] newArray(int size) {
            return new RepoViewModel[size];
        }
    };
    //endregion

    public RepoViewModel(String repoUri, boolean isEnabled, boolean isCanBeEdit) {
        this.mRepoUri = repoUri;
        this.isEnabled = isEnabled;
        this.isCanBeEdit = isCanBeEdit;
    }

    @Bindable
    public boolean isEnabled() {
        return isEnabled;
    }

    @Bindable
    public boolean isCanBeEdit() {
        return isCanBeEdit;
    }

    @Bindable
    public String getRepoUri() {
        return mRepoUri;
    }

    // ---------- SETTERS ----------
    public void setRepoUri(String repoUri) {
        mRepoUri = repoUri;
        notifyPropertyChanged(BR.repoUri);
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        notifyPropertyChanged(BR.enabled);
    }

    public void setCanBeEdit(boolean canBeEdit) {
        isCanBeEdit = canBeEdit;
        notifyPropertyChanged(BR.canBeEdit);
    }
}
