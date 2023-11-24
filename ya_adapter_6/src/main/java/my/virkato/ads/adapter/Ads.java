package my.virkato.ads.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;

import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdInfo;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

public class Ads {
    public static class AdUnit {
        private final InterstitialAd iAd;
        private final RewardedAd rAd;
        private final String uId;

        public AdUnit(InterstitialAd ad, String unitId) {
            iAd = ad;
            rAd = null;
            uId = unitId;
        }

        public AdUnit(RewardedAd ad, String unitId) {
            iAd = null;
            rAd = ad;
            uId = unitId;
        }

        public void show(/*@NonNull*/ Activity activity) {
            if (iAd != null) iAd.show(activity);
            if (rAd != null) rAd.show(activity);
        }

        public String getUnitId() {
            return uId;
        }
    }

    public interface CommonAdEventListener {
        void onAdLoaded(AdUnit ad);

        void onAdFailedToLoad(/*@NonNull*/ AdRequestError adRequestError);

        void onAdShown();

        void onAdDismissed();

        void onAdClicked();

        void onLeftApplication();

        void onReturnedToApplication();

        void onImpression(/*@Nullable*/ ImpressionData impressionData);

        void onRewarded(/*@NonNull*/ Reward rewarded);
    }

    private final CommonAdEventListener bannerListener;
    private final CommonAdEventListener interstitialListener;
    private final CommonAdEventListener rewardedListener;

    /**
     * Первоначальная настройка SDK
     *
     * @param context приложение
     */
    public Ads(Context context, CommonAdEventListener bannerAdEventListener, CommonAdEventListener interstitialAdEventListener, CommonAdEventListener rewardedAdEventListener) {
        bannerListener = bannerAdEventListener;
        interstitialListener = interstitialAdEventListener;
        rewardedListener = rewardedAdEventListener;
        MobileAds.initialize(context, () -> {
            // initialized
        });
        MobileAds.setAgeRestrictedUser(false);
        MobileAds.setLocationConsent(false);
        MobileAds.setUserConsent(false);
    }

    /**
     * Настройка баннера и загрузка рекламы
     *
     * @param banner   баннер
     * @param unitId   идентификатор
     * @param maxWidth максимальная ширина
     */
    public void loadBanner(BannerAdView banner, String unitId, int maxWidth) {
        banner.setAdUnitId(unitId);
        banner.setAdSize(BannerAdSize.stickySize(banner.getContext(), maxWidth));
        banner.loadAd(new AdRequest.Builder().build());
        banner.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                if (bannerListener != null) bannerListener.onAdLoaded(null);
                Log.d("banner", "onAdLoaded: !");
            }

            @Override
            public void onAdFailedToLoad(/*@NonNull*/ AdRequestError adRequestError) {
                if (bannerListener != null) bannerListener.onAdFailedToLoad(adRequestError);
                Log.d("banner", "onAdFailedToLoad: " + adRequestError.getDescription());
            }

            @Override
            public void onAdClicked() {
                if (bannerListener != null) bannerListener.onAdClicked();
                Log.d("banner", "onAdClicked: !");
            }

            @Override
            public void onLeftApplication() {
                if (bannerListener != null) bannerListener.onLeftApplication();
                Log.d("banner", "onLeftApplication: !");
            }

            @Override
            public void onReturnedToApplication() {
                if (bannerListener != null) bannerListener.onReturnedToApplication();
                Log.d("banner", "onReturnedToApplication: !");
            }

            @Override
            public void onImpression(/*@Nullable*/ ImpressionData impressionData) {
                if (bannerListener != null) bannerListener.onImpression(impressionData);
                String data = "";
                if (impressionData != null) {
                    data = impressionData.getRawData();
                }
                Log.d("banner", "onImpression: " + data);
            }
        });
    }

    /**
     * настройка страничной рекламы и загрузка
     *
     * @param context приложение
     * @param unitId  идентификатор
     */
    public void loadInterstitial(Context context, String unitId) {
        InterstitialAdLoader adLoader = new InterstitialAdLoader(context);
        adLoader.setAdLoadListener(new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(/*@NonNull*/ InterstitialAd loadedAd) {
                AdUnit adUnit = new AdUnit(loadedAd, unitId);
                loadedAd.setAdEventListener(new InterstitialAdEventListener() {
                    @Override
                    public void onAdShown() {
                        if (interstitialListener != null) interstitialListener.onAdShown();
                    }

                    @Override
                    public void onAdFailedToShow(/*@NonNull*/ AdError adError) {
                    }

                    @Override
                    public void onAdDismissed() {
                        if (interstitialListener != null) interstitialListener.onAdDismissed();
                    }

                    @Override
                    public void onAdClicked() {
                        if (interstitialListener != null) interstitialListener.onAdClicked();
                    }

                    @Override
                    public void onAdImpression(/*@Nullable*/ ImpressionData impressionData) {
                        if (interstitialListener != null) interstitialListener.onImpression(impressionData);
                    }
                });
                if (interstitialListener != null) interstitialListener.onAdLoaded(adUnit);
            }

            @Override
            public void onAdFailedToLoad(/*@NonNull*/ AdRequestError adRequestError) {
                if (interstitialListener != null) interstitialListener.onAdFailedToLoad(adRequestError);
            }
        });
        adLoader.loadAd(new AdRequestConfiguration.Builder(unitId).build());
    }

    /**
     * Настройка наградной рекламы и загрузка
     *
     * @param context приложение
     * @param unitId  идентификатор
     */
    public void loadRewarded(Context context, String unitId) {
        RewardedAdLoader adLoader = new RewardedAdLoader(context);
        adLoader.setAdLoadListener(new RewardedAdLoadListener() {
            @Override
            public void onAdLoaded(/*@NonNull*/ RewardedAd loadedAd) {
                AdUnit adUnit = new AdUnit(loadedAd, unitId);
                loadedAd.setAdEventListener(new RewardedAdEventListener() {
                    @Override
                    public void onAdShown() {
                        if (rewardedListener != null) rewardedListener.onAdShown();
                    }

                    @Override
                    public void onAdFailedToShow(/*@NonNull*/ AdError adError) {
                    }

                    @Override
                    public void onAdDismissed() {
                        if (rewardedListener != null) rewardedListener.onAdDismissed();
                    }

                    @Override
                    public void onAdClicked() {
                        if (rewardedListener != null) rewardedListener.onAdClicked();
                    }

                    @Override
                    public void onAdImpression(/*@Nullable*/ ImpressionData impressionData) {
                        if (rewardedListener != null) rewardedListener.onImpression(impressionData);
                    }

                    @Override
                    public void onRewarded(/*@NonNull*/ Reward rewarded) {
                        if (rewardedListener != null) rewardedListener.onRewarded(rewarded);
                    }
                });

                if (rewardedListener != null) rewardedListener.onAdLoaded(adUnit);
            }

            @Override
            public void onAdFailedToLoad(/*@NonNull*/ AdRequestError adRequestError) {
                if (rewardedListener != null) rewardedListener.onAdFailedToLoad(adRequestError);
            }
        });
        adLoader.loadAd(new AdRequestConfiguration.Builder(unitId).build());
    }
}
