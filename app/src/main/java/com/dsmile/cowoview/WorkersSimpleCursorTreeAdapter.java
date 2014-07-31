package com.dsmile.cowoview;

import java.util.HashMap;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.graphics.Bitmap;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.Loader;
import android.widget.SimpleCursorTreeAdapter;
import static android.provider.BaseColumns._ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class WorkersSimpleCursorTreeAdapter extends SimpleCursorTreeAdapter {

    private MainActivity mActivity;
    protected final HashMap<Integer, Integer> mGroupMap;
    private LayoutInflater mInflater;

    // Please Note: Here cursor is not provided to avoid querying on main
    // thread.
    public WorkersSimpleCursorTreeAdapter(Context context, int groupLayout,
                                          int childLayout, String[] groupFrom, int[] groupTo,
                                          String[] childrenFrom, int[] childrenTo) {

        super(context, null, groupLayout, groupFrom, groupTo, childLayout, childrenFrom, childrenTo);
        mActivity = (MainActivity) context;
        mInflater = LayoutInflater.from(context);
        mGroupMap = new HashMap<Integer, Integer>();
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        // Logic to get the child cursor on the basis of selected group.
        int groupPos = groupCursor.getPosition();
        int groupId = groupCursor.getInt(groupCursor.getColumnIndex(_ID));

        mGroupMap.put(groupId, groupPos);
        Loader<Cursor> loader = mActivity.getSupportLoaderManager().getLoader(groupId);
        if (loader != null && !loader.isReset()) {
            mActivity.getSupportLoaderManager().restartLoader(groupId, null, mActivity);
        } else {
            mActivity.getSupportLoaderManager().initLoader(groupId, null, mActivity);
        }

        return null;

    }

    @Override
    public View newChildView(Context context, Cursor cursor,
                             boolean isLastChild, ViewGroup parent) {

        final View view = mInflater.inflate(R.layout.list_child, parent, false);
        return view;
    }

    @Override
    public void bindChildView(View view, Context context, Cursor cursor,
                              boolean isLastChild) {
        TextView txtListAge = (TextView) view.findViewById(R.id.lblListAge);
        TextView txtListName = (TextView) view.findViewById(R.id.lblListChildName);
        ImageView imgAvatar = (ImageView) view.findViewById(R.id.avatar);

        txtListName.setText(cursor.getString(2) + " " + cursor.getString(1));

        txtListAge.setText(DataConditioning.properAgeLabel(DataConditioning.properAge(cursor.getString(3))));
        UrlImageViewHelper.setUrlDrawable(imgAvatar, cursor.getString(4), R.drawable.no_avatar,
                new UrlImageViewCallback() {
                    @Override
                    public void onLoaded(ImageView imageView, Bitmap loadedBitmap, String url,
                                         boolean loadedFromCache) {
                        if (!loadedFromCache) {
                            ScaleAnimation scale = new ScaleAnimation(0, 1, 0, 1,
                                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                            scale.setDuration(300);
                            scale.setInterpolator(new OvershootInterpolator());
                            imageView.startAnimation(scale);
                        }
                }
        });
    }

    public HashMap<Integer, Integer> getGroupMap() {
        return mGroupMap;
    }

}