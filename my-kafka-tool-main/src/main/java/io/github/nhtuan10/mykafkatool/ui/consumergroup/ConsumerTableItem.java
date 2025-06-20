package io.github.nhtuan10.mykafkatool.ui.consumergroup;

import io.github.nhtuan10.mykafkatool.annotation.TableViewColumn;
import io.github.nhtuan10.mykafkatool.annotationprocessor.FXModel;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.common.ConsumerGroupState;

@FXModel
@EqualsAndHashCode(callSuper = true)
@ToString
public final class ConsumerTableItem extends ConsumerGroupTopic implements ConsumerTableItemFXModel {

    @TableViewColumn
    StringProperty consumerID;
    @TableViewColumn
    IntegerProperty partition;
    @TableViewColumn
    LongProperty start;
    @TableViewColumn
    LongProperty end;
    @TableViewColumn
    StringProperty committedOffset;
    @TableViewColumn
    StringProperty lag;
    @TableViewColumn
    StringProperty lastCommit;
    @TableViewColumn
    StringProperty host;

    ConsumerGroupState state;
}
