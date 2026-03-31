module com.example.industrialsnmpreader {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.snmp4j;
    requires java.sql;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    opens com.example.industrialsnmpreader to javafx.fxml;
    exports com.example.industrialsnmpreader;
}