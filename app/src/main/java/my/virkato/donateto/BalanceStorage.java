package my.virkato.donateto;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Хранилище баланса на устройстве пользователя
 */
public class BalanceStorage {

    private final SharedPreferences sp;

    public BalanceStorage(Context context) {
        sp = context.getSharedPreferences("account", MODE_PRIVATE);
    }


    /**
     * Состояние баланса
     *
     * @return количество денег
     */
    public BigDecimal getBalance(boolean rounded) {
        if (rounded) {
            return new BigDecimal(sp.getString("balance", "0.0")).setScale(3, RoundingMode.HALF_UP);
        } else {
            return new BigDecimal(sp.getString("balance", "0.0"));
        }
    }


    /**
     * Изменить состояние баланса
     *
     * @param balance новое значение баланса
     */
    public void setBalance(BigDecimal balance) {
        sp.edit().putString("balance", balance.toString()).apply();
    }


    /**
     * Начать отслеживание состояния баланса
     *
     * @param listener слушатель находящийся в пользовательском коде
     */
    public void startListening(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sp.registerOnSharedPreferenceChangeListener(listener);
    }


    /**
     * Закончить отслеживание состояния баланса
     *
     * @param listener слушатель находящийся в пользовательском коде
     */
    public void stopListening(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

}
