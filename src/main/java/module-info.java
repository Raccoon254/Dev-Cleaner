module com.kentom.devcleaner {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.logging;  // For java.util.logging package
    requires java.prefs;    // For java.util.prefs package

    opens com.kentom.devcleaner to javafx.fxml;
    exports com.kentom.devcleaner;
}