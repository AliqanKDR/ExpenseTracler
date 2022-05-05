package com.software.factory.expense.tracker.core.impls;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.software.factory.expense.tracker.core.abstracts.AbstractTreeNode;
import com.software.factory.expense.tracker.core.exceptions.AmountException;
import com.software.factory.expense.tracker.core.exceptions.CurrencyException;
import com.software.factory.expense.tracker.core.interfaces.Storage;
import com.software.factory.expense.tracker.core.utils.RateUtils;

public class DefaultStorage extends AbstractTreeNode<DefaultStorage> implements Storage<DefaultStorage> {

    // сразу инициализируем пустые коллекции, потому что хоть одна валюта будет
    private Map<Currency, BigDecimal> currencyAmounts = new HashMap<>();
//    private List<Currency> currencyList = new ArrayList<>();


    public DefaultStorage() {
    }

    public DefaultStorage(String name) {
        super(name);
    }

    public DefaultStorage(String name, long id) {
        super(name, id);
    }

    public DefaultStorage(List<Currency> currencyList, Map<Currency, BigDecimal> currencyAmounts, String name) {
        super(name);
        this.currencyAmounts = currencyAmounts;
    }

    public DefaultStorage(Map<Currency, BigDecimal> currencyAmounts) {
        this.currencyAmounts = currencyAmounts;
    }


    @Override
    public Map<Currency, BigDecimal> getCurrencyAmounts() {
        return currencyAmounts;
    }


    @Override
    public BigDecimal getAmount(Currency currency) throws CurrencyException {
//        checkCurrencyExist(currency); // в Spring через AOP легче внедрять повторяющиеся участки кода
        return currencyAmounts.get(currency);
    }


    @Override
    public void updateAmount(BigDecimal amount, Currency currency) throws CurrencyException, AmountException {


        //checkCurrencyExist(currency);
        //checkAmount(amount);// не даем балансу уйти в минус

        currencyAmounts.put(currency, amount);
    }


    // проверка, есть ли такая валюта в данном хранилище
    private void checkCurrencyExist(Currency currency) throws CurrencyException {
        if (!currencyAmounts.containsKey(currency)) {
            throw new CurrencyException("Currency " + currency + " not exist");
        }
    }


    // сумма не должна быть меньше нуля (в реальности такое невозможно, мы не можем потратить больше того, что есть)
    private void checkAmount(BigDecimal amount) throws AmountException {

//        if (amount.compareTo(BigDecimal.ZERO) < 0) {
//            throw new AmountException("Amount can't be < 0");
//        }

    }


    @Override
    public void addCurrency(Currency currency, BigDecimal initAmount) throws CurrencyException {

        if (currencyAmounts.containsKey(currency)) {
            throw new CurrencyException("Currency already exist");// пока просто сообщение на англ, без локализации
        }

        currencyAmounts.put(currency, initAmount); // обновляем валюту и баланс по ней (если такая валюту уже была - она заменится (т.к. ключи в map должны быть уникальными)


    }

    @Override
    public void deleteCurrency(Currency currency) throws CurrencyException {

        checkCurrencyExist(currency);

        // не даем удалять валюту, если в хранилище есть деньги по этой валюте
//        if (!currencyAmounts.get(currency).equals(BigDecimal.ZERO)) {
//            throw new CurrencyException("Can't delete currency with amount");
//        }

        currencyAmounts.remove(currency);


    }

    @Override
    public void deleteAllCurrencies() {
        currencyAmounts.clear();
    }


    @Override
    public List<Currency> getAvailableCurrencies() {
        return new ArrayList(currencyAmounts.keySet()); // список List получаем из ключей карты
    }


    // считает средний баланс в одной валюте, включая все дочерние storage
    @Override
    public BigDecimal getApproxAmount(Currency targetCurrency) {// targetCurrency - валюта, в которую переводим

        BigDecimal sum = BigDecimal.ZERO;

        for (Currency c : getAvailableCurrencies()) {

            try {
                if (c.equals(targetCurrency)) {
                    sum = sum.add(getAmount(c)); // если в storage есть баланс по валюте, в которую переводим - просто прибавляем (не конвертируем по курсу)
                } else { // все остальные валюты переводим по курсу
                    sum = sum.add(RateUtils.getRate(targetCurrency, c).multiply(getAmount(c))); // переводим в нужную валюту, согласно таблице конвертации
                }
            } catch (CurrencyException e) {
                e.printStackTrace();
            }

        }

        // прибавляем все балансы дочерних элементов
        for (Storage s : getChilds()) {
            try {
                sum = sum.add(s.getApproxAmount(targetCurrency)); // каждый дочерний элемент содержит сумму все дочерних балансов - поэтому по цепочке получаем все балансы
            } catch (CurrencyException e) {
                e.printStackTrace();
            }
        }

        return sum;
    }


    @Override
    public Currency getCurrency(String code) throws CurrencyException {
        // количество валют для каждого хранилища будет небольшим - поэтому можно провоить поиск через цикл
        // можно использовать библиотеку Apache Commons Collections

        for (Currency currency : getAvailableCurrencies()) {
            if (currency.getCurrencyCode().equals(code)) {
                return currency;
            }
        }

        throw new CurrencyException("Currency (code=" + code + ") not exist in storage");

    }


}

