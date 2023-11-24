package my.virkato.donateto;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.rewarded.Reward;

import my.virkato.ads.adapter.Ads;


public class MainActivity extends AppCompatActivity {

    private Ads.AdUnit pageAd = null;
    private Ads.AdUnit rewardedAd = null;

    private Button b_page;
    private Button b_rewarded;

    private final Ads.CommonAdEventListener interstitialListener = new Ads.CommonAdEventListener() {
        @Override
        public void onAdLoaded(Ads.AdUnit ad) {
            pageAd = ad;
            // здесь решать вопрос о показе страничной рекламы
            b_page.setEnabled(true);
        }

        @Override
        public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
            // здесь решать вопрос о повторной загрузке страничной рекламы
            Log.e("interstitial", "onAdFailedToLoad: " + adRequestError.getDescription());
            b_page.postDelayed(() -> loadPageAd(), 5000);
        }

        @Override
        public void onAdShown() {
            // начало показа
            b_page.setEnabled(false);
        }

        @Override
        public void onAdDismissed() {
            // показ завершён
            Toast.makeText(getApplicationContext(), "Спасибо за просмотр", Toast.LENGTH_SHORT).show();
            loadPageAd();
        }

        @Override
        public void onAdClicked() {
            // пользователь кликнул рекламу
            Toast.makeText(getApplicationContext(), "Клик зафиксирован", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLeftApplication() {
        }

        @Override
        public void onReturnedToApplication() {
        }

        @Override
        public void onImpression(@Nullable ImpressionData impressionData) {
            // какие-то необработанные данные в JSON
            String data = impressionData == null ? "" : impressionData.getRawData();
            Log.d("interstitial", "onAdImpression: " + data);
        }

        @Override
        public void onRewarded(@NonNull Reward rewarded) {
        }
    };


    private final Ads.CommonAdEventListener rewardedListener = new Ads.CommonAdEventListener() {
        @Override
        public void onAdLoaded(Ads.AdUnit ad) {
            rewardedAd = ad;
            // здесь решать вопрос о показе наградной рекламы
            b_rewarded.setEnabled(true);
        }

        @Override
        public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
            // здесь решать вопрос о повторной загрузке наградной рекламы
            Log.e("rewarded", "onAdFailedToLoad: " + adRequestError.getDescription());
            b_rewarded.postDelayed(() -> loadRewardedAd(), 5000);
        }

        @Override
        public void onAdShown() {
            // начало показа
            b_rewarded.setEnabled(false);
        }

        @Override
        public void onAdDismissed() {
            // показ завершён
            Toast.makeText(getApplicationContext(), "Спасибо за просмотр", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }

        @Override
        public void onAdClicked() {
            // пользователь кликнул рекламу
            Toast.makeText(getApplicationContext(), "Клик зафиксирован", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLeftApplication() {
        }

        @Override
        public void onReturnedToApplication() {
        }

        @Override
        public void onImpression(@Nullable ImpressionData impressionData) {
            // какие-то необработанные данные в JSON
            String data = impressionData == null ? "" : impressionData.getRawData();
            Log.d("rewarded", "onAdImpression: " + data);
        }

        @Override
        public void onRewarded(@NonNull Reward rewarded) {
            // награда получена
            int amount = rewarded.getAmount(); // количество
            String type = rewarded.getType();  // название награды
            Toast.makeText(getApplicationContext(), "Получено: '" + type + "' = " + amount, Toast.LENGTH_SHORT).show();
        }
    };


    /**
     * Рекламный сервис (подключенная библиотека)
     */
    private Ads ads;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // инициализация рекламного сервиса
        ads = new Ads(this, null, interstitialListener, rewardedListener);

        // загрузка рекламного баннера
        ads.loadBanner(findViewById(R.id.yandex_ads_banner), getString(R.string.ads_unit_banner), 320);

        // загрузка страничной рекламы
        b_page = findViewById(R.id.b_page);
        b_page.setOnClickListener(view -> showPageAd());
        loadPageAd();

        // загрузка наградной рекламы (видео)
        b_rewarded = findViewById(R.id.b_rewarded);
        b_rewarded.setOnClickListener(view -> showRewardedAd());
        loadRewardedAd();
    }

    /**
     * Загрузить страничную рекламу
     */
    private void loadPageAd() {
        b_page.setEnabled(false);
        pageAd = null;
        ads.loadInterstitial(this, getString(R.string.ads_unit_page));
    }

    /**
     * Показать страничную рекламу, если загружена
     */
    private void showPageAd() {
        if (pageAd != null) pageAd.show(this);
    }

    /**
     * Загрузить наградную рекламу
     */
    private void loadRewardedAd() {
        b_rewarded.setEnabled(false);
        rewardedAd = null;
        ads.loadRewarded(this, getString(R.string.ads_unit_reward));
    }

    /**
     * Показать наградную рекламу, если загружена
     */
    private void showRewardedAd() {
        if (rewardedAd != null) rewardedAd.show(this);
    }
}