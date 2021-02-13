import javafx.fxml.FXML
import javafx.scene.control.TextArea
import javafx.scene.layout.AnchorPane
import org.mariuszgromada.math.mxparser.mXparser


class HelpController {
  @FXML
  lateinit var textArea: TextArea

  @FXML
  fun initialize() {
    textArea.text = mXparser.getHelp()

  }

}