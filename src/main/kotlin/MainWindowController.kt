import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.chart.*
import javafx.scene.control.*
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.shape.StrokeType
import javafx.stage.Stage
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.gillius.jfxutils.JFXUtil
import org.gillius.jfxutils.chart.ChartPanManager
import org.gillius.jfxutils.chart.ChartZoomManager
import org.mariuszgromada.math.mxparser.*
import java.net.URL
import java.util.*
import java.util.regex.Pattern


private val EXAMPLE_EXPRESSION = """
// expression example
fun f(a) = 5 * a + 1
val y = x^2 * sin(x)
return f(y)

/*
// complex expression example
fun re(a) = a^2 * sin(a)
fun im(a) = a^2 * cos(a)
return (re(x), im(x))
*/
""".trimIndent()

class MainWindowController {
  private val parser = Parser()

  private lateinit var expressionCodeArea: CodeArea

  private var yMax = 0.0
  private var yMin = 0.0

  @FXML
  fun initialize() {
    initChart()
    initHelp()
    initExpressionArea()
    initComputeButton()
  }

  private fun initExpressionArea() {
    expressionCodeArea = CodeArea()

    expressionCodeArea.let { area ->
      anchorPane.children.add(area)
      AnchorPane.setTopAnchor(area, 0.0)
      AnchorPane.setBottomAnchor(area, 0.0)
      AnchorPane.setRightAnchor(area, 0.0)
      AnchorPane.setLeftAnchor(area, 0.0)

      area.richChanges()
        .filter { it.inserted != it.removed }
        .subscribe { area.setStyleSpans(0, computeHighlighting(area.text)) }
      area.style = """
        -fx-font-family: monospaced;
        -fx-font-size: 12pt;
      """

      area.insertText(0, EXAMPLE_EXPRESSION)
    }
  }

  private fun compute() {
    chart.data.clear()
    valuesTable.text = ""

    withClock {
      val expression = expressionCodeArea.text

      val x = generateSequence(xFrom.text.toDouble()) { x ->
        val next = x + xStep.text.toDouble()
        when {
          next <= xTo.text.toDouble() -> next
          else -> null
        }
      }.toList()
      val y = x.map { parser.parse(expression, it) }
      val yReal = y.map { it.first }
      val yImag = y.mapNotNull { it.second }

      addSeries(x, y.map { it.first })

      if (yImag.isNotEmpty()) {
        addSeries(x, yImag, "imaginary")
      }

      valuesTable.text = buildValuesTable(x, yReal, yImag)
    }
  }

  private fun addSeries(x: List<Double>, y: List<Double>, componentName: String = "real") {
    val series: XYChart.Series<Number, Number> = XYChart.Series<Number, Number>().apply {
      data.addAll(seriesData(x, y))
      name = componentName
    }
    chart.data.add(series)

    series.node.style = "-fx-stroke-width: 2px;"
  }

  private fun buildValuesTable(x: List<Double>, yReal: List<Double>, yImag: List<Double> = emptyList()): String {
    val columnSeparator = "\t"

    return StringBuilder().apply {
      append("x")
      when {
        yImag.isEmpty() -> {
          append(String.format(Locale.US, "%16s", "y"))
        }
        else -> {
          append(String.format(Locale.US, "%20s", "yReal"))
          append(columnSeparator)
          append(String.format(Locale.US, "%18s", "yImaginary"))
        }
      }
      appendln()

      x.forEachIndexed { idx, xValue ->
        append(String.format(Locale.US, "%.8f", xValue))
        append(columnSeparator)
        append(String.format(Locale.US, "%.8f", yReal[idx]))

        if (yImag.isNotEmpty()) {
          append(columnSeparator)
          append(String.format(Locale.US, "%.8f", yImag[idx]))
        }
        appendln()
      }
    }.toString()
  }

  private fun initComputeButton() {
    computeButton.setOnMouseClicked {
      compute()
    }
  }

  private fun initChart() {
    setPanning()
    setZooming()
    setChartSettings()
    setDoubleMouseClickRescaling()
  }

  private fun setPanning() {
    // panning works via either secondary (right) mouse or primary with ctrl held down
    val panner = ChartPanManager(chart)
    panner.setMouseFilter { event ->
      if (event.button === MouseButton.SECONDARY || event.button === MouseButton.PRIMARY && event.isShortcutDown) {
        // let it through
      } else {
        event.consume()
      }
    }
    panner.start()
  }

