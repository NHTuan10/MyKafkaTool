package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@FXModel
@EqualsAndHashCode(callSuper = true)
@ToString
public final class ConsumerGroupTableItem extends ConsumerGroupTopic implements ConsumerGroupTableItemFXModel {
    //    @TableViewColumn
//    SimpleStringProperty groupId;
//    @TableViewColumn
//    SimpleStringProperty topic;
    @TableViewColumn
    SimpleIntegerProperty numberOfMembers;
    @TableViewColumn
    SimpleStringProperty lag;
    @TableViewColumn
    SimpleStringProperty state;
}
