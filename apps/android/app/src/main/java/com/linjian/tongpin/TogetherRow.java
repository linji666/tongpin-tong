package com.linjian.tongpin;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.linjian.tongpin.data.PlaybackSnapshot;
import com.linjian.tongpin.data.Prefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * 播放页下面的一条“一起听”。
 * 左边是林霁，右边是桐桐，两个头像随歌曲进度从两边往中间靠，走到底就碰在一起。
 * 不修改 MainActivity 的原有布局，只往它的播放区里追加一层。
 */
public final class TogetherRow extends LinearLayout {

    private static final String TAG = "tongpin.together";
    private static final long TICK_MS = 500L;

    private static final int AVATAR_DP = 56;
    private static final int GAP_OPEN_DP = 104;
    private static final int GAP_TOUCH_DP = 4;

    private static final String NESTED =
            "apps/android/app/src/main/res/drawable-nodpi/"
                    + "apps/android/app/src/main/res/drawable-nodpi/";

    private static final String[] HOST_ME = new String[]{
            "https://cdn.jsdelivr.net/gh/linji666/tongpin-tong@main/" + NESTED + "avatar_me.jpg",
            "https://raw.githubusercontent.com/linji666/tongpin-tong/main/" + NESTED + "avatar_me.jpg",
            "https://ghproxy.net/https://raw.githubusercontent.com/linji666/tongpin-tong/main/" + NESTED + "avatar_me.jpg"
    };

    private static final String[] HOST_HER = new String[]{
            "https://cdn.jsdelivr.net/gh/linji666/tongpin-tong@main/" + NESTED + "avatar_her.jpg",
            "https://raw.githubusercontent.com/linji666/tongpin-tong/main/" + NESTED + "avatar_her.jpg",
            "https://ghproxy.net/https://raw.githubusercontent.com/linji666/tongpin-tong/main/" + NESTED + "avatar_her.jpg"
    };

    private static final int COLOR_CARD = 0xFFF4EDE2;
    private static final int COLOR_BORDER = 0xFFE8DAC9;
    private static final int COLOR_HINT = 0xFF7B6F62;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ImageView leftAvatar;
    private final ImageView rightAvatar;
    private final TextView hint;
    private final View spacer;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private TogetherRow(Activity activity) {
        super(activity);
        setOrientation(VERTICAL);
        setPadding(dp(16), dp(18), dp(16), dp(16));
        setBackground(rounded(dp(20), COLOR_CARD, COLOR_BORDER));

        TextView title = new TextView(activity);
        title.setText("一起听");
        title.setTextSize(12f);
        title.setTextColor(COLOR_HINT);
        title.setTypeface(Typeface.create("sans", Typeface.BOLD));
        addView(title);

        LinearLayout stage = new LinearLayout(activity);
        stage.setOrientation(HORIZONTAL);
        stage.setGravity(Gravity.CENTER);
        stage.setPadding(0, dp(14), 0, dp(6));
        addView(stage, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        leftAvatar = avatar(activity);
        stage.addView(leftAvatar, circleParams());

        spacer = new View(activity);
        stage.addView(spacer, new LayoutParams(dp(GAP_OPEN_DP), dp(2)));

        rightAvatar = avatar(activity);
        stage.addView(rightAvatar, circleParams());

        bindAvatar(activity, leftAvatar, HOST_ME, "avatar_me.jpg", R.drawable.ic_tong_dog);
        bindAvatar(activity, rightAvatar, HOST_HER, "avatar_her.jpg", R.drawable.ic_tong_cat);

        hint = new TextView(activity);
        hint.setTextSize(12f);
        hint.setTextColor(COLOR_HINT);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(8), 0, 0);
        hint.setText("还没开始放");
        addView(hint);
    }

    public static void attach(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        ProgressBar probe = findProgressBar(content);
        if (probe == null) return;
        View card = (View) probe.getParent();
        if (!(card instanceof ViewGroup)) return;
        View host = (View) card.getParent();
        if (!(host instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) host;
        if (group.findViewWithTag(TAG) != null) return;
        TogetherRow row = new TogetherRow(activity);
        row.setTag(TAG);
        group.addView(row, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.start();
    }

    private static ProgressBar findProgressBar(View view) {
        if (view instanceof ProgressBar) return (ProgressBar) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ProgressBar found = findProgressBar(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void start() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }

    private void refresh() {
        Activity activity = (Activity) getContext();
        PlaybackSnapshot snap = Prefs.playback(activity);
        long duration = snap.durationMs;
        long position = snap.positionMs;
        if (snap.playing && snap.observedAt > 0L) {
            position += Math.max(0L, System.currentTimeMillis() - snap.observedAt);
        }
        if (duration > 0L) position = Math.min(position, duration);

        float progress = duration > 0L ? Math.min(1f, position / (float) duration) : 0f;
        int gap = Math.round(GAP_OPEN_DP + (GAP_TOUCH_DP - GAP_OPEN_DP) * progress);

        ViewGroup.LayoutParams params = spacer.getLayoutParams();
        if (params.width != dp(gap)) {
            params.width = dp(gap);
            spacer.setLayoutParams(params);
        }

        boolean touching = progress >= 0.985f;
        float scale = touching ? 1.12f : 1f;
        leftAvatar.setScaleX(scale);
        leftAvatar.setScaleY(scale);
        rightAvatar.setScaleX(scale);
        rightAvatar.setScaleY(scale);
        leftAvatar.setBackground(touching ? ovalGlow() : null);
        rightAvatar.setBackground(touching ? ovalGlow() : null);

        String name = snap.title == null ? "" : snap.title.trim();
        if (name.isEmpty() || "等待播放器".equals(name)) {
            hint.setText("还没开始放");
        } else if (touching) {
            hint.setText("碰到啦 · 《" + name + "》");
        } else {
            hint.setText("《" + name + "》 · " + Math.round(progress * 100f) + "%");
        }
    }

    private static ImageView avatar(Activity activity) {
        ImageView view = new ImageView(activity);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setClipToOutline(true);
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View target, Outline outline) {
                outline.setOval(0, 0, target.getWidth(), target.getHeight());
            }
        });
        return view;
    }

    /** 先放内置图，再从缓存或网络换成真头像；多个源依次试。 */
    private static void bindAvatar(
            Context context,
            final ImageView view,
            final String[] hosts,
            String cacheName,
            int fallbackRes
    ) {
        view.setImageResource(fallbackRes);
        final File cache = new File(context.getCacheDir(), cacheName);
        Bitmap cached = BitmapFactory.decodeFile(cache.getAbsolutePath());
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String host : hosts) {
                    if (download(host, cache)) {
                        final Bitmap bitmap = BitmapFactory.decodeFile(cache.getAbsolutePath());
                        if (bitmap == null) continue;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                view.setImageBitmap(bitmap);
                            }
                        });
                        return;
                    }
                }
            }
        }).start();
    }

    private static boolean download(String url, File target) {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "tongpin-android");
            InputStream input = connection.getInputStream();
            File temp = new File(target.getAbsolutePath() + ".part");
            FileOutputStream output = new FileOutputStream(temp);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
            output.close();
            input.close();
            if (temp.length() < 1024) {
                temp.delete();
                return false;
            }
            if (target.exists()) target.delete();
            return temp.renameTo(target);
        } catch (Throwable error) {
            return false;
        }
    }

    private LayoutParams circleParams() {
        return new LayoutParams(dp(AVATAR_DP), dp(AVATAR_DP));
    }

    private static GradientDrawable ovalGlow() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(0x33FFD9A8);
        drawable.setStroke(4, 0xFFFFC978);
        return drawable;
    }

    private static GradientDrawable rounded(int radius, int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(radius);
        drawable.setColor(fill);
        drawable.setStroke(1, stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
