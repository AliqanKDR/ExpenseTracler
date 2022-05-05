package com.software.factory.expense.tracker.core.abstracts;

import java.util.Calendar;

import com.software.factory.expense.tracker.core.enums.OperationType;
import com.software.factory.expense.tracker.core.interfaces.Operation;

public abstract class AbstractOperation implements Operation{

    private long id;
    private String name;
    private String iconName;
    private Calendar dateTime; // дата и время выполнения операции (подставлять автоматически при создании, но можно будет изменять в любое время)
    private String description; // доп. информация, которую вводит пользователь
    private OperationType operationType;// тип операции (доход, расход, перевод, конвертация)

    public AbstractOperation(OperationType operationType) {
        this.operationType = operationType;
    }

    public AbstractOperation(long id, Calendar dateTime, String description, OperationType operationType) {
        this.id = id;
        this.dateTime = dateTime;
        this.description = description;
        this.operationType = operationType;
    }

    public AbstractOperation(long id, OperationType operationType) {
        this.id = id;
        this.operationType = operationType;
    }


    @Override
    public String getIconName() {
        return iconName;
    }

    @Override
    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public Calendar getDateTime() {
        return dateTime;
    }

    public void setDateTime(Calendar dateTime) {
        this.dateTime = dateTime;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }




    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractOperation that = (AbstractOperation) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
