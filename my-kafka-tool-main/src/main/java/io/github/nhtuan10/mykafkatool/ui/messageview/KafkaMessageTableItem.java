package io.github.nhtuan10.mykafkatool.ui.messageview;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModelIgnore;
import io.github.nhtuan10.mykafkatool.model.kafka.SchemaMetadataFromRegistry;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.header.Headers;

import java.util.List;

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
    @FXModelIgnore
    @Setter
    @Getter
    List<SchemaMetadataFromRegistry> schemaList;
}
