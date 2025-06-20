package io.github.nhtuan10.mykafkatool.ui.schemaregistry;

import io.github.nhtuan10.mykafkatool.annotation.FilterableTableItemField;
import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.StringProperty;

@FXModel
public final class SchemaTableItem implements SchemaTableItemFXModel {
    @TableViewColumn
    StringProperty subject;
    @TableViewColumn
    StringProperty schemaId;
    @TableViewColumn
    StringProperty latestVersion;
    @TableViewColumn
    StringProperty type;
    @TableViewColumn
    StringProperty compatibility;
    @FilterableTableItemField
    StringProperty schema;

    String clusterName;
}
