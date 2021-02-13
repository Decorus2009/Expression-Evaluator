import javafx.application.Application
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.FXMLLoader.load
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage

@FXML
lateinit var mainController: MainWindowController

@FXML
lateinit var helpController: HelpController

// https://annimon.com/article/2059
class MainApp : Application() {
  private val layout = "fxml/MainWindow.fxml"

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      launch(MainApp::class.java)

//      helpController
//      mainController
    }
  }

  @Throws(Exception::class)
  override fun start(primaryStage: Stage) {
    primaryStage.scene = Scene(load<Parent?>(MainApp::class.java.getResource(layout)))
    primaryStage.scene.stylesheets.add("css/all.css")
    primaryStage.show()
  }
}