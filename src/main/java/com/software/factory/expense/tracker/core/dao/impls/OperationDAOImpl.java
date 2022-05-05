package com.software.factory.expense.tracker.core.dao.impls;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.software.factory.expense.tracker.core.abstracts.AbstractOperation;
import com.software.factory.expense.tracker.core.dao.interfaces.OperationDAO;
import com.software.factory.expense.tracker.core.dao.interfaces.SourceDAO;
import com.software.factory.expense.tracker.core.dao.interfaces.StorageDAO;
import com.software.factory.expense.tracker.core.database.SQLiteConnection;
import com.software.factory.expense.tracker.core.enums.OperationType;
import com.software.factory.expense.tracker.core.impls.operations.ConvertOperation;
import com.software.factory.expense.tracker.core.impls.operations.IncomeOperation;
import com.software.factory.expense.tracker.core.impls.operations.OutcomeOperation;
import com.software.factory.expense.tracker.core.impls.operations.TransferOperation;
import com.software.factory.expense.tracker.core.interfaces.Operation;
import com.software.factory.expense.tracker.core.interfaces.Source;
import com.software.factory.expense.tracker.core.interfaces.Storage;

public class OperationDAOImpl implements OperationDAO {

    private List<Operation> operationList = new ArrayList<>();

    // чтобы эти объекты не получать еще раз из БД - передаем сюда
    private Map<Long, Source> sourceIdentityMap;// чтобы получить по id нужный Source
    private Map<Long, Storage> storageIdentityMap; // чтобы получить по id нужный Storage

    public OperationDAOImpl(Map<Long, Source> sourceIdentityMap, Map<Long, Storage> storageIdentityMap) {// IdentityMap - распространенное понятие, это коллекция, где вместо ключа id, а значение - это сам объект
        this.sourceIdentityMap = sourceIdentityMap;
        this.storageIdentityMap = storageIdentityMap;
    }

    @Override
    public List<Operation> getAll() {
        operationList.clear();

        try (Statement stmt = SQLiteConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("select * from " + OPERATION_TABLE);) {

            while (rs.next()) {

                operationList.add(fillOperation(rs));

            }

            return operationList;// должен содержать только корневые элементы

        } catch (SQLException e) {
            Logger.getLogger(OperationDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }


    @Override
    public Operation get(long id) {
        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("select * from " + OPERATION_TABLE + " where id=?");) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery();) {

                if (rs.next()) {
                    AbstractOperation operation = fillOperation(rs);
                    return operation;
                }

            }

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }

    // заполняет объект операции (общие + специфичные поля)
    private AbstractOperation fillOperation(ResultSet rs) throws SQLException {

        OperationType operationType = OperationType.getType(rs.getInt("type_id"));

        // заполняем специфичные поля
        AbstractOperation operation = createOperation(operationType, rs);

        // заполняем общие поля для всех типов операций
        operation.setId(rs.getLong("id"));
        operation.setOperationType(operationType);
        Calendar datetime = Calendar.getInstance();
        datetime.setTimeInMillis(rs.getLong("datetime"));
        operation.setDateTime(datetime);
        operation.setDescription(rs.getString("description"));

        return operation;
    }

