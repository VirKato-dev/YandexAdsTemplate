package my.virkato.donateto;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputLayout;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.rewarded.Reward;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

import my.virkato.ads.adapter.Ads;


public class MainActivity extends AppCompatActivity {

    private Button b_page;

    private Button b_rewarded;

    private TextView t_balance;

    private Button b_withdraw;

    private TextInputLayout til_phone;

    /**
     * Минимальная сумма на вывод
     */
    private static final BigDecimal MIN_MONEY = new BigDecimal("100");

    /**
     * Вознаграждение за полный просмотр наградной рекламы
     */
    private static final BigDecimal REWARD_FULL = new BigDecimal("0.5");

    /**
     * Вознаграждение за неполный просмотр наградной рекламы
     */
    private static final BigDecimal REWARD_PART_REW = new BigDecimal("0.1");

    /**
     * Вознаграждение за просмотр страничной рекламы
     */
    private static final BigDecimal REWARD_PART_INT = new BigDecimal("0.05");

    /**
     * Задержка перед следующей попыткой загрузки рекламы
     */
    private static final long DELAY = 5000L;


    /**
     * Текущий баланс
     */
    private BalanceStorage bs;

    /**
     * Слушатель текущего баланса
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener onChanged = (sharedPreferences, key) -> {
        if (key != null && key.equals("balance")) showBalance();
    };


    /**
     * Адаптер к текущей библиотеке РСЯ
     */
    private Ads ads;

    /**
     * Текущий блок страничной рекламы
     */
    private Ads.AdUnit interstitialAd = null;

    /**
     * Текущий блок наградной рекламы
     */
    private Ads.AdUnit rewardedAd = null;

    /**
     * Слушатель страничной рекламы
     */
    private final Ads.CommonAdEventListener interstitialListener = new Ads.CommonAdEventListener() {
        @Override
        public void onAdLoaded(Ads.AdUnit ad) {
            interstitialAd = ad;
            // здесь решать вопрос о показе страничной рекламы
            b_page.setEnabled(true);
        }

        @Override
        public void onAdFailedToLoad(AdRequestError adRequestError) {
            // здесь решать вопрос о повторной загрузке страничной рекламы
            Log.e("interstitial", "onAdFailedToLoad: " + adRequestError.getDescription());
            b_page.postDelayed(() -> loadPageAd(), DELAY);
        }

        @Override
        public void onAdShown() {
            // начало показа
            b_page.setEnabled(false);
        }

        @Override
        public void onAdDismissed() {
            // показ завершён
            BigDecimal last = bs.getBalance(false);
            BigDecimal curr = PriceStorage.getPrice(interstitialAd.getUnitId());
            bs.setBalance(last.add(curr));
            Toast.makeText(getApplicationContext(), "Баланс увеличен на " + curr.setScale(3, RoundingMode.HALF_DOWN), Toast.LENGTH_SHORT).show();
            loadPageAd();
        }

        @Override
        public void onAdClicked() {
            // пользователь кликнул рекламу
            // Toast.makeText(getApplicationContext(), "Клик зафиксирован", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLeftApplication() {
        }

        @Override
        public void onReturnedToApplication() {
        }

        @Override
        public void onImpression(ImpressionData impressionData) {
            // можно назначить фиксированную плату за просмотр страничной рекламы
            PriceStorage.setPrice(interstitialAd.getUnitId(), REWARD_PART_INT);
            if (false) { // либо получить стоимость оплаты от РСЯ
                String data = impressionData == null ? "" : impressionData.getRawData();
                Log.d("interstitial", "onAdImpression: " + data);
                try {
                    // вытащили из данных о показываемой рекламе
                    PriceStorage.setPrice(interstitialAd.getUnitId(), new BigDecimal(new JSONObject(data).getString("revenue")));
                } catch (JSONException e) {
                    Log.e("interstitial", e.getLocalizedMessage());
                }
            }
        }

        @Override
        public void onRewarded(Reward rewarded) {
        }
    };


