package my.virkato.donateto;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputLayout;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class MainActivity extends AppCompatActivity {

    private InterstitialAd interstitialAd = null;
    private RewardedAd rewardedAd = null;

    private Button b_page;
    private Button b_rewarded;
    private TextView t_balance;
    private Button b_withdraw;
    private TextInputLayout til_phone;

    private BigDecimal balance = BigDecimal.ZERO;

    private final SharedPreferences.OnSharedPreferenceChangeListener onChaged = (sharedPreferences, key) -> {
        if (key != null && key.equals("balance")) {
            balance = new BigDecimal(sharedPreferences.getString(key, "0.0"))
                    .setScale(3, RoundingMode.HALF_UP);
            t_balance.setText(balance.toString());

            // кнопка вывода доступна при балансе >= 10
            b_withdraw.setEnabled(balance.compareTo(BigDecimal.TEN) >= 0);
        }
    };


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

        // следим за изменение баланса в Shared Preferences
        til_phone = findViewById(R.id.til_phone);
        b_withdraw = findViewById(R.id.b_withdraw);
        b_withdraw.setEnabled(false);
        b_withdraw.setOnClickListener(v -> {
            // подать заявку в телегу
            String phone = til_phone.getEditText().getText().toString().trim();
            if (!phone.equals("") && balance.compareTo(BigDecimal.valueOf(100)) >= 0) {
                // если указан номер телефона и баланс >= 100
                sendWithdrawalTicket(phone);
            }
        });
        t_balance = findViewById(R.id.t_balance);
        balance = new BigDecimal(MainApplication.getBalance()).setScale(3, RoundingMode.HALF_UP);
        t_balance.setText(balance.toString());
        MainApplication.getSharedPreferences().registerOnSharedPreferenceChangeListener(onChaged);

        //todo убрать после теста
        sendWithdrawalTicket("VirKato");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainApplication.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onChaged);
    }

    private void sendWithdrawalTicket(String phone) {
        String token = "1234567890:ABCDEFGHIJKLMNO"; // спросить https://t.me/BotFather
        String url = "https://api.telegram.org/bot" + token + "/sendMessage";
        JSONObject jo = new JSONObject();
        try {
            jo.put("chat_id", "1234567890"); // спросить https://t.me/getmyid_bot
            jo.put("text", "Withdrawal ticket from " + phone + " for " + balance);
        } catch (JSONException ignored) {
        }

        final JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, jo,
                response -> {
                    Log.e("bot", response.toString());
                    try {
                        if (response.getBoolean("ok")) {
                            // обнуляем баланс
                            MainApplication.setBalance(BigDecimal.ZERO);
                        }
                    } catch (JSONException ignore) {
                    }
                },
                error -> {
                }
        );
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
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
                // за страничную рекламу добавляем полцены к балансу
                private final String unitId = interstitialAd.getInfo().getAdUnitId();

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
//                    Toast.makeText(getApplicationContext(), "Спасибо за просмотр", Toast.LENGTH_SHORT).show();
                    BigDecimal last = new BigDecimal(MainApplication.getBalance());
                    BigDecimal curr = Ads.getPrice(unitId);
                    MainApplication.setBalance(last.add(curr));
                    Toast.makeText(getApplicationContext(), "Баланс увеличен на " + curr.setScale(3, RoundingMode.HALF_DOWN), Toast.LENGTH_SHORT).show();
                    loadPageAd();
                }

                @Override
                public void onAdClicked() {
                    // пользователь кликнул рекламу
//                    Toast.makeText(getApplicationContext(), "Клик зафиксирован", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAdImpression(@Nullable ImpressionData impressionData) {
                    // какие-то необработанные данные в JSON
                    String data = impressionData == null ? "" : impressionData.getRawData();
                    Log.d("interstitial", "onAdImpression: " + data);
//                    Toast.makeText(getApplicationContext(), impressionData.getRawData(), Toast.LENGTH_LONG).show();
                    try {
                        // вытащили из данных о показываемой рекламе
                        Ads.setPrice(unitId, new BigDecimal(new JSONObject(data).getString("revenue")));
                    } catch (JSONException e) {
                        Log.e("interstitial", e.getLocalizedMessage());
                    }
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
                private final String unitId = rewardedAd.getInfo().getAdUnitId();

                @Override
                public void onAdShown() {
                    // начало показа
                    b_rewarded.setEnabled(false);
                }

                @Override
                public void onAdFailedToShow(@NonNull AdError adError) {
                    // срыв показа
//                    Toast.makeText(getApplicationContext(), adError.getDescription(), Toast.LENGTH_SHORT).show();
                    loadRewardedAd();
                }

                @Override
                public void onAdDismissed() {
                    // показ завершён
//                    Toast.makeText(getApplicationContext(), "Спасибо за просмотр", Toast.LENGTH_SHORT).show();
                    BigDecimal last = new BigDecimal(MainApplication.getBalance());
                    BigDecimal curr = Ads.getPrice(unitId);
                    MainApplication.setBalance(last.add(curr));
                    Toast.makeText(getApplicationContext(), "Баланс увеличен на " + curr.setScale(3, RoundingMode.HALF_UP), Toast.LENGTH_SHORT).show();
                    loadRewardedAd();
                }

                @Override
                public void onAdClicked() {
                    // пользователь кликнул рекламу
//                    Toast.makeText(getApplicationContext(), "Клик зафиксирован", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAdImpression(@Nullable ImpressionData impressionData) {
                    // какие-то необработанные данные в JSON
                    String data = impressionData == null ? "" : impressionData.getRawData();
                    Log.d("rewarded", "onAdImpression: " + data);
//                    Toast.makeText(getApplicationContext(), impressionData.getRawData(), Toast.LENGTH_LONG).show();
                    if (interstitialAd != null) {
                        try {
                            // вытащили из данных о показываемой рекламе
                            Ads.setPrice(unitId, new BigDecimal(new JSONObject(data).getString("revenue")));
                        } catch (JSONException e) {
                            Log.e("rewarded", e.getLocalizedMessage());
                        }
                    } else {
                        // цена за просмотр наградной рекламы, если не удалось получить её цену
                        Ads.setPrice(unitId, new BigDecimal("0.01"));
                    }
                }

                @Override
                public void onRewarded(@NonNull Reward reward) {
                    // награда получена
                    int amount = reward.getAmount(); // количество
                    String type = reward.getType(); // название награды
//                    Toast.makeText(getApplicationContext(), "Получено: '" + type + "' = " + amount, Toast.LENGTH_SHORT).show();
                    // за полный просмотр награда больше
                    Ads.setPrice(unitId, new BigDecimal("0.05"));

                }
            });
            rewardedAd.show(this);
        }
    }
}