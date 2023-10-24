package my.virkato.donateto;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;


public class Ads {

    /**
     * Оплата за просмотры рекламы в соответствующем рекламном блоке
     */
    private static Map<String, BigDecimal> prices = new HashMap<>();

    private Ads() {
    }

    /**
     * Первоначальная настройка SDK
     *
     * @param context приложение
     */
    public static void init(Context context) {
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
    public static void load(BannerAdView banner, String unitId, int maxWidth) {
        banner.setAdUnitId(unitId);
        banner.setAdSize(BannerAdSize.stickySize(banner.getContext(), maxWidth));
        banner.loadAd(new AdRequest.Builder().build());
        banner.setBannerAdEventListener(new BannerAdEventListener() {
            // за баннер не плюсуем баланс, т.к. нет гарантии, что баннер виден на экране

            @Override
            public void onAdLoaded() {
                Log.d("banner", "onAdLoaded: !");
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                Log.d("banner", "onAdFailedToLoad: " + adRequestError.getDescription());
            }

            @Override
            public void onAdClicked() {
                Log.d("banner", "onAdClicked: !");
            }

            @Override
            public void onLeftApplication() {
                Log.d("banner", "onLeftApplication: !");
            }

            @Override
            public void onReturnedToApplication() {
                Log.d("banner", "onReturnedToApplication: !");
            }

            @Override
            public void onImpression(@Nullable ImpressionData impressionData) {
                String data = "";
                if (impressionData != null) {
                    data = impressionData.getRawData();
                }
                Log.d("banner", "onImpression: " + data);
                try {
                    // вытащили из данных о показываемой рекламе
                    setPrice(unitId, new BigDecimal(new JSONObject(data).getString("revenue")));
                } catch (JSONException e) {
                    Log.e("banner", e.getLocalizedMessage());
                }
            }
        });
    }

    /**
     * настройка страничной рекламы и загрузка
     *
     * @param context        приложение
     * @param unitId         идентификатор
     * @param adLoadListener обработчик процесса загрузки
     */
    public static void load(Context context, String unitId, InterstitialAdLoadListener adLoadListener) {
        InterstitialAdLoader adLoader = new InterstitialAdLoader(context);
        adLoader.setAdLoadListener(adLoadListener);
        adLoader.loadAd(new AdRequestConfiguration.Builder(unitId).build());
    }

    /**
     * Настройка наградной рекламы и загрузка
     *
     * @param context        приложение
     * @param unitId         идентификатор
     * @param adLoadListener обработчик процесса загрузки
     */
    public static void load(Context context, String unitId, RewardedAdLoadListener adLoadListener) {
        RewardedAdLoader adLoader = new RewardedAdLoader(context);
        adLoader.setAdLoadListener(adLoadListener);
        adLoader.loadAd(new AdRequestConfiguration.Builder(unitId).build());
    }

    /**
     * Получить цену за просмотр рекламы в указанном рекламном блоке
     *
     * @param unitId рекламный блок
     * @return цена
     */
    public static BigDecimal getPrice(String unitId) {
        BigDecimal price = prices.get(unitId);
        if (price == null) price = BigDecimal.ZERO;
        // вернём для расчётов только половину цены
        return price.divide(BigDecimal.valueOf(2), RoundingMode.HALF_DOWN);
    }

    /**
     * Установить цену за просмотр рекламы в указанном рекламном блоке
     *
     * @param unitId рекламный блок
     * @param price  цена
     */
    public static void setPrice(String unitId, BigDecimal price) {
        prices.put(unitId, price);
    }
}
