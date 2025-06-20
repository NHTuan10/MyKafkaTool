package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@FXModel
@EqualsAndHashCode(callSuper = true)
@ToString
public final class ConsumerGroupTableItem extends ConsumerGroupTopic implements ConsumerGroupTableItemFXModel {
    @TableViewColumn
    IntegerProperty numberOfMembers;
    @TableViewColumn
    StringProperty lag;
    @TableViewColumn
    StringProperty state;
}
