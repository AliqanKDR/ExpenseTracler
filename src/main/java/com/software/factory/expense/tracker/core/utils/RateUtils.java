package com.software.factory.expense.tracker.core.utils;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

public class RateUtils {

    // карта с курсами валют
    // при запуске приложения можно обновлять по всем включенным валютам
    // одна валюта является основной - по-умолчанию, к ней уже строится вся таблица других валют
    //
    public static Map<String, Map<String, BigDecimal>> rateMap = new HashMap<>();

    static {

        Map<String, BigDecimal> convertMap= new HashMap<>();
        convertMap.put(Currency.getInstance("USD").getCurrencyCode(), new BigDecimal(60));// 1 доллар = 60 рублей
        convertMap.put(Currency.getInstance("RUB").getCurrencyCode(), new BigDecimal(0.2));// 1 тенге = 0.2 рубля

        // ключ - валюта, в которую переводим
        rateMap.put(Currency.getInstance("KZT").getCurrencyCode(), convertMap); // convertMap - отношения других валют к рублю

    }

    // получить нужный курс для двух валют
    public static BigDecimal getRate(Currency currencyFrom, Currency currencyTo){
        return rateMap.get(currencyFrom.getCurrencyCode()).get(currencyTo.getCurrencyCode());
    }

    public static void setRateMap(Map<String, Map<String, BigDecimal>> rateMap) {
        RateUtils.rateMap = rateMap;
    }
}
