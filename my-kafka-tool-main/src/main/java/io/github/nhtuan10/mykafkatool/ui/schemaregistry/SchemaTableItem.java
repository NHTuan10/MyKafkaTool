package io.github.nhtuan10.mykafkatool.ui.schemaregistry;

import io.github.nhtuan10.mykafkatool.annotation.FilterableTableItemField;
import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@FXModel
public final class SchemaTableItem implements SchemaTableItemFXModel {
    @TableViewColumn
    StringProperty subject;
    @TableViewColumn
    StringProperty schemaId;
    @TableViewColumn
    StringProperty version;
    @TableViewColumn
    StringProperty type;
    @TableViewColumn
    StringProperty compatibility;
    @FilterableTableItemField
    StringProperty schema;

    String clusterName;
    @Getter
    @Setter
    List allVersions;
}
