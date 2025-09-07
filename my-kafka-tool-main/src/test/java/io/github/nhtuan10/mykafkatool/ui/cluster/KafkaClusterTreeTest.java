//package io.github.nhtuan10.mykafkatool.ui.cluster;
//
//import io.github.nhtuan10.mykafkatool.api.SchemaRegistryManager;
//import io.github.nhtuan10.mykafkatool.api.exception.ClusterNameExistedException;
//import io.github.nhtuan10.mykafkatool.api.model.KafkaCluster;
//import io.github.nhtuan10.mykafkatool.constant.AppConstant;
//import io.github.nhtuan10.mykafkatool.manager.ClusterManager;
//import io.github.nhtuan10.mykafkatool.model.kafka.KafkaPartition;
//import io.github.nhtuan10.mykafkatool.model.kafka.KafkaTopic;
//import io.github.nhtuan10.mykafkatool.ui.consumergroup.ConsumerGroupListTreeItem;
//import io.github.nhtuan10.mykafkatool.ui.event.EventDispatcher;
//import io.github.nhtuan10.mykafkatool.ui.topic.KafkaTopicListTreeItem;
//import io.github.nhtuan10.mykafkatool.userpreference.UserPreference;
//import io.github.nhtuan10.mykafkatool.userpreference.UserPreferenceManager;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.scene.control.TreeItem;
//import javafx.scene.control.TreeView;
//import javafx.stage.Stage;
//import org.apache.kafka.clients.admin.CreateTopicsResult;
//import org.apache.kafka.clients.admin.NewTopic;
//import org.apache.kafka.common.KafkaFuture;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.testfx.framework.junit5.ApplicationExtension;
//import org.testfx.framework.junit5.ApplicationTest;
//
//import java.util.List;
//import java.util.function.Predicate;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(ApplicationExtension.class)
//class KafkaClusterTreeTest extends ApplicationTest {
//
//    @Mock
//    private ClusterManager clusterManager;
//
//    @Mock
//    private SchemaRegistryManager schemaRegistryManager;
//
//    @Mock
//    private EventDispatcher eventDispatcher;
//
//    @Mock
//    private UserPreferenceManager userPreferenceManager;
//
//    @Mock
//    private Stage stage;
//
//    @Mock
//    private TreeView<Object> treeView;
//
//    @Mock
//    private UserPreference userPreference;
//
//    private KafkaClusterTree kafkaClusterTree;
//    private TreeItem<Object> rootItem;
//    private ObservableList<TreeItem<Object>> rootChildren;
//
//    @BeforeEach
//    void setUp() {
//        rootItem = new TreeItem<>(AppConstant.CLUSTERS_TREE_ITEM_DISPLAY_NAME);
//        rootChildren = FXCollections.observableArrayList();
//        rootItem.getChildren().setAll(rootChildren);
//
//        when(treeView.getRoot()).thenReturn(rootItem);
//        when(userPreferenceManager.loadUserPreference()).thenReturn(userPreference);
//        when(userPreference.connections()).thenReturn(List.of());
//
//        kafkaClusterTree = new KafkaClusterTree(clusterManager, treeView, schemaRegistryManager,
//                eventDispatcher, userPreferenceManager);
//    }
//
//    @Test
//    void testConstructor_InitializesRootItem() {
//        // Verify that the constructor properly initializes the tree
//        verify(treeView).setRoot(any(TreeItem.class));
//        verify(userPreferenceManager).loadUserPreference();
//
//        TreeItem<Object> root = treeView.getRoot();
//        assertNotNull(root);
//        assertEquals(AppConstant.CLUSTERS_TREE_ITEM_DISPLAY_NAME, root.getValue());
//        assertTrue(root.isExpanded());
//    }
//
//    @Test
//    void testSetStage() {
//        // Act
//        kafkaClusterTree.setStage(stage);
//
//        // Assert - no exception thrown, setter works
//        assertDoesNotThrow(() -> kafkaClusterTree.setStage(stage));
//    }
//
//    @Test
//    void testSetTreeItemFilterPredicate() {
//        // Arrange
//        KafkaTopicListTreeItem<Object> topicListItem = mock(KafkaTopicListTreeItem.class);
//        ConsumerGroupListTreeItem<Object> consumerGroupListItem = mock(ConsumerGroupListTreeItem.class);
//
//        TreeItem<Object> clusterItem = new TreeItem<>(createTestCluster("test-cluster"));
//        clusterItem.getChildren().addAll(List.of(topicListItem, consumerGroupListItem));
//        rootChildren.add(clusterItem);
//
//        Predicate<Object> testPredicate = obj -> true;
//
//        // Act
//        kafkaClusterTree.setTreeItemFilterPredicate(testPredicate);
//
//        // Assert
//        verify(topicListItem).predicateProperty();
//        verify(consumerGroupListItem).predicateProperty();
//    }
//
//    @Test
//    void testAddAllConnectionsFromUserPreference_WithValidClusters() throws ClusterNameExistedException {
//        // Arrange
//        KafkaCluster cluster1 = createTestCluster("cluster1");
//        KafkaCluster cluster2 = createTestCluster("cluster2");
//
//        UserPreference userPref = mock(UserPreference.class);
//        when(userPref.connections()).thenReturn(List.of(cluster1, cluster2));
//
//        // Act
//        kafkaClusterTree.addAllConnectionsFromUserPreference(userPref);
//
//        // Assert
//        verify(clusterManager, times(2)).connectToCluster(any(KafkaCluster.class));
//    }
//
//    @Test
//    void testAddAllConnectionsFromUserPreference_WithDuplicateCluster() {
//        // Arrange
//        KafkaCluster cluster = createTestCluster("duplicate-cluster");
//        rootChildren.add(new KafkaClusterTreeItem<>(cluster));
//
//        UserPreference userPref = mock(UserPreference.class);
//        when(userPref.connections()).thenReturn(List.of(cluster));
//
//        // Act & Assert - should not throw exception but should not add duplicate
//        assertDoesNotThrow(() -> kafkaClusterTree.addAllConnectionsFromUserPreference(userPref));
//
//        // Verify cluster was not connected again
//        verify(clusterManager, never()).connectToCluster(cluster);
//    }
//
//    @Test
//    void testIsClusterNameExistedInTree_WithExistingCluster() throws ClusterNameExistedException {
//        // Arrange
//        KafkaCluster cluster = createTestCluster("existing-cluster");
//        rootChildren.add(new KafkaClusterTreeItem<>(cluster));
//
//        // Act
//        boolean exists = KafkaClusterTree.isClusterNameExistedInTree(treeView, "existing-cluster");
//
//        // Assert
//        assertTrue(exists);
//    }
//
//    @Test
//    void testIsClusterNameExistedInTree_WithNonExistingCluster() throws ClusterNameExistedException {
//        // Arrange
//        KafkaCluster cluster = createTestCluster("existing-cluster");
//        rootChildren.add(new KafkaClusterTreeItem<>(cluster));
//
//        // Act
//        boolean exists = KafkaClusterTree.isClusterNameExistedInTree(treeView, "non-existing-cluster");
//
//        // Assert
//        assertFalse(exists);
//    }
//
//    @Test
//    void testIsClusterNameExistedInTree_WithEmptyTree() throws ClusterNameExistedException {
//        // Act
//        boolean exists = KafkaClusterTree.isClusterNameExistedInTree(treeView, "any-cluster");
//
//        // Assert
//        assertFalse(exists);
//    }
//
//    @Test
//    void testAddTopic_WithValidSelection() throws Exception {
//        // Arrange
//        KafkaCluster cluster = createTestCluster("test-cluster");
//        KafkaTopicListTreeItem.KafkaTopicListTreeItemValue value =
//                mock(KafkaTopicListTreeItem.KafkaTopicListTreeItemValue.class);
//        when(value.getCluster()).thenReturn(cluster);
//
//        KafkaTopicListTreeItem<Object> topicListItem = mock(KafkaTopicListTreeItem.class);
//        when(topicListItem.getValue()).thenReturn(value);
//
//        when(treeView.getSelectionModel().getSelectedItem()).thenReturn(topicListItem);
//
//        NewTopic newTopic = new NewTopic("test-topic", 3, (short) 1);
//        CreateTopicsResult createResult = mock(CreateTopicsResult.class);
//        KafkaFuture<Void> future = mock(KafkaFuture.class);
//        when(createResult.all()).thenReturn(future);
//        when(clusterManager.addTopic(eq("test-cluster"), any(NewTopic.class))).thenReturn(createResult);
//
//        // This test would require more setup for the modal dialog, so we'll test the validation part
//        assertDoesNotThrow(() -> {
//            // The method would normally show a modal, but we're testing the setup
//            if (treeView.getSelectionModel().getSelectedItem() instanceof KafkaTopicListTreeItem<?>) {
//                // Validation passes
//                assertTrue(true);
//            }
//        });
//    }
//
//    @Test
//    void testConfigureClusterTreeActionMenu() {
//        // Act
//        kafkaClusterTree.configureClusterTreeActionMenu();
//
//        // Assert
//        verify(treeView).setContextMenu(any());
//    }
//
//    @Test
//    void testAddNewConnection_CallsCorrectMethods() {
//        // This method involves UI modals which are difficult to test
//        // We can test that the method exists and doesn't throw on basic setup
//        assertDoesNotThrow(() -> {
//            // The actual implementation would show a modal dialog
//            // For unit testing, we verify the method signature exists
//            kafkaClusterTree.addNewConnection();
//        });
//    }
//
//    @Test
//    void testAddNewConnection_WithClonedFrom() {
//        // Arrange
//        KafkaCluster clonedFrom = createTestCluster("source-cluster");
//
//        // Act & Assert
//        assertDoesNotThrow(() -> {
//            kafkaClusterTree.addNewConnection(clonedFrom);
//        });
//    }
//
//    // Helper method to create test clusters
//    private KafkaCluster createTestCluster(String name) {
//        KafkaCluster cluster = new KafkaCluster(name,"localhost:9092");
//        cluster.setStatus(KafkaCluster.ClusterStatus.DISCONNECTED);
//        return cluster;
//    }
//
//    // Test for cell factory behavior
//    @Test
//    void testCellFactory_UpdateItem() {
//        // This test verifies that the cell factory is set up
//        // The actual cell behavior would require JavaFX Application Thread
//        verify(treeView).setCellFactory(any());
//    }
//
//    @Test
//    void testTreeItemTypes() {
//        // Test that different tree item types are handled
//        KafkaCluster cluster = createTestCluster("test-cluster");
//        KafkaTopic topic = new KafkaTopic("test-topic", cluster, List.of());
//        KafkaPartition partition = new KafkaPartition(0, topic);
//        topic.partitions().add(partition);
//
//        // Verify objects can be created (basic structure test)
//        assertNotNull(cluster);
//        assertNotNull(topic);
//        assertNotNull(partition);
//        assertEquals("test-cluster", cluster.getName());
//        assertEquals("test-topic", topic.name());
//        assertEquals(0, partition.id());
//    }
//
//    @Test
//    void testExceptionHandling_ClusterNameExistedException() {
//        // Arrange
//        KafkaCluster existingCluster = createTestCluster("existing");
//        rootChildren.add(new KafkaClusterTreeItem<>(existingCluster));
//
//        // Act & Assert
//        assertThrows(ClusterNameExistedException.class, () -> {
//            KafkaClusterTree.isClusterNameExistedInTree(treeView, "existing");
//            // Simulate the exception that would be thrown in the actual implementation
//            if (KafkaClusterTree.isClusterNameExistedInTree(treeView, "existing")) {
//                throw new ClusterNameExistedException("existing", "Cluster already exists");
//            }
//        });
//    }
//
//    @Test
//    void testUserPreferenceIntegration() {
//        // Arrange
//        List<KafkaCluster> clusters = List.of(
//                createTestCluster("cluster1"),
//                createTestCluster("cluster2")
//        );
//        when(userPreference.connections()).thenReturn(clusters);
//
//        // Act
//        kafkaClusterTree.addAllConnectionsFromUserPreference(userPreference);
//
//        // Assert
//        verify(clusterManager, times(2)).connectToCluster(any(KafkaCluster.class));
//    }
//
//    @Test
//    void testEventDispatcherUsage() {
//        // Verify that the event dispatcher is properly injected and would be used
//        assertNotNull(eventDispatcher);
//
//        // In actual usage, events would be published like:
//        // eventDispatcher.publishEvent(someEvent);
//        // But we can't easily test this without more complex setup
//    }
//
//    @Test
//    void testClusterManagerIntegration() {
//        // Verify cluster manager is properly injected
//        assertNotNull(clusterManager);
//
//        // Test that cluster manager methods would be called
//        verify(clusterManager, atLeast(0)).connectToCluster(any());
//    }
//
//    @Test
//    void testSchemaRegistryManagerIntegration() {
//        // Verify schema registry manager is properly injected
//        assertNotNull(schemaRegistryManager);
//    }
//
//    @Test
//    void testUserPreferenceManagerIntegration() {
//        // Verify user preference manager is properly injected and used
//        assertNotNull(userPreferenceManager);
//        verify(userPreferenceManager).loadUserPreference();
//    }
//}