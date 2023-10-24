package my.virkato.donateto;

import android.app.Application;
import android.content.SharedPreferences;

import java.math.BigDecimal;


public class MainApplication extends Application {
    private static SharedPreferences sp;

    @Override
    public void onCreate() {
        super.onCreate();
        sp = getSharedPreferences("account", MODE_PRIVATE);
        Ads.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    /**
     * Вернуть ссылку на приватное хранилище для наблюдения за его состоянием
     *
     * @return файл приватного хранилища
     */
    public static SharedPreferences getSharedPreferences() {
        return sp;
    }

    /**
     * Состояние баланса
     *
     * @return количество денег
     */
    public static String getBalance() {
        return sp.getString("balance", "0.0");
    }

    /**
     * Изменить состояние баланса
     *
     * @param balance новое значение баланса
     */
    public static void setBalance(BigDecimal balance) {
        sp.edit().putString("balance", balance.toString()).apply();
    }
}