  private fun setZooming() {
    StackPane().apply {
      if (chart.parent != null) {
        JFXUtil.replaceComponent(chart, this)
      }

      val selectRect = Rectangle(0.0, 0.0, 0.0, 0.0)
      with(selectRect) {
        fill = Color.DARKGRAY
        isMouseTransparent = true
        opacity = 0.15
        stroke = Color.rgb(0, 0x29, 0x66)
        strokeType = StrokeType.INSIDE
        strokeWidth = 1.0
      }
      StackPane.setAlignment(selectRect, Pos.TOP_LEFT)

      children.addAll(chart, selectRect)

      with(ChartZoomManager(this@apply, selectRect, chart)) {
        mouseFilter = EventHandler<MouseEvent> { mouseEvent ->
          if (mouseEvent.button !== MouseButton.PRIMARY || mouseEvent.isShortcutDown) {
            mouseEvent.consume()
          }
        }
        start()
      }
    }
  }

  private fun setChartSettings() = with(chart) {
    createSymbols = false
    animated = false
    isLegendVisible = true
  }

  private fun setDoubleMouseClickRescaling() = chart.lookup(".chart-plot-background").setOnMouseClicked { event ->
    fun Double.padding() = this / 10

    if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
      with(xAxis) {
        lowerBound = xFrom.text.toDouble().let { it - it.padding() }
        upperBound = xTo.text.toDouble().let { it + it.padding() }
        tickUnit = (upperBound - lowerBound) / 10
      }

      if (chart.data.isEmpty()) {
        yMin = 0.0
        yMax = 10.0
      } else {
        val allYValues = chart.data.flatMap { series -> series.data.map { it.yValue as Double } }
        yMin = allYValues.minBy { it }!!
        yMax = allYValues.maxBy { it }!!
      }

      with(yAxis) {
        lowerBound = yMin.let { it + it.padding() }
        upperBound = yMax.let { it + it.padding() }
        tickUnit = (upperBound - lowerBound) / 5
      }
    }
  }

  private fun initHelp() = helpButton.setOnMouseClicked {
    val page = with(FXMLLoader()) {
      location = MainApp::class.java.getResource("fxml/Help.fxml")
      load<AnchorPane>()
    }
    with(Stage()) {
      title = "Help"
      scene = Scene(page)
      /* works after pressing directory button or switching between angle and T modes. Why? */
      addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
        if (KeyCode.ESCAPE == event.code) {
          close()
        }
      }
      showAndWait()
    }
  }

  private fun seriesData(x: List<Double>, y: List<Double>) = x.indices.map { XYChart.Data<Number, Number>(x[it], y[it]) }

  private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
    val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
    val KEYWORD_PATTERN = "(\\bval\\b|\\bfun\\b|\\breturn\\b)"
    val PATTERN = Pattern.compile("(?<KEYWORD>$KEYWORD_PATTERN)|(?<COMMENT>$COMMENT_PATTERN)")

    val matcher = PATTERN.matcher(text)
    var lastKwEnd = 0
    val spansBuilder = StyleSpansBuilder<Collection<String>>()
    while (matcher.find()) {
      val styleClass = (when {
        matcher.group("COMMENT") != null -> "comment"
        matcher.group("KEYWORD") != null -> "keyword"
        else -> null
      })!! /* never happens */
      spansBuilder.add(emptyList(), matcher.start() - lastKwEnd)
      spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
      lastKwEnd = matcher.end()
    }
    spansBuilder.add(emptyList(), text.length - lastKwEnd)
    return spansBuilder.create()
  }

  private fun withClock(block: () -> Unit) {
    val start = System.nanoTime()
    block()
    val stop = System.nanoTime()
    computationTimeLabel.text = "Computation time: ${String.format(Locale.US, "%.2f", (stop - start).toDouble() / 1E6)}ms"
  }

  @FXML
  private lateinit var chart: LineChart<Number, Number>

  @FXML
  private lateinit var xAxis: NumberAxis

  @FXML
  private lateinit var yAxis: NumberAxis

  @FXML
  private lateinit var xFrom: TextField

  @FXML
  private lateinit var xTo: TextField

  @FXML
  private lateinit var xStep: TextField

  @FXML
  private lateinit var yFrom: TextField

  @FXML
  private lateinit var yTo: TextField

  @FXML
  private lateinit var computeButton: Button

  @FXML
  private lateinit var helpButton: Button

  @FXML
  private lateinit var anchorPane: AnchorPane

  @FXML
  private lateinit var valuesTable: TextArea

  @FXML
  private lateinit var computationTimeLabel: Label

  @FXML // ResourceBundle that was given to the FXMLLoader
  private lateinit var resources: ResourceBundle

  @FXML // URL location of the FXML file that was given to the FXMLLoader
  private lateinit var location: URL
}