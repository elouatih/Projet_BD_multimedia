import view.BaseController;
import database.OracleDB;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class Application extends javafx.application.Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        Application.primaryStage = primaryStage;
        Application.primaryStage.setTitle("Klex Data Manager");
        Application.primaryStage.setOnCloseRequest(e -> OracleDB.cleanup());

        if (OracleDB.checkForOracleDriver() && OracleDB.attemptConnect())
            initRootLayout();
    }

    private void initRootLayout() {
        primaryStage.setScene(loadScene("/view/root.fxml"));
        primaryStage.show();
    }

    public static Scene loadScene(String fxmlFile) {
        return loadScene(fxmlFile, null);
    }

    public static Scene loadScene(String fxmlFile, Consumer<BaseController> controllerInitialization) {
        try {
            FXMLLoader loader = new FXMLLoader(Application.class.getResource(fxmlFile));
            Parent layout = loader.load();
            BaseController controller = loader.getController();
            if (controller != null) {
                controller.setPrimaryStage(primaryStage);
                if (controllerInitialization != null) {
                    controllerInitialization.accept(controller);
                }

                controller.postInit();
            }

            return new Scene(layout);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
