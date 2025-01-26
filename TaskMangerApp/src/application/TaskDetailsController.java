package application;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class TaskDetailsController {
    @FXML private Label titleLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label dueDateLabel;
    @FXML private Label priorityLabel;
    @FXML private Label statusLabel;
    @FXML private Label categoryLabel;

    public void setTask(Task task) {
        titleLabel.setText(task.getTitle());
        descriptionArea.setText(task.getDescription());
        dueDateLabel.setText(task.getDueDate() != null ? task.getDueDate().toString() : "No due date");
        priorityLabel.setText(task.getPriority());
        statusLabel.setText(task.getStatus());
        categoryLabel.setText(task.getCategoryName());
    }

    @FXML
    private void closeWindow() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }
}