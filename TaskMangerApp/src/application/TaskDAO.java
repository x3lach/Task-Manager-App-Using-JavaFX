package application;

import java.sql.Types;  
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {
    public List<Task> getAllTasks() {
    	
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT * FROM tasks";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

        	while (rs.next()) {
                Task task = new Task();
                task.setTaskId(rs.getInt("task_id"));
                task.setTitle(rs.getString("title"));
                task.setDescription(rs.getString("description"));
                
                // Add recurrence fields
                task.setRecurring(rs.getBoolean("is_recurring"));
                task.setRecurrenceInterval(rs.getString("recurrence_interval"));
                task.setRecurrenceFrequency(rs.getInt("recurrence_frequency"));
                
                Date dueDateSQL = rs.getDate("due_date");
                LocalDate dueDate = (dueDateSQL != null) ? dueDateSQL.toLocalDate() : null;
                task.setDueDate(dueDate);
                
                task.setPriority(rs.getString("priority"));
                task.setStatus(rs.getString("status"));
                task.setCategoryId(rs.getInt("category_id"));
                task.setReminderDays(rs.getInt("reminder_days")); 
                tasks.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public List<Task> getOverdueTasks() {
        List<Task> overdue = new ArrayList<>();
        String query = "SELECT * FROM tasks WHERE due_date < CURDATE() AND status != 'Completed'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
        	while (rs.next()) {
                Task task = new Task();
                task.setTaskId(rs.getInt("task_id"));
                task.setTitle(rs.getString("title"));
                task.setDescription(rs.getString("description"));
                
                // Add recurrence fields
                task.setRecurring(rs.getBoolean("is_recurring"));
                task.setRecurrenceInterval(rs.getString("recurrence_interval"));
                task.setRecurrenceFrequency(rs.getInt("recurrence_frequency"));
                
                Date dueDateSQL = rs.getDate("due_date");
                LocalDate dueDate = (dueDateSQL != null) ? dueDateSQL.toLocalDate() : null;
                task.setDueDate(dueDate);
                
                task.setPriority(rs.getString("priority"));
                task.setStatus(rs.getString("status"));
                task.setCategoryId(rs.getInt("category_id"));
                overdue.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return overdue;
    }

    public void addTask(Task task) {
        String query = "INSERT INTO tasks (title, description, due_date, priority, status, category_id, reminder_days, is_recurring, recurrence_interval, recurrence_frequency) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set mandatory fields
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());

            // Handle optional due date
            if (task.getDueDate() != null) {
                pstmt.setDate(3, Date.valueOf(task.getDueDate()));
            } else {
                pstmt.setNull(3, Types.DATE);
            }

            // Set priority and status
            pstmt.setString(4, task.getPriority());
            pstmt.setString(5, task.getStatus());

            // Set category ID
            pstmt.setInt(6, task.getCategoryId());

            // Set reminder days
            pstmt.setInt(7, task.getReminderDays());

            // Set recurrence fields
            pstmt.setBoolean(8, task.isRecurring());
            pstmt.setString(9, task.getRecurrenceInterval());
            pstmt.setInt(10, task.getRecurrenceFrequency());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTask(Task task) {
    	String query = "UPDATE tasks SET description=?, due_date=?, priority=?, status=?, category_id=?, reminder_days=?, is_recurring=?, recurrence_interval=?, recurrence_frequency=? " +
    	               "WHERE task_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

        	pstmt.setString(1, task.getDescription());
            if (task.getDueDate() != null) {
                pstmt.setDate(2, Date.valueOf(task.getDueDate()));
            } else {
                pstmt.setNull(2, Types.DATE);
            }

            pstmt.setString(3, task.getPriority());
            pstmt.setString(4, task.getStatus());
            pstmt.setInt(5, task.getCategoryId());
            pstmt.setInt(6, task.getReminderDays());
            pstmt.setInt(7, task.getTaskId());
            pstmt.setBoolean(8, task.isRecurring());
            pstmt.setString(9, task.getRecurrenceInterval());
            pstmt.setInt(10, task.getRecurrenceFrequency());
            pstmt.setBoolean(7, task.isRecurring());
            pstmt.setString(8, task.getRecurrenceInterval());
            pstmt.setInt(9, task.getRecurrenceFrequency());
            pstmt.setInt(10, task.getTaskId()); 
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void handleRecurringTask(Task completedTask) {
        if (!completedTask.isRecurring()) return;
        LocalDate nextDueDate = calculateNextDueDate(
            completedTask.getDueDate(), 
            completedTask.getRecurrenceInterval()
        );

        Task newTask = new Task();
        newTask.setTitle(completedTask.getTitle());
        newTask.setDescription(completedTask.getDescription());
        newTask.setDueDate(nextDueDate);
        newTask.setPriority(completedTask.getPriority());
        newTask.setStatus("Pending");
        newTask.setCategoryId(completedTask.getCategoryId());
        newTask.setRecurring(completedTask.isRecurring());
        newTask.setRecurrenceInterval(completedTask.getRecurrenceInterval());
        newTask.setRecurrenceFrequency(completedTask.getRecurrenceFrequency() - 1); 

        if (newTask.getRecurrenceFrequency() > 0 || completedTask.getRecurrenceFrequency() == 0) {
            addTask(newTask);
        }
    }

    private LocalDate calculateNextDueDate(LocalDate dueDate, String interval) {
        switch (interval) {
            case "Daily":   return dueDate.plusDays(1);
            case "Weekly":  return dueDate.plusWeeks(1);
            case "Monthly": return dueDate.plusMonths(1);
            default:        return dueDate;
        }
    }
    public void deleteTask(int taskId) {
        String query = "DELETE FROM tasks WHERE task_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, taskId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}