package com.software.factory.expense.tracker.core.interfaces;

import java.io.Serializable;

// содержит общие параметры node c иконкой
public interface IconNode extends Serializable {
    String getName();

    String getIconName();

    void setIconName(String name);

    void setName(String name);

    long getId(); // каждый элемент дерева должен иметь свой уникальный идентификатор

    void setId(long id); // установить id
}
