package application;

import java.util.List;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox; 

public class TaskFormController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Spinner<Integer> reminderSpinner;
    @FXML private CheckBox recurringCheckbox;
    @FXML private ComboBox<String> recurrenceIntervalCombo;
    @FXML private Spinner<Integer> recurrenceFrequencySpinner;

    private Task currentTask;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setTask(Task task) {
        currentTask = task;
        titleField.setText(task.getTitle());
        titleField.setDisable(true); 
        descriptionField.setText(task.getDescription());
        dueDatePicker.setValue(task.getDueDate());
        priorityComboBox.setValue(task.getPriority());
        statusComboBox.setValue(task.getStatus());
        categoryComboBox.setValue(getCategoryName(task.getCategoryId()));
    }

    private String getCategoryName(int categoryId) {
        CategoryDAO categoryDAO = new CategoryDAO();
        return categoryDAO.getAllCategories().stream()
                .filter(c -> c.getCategoryId() == categoryId)
                .findFirst()
                .map(Category::getName)
                .orElse("");
    }

    @FXML
    private void saveTask() {
        if (titleField.getText().isEmpty()) {
            showAlert("Title is required!");
            return;
        }

        Task task = new Task();
        task.setTitle(titleField.getText());
        task.setDescription(descriptionField.getText());
        task.setDueDate(dueDatePicker.getValue());
        task.setPriority(priorityComboBox.getValue());
        task.setStatus(statusComboBox.getValue());
        task.setCategoryId(getCategoryId(categoryComboBox.getValue()));
        task.setReminderDays(reminderSpinner.getValue()); // Add this line
        task.setRecurring(recurringCheckbox.isSelected());
        task.setRecurrenceInterval(recurrenceIntervalCombo.getValue());
        task.setRecurrenceFrequency(recurrenceFrequencySpinner.getValue());
        TaskDAO taskDAO = new TaskDAO();
        if (currentTask == null) {
            taskDAO.addTask(task);
        } else {
            task.setTaskId(currentTask.getTaskId());
            taskDAO.updateTask(task);
        }
        if (mainController != null) {
            mainController.refreshTaskList();
        }
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Success");
        successAlert.setHeaderText(null);
        successAlert.setContentText("Task saved successfully!");
        successAlert.showAndWait();
        cancel();
    }

    @FXML
    private void cancel() {
        titleField.getScene().getWindow().hide();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
    private int getCategoryId(String categoryName) {
        CategoryDAO categoryDAO = new CategoryDAO();
        return categoryDAO.getAllCategories().stream()
                .filter(c -> c.getName().equals(categoryName))
                .findFirst()
                .map(Category::getCategoryId)
                .orElse(-1); 
    }

    public void initialize() {
    	recurrenceIntervalCombo.getItems().addAll("Daily", "Weekly", "Monthly");
    	recurrenceFrequencySpinner.setValueFactory(
    	    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 365, 0)
    	);
        SpinnerValueFactory.IntegerSpinnerValueFactory reminderFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 30, 0);
        reminderSpinner.setValueFactory(reminderFactory);
        CategoryDAO categoryDAO = new CategoryDAO();
        List<String> categories = categoryDAO.getAllCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        categoryComboBox.getItems().addAll(categories);
        priorityComboBox.getItems().addAll("Low", "Medium", "High");
        priorityComboBox.setValue("Medium"); 
        statusComboBox.getItems().addAll("Pending", "Completed");
        statusComboBox.setValue("Pending"); 
        if (currentTask != null) {
            titleField.setText(currentTask.getTitle());
            descriptionField.setText(currentTask.getDescription());
            dueDatePicker.setValue(currentTask.getDueDate());
            priorityComboBox.setValue(currentTask.getPriority());
            statusComboBox.setValue(currentTask.getStatus());
            categoryComboBox.setValue(getCategoryName(currentTask.getCategoryId()));
            reminderSpinner.getValueFactory().setValue(currentTask.getReminderDays()); 
        }
    }
}