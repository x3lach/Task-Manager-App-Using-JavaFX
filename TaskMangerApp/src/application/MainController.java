package application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.Timer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox; 
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.File;
import java.io.FileWriter;
import javafx.stage.FileChooser;

public class MainController {
    @FXML private TableView<Task> tasksTable;
    @FXML private ComboBox<String> categoryFilter, statusFilter, priorityFilter;
    @FXML private TextField searchField;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;

    
    private ObservableList<Category> categories = FXCollections.observableArrayList();
    private ObservableList<Task> tasksList = FXCollections.observableArrayList();
    private FilteredList<Task> filteredTasks = new FilteredList<>(tasksList);
    private TaskDAO taskDAO = new TaskDAO();

    public void initialize() {
        loadCategories(); 
        setupTableColumns();
        loadTasks();
        initializeFilters();
        initializeAutoSave();
        checkDueDates();
    }
    private void loadCategories() {
        categories.setAll(new CategoryDAO().getAllCategories());
    }

    private List<String> getCategoryNames() {
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.toList());
    }
   
    @FXML
    private void openTaskForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/TaskForm.fxml"));
            Parent root = loader.load();

            TaskFormController taskFormController = loader.getController();
            taskFormController.setMainController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("New Task");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setContentText("Failed to open task form!");
            errorAlert.show();
        }
    }
    
    private void setupTableColumns() {
        tasksTable.getColumns().clear();

        // 1. Title Column
        TableColumn<Task, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setPrefWidth(150);
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title")); 

        // 2. Due Date Column
        TableColumn<Task, LocalDate> dueDateColumn = new TableColumn<>("Due Date");
        dueDateColumn.setPrefWidth(100);
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate")); 

        // 3. Priority Column (with color coding)
        TableColumn<Task, String> priorityColumn = new TableColumn<>("Priority");
        priorityColumn.setPrefWidth(80);
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority")); 
        priorityColumn.setCellFactory(column -> new TableCell<Task, String>() {
            @Override
            protected void updateItem(String priority, boolean empty) {
                super.updateItem(priority, empty);
                if (empty || priority == null) {
                    setStyle("");
                } else {
                    setText(priority); 
                    switch (priority) {
                        case "High":
                            setStyle("-fx-background-color: #ffcccc; -fx-alignment: CENTER;");
                            break;
                        case "Medium":
                            setStyle("-fx-background-color: #ffffcc; -fx-alignment: CENTER;");
                            break;
                        case "Low":
                            setStyle("-fx-background-color: #ccffcc; -fx-alignment: CENTER;");
                            break;
                    }
                }
            }
        });

        // 4. Description Column
        TableColumn<Task, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setPrefWidth(250);
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description")); 

        // 5. Status Column (with checkbox)
        TableColumn<Task, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setPrefWidth(100);
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status")); 
        statusColumn.setCellFactory(column -> new TableCell<Task, String>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                } else {
                    Task task = getTableView().getItems().get(getIndex());
                    checkBox.setSelected("Completed".equals(status));
                    
                    checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        task.setStatus(newVal ? "Completed" : "Pending");
                        taskDAO.updateTask(task);
                        
                        if (newVal) {  
                            taskDAO.handleRecurringTask(task);
                            refreshTaskList();  
                        }
                        
                        updateProgress();
                    });
                    
                    setGraphic(checkBox);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

     // 6. Actions Column (Edit and Delete)
        TableColumn<Task, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setPrefWidth(150); // Wider for two buttons

        actionsColumn.setCellFactory(column -> new TableCell<Task, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox buttons = new HBox(editBtn, deleteBtn);

            {
                buttons.setSpacing(5);
                editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #ff6666; -fx-text-fill: white;");

                // Edit Button Action
                editBtn.setOnAction(event -> {
                    Task task = getTableView().getItems().get(getIndex());
                    openEditForm(task); // Open form with task data
                });

                // Delete Button Action (existing code)
                deleteBtn.setOnAction(event -> {
                    Task task = getTableView().getItems().get(getIndex());
                    if (showDeleteConfirmation()) {
                        taskDAO.deleteTask(task.getTaskId());
                        refreshTaskList();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });

        // Add all columns to the table IN ORDER
        tasksTable.getColumns().addAll(
            titleColumn,
            dueDateColumn,
            priorityColumn,
            descriptionColumn,
            statusColumn,
            actionsColumn
        );

        tasksTable.setItems(filteredTasks); 
    }
    @FXML
    private void exportToTXT() {
        if (filteredTasks.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No tasks to export!").show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Text File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showSaveDialog(tasksTable.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
            	writer.write(String.format("%-20s %-30s %-15s %-10s %-10s %-15s %-10s %-10s %-10s%n",
                        "Title", "Description", "Due Date", "Priority", "Status", "Category", "Recurring", "Interval", "Frequency"));
                for (Task task : filteredTasks) {
                    String dueDate = task.getDueDate() != null ? task.getDueDate().toString() : "N/A";
                    String line = String.format("%-20s %-30s %-15s %-10s %-10s %-15s %-10s %-10s %-10d%n",
                            task.getTitle(),
                            task.getDescription(),
                            dueDate,
                            task.getPriority(),
                            task.getStatus(),
                            task.getCategoryName(),
                            task.isRecurring() ? "Yes" : "No",
                            task.getRecurrenceInterval() != null ? task.getRecurrenceInterval() : "N/A",
                            task.getRecurrenceFrequency());

                    writer.write(line);
                }

                new Alert(Alert.AlertType.INFORMATION, "Tasks exported to text file!").show();
            } catch (IOException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to export: " + e.getMessage()).show();
            }
        }
    }
    private void showTaskDetails(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/TaskDetails.fxml"));
            Parent root = loader.load();
            
            TaskDetailsController controller = loader.getController();
            controller.setTask(task); 
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Task Details");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open details window!").show();
        }
    }
    private boolean showDeleteConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Task");
        alert.setContentText("Are you sure you want to delete this task?");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
    
    private void loadTasks() {
        tasksList.setAll(taskDAO.getAllTasks());
        updateProgress();
    }

    private void initializeFilters() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("All"); 
        categoryNames.addAll(getCategoryNames()); 
        categoryFilter.getItems().addAll(categoryNames);
        statusFilter.getItems().addAll("All", "Pending", "Completed");
        priorityFilter.getItems().addAll("All", "Low", "Medium", "High");
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        categoryFilter.setValue("All");
        statusFilter.setValue("All");
        priorityFilter.setValue("All");
    }

    private void updateFilter() {
        Predicate<Task> combinedPredicate = task ->
            // Category filter
            (categoryFilter.getValue() == null || 
             categoryFilter.getValue().equals("All") || 
             task.getCategoryName().equals(categoryFilter.getValue())) &&

            // Status filter
            (statusFilter.getValue() == null || 
             statusFilter.getValue().equals("All") || 
             task.getStatus().equals(statusFilter.getValue())) &&

            // Priority filter
            (priorityFilter.getValue() == null || 
             priorityFilter.getValue().equals("All") || 
             task.getPriority().equals(priorityFilter.getValue())) &&

            // Search field
            (searchField.getText().isEmpty() || 
             task.getTitle().toLowerCase().contains(searchField.getText().toLowerCase()));

        filteredTasks.setPredicate(combinedPredicate);
    }


    private void updateProgress() {
        long total = tasksList.size();
        long completed = tasksList.stream()
                .filter(task -> "Completed".equals(task.getStatus()))
                .count();
        progressBar.setProgress(total > 0 ? (double) completed / total : 0);
        progressLabel.setText(String.format("%d/%d tasks completed", completed, total));
    }

    private void initializeAutoSave() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    tasksList.forEach(taskDAO::updateTask);
                    System.out.println("Auto-save completed at " + new Date());
                });
            }
        }, 0, 300000); 
    }

    private void checkDueDates() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                	String query = "SELECT * FROM tasks WHERE " +
                            "(CURDATE() >= due_date - INTERVAL reminder_days DAY) " + // Notify BEFORE due date
                            "AND status != 'Completed'";

                    try (Connection conn = DatabaseConnection.getConnection();
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {

                        List<Task> tasks = new ArrayList<>();
                        while (rs.next()) {
                            Task task = new Task();
                            task.setTaskId(rs.getInt("task_id"));
                            task.setTitle(rs.getString("title"));
                            task.setDescription(rs.getString("description"));
                            task.setDueDate(rs.getDate("due_date").toLocalDate());
                            task.setPriority(rs.getString("priority"));
                            task.setStatus(rs.getString("status"));
                            task.setCategoryId(rs.getInt("category_id"));
                            task.setReminderDays(rs.getInt("reminder_days"));
                            task.setReminderDays(rs.getInt("reminder_days")); 
                            tasks.add(task);
                        }

                        if (!tasks.isEmpty()) {
                            new Alert(Alert.AlertType.WARNING, 
                                "You have " + tasks.size() + " tasks due soon!").show();
                        }

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }
        }, 0, 86400000); 
    }
    
    public void refreshTaskList() {
        loadTasks();
        tasksTable.refresh(); 

    }
    private void openEditForm(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/TaskForm.fxml"));
            Parent root = loader.load();

            TaskFormController controller = loader.getController();
            controller.setMainController(this);
            controller.setTask(task); 

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Task");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open edit form!").show();
        }
    }
    
}