package com.marverenic.music.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistActivity;
import com.marverenic.music.activity.instance.AutoPlaylistEditActivity;
import com.marverenic.music.activity.instance.PlaylistActivity;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;

import javax.inject.Inject;

import timber.log.Timber;

import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.marverenic.music.activity.instance.PlaylistActivity.PLAYLIST_EXTRA;

public class PlaylistViewModel extends BaseObservable {

    @Inject PlaylistStore mPlaylistStore;

    private Context mContext;
    private Playlist mPlaylist;

    public PlaylistViewModel(Context context) {
        mContext = context;
        JockeyApplication.getComponent(mContext).inject(this);
    }

    public void setPlaylist(Playlist playlist) {
        mPlaylist = playlist;
        notifyChange();
    }

    public String getName() {
        return mPlaylist.getPlaylistName();
    }

    public int getSmartIndicatorVisibility() {
        if (mPlaylist instanceof AutoPlaylist) {
            return VISIBLE;
        } else {
            return GONE;
        }
    }

    public View.OnClickListener onClickPlaylist() {
        return v -> {
            if (mPlaylist instanceof AutoPlaylist) {
                Intent intent = AutoPlaylistActivity.newIntent(mContext, (AutoPlaylist) mPlaylist);
                mContext.startActivity(intent);
            } else {
                Navigate.to(mContext, PlaylistActivity.class, PLAYLIST_EXTRA, mPlaylist);
            }
        };
    }

    public View.OnClickListener onClickMenu() {
        return v -> {
            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources().getStringArray(
                    (mPlaylist instanceof AutoPlaylist)
                            ? R.array.queue_options_smart_playlist
                            : R.array.queue_options_playlist);

            for (int i = 0; i < options.length;  i++) {
                menu.getMenu().add(Menu.NONE, i, i, options[i]);
            }

            menu.setOnMenuItemClickListener(
                    (mPlaylist instanceof  AutoPlaylist)
                            ? onSmartMenuItemClick(v)
                            : onMenuItemClick(v));

            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Queue this playlist next
                    queuePlaylistNext();
                    return true;
                case 1: //Queue this playlist last
                    queuePlaylistLast();
                    return true;
                case 2: //Delete this playlist
                    deletePlaylist(view);
                    return true;
            }
            return false;
        };
    }

    private PopupMenu.OnMenuItemClickListener onSmartMenuItemClick(View view) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case 0: //Queue this playlist next
                    queuePlaylistNext();
                    return true;
                case 1: //Queue this playlist last
                    queuePlaylistLast();
                    return true;
                case 2: //Edit this playlist
                    editThisAsAutoPlaylist();
                    return true;
                case 3: // Delete this playlist
                    deleteAutoPlaylist(view);
                    return true;
            }
            return false;
        };
    }

    private void queuePlaylistNext() {
        mPlaylistStore.getSongs(mPlaylist).subscribe(
                PlayerController::queueNext,
                throwable -> {
                    Timber.e(throwable, "Failed to get songs");
                });
    }

    private void queuePlaylistLast() {
        mPlaylistStore.getSongs(mPlaylist).subscribe(
                PlayerController::queueLast,
                throwable -> {
                    Timber.e(throwable, "Failed to get songs");
                });
    }

    private void editThisAsAutoPlaylist() {
        AutoPlaylist autoPlaylist = (AutoPlaylist) mPlaylist;
        Intent intent = AutoPlaylistEditActivity.newIntent(mContext, autoPlaylist);
        mContext.startActivity(intent);
    }

    private void deletePlaylist(View snackbarContainer) {
        Playlist removed = mPlaylist;
        String playlistName = mPlaylist.getPlaylistName();
        String message = mContext.getString(R.string.message_removed_playlist, playlistName);

        mPlaylistStore.getSongs(removed)
                .subscribe(originalContents -> {
                    mPlaylistStore.removePlaylist(removed);

                    Snackbar.make(snackbarContainer, message, LENGTH_LONG)
                            .setAction(R.string.action_undo, view -> {
                                mPlaylistStore.makePlaylist(playlistName, originalContents);
                            })
                            .show();
                }, throwable -> {
                    Timber.e(throwable, "Failed to get playlist contents");

                    // If we can't get the original contents of the playlist, remove it anyway but
                    // don't give an undo option
                    mPlaylistStore.removePlaylist(removed);
                    Snackbar.make(snackbarContainer, message, LENGTH_LONG).show();
                });
    }

    private void deleteAutoPlaylist(View snackbarContainer) {
        mPlaylistStore.removePlaylist(mPlaylist);

        String playlistName = mPlaylist.getPlaylistName();
        String message = mContext.getString(R.string.message_removed_playlist, playlistName);
        AutoPlaylist removed = (AutoPlaylist) mPlaylist;

        Snackbar.make(snackbarContainer, message, LENGTH_LONG)
                .setAction(R.string.action_undo, view -> {
                    mPlaylistStore.makePlaylist(removed);
                })
                .show();
    }

}