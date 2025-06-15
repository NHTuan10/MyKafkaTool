package io.github.nhtuan10.mykafkatool.ui.messageview;

import io.github.nhtuan10.mykafkatool.ui.control.EditableTableControl;
import io.github.nhtuan10.mykafkatool.ui.topic.UIPropertyTableItem;
import io.github.nhtuan10.mykafkatool.ui.util.TableViewConfigurer;
import javafx.fxml.FXML;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaMessageHeaderTable extends EditableTableControl<UIPropertyTableItem> {
    @FXML
    @SuppressWarnings(value = "unchecked")
    protected void initialize() {
        super.initialize();
        this.refreshBtn.setVisible(false);
//        this.refreshBtn.setManaged(false);
        TableViewConfigurer.configureEditableTableCell(table, UIPropertyTableItem.class, stageHolder, (TableViewConfigurer.TableViewConfiguration<UIPropertyTableItem>) TableViewConfigurer.TableViewConfiguration.DEFAULT);
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
