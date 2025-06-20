package io.github.nhtuan10.mykafkatool.ui.messageview;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;

@FXModel
@EqualsAndHashCode
public final class KafkaMessageHeaderTableItem implements KafkaMessageHeaderTableItemFXModel {
    @TableViewColumn
    StringProperty key;
    @TableViewColumn
    StringProperty value;
}
