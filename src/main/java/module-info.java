module com.kentom.dev_cleaner {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.kentom.dev_cleaner to javafx.fxml;
    exports com.kentom.dev_cleaner;
}