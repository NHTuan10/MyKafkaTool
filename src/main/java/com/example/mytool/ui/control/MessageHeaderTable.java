package com.example.mytool.ui.control;

import com.example.mytool.ui.TableViewConfigurer;
import com.example.mytool.ui.UIPropertyTableItem;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
public class MessageHeaderTable extends EditableTableControl<UIPropertyTableItem> {
    @FXML
    protected void initialize() {
        super.initialize();
        this.refreshBtn.setVisible(false);
//        this.refreshBtn.setManaged(false);
        TableViewConfigurer.configureEditableTableCell(table, UIPropertyTableItem.class);
//        headerTable.setEditable(editable);
    }

    @FXML
    protected void addItem() {
        tableItems.add(new UIPropertyTableItem("", ""));
    }

    @Override
    protected Predicate<UIPropertyTableItem> filterPredicate(Filter filter) {
        return buildFilterPredicate(filter, UIPropertyTableItem::getName, UIPropertyTableItem::getValue);
    }

    public void setEditable(boolean editable) {
        this.editable.set(editable);
    }
}