    // создаем нужный экземпляр и заполняем его специфичные поля
    private AbstractOperation createOperation(OperationType operationType, ResultSet rs) throws SQLException {


        // список операций должен загружаться при запуске программы только один - тогда можно использовать лямбда выражения для поиска объектов по id
        // для всех новых операций - поиск по id проводиться не будет
        switch (operationType) {
            case INCOME: { // доход
                IncomeOperation operation = new IncomeOperation();

                operation.setFromSource(sourceIdentityMap.get(rs.getLong("from_source_id"))); // откуда поступили деньги
                operation.setFromCurrency(Currency.getInstance(rs.getString("from_currency_code"))); // в какой валюте поступили
                operation.setFromAmount(new BigDecimal(rs.getDouble("from_amount"))); // сумма поступления
                operation.setToStorage(storageIdentityMap.get(rs.getLong("to_storage_id")));// куда положим эти деньги


                return operation;
            }
            case OUTCOME: { // расход
                OutcomeOperation operation = new OutcomeOperation();

                operation.setFromStorage(storageIdentityMap.get(rs.getLong("from_storage_id"))); // откуда взяли деньги
                operation.setFromCurrency(Currency.getInstance(rs.getString("from_currency_code")));// в какой валюте расход
                operation.setFromAmount(new BigDecimal(rs.getDouble("from_amount"))); // сумма расхода
                operation.setToSource(sourceIdentityMap.get(rs.getLong("to_source_id")));// на что потратили


                return operation;
            }

            case TRANSFER: { // перевод в одной валюте между хранилищами
                TransferOperation operation = new TransferOperation();

                operation.setFromStorage(storageIdentityMap.get(rs.getLong("from_storage_id")));// откуда переводим
                operation.setFromCurrency(Currency.getInstance(rs.getString("from_currency_code"))); // в какой валюте переводим
                operation.setFromAmount(new BigDecimal(rs.getDouble("from_amount"))); // какую сумму переводим
                operation.setToStorage(storageIdentityMap.get(rs.getLong("to_storage_id"))); // не создаем новый объект, используем ранее созданный объект источника

                return operation;
            }

            case CONVERT: { // конвертация из любой валюты в любую между хранилищами
                ConvertOperation operation = new ConvertOperation();

                operation.setFromStorage(storageIdentityMap.get(rs.getLong("from_storage_id"))); // откуда конвертируем
                operation.setFromCurrency(Currency.getInstance(rs.getString("from_currency_code"))); // в каой валюте
                operation.setFromAmount(new BigDecimal(rs.getDouble("from_amount"))); // какая сумма в исходной валюте


                operation.setToStorage(storageIdentityMap.get(rs.getLong("to_storage_id"))); // куда конвертируем
                operation.setToCurrency((Currency.getInstance(rs.getString("to_currency_code")))); // валюта поступления
                operation.setToAmount(new BigDecimal(rs.getDouble("to_amount"))); // какая итоговая сумма поступила в этой валюте


                return operation;
            }


        }

        return null;// если ни один из типов операция не подошел, можно также выбрасывать исключение

    }


    @Override
    // при обновлении не даем менять тип операции - только данные самой операции (дата, суммы, источники, хранилища, описание)
    public boolean update(Operation operation) throws SQLException{
        return (delete(operation) && add(operation));// при обновлении - удаляем старую операцию, добавляем новую, т.к. могут поменяться хранилища, источники
    }

