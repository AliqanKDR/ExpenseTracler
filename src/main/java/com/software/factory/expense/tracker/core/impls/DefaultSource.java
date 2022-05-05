package com.software.factory.expense.tracker.core.impls;

import java.util.List;

import com.software.factory.expense.tracker.core.abstracts.AbstractTreeNode;
import com.software.factory.expense.tracker.core.enums.OperationType;
import com.software.factory.expense.tracker.core.interfaces.Source;


public class DefaultSource extends AbstractTreeNode<DefaultSource> implements Source<DefaultSource> {

    private OperationType operationType;

    public DefaultSource() {
    }

    public DefaultSource(String name) {
        super(name);
    }

    public DefaultSource(List<DefaultSource> childs) {
        super(childs);
    }

    public DefaultSource(String name, long id) {
        super(name, id);
    }

    public DefaultSource(long id, List<DefaultSource> childs, DefaultSource parent, String name) {
        super(id, childs, parent, name);
    }

    public DefaultSource(String name, long id, OperationType operationType) {
        super(name, id);
        this.operationType = operationType;
    }


    @Override
    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        if (!hasParent()) {// если есть родитель - то оставить его OperationType - можно также выбрасывать исключение, если не совпадают типы
            this.operationType = operationType;
        }
    }

    @Override
    public void add(DefaultSource child) {

        // TODO применить паттерн
        // для дочернего элемента устанавливаем тип операции родительского элемента

        child.setOperationType(operationType);

        super.add(child);
    }

    @Override
    public void setParent(DefaultSource parent) {

        operationType = parent.getOperationType();// при установке родителя - автоматически проставляем тип операции как у родителя

        super.setParent(parent);
    }


}
