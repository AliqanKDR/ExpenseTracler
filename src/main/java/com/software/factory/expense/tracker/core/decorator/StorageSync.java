package com.software.factory.expense.tracker.core.decorator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.software.factory.expense.tracker.core.dao.interfaces.StorageDAO;
import com.software.factory.expense.tracker.core.exceptions.AmountException;
import com.software.factory.expense.tracker.core.exceptions.CurrencyException;
import com.software.factory.expense.tracker.core.interfaces.Storage;
import com.software.factory.expense.tracker.core.utils.TreeUtils;

// синхронизирует все действия между объектами коллекции и базой данных
// паттерн Декоратор (измененный)
public class StorageSync extends AbstractSync<Storage> implements StorageDAO {

    private TreeUtils<Storage> treeUtils = new TreeUtils();// построитель дерева

    // Все коллекции хранят ссылки на одни и те же объекты, но в разных "срезах"
    // при удалении - удалять нужно из всех коллекций
    private List<Storage> treeList = new ArrayList<>(); // хранит деревья объектов без разделения по типам операции
    private Map<Long, Storage> identityMap = new HashMap<>(); // нет деревьев, каждый объект хранится отдельно, нужно для быстрого доступа к любому объекту по id (чтобы каждый раз не использовать перебор по всей коллекции List и не обращаться к бд)

    private StorageDAO storageDAO;// реализация слоя работы с БД

    public StorageSync(StorageDAO storageDAO) {
        this.storageDAO = storageDAO;
        init();
    }

    public void init() {
        List<Storage> storageList = storageDAO.getAll();// запрос в БД происходит только один раз, чтобы заполнить коллекцию storageList

        for (Storage s : storageList) {
            identityMap.put(s.getId(), s);
            treeUtils.addToTree(s.getParentId(), s, treeList, storageList);
        }

    }


    @Override
    public List<Storage> getAll() {// возвращает объекты уже в виде деревьев
        return treeList;
    }

    @Override
    public Storage get(long id) {// не делаем запрос в БД, а получаем ранее загруженный объект из коллекции
        return identityMap.get(id);
    }


    @Override
    // TODO подумать как сделать - сначала обновлять в базе, а потом уже в коллекции (либо - если в базе не обновилось - откатить изменения в объекте коллекции)
    public boolean update(Storage storage) throws SQLException {
        if (storageDAO.update(storage)) {

            Storage s = identityMap.get(storage.getId());

            // данные обновлятся сразу во всех коллекциях, т.к. они ссылаются на один и тот же объект
            // не нужно пробегать по всем коллекциям и обновлять в них
            s.setName(storage.getName());
            s.setIconName(storage.getIconName());
            s.getCurrencyAmounts().clear();
            s.getAvailableCurrencies().clear();
            s.getCurrencyAmounts().putAll(storage.getCurrencyAmounts());
            s.getAvailableCurrencies().addAll(storage.getAvailableCurrencies());

            return true;
        }
        return false;
    }

    @Override
    public boolean delete(Storage storage) throws SQLException {
        // TODO добавить нужные Exceptions
        if (storageDAO.delete(storage)) {
            removeFromCollections(storage);

            return true;
        }
        return false;
    }


    // добавляет объект во все коллекции
    private void addToCollections(Storage storage) {

        identityMap.put(storage.getId(), storage);

        if (storage.hasParent()) {
            Storage parent = identityMap.get(storage.getParent().getId());
            if (!parent.getChilds().contains(storage)) {// если ранее не был добавлен уже
                parent.add(storage);
            }
        } else {// если добавляем элемент, у которого нет родителей (корневой)
            treeList.add(storage);
        }
    }


    // удаляет объект из всех коллекций
    private void removeFromCollections(Storage storage) {
        storage = identityMap.remove(storage.getId());
        if (storage == null){
            return;
        }

        if (storage.hasParent()) {// если удаляем дочерний элемент
            storage.getParent().remove(storage);// т.к. у каждого дочернего элемента есть ссылка на родительский - можно быстро удалять элемент из дерева без поиска по всему дереву
        } else {// если удаляем элемент, у которого нет родителей
            treeList.remove(storage);
        }
    }

    @Override
    public boolean add(Storage storage) throws SQLException {

        if (storageDAO.add(storage)) {// если в БД добавилось нормально
            addToCollections(storage);
            return true;
        } else {// откатываем добавление
            // для отката можно использовать паттерн Command (для функции Undo)
        }

        return false;
    }

    @Override
    public List<Storage> search(String... params) {
        return storageDAO.search(params);
    }


    // если понадобится напрямую получить объекты из БД - можно использовать storageDAO
    public StorageDAO getStorageDAO() {
        return storageDAO;
    }


    @Override
    public boolean addCurrency(Storage storage, Currency currency, BigDecimal initAmount) throws CurrencyException {
        if (storageDAO.addCurrency(storage, currency, initAmount)) {// если в БД добавилось нормально
            identityMap.get(storage.getId()).addCurrency(currency, initAmount);
            return true;
        }

        return false;
    }

    @Override
    public boolean deleteCurrency(Storage storage, Currency currency) throws CurrencyException {
        if (storageDAO.deleteCurrency(storage, currency)) {// если в БД удалилось нормально
            identityMap.get(storage.getId()).deleteCurrency(currency);
            return true;
        }

        return false;
    }

    @Override
    public boolean updateAmount(Storage storage, Currency currency, BigDecimal amount) {
        if (storageDAO.updateAmount(storage, currency, amount)) {
            try {
                identityMap.get(storage.getId()).updateAmount(amount, currency);
            } catch (CurrencyException e) {
                e.printStackTrace();
            } catch (AmountException e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    @Override
    public int getRefCount(Storage storage) {
        return storageDAO.getRefCount(storage);
    }

    public BigDecimal getTotalBalance(Currency currency) {
        BigDecimal sum = BigDecimal.ZERO;

        for (Storage s : treeList) {
            try {
                sum = sum.add(s.getApproxAmount(currency));
            } catch (CurrencyException e) {
                e.printStackTrace();
            }
        }

        return sum;
    }

    public Map<Long, Storage> getIdentityMap() {
        return identityMap;
    }


}
