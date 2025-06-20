package io.github.nhtuan10.mykafkatool.ui.messageview;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;
import org.apache.kafka.common.header.Headers;

@FXModel
public final class KafkaMessageTableItem implements KafkaMessageTableItemFXModel {
    @TableViewColumn
    IntegerProperty partition;
    @TableViewColumn
    LongProperty offset;
    @TableViewColumn
    StringProperty key;
    @TableViewColumn
    StringProperty value;
    @TableViewColumn
    StringProperty timestamp;
    @TableViewColumn
    IntegerProperty serializedValueSize;

    String valueContentType;
    Headers headers;
    String schema;
    boolean isErrorItem;

}
