module TaskManagerApp {

	requires javafx.controls;  
    requires javafx.fxml;      
    requires java.sql;         
    requires mysql.connector.j; 
    
    opens application to javafx.fxml, javafx.base;

    exports application;
}