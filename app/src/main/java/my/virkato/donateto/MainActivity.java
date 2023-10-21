package my.virkato.donateto;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;


public class MainActivity extends AppCompatActivity {

    private InterstitialAd interstitialAd = null;
    private RewardedAd rewardedAd = null;

    private Button b_page;
    private Button b_rewarded;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // загрузка рекламного баннера
        Ads.load(findViewById(R.id.yandex_ads_banner), getString(R.string.ads_unit_banner), 320);

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
        interstitialAd = null;
        Ads.load(this, getString(R.string.ads_unit_page), new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                // здесь решать вопрос о показе страничной рекламы
                b_page.setEnabled(true);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                // здесь решать вопрос о повторной загрузке страничной рекламы
                Log.e("interstitial", "onAdFailedToLoad: " + adRequestError.getDescription());
//                Toast.makeText(getApplicationContext(), "Page: " + adRequestError.getDescription(), Toast.LENGTH_LONG).show();
                b_page.postDelayed(() -> loadPageAd(), 5000);
            }
        });
    }

    /**
     * Показать страничную рекламу, если загружена
     */
    private void showPageAd() {
        if (interstitialAd != null) {
            interstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                @Override
                public void onAdShown() {
                    // начало показа
                    b_page.setEnabled(false);
                }

                @Override
                public void onAdFailedToShow(@NonNull AdError adError) {
                    // срыв показа
                    Toast.makeText(getApplicationContext(), adError.getDescription(), Toast.LENGTH_SHORT).show();
                    loadPageAd();
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
                public void onAdImpression(@Nullable ImpressionData impressionData) {
                    // какие-то необработанные данные в JSON
                    String data = impressionData == null ? "" : impressionData.getRawData();
                    Log.d("interstitial", "onAdImpression: " + data);
//                    Toast.makeText(getApplicationContext(), impressionData.getRawData(), Toast.LENGTH_LONG).show();
                }
            });
            interstitialAd.show(this);
        }
    }

    /**
     * Загрузить наградную рекламу
     */
    private void loadRewardedAd() {
        b_rewarded.setEnabled(false);
        rewardedAd = null;
        Ads.load(this, getString(R.string.ads_unit_reward), new RewardedAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                // здесь решать вопрос о показе наградной рекламы
                b_rewarded.setEnabled(true);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                // здесь решать вопрос о повторной загрузке наградной рекламы
                Log.e("rewarded", "onAdFailedToLoad: " + adRequestError.getDescription());
//                Toast.makeText(getApplicationContext(), "Rewarded: " + adRequestError.getDescription(), Toast.LENGTH_LONG).show();
                b_rewarded.postDelayed(() -> loadRewardedAd(), 5000);
            }
        });
    }

    /**
     * Показать наградную рекламу, если загружена
     */
    private void showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd.setAdEventListener(new RewardedAdEventListener() {
                @Override
                public void onAdShown() {
                    // начало показа
                    b_rewarded.setEnabled(false);
                }

                @Override
                public void onAdFailedToShow(@NonNull AdError adError) {
                    // срыв показа
                    Toast.makeText(getApplicationContext(), adError.getDescription(), Toast.LENGTH_SHORT).show();
                    loadRewardedAd();
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
                public void onAdImpression(@Nullable ImpressionData impressionData) {
                    // какие-то необработанные данные в JSON
                    String data = impressionData == null ? "" : impressionData.getRawData();
                    Log.d("rewarded", "onAdImpression: " + data);
//                    Toast.makeText(getApplicationContext(), impressionData.getRawData(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onRewarded(@NonNull Reward reward) {
                    // награда получена
                    int amount = reward.getAmount(); // количество
                    String type = reward.getType(); // название награды
                    Toast.makeText(getApplicationContext(), "Получено: '" + type + "' = " + amount, Toast.LENGTH_SHORT).show();
                }
            });
            rewardedAd.show(this);
        }
    }
}