    /**
     * Слушатель наградной рекламы
     */
    private final Ads.CommonAdEventListener rewardedListener = new Ads.CommonAdEventListener() {
        @Override
        public void onAdLoaded(Ads.AdUnit ad) {
            rewardedAd = ad;
            // здесь решать вопрос о показе наградной рекламы
            b_rewarded.setEnabled(true);
        }

        @Override
        public void onAdFailedToLoad(AdRequestError adRequestError) {
            // здесь решать вопрос о повторной загрузке наградной рекламы
            Log.e("rewarded", "onAdFailedToLoad: " + adRequestError.getDescription());
            b_rewarded.postDelayed(() -> loadRewardedAd(), DELAY);
        }

        @Override
        public void onAdShown() {
            // начало показа
            b_rewarded.setEnabled(false);
        }

        @Override
        public void onAdDismissed() {
            // показ завершён
            BigDecimal last = bs.getBalance(false);
            BigDecimal curr = PriceStorage.getPrice(rewardedAd.getUnitId());
            bs.setBalance(last.add(curr));
            Toast.makeText(getApplicationContext(), "Баланс увеличен на " + curr.setScale(3, RoundingMode.HALF_UP), Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }

        @Override
        public void onAdClicked() {
            // пользователь кликнул рекламу
            // Toast.makeText(getApplicationContext(), "Клик зафиксирован", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLeftApplication() {
        }

        @Override
        public void onReturnedToApplication() {
        }

        @Override
        public void onImpression(ImpressionData impressionData) {
            // можно назначить фиксированную плату за просмотр страничной рекламы
            PriceStorage.setPrice(rewardedAd.getUnitId(), REWARD_PART_REW);
            if (false) { // либо получить стоимость оплаты от РСЯ
                String data = impressionData == null ? "" : impressionData.getRawData();
                Log.d("rewarded", "onAdImpression: " + data);
                if (rewardedAd != null) {
                    try {
                        // вытащили из данных о показываемой рекламе
                        PriceStorage.setPrice(rewardedAd.getUnitId(), new BigDecimal(new JSONObject(data).getString("revenue")));
                    } catch (JSONException e) {
                        Log.e("rewarded", e.getLocalizedMessage());
                    }
                }
            }
        }

        @Override
        public void onRewarded(Reward rewarded) {
            // награда получена
            int amount = rewarded.getAmount(); // количество
            String type = rewarded.getType();  // название награды
            Toast.makeText(getApplicationContext(), "Получено: '" + type + "' = " + amount, Toast.LENGTH_SHORT).show();
            // за полный просмотр награда больше
            PriceStorage.setPrice(rewardedAd.getUnitId(), REWARD_FULL);
        }
    };

    // ОСНОВНАЯ ЛОГИКА

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bs = new BalanceStorage(this);
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

        // следим за изменение баланса в Shared Preferences
        til_phone = findViewById(R.id.til_phone);
        b_withdraw = findViewById(R.id.b_withdraw);
        b_withdraw.setEnabled(false);
        b_withdraw.setOnClickListener(v -> {
            // подать заявку в телегу
            String phone = til_phone.getEditText().getText().toString().trim();
            if (!phone.equals("")) { // если указан номер телефона
                sendWithdrawalTicket(phone);
            } else {
                Toast.makeText(v.getContext(), "Не указан номер телефона", Toast.LENGTH_SHORT).show();
            }
        });

        t_balance = findViewById(R.id.t_balance);
        bs.startListening(onChanged);

        showBalance();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bs.stopListening(onChanged);
    }


    /**
     * Показать текущий баланс
     */
    private void showBalance() {
        t_balance.setText(bs.getBalance(true).toString());
        // кнопка вывода доступна при балансе >= MIN_MONEY
        b_withdraw.setEnabled(bs.getBalance(false).compareTo(MIN_MONEY) >= 0);
    }


    /**
     * Оповестить себя через телеграм о заявке на вывод средств
     *
     * @param phone номер телефона пользователя
     */
    private void sendWithdrawalTicket(String phone) {
        String token = "1234567890:ABCDEFGHIJKLMNO"; // спросить https://t.me/BotFather
        String url = "https://api.telegram.org/bot" + token + "/sendMessage";
        JSONObject jo = new JSONObject();
        try {
            jo.put("chat_id", "1234567890"); // спросить https://t.me/getmyid_bot
            jo.put("text", "Withdrawal ticket from " + phone + " for " + bs.getBalance(true));
        } catch (JSONException ignored) {
        }

        final JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, jo,
                response -> {
                    Log.e("bot", response.toString());
                    try {
                        if (response.getBoolean("ok")) {
                            // обнуляем баланс
                            bs.setBalance(BigDecimal.ZERO);
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
        ads.loadInterstitial(this, getString(R.string.ads_unit_page));
    }


    /**
     * Показать страничную рекламу, если загружена
     */
    private void showPageAd() {
        if (interstitialAd != null) interstitialAd.show(this);
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