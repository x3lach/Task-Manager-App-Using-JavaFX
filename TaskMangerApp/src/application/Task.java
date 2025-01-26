package application;

import java.time.LocalDate;

public class Task {
	
	private int taskId;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String priority;
    private String status;
    private int categoryId;
    private int reminderDays;
    private boolean isRecurring;
    private String recurrenceInterval; // "Daily", "Weekly", "Monthly"
    private int recurrenceFrequency; // Number of times to repeat (0 = infinite)
    
    public Task() {}
    
	public Task(int taskId, String title, String description, LocalDate dueDate, String priority, String status,
			int categoryId, int reminderDays, boolean isRecurring, String recurrenceInterval, int recurrenceFrequency) {
		this.taskId = taskId;
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.priority = priority;
		this.status = status;
		this.categoryId = categoryId;
		this.reminderDays = reminderDays;
		this.isRecurring = isRecurring;
		this.recurrenceInterval = recurrenceInterval;
		this.recurrenceFrequency = recurrenceFrequency;
	}

	public boolean isRecurring() {
		return isRecurring;
	}

	public void setRecurring(boolean isRecurring) {
		this.isRecurring = isRecurring;
	}

	public String getRecurrenceInterval() {
		return recurrenceInterval;
	}

	public void setRecurrenceInterval(String recurrenceInterval) {
		this.recurrenceInterval = recurrenceInterval;
	}

	public int getRecurrenceFrequency() {
		return recurrenceFrequency;
	}

	public void setRecurrenceFrequency(int recurrenceFrequency) {
		this.recurrenceFrequency = recurrenceFrequency;
	}

	public int getReminderDays() {
		return reminderDays;
	}

	public void setReminderDays(int reminderDays) {
		this.reminderDays = reminderDays;
	}

	public int getTaskId() {
		return taskId;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}
    
	public String getCategoryName() {
        CategoryDAO categoryDAO = new CategoryDAO();
        return categoryDAO.getAllCategories().stream()
                .filter(c -> c.getCategoryId() == this.categoryId)
                .findFirst()
                .map(Category::getName)
                .orElse("Uncategorized");
    }
    
}
