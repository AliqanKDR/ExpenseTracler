package com.software.factory.expense.tracker.core.dao.impls;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.software.factory.expense.tracker.core.dao.interfaces.SourceDAO;
import com.software.factory.expense.tracker.core.database.SQLiteConnection;
import com.software.factory.expense.tracker.core.enums.OperationType;
import com.software.factory.expense.tracker.core.impls.DefaultSource;
import com.software.factory.expense.tracker.core.interfaces.Source;


//TODO можно реализовать общий абстрактный класс и вынести туда общие методы (getAll, delete и пр.)
// реализация DAO не должна заниматься лишними делами - только связь с БД, заполнение объектов
public class SourceDAOImpl implements SourceDAO {

    private List<Source> sourceList = new ArrayList<>();// хранит все элементы сплошным списком, без разделения по деревьям и пр.


    @Override
    public List<Source> getAll() {
        sourceList.clear();

        try (Statement stmt = SQLiteConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("select * from " + SOURCE_TABLE + " order by parent_id");) {

            while (rs.next()) {
                sourceList.add(fillSource(rs));
            }

            return sourceList;// должен содержать только корневые элементы

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }


    public int getRefCount(Source source){
        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("select ref_count from " + SOURCE_TABLE + " where id=?");) {

            stmt.setLong(1, source.getId());

            try (ResultSet rs = stmt.executeQuery();) {

                if (rs.next()) {
                    return rs.getInt("ref_count");
                }

            }

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return -1;
    }

    @Override
    public Source get(long id) {

        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("select * from " + SOURCE_TABLE + " where id=?");) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery();) {
                DefaultSource source = null;

                if (rs.next()) {
                    source = fillSource(rs);
                }

                return source;
            }

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }

    // заполняет объект source
    private DefaultSource fillSource(ResultSet rs) throws SQLException {


        DefaultSource source = new DefaultSource();
        source.setId(rs.getLong("id"));
        source.setName(rs.getString("name"));
        source.setParentId(rs.getLong("parent_id"));// поле используется для первичного построения дерева - в дальнейшем лучше использовать получение объекта getParent
        source.setIconName(rs.getString("icon_name"));
        source.setOperationType(OperationType.getType(rs.getInt("operation_type_id")));
        source.setRefCount(rs.getInt("ref_count"));

        return source;
    }



    @Override
    public boolean update(Source source) {
        // для упрощения - у хранилища даем изменить только название, изменять parent_id нельзя (для этого можно удалить и заново создать)
        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("update " + SOURCE_TABLE + " set name=?, icon_name=? where id=?");) {

            stmt.setString(1, source.getName());// у созданного элемента - разрешаем менять только название
            stmt.setString(2, source.getIconName());
            stmt.setLong(3, source.getId());



            // не даем обновлять operationType - тип устанавливается только один раз при создании корневеого элемента

            if (stmt.executeUpdate() == 1) {
                return true;
            }

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return false;
    }

    @Override
    public boolean delete(Source source) throws SQLException {
        // TODO реализовать - если есть ли операции по данному хранилищу - запрещать удаление
        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("delete from " + SOURCE_TABLE + " where id=?");) {

            stmt.setLong(1, source.getId());

            if (stmt.executeUpdate() == 1) {
                return true;
            }
        }


        return false;
    }

    @Override
    // добавляет объект в БД и присваивает ему сгенерированный id
    public boolean add(Source source) {
        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("insert into " + SOURCE_TABLE + "(name, parent_id, operation_type_id, icon_name) values(?,?,?,?)");
             Statement stmtId = SQLiteConnection.getConnection().createStatement();
        ) {

            stmt.setString(1, source.getName());

            if (source.hasParent()) {
                stmt.setLong(2, source.getParent().getId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }

            stmt.setLong(3, source.getOperationType().getId());
            stmt.setString(4, source.getIconName());

            if (stmt.executeUpdate() == 1) {// если объект добавился нормально

                try (ResultSet rs = stmtId.executeQuery("SELECT last_insert_rowid()");) {// получаем id вставленной записи

                    if (rs.next()) {
                        source.setId(rs.getLong(1));// не забываем просвоить новый id в объект
                        return true;
                    }


                }

            }

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return false;
    }




    @Override
    public List<Source> getList(OperationType operationType) {
        sourceList.clear();

        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement("select * from " + SOURCE_TABLE + " where operation_type_id=?");) {

            stmt.setLong(1, operationType.getId());

            try (ResultSet rs = stmt.executeQuery();) {

                while (rs.next()) {
                    sourceList.add(fillSource(rs));
                }

                return sourceList;
            }

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }


    // можно было поиск делать по коллекции, т.к. все данные уже загружены, но если реализовывать постраничный вывод (на будущее) - лучше поиск делать в БД
    @Override
    public List<Source> search(String... params) {
        ArrayList<Source> list = new ArrayList<>();// результаты поиска сохраняем во временную коллекцию (не sourceList) - чтобы вы любое время можно было вернуться к исходным данным

        String searchStr = "%"+params[0]+"%";
        String sql = "select * from " + SOURCE_TABLE + " where name like ? ";

        if (params.length==2){// в params[1] будем записывать тип для фильтрации (если необходимо), например это нужно для поиска справочных значения при редактировании операции дохода или расхода (чтобы только нужный тип искал)
            sql += "and operation_type_id=?";
        }


        try (PreparedStatement stmt = SQLiteConnection.getConnection().prepareStatement(sql);) {


            stmt.setString(1, searchStr);

            if (params.length==2){
                stmt.setLong(2, Long.valueOf(params[1]));
            }

            try (ResultSet rs = stmt.executeQuery();) {

                while (rs.next()) {
                    list.add(fillSource(rs));
                }

                return list;
            }

        } catch (SQLException e) {
            Logger.getLogger(SourceDAOImpl.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }


}
