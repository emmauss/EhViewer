/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.gallery;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import com.hippo.gl.glrenderer.GLCanvas;
import com.hippo.gl.view.GLRoot;
import com.hippo.image.Image;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.ConcurrentPool;
import com.hippo.yorozuya.OSUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class GalleryProvider {

    public static final int STATE_WAIT = -1;
    public static final int STATE_ERROR = -2;

    // With dot
    public static final String[] SUPPORT_IMAGE_EXTENSIONS = {
            ".jpg", // Joint Photographic Experts Group
            ".jpeg",
            ".png", // Portable Network Graphics
            ".gif", // Graphics Interchange Format
    };

    private final ConcurrentPool<NotifyTask> mNotifyTaskPool = new ConcurrentPool<>(5);
    private volatile GalleryProviderListener mGalleryProviderListener;
    private volatile GLRoot mGLRoot;

    private boolean mStarted = false;

    @UiThread
    public void start() {
        OSUtils.checkMainLoop();

        if (mStarted) {
            throw new IllegalStateException("Can't start it twice");
        }
        mStarted = true;
    }

    @UiThread
    public void stop() {
        OSUtils.checkMainLoop();
    }

    public void setGLRoot(GLRoot glRoot) {
        mGLRoot = glRoot;
    }


    /**
     * @return {@link #STATE_WAIT} for wait, 0 for empty
     */
    public abstract int size();

    public abstract void request(int index);

    public void forceRequest(int index) {
        request(index);
    }

    public abstract void cancelRequest(int index);

    public abstract String getError();

    public int getStartPage() {
        return 0;
    }

    /**
     * @return without extension
     */
    @NonNull
    public abstract String getImageFilename(int index);

    public abstract boolean save(int index, @NonNull UniFile file);

    /**
     * @param filename without extension
     */
    @Nullable
    public abstract UniFile save(int index, @NonNull UniFile dir, @NonNull String filename);

    public void putStartPage(int page) {}

    public void setGalleryProviderListener(GalleryProviderListener listener) {
        mGalleryProviderListener = listener;
    }

    public void notifyDataChanged() {
        notify(NotifyTask.TYPE_DATA_CHANGED, -1, 0.0f, null, null);
    }

    public void notifyDataChanged(int index) {
        notify(NotifyTask.TYPE_DATA_CHANGED, index, 0.0f, null, null);
    }

    public void notifyPageWait(int index) {
        notify(NotifyTask.TYPE_WAIT, index, 0.0f, null, null);
    }

    public void notifyPagePercent(int index, float percent) {
        notify(NotifyTask.TYPE_PERCENT, index, percent, null, null);
    }

    public void notifyPageSucceed(int index, Image image) {
        notify(NotifyTask.TYPE_SUCCEED, index, 0.0f, image, null);
    }

    public void notifyPageFailed(int index, String error) {
        notify(NotifyTask.TYPE_FAILED, index, 0.0f, null, error);
    }

    private void notify(@NotifyTask.Type int type, int index, float percent, Image image, String error) {
        GalleryProviderListener listener = mGalleryProviderListener;
        if (listener == null) {
            return;
        }

        GLRoot glRoot = mGLRoot;
        if (glRoot == null) {
            return;
        }

        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask(listener, mNotifyTaskPool);
        }
        task.setData(type, index, percent, image, error);
        glRoot.addOnGLIdleListener(task);
    }

    private static class NotifyTask implements GLRoot.OnGLIdleListener {

        @IntDef({TYPE_DATA_CHANGED, NotifyTask.TYPE_WAIT, TYPE_PERCENT, TYPE_SUCCEED, TYPE_FAILED})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {}

        public static final int TYPE_DATA_CHANGED = 0;
        public static final int TYPE_WAIT = 1;
        public static final int TYPE_PERCENT = 2;
        public static final int TYPE_SUCCEED = 3;
        public static final int TYPE_FAILED = 4;

        private final GalleryProviderListener mGalleryProviderListener;
        private final ConcurrentPool<NotifyTask> mPool;

        @Type
        private int mType;
        private int mIndex;
        private float mPercent;
        private Image mImage;
        private String mError;

        public NotifyTask(GalleryProviderListener galleryProviderListener, ConcurrentPool<NotifyTask> pool) {
            mGalleryProviderListener = galleryProviderListener;
            mPool = pool;
        }

        public void setData(@Type int type, int index, float percent, Image image, String error) {
            mType = type;
            mIndex = index;
            mPercent = percent;
            mImage = image;
            mError = error;
        }

        @Override
        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
            switch (mType) {
                case TYPE_DATA_CHANGED:
                    if (mIndex < 0) {
                        mGalleryProviderListener.onDataChanged();
                    } else {
                        mGalleryProviderListener.onDataChanged(mIndex);
                    }
                    break;
                case TYPE_WAIT:
                    mGalleryProviderListener.onPageWait(mIndex);
                    break;
                case TYPE_PERCENT:
                    mGalleryProviderListener.onPagePercent(mIndex, mPercent);
                    break;
                case TYPE_SUCCEED:
                    mGalleryProviderListener.onPageSucceed(mIndex, mImage);
                    break;
                case TYPE_FAILED:
                    mGalleryProviderListener.onPageFailed(mIndex, mError);
                    break;
            }

            // Clean data
            mImage = null;
            mError = null;
            // Push back
            mPool.push(this);

            return false;
        }
    }
}
