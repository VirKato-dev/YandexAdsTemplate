package my.virkato.donateto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Хранилище цен за просмотренные блоки рекламы
 */
public class PriceStorage {

    /**
     * Цена за каждый блок рекламы
     */
    private static final Map<String, BigDecimal> prices = new HashMap<>();


    /**
     * Получить цену за просмотр рекламы в указанном рекламном блоке
     *
     * @param unitId рекламный блок
     * @return цена
     */
    public static BigDecimal getPrice(String unitId) {
        BigDecimal price = prices.get(unitId);
        if (price == null) price = BigDecimal.ZERO;
        if (true) {
            // если у вас фиксированная оплата за просмотры блоков рекламы
            return price;
        } else {
            // если стоимость просмотра получена от РСЯ, то можно платить половину стоимости, например
            return price.divide(BigDecimal.valueOf(2), RoundingMode.HALF_DOWN);
        }
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
