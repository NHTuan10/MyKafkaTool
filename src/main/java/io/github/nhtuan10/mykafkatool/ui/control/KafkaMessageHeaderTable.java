package io.github.nhtuan10.mykafkatool.ui.control;

import io.github.nhtuan10.mykafkatool.ui.TableViewConfigurer;
import io.github.nhtuan10.mykafkatool.ui.UIPropertyTableItem;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaMessageHeaderTable extends EditableTableControl<UIPropertyTableItem> {
    @FXML
    protected void initialize() {
        super.initialize();
        this.refreshBtn.setVisible(false);
//        this.refreshBtn.setManaged(false);
        TableViewConfigurer.configureEditableTableCell(table, UIPropertyTableItem.class);
        numberOfRowsLabel.textProperty().bind(noRowsIntProp.asString().concat(" Headers"));
    }

    @FXML
    protected void addItem() {
        addItem(new UIPropertyTableItem("", ""));
    }

//    @Override
//    protected Predicate<UIPropertyTableItem> filterPredicate(Filter filter) {
//        return Filter.buildFilterPredicate(filter, UIPropertyTableItem::getName, UIPropertyTableItem::getValue);
//    }

}