    @Override
    public boolean delete(Operation operation) throws SQLException{
        // TODO реализовать - если есть ли операции по данному хранилищу - запрещать удаление
        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("delete from " + OPERATION_TABLE + " where id=?");) {

            stmt.setLong(1, operation.getId());

            if (stmt.executeUpdate() == 1) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean add(Operation operation) {


        String sql = createInsertSql(operation); // подготовить sql с нужными параметрами, в зависимости от типа операции


        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement(sql);
             Statement stmtId = SQLiteConnection.getConnection().createStatement();

        ) {

            // общие поля для всех типов операций
            stmt.setLong(1, operation.getDateTime().getTimeInMillis());
            stmt.setLong(2, operation.getOperationType().getId());
            stmt.setString(3, operation.getDescription());


            // выбираем специфичные поля
            switch (operation.getOperationType()) {
                case INCOME:
                    IncomeOperation incomeOperation = (IncomeOperation)operation;

                    stmt.setLong(4, incomeOperation.getFromSource().getId());
                    stmt.setString(5, incomeOperation.getFromCurrency().getCurrencyCode());
                    stmt.setDouble(6, incomeOperation.getFromAmount().doubleValue());
                    stmt.setLong(7, incomeOperation.getToStorage().getId());

                    break;

                case OUTCOME:
                    OutcomeOperation outcomeOperation = (OutcomeOperation)operation;

                    stmt.setLong(4, outcomeOperation.getFromStorage().getId());
                    stmt.setString(5, outcomeOperation.getFromCurrency().getCurrencyCode());
                    stmt.setDouble(6, outcomeOperation.getFromAmount().doubleValue());
                    stmt.setLong(7, outcomeOperation.getToSource().getId());
                    break;

                case TRANSFER:
                    TransferOperation transferOperation = (TransferOperation)operation;

                    stmt.setLong(4, transferOperation.getFromStorage().getId());
                    stmt.setString(5, transferOperation.getFromCurrency().getCurrencyCode());
                    stmt.setDouble(6, transferOperation.getFromAmount().doubleValue());
                    stmt.setLong(7, transferOperation.getToStorage().getId());
                    break;

                case CONVERT:
                    ConvertOperation convertOperation = (ConvertOperation)operation;

                    stmt.setLong(4, convertOperation.getFromStorage().getId());
                    stmt.setString(5, convertOperation.getFromCurrency().getCurrencyCode());
                    stmt.setDouble(6, convertOperation.getFromAmount().doubleValue());
                    stmt.setLong(7, convertOperation.getToStorage().getId());
                    stmt.setString(8, convertOperation.getToCurrency().getCurrencyCode());
                    stmt.setDouble(9, convertOperation.getToAmount().doubleValue());
                    break;
            }


            if (stmt.executeUpdate() == 1) {// если объект добавился нормально

                try (ResultSet rs = stmtId.executeQuery("SELECT last_insert_rowid()");) {// получаем id вставленной записи

                    if (rs.next()) {
                        operation.setId(rs.getLong(1));// не забываем просвоить новый id в объект
                        return true;
                    }

                }


            }

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return false;
    }



    private String createInsertSql(Operation operation) {

        StringBuilder sb = new StringBuilder("insert into " + OPERATION_TABLE + " (datetime, type_id, description, ");// sql собирается частями, в зависимости от типа операции

        switch (operation.getOperationType()) {
            case INCOME:
                return sb.append("from_source_id, from_currency_code, from_amount, to_storage_id) values(?,?,?,?,?,?,?)").toString();
            case OUTCOME:
                return sb.append("from_storage_id, from_currency_code, from_amount, to_source_id) values(?,?,?,?,?,?,?)").toString();
            case TRANSFER:
                return sb.append("from_storage_id, from_currency_code, from_amount, to_storage_id) values(?,?,?,?,?,?,?)").toString();
            case CONVERT:
                return sb.append("from_storage_id, from_currency_code, from_amount, to_storage_id, to_currency_code, to_amount) values(?,?,?,?,?,?,?,?,?)").toString();
        }

        return null;

    }


    @Override
    public List<Operation> getList(OperationType operationType) {
        operationList.clear();

        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("select * from " + OPERATION_TABLE + " where type_id=?")) {

            stmt.setLong(1, operationType.getId());

            try (ResultSet rs = stmt.executeQuery();) {

                while (rs.next()) {

                    operationList.add(fillOperation(rs));

                }

                return operationList;// должен содержать только корневые элементы

            } catch (SQLException e) {
                Logger.getLogger(OperationDAOImpl.class.getName()).log(Level.SEVERE, null, e);
            }


        } catch (SQLException e) {
            Logger.getLogger(OperationDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }

    @Override
    public List<Operation> search(String... params) {
        ArrayList<Operation> list = new ArrayList<>();// результаты поиска сохраняем во временную коллекцию (не operationList) - чтобы вы любое время можно было вернуться к исходным данным

        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("select * from " + OPERATION_TABLE+
                " where description like ? " +
                "or from_source_id=(select id from "+ SourceDAO.SOURCE_TABLE +" where name like ?)" +
                "or to_source_id=(select id from "+ SourceDAO.SOURCE_TABLE +" where name like ?)" +
                "or from_storage_id=(select id from "+ StorageDAO.STORAGE_TABLE +" where name like?) " +
                "or to_storage_id=(select id from "+ StorageDAO.STORAGE_TABLE +" where name like ?)")) {

            String searchStr = "%"+params[0]+"%";

            stmt.setString(1, searchStr);
            stmt.setString(2, searchStr);
            stmt.setString(3, searchStr);
            stmt.setString(4, searchStr);
            stmt.setString(5, searchStr);

            try (ResultSet rs = stmt.executeQuery();) {

                while (rs.next()) {

                    list.add(fillOperation(rs));

                }

                return list;

            } catch (SQLException e) {
                Logger.getLogger(OperationDAOImpl.class.getName()).log(Level.SEVERE, null, e);
            }


        } catch (SQLException e) {
            Logger.getLogger(OperationDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }
}
