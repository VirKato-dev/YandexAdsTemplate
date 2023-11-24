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
import java.util.HashMap;
import java.util.Map;

import my.virkato.ads.adapter.Ads;


public class MainActivity extends AppCompatActivity {

    private Ads.AdUnit interstitialAd = null;
    private Ads.AdUnit rewardedAd = null;

    private Button b_page;
    private Button b_rewarded;
    private TextView t_balance;
    private Button b_withdraw;
    private TextInputLayout til_phone;

    private BigDecimal balance = BigDecimal.ZERO;

    private SharedPreferences sp;

    private final SharedPreferences.OnSharedPreferenceChangeListener onChaged = (sharedPreferences, key) -> {
        if (key != null && key.equals("balance")) {
            balance = new BigDecimal(sharedPreferences.getString(key, "0.0"))
                    .setScale(3, RoundingMode.HALF_UP);
            t_balance.setText(balance.toString());

            // кнопка вывода доступна при балансе >= 100
            b_withdraw.setEnabled(balance.compareTo(BigDecimal.valueOf(100)) >= 0);
        }
    };

    private Ads ads;

    private final Ads.CommonAdEventListener interstitialListener = new Ads.CommonAdEventListener() {
        //todo вынести в Ads
        private final String unitId = getString(R.string.ads_unit_page);

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
            BigDecimal last = new BigDecimal(getBalance());
            BigDecimal curr = getPrice(unitId);
            setBalance(last.add(curr));
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
            // какие-то необработанные данные в JSON
            String data = impressionData == null ? "" : impressionData.getRawData();
            Log.d("interstitial", "onAdImpression: " + data);
            try {
                // вытащили из данных о показываемой рекламе
                setPrice(unitId, new BigDecimal(new JSONObject(data).getString("revenue")));
            } catch (JSONException e) {
                Log.e("interstitial", e.getLocalizedMessage());
            }
        }

        @Override
        public void onRewarded(Reward rewarded) {
        }
    };


    private final Ads.CommonAdEventListener rewardedListener = new Ads.CommonAdEventListener() {
        //todo вынести в Ads
        private final String unitId = getString(R.string.ads_unit_reward);

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
            BigDecimal last = new BigDecimal(getBalance());
            BigDecimal curr = getPrice(unitId);
            setBalance(last.add(curr));
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
            // какие-то необработанные данные в JSON
            String data = impressionData == null ? "" : impressionData.getRawData();
            Log.d("rewarded", "onAdImpression: " + data);
            if (interstitialAd != null) {
                try {
                    // вытащили из данных о показываемой рекламе
                    setPrice(unitId, new BigDecimal(new JSONObject(data).getString("revenue")));
                } catch (JSONException e) {
                    Log.e("rewarded", e.getLocalizedMessage());
                }
            } else {
                // цена за просмотр наградной рекламы, если не удалось получить её цену
                setPrice(unitId, new BigDecimal("0.01"));
            }
        }

        @Override
        public void onRewarded(Reward rewarded) {
            // награда получена
            int amount = rewarded.getAmount(); // количество
            String type = rewarded.getType();  // название награды
            // Toast.makeText(getApplicationContext(), "Получено: '" + type + "' = " + amount, Toast.LENGTH_SHORT).show();
            // за полный просмотр награда больше
            setPrice(unitId, new BigDecimal("0.05"));
        }
    };


    /**
     * Оплата за просмотры рекламы в соответствующем рекламном блоке
     */
    private final Map<String, BigDecimal> prices = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("account", MODE_PRIVATE);
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
            }
        });
        t_balance = findViewById(R.id.t_balance);
        balance = new BigDecimal(getBalance()).setScale(3, RoundingMode.HALF_UP);
        t_balance.setText(balance.toString());
        sp.registerOnSharedPreferenceChangeListener(onChaged);

        //todo убрать после теста
        sendWithdrawalTicket("VirKato");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        sp.unregisterOnSharedPreferenceChangeListener(onChaged);
    }


    /**
     * Получить цену за просмотр рекламы в указанном рекламном блоке
     *
     * @param unitId рекламный блок
     * @return цена
     */
    private BigDecimal getPrice(String unitId) {
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
    private void setPrice(String unitId, BigDecimal price) {
        prices.put(unitId, price);
    }


    /**
     * Состояние баланса
     *
     * @return количество денег
     */
    private String getBalance() {
        return sp.getString("balance", "0.0");
    }


    /**
     * Изменить состояние баланса
     *
     * @param balance новое значение баланса
     */
    private void setBalance(BigDecimal balance) {
        sp.edit().putString("balance", balance.toString()).apply();
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
                            setBalance(BigDecimal.ZERO);
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