package com.bubblant.game;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AdMob integration: GDPR consent (UMP), banner, interstitial (frequency capped) and rewarded ads.
 * Ad unit ids come from BuildConfig (test ids in debug builds).
 */
public final class AdManager {
    private static final long INTERSTITIAL_GAP_MS = 75_000;
    private static final int LEVELS_BETWEEN_INTERSTITIALS = 3;

    private static ConsentInformation consent;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static InterstitialAd interstitial;
    private static RewardedAd rewarded;
    private static boolean loadingInter, loadingRewarded;
    private static long lastInterstitial = SystemClock.elapsedRealtime();
    private static int levelEnds;
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static Context app;

    public static void gatherConsent(Activity a) {
        app = a.getApplicationContext();
        consent = UserMessagingPlatform.getConsentInformation(a);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(a, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(a, err -> {
                    if (consent.canRequestAds()) init();
                }),
                err -> {
                    if (consent.canRequestAds()) init();
                });
        if (consent.canRequestAds()) init();
    }

    private static void init() {
        if (!initialized.compareAndSet(false, true)) return;
        RequestConfiguration cfg = MobileAds.getRequestConfiguration().toBuilder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                .build();
        MobileAds.setRequestConfiguration(cfg);
        new Thread(() -> {
            MobileAds.initialize(app, status -> {});
            main.post(() -> {
                loadInterstitial();
                loadRewarded();
            });
        }).start();
    }

    public static boolean ready() { return initialized.get(); }

    // ---- banner ----
    public static AdView loadBanner(Activity a, FrameLayout container) {
        if (!ready()) {
            main.postDelayed(() -> { if (!a.isFinishing()) loadBanner(a, container); }, 1500);
            return null;
        }
        container.removeAllViews();
        AdView view = new AdView(a);
        view.setAdUnitId(BuildConfig.AD_BANNER);
        DisplayMetrics dm = a.getResources().getDisplayMetrics();
        int widthDp = (int) (dm.widthPixels / dm.density);
        view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(a, widthDp));
        container.addView(view);
        view.loadAd(new AdRequest.Builder().build());
        return view;
    }

    // ---- interstitial ----
    private static void loadInterstitial() {
        if (!ready() || interstitial != null || loadingInter) return;
        loadingInter = true;
        InterstitialAd.load(app, BuildConfig.AD_INTERSTITIAL, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull InterstitialAd ad) { interstitial = ad; loadingInter = false; }
            @Override public void onAdFailedToLoad(@NonNull LoadAdError e) { interstitial = null; loadingInter = false; }
        });
    }

    /** Called when a level ends; occasionally shows an interstitial, then runs {@code after}. */
    public static void onLevelEnd(Activity a, int levelNumber, Runnable after) {
        levelEnds++;
        long now = SystemClock.elapsedRealtime();
        boolean due = levelNumber > 3 && levelEnds >= LEVELS_BETWEEN_INTERSTITIALS && now - lastInterstitial > INTERSTITIAL_GAP_MS;
        if (!due || interstitial == null) {
            loadInterstitial();
            after.run();
            return;
        }
        InterstitialAd ad = interstitial;
        interstitial = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { loadInterstitial(); after.run(); }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError e) { loadInterstitial(); after.run(); }
        });
        levelEnds = 0;
        lastInterstitial = now;
        ad.show(a);
    }

    // ---- rewarded ----
    private static void loadRewarded() {
        if (!ready() || rewarded != null || loadingRewarded) return;
        loadingRewarded = true;
        RewardedAd.load(app, BuildConfig.AD_REWARDED, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull RewardedAd ad) { rewarded = ad; loadingRewarded = false; }
            @Override public void onAdFailedToLoad(@NonNull LoadAdError e) { rewarded = null; loadingRewarded = false; }
        });
    }

    public static boolean rewardedReady() {
        if (rewarded == null) loadRewarded();
        return rewarded != null;
    }

    public static void showRewarded(Activity a, Runnable onReward, Runnable onClosed) {
        RewardedAd ad = rewarded;
        if (ad == null) { loadRewarded(); onClosed.run(); return; }
        rewarded = null;
        final boolean[] earned = {false};
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                loadRewarded();
                if (earned[0]) onReward.run(); else onClosed.run();
            }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError e) { loadRewarded(); onClosed.run(); }
        });
        ad.show(a, item -> earned[0] = true);
        lastInterstitial = SystemClock.elapsedRealtime(); // don't stack an interstitial right after
    }

    // ---- privacy ----
    public static boolean privacyOptionsRequired() {
        return consent != null && consent.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void showPrivacyOptions(Activity a) {
        UserMessagingPlatform.showPrivacyOptionsForm(a, err -> {});
    }

    private AdManager() {}
}
