import org.mariuszgromada.math.mxparser.*
import org.mariuszgromada.math.mxparser.Function
import kotlin.text.StringBuilder

private const val ARG_PREFIX = "val "
private const val FUN_PREFIX = "fun "
private const val RET_PREFIX = "return"

data class LineDescriptor(val line: String, val lineNumber: Int)

class Parser {
  private val previousArguments = mutableListOf<Argument>()
  private val previousFunctions = mutableListOf<Function>()

  private fun prepare(expr: String) {
    val lineDescriptors = expr.split(System.lineSeparator(), "\n").mapIndexed { idx, line ->  LineDescriptor(line, idx) }


  }

  fun parse(expr: String, x: Double, debugMode: Boolean = false): Pair<Double, Double?> {
    clearState()
    previousArguments += Argument("x", x) // x is an implicit argument taken as current computation value of x on x-axis

    val expressionLines = expr
      .replace(Regex("(?s)/\\*.*\\*/"), "")                                    // exclude multi-line comments
      .replace(Regex("\\s*[/]{2,}.*"), "")                                     // exclude single-line comments
      .split(System.lineSeparator(), "\n").asSequence() // '\n' to account multiline Kotlin strings separated by '\n'
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .filterNot { it.startsWith("//") }
      .toList()
      .also {
        if (it.isEmpty()) {
          throw IllegalStateException("Empty expression")
        }
      }

    expressionLines.forEachIndexed { idx, line ->
      if (debugMode) {
        println("Line $idx: \"$line\"")
      }
      when {
        /**
         * Argument definition can reference earlier defined function. E.g.
         *   val f = Function("f(a, b) = 5 * a + b")
         *   val x = Argument("x = 3 + f(1, 2)", f)
         *
         *   x.checkSyntax() -> 'true'
         *   x.argumentValue -> 10.0
         */
        line.startsWith(ARG_PREFIX) -> {
          val argumentCandidateString = line.substring(ARG_PREFIX.length) // e.g. val y = x^2 -> y = x^2
          val dependencyArgs = previousArguments.filter { argumentCandidateString.hasArgumentExpressionFor(it) }
          val dependencyFunctions = previousFunctions.filter { argumentCandidateString.hasFunctionExpressionFor(it) }
          val argumentCandidate = Argument(argumentCandidateString, *(dependencyArgs + dependencyFunctions).toTypedArray())

          requireSyntaxCorrectness(argumentCandidate, line)
          previousArguments += argumentCandidate
          if (debugMode) {
            argumentCandidate.debugInfo()
          }
        }
        /**
         * Seems that function definition cannot reference previously defined arguments
         * Consider the following code
         *   val x = Argument("x = 3")
         *   val f = Function("f(a, b) = 5 * a + b")
         *   val g = Function("g(a, b) = f(a, b) * a + b + x", f, x)
         *
         *   g.checkSyntax() -> 'true' but
         *   g.calculate(1.0, 2.0) -> 'NaN'
         */
        line.startsWith(FUN_PREFIX) -> {
          val functionCandidateString = line.substring(FUN_PREFIX.length)
          val dependencyFunctions = previousFunctions.filter { functionCandidateString.hasFunctionExpressionFor(it) }
          val functionCandidate = Function(functionCandidateString, *dependencyFunctions.toTypedArray())

          requireSyntaxCorrectness(functionCandidate, line)
          previousFunctions += functionCandidate
          if (debugMode) {
            functionCandidate.debugInfo()
          }
        }
        line.startsWith(RET_PREFIX) -> {
          if (idx != expressionLines.lastIndex) {
            throw IllegalStateException("Return expression must be the last statement")
          }

          val expressionLine = line.substring(RET_PREFIX.length).trim()

          return when {
            line.returnsComplex() -> {
              with(expressionLine) {
                val componentExpressions = substring(1, length - 1).split(",").also { check(it.size == 2) }
                componentExpressions.first().toValidExpression().calculate() to
                componentExpressions.last().toValidExpression().calculate()
              }
            }
            else -> expressionLine.toValidExpression().calculate() to null
          }
        }
        else -> throw IllegalStateException("Unknown format in line \"$line\". Line should start with 'val', 'fun' or 'return'")
      }
    }
    // shouldn't be reached if expression format is correct
    throw IllegalStateException("Return expression not found")
  }

  private fun String.toValidExpression(debugMode: Boolean = false): Expression {
    val dependencyArgs = previousArguments.filter { this.hasArgumentExpressionFor(it) }
    val dependencyFunctions = previousFunctions.filter { this.hasFunctionExpressionFor(it) }
    val calcExpressionCandidate = Expression(this, *(dependencyArgs + dependencyFunctions).toTypedArray())

    calcExpressionCandidate.requireSyntaxCorrectness()
    if (debugMode) {
      calcExpressionCandidate.debugInfo()
    }
    return calcExpressionCandidate
  }

  private fun clearState() {
    previousArguments.run { if (isNotEmpty()) clear() }
    previousFunctions.run { if (isNotEmpty()) clear() }
  }

  private fun requireSyntaxCorrectness(argumentCandidate: Argument, line: String) {
    if (argumentCandidate.checkSyntax() != Argument.NO_SYNTAX_ERRORS) {
      throw IllegalStateException("Unknown argument format in line \"$line\". Error: ${argumentCandidate.errorMessage}")
    }
  }

  private fun requireSyntaxCorrectness(functionCandidate: Function, line: String) {
    if (functionCandidate.checkSyntax() != Function.NO_SYNTAX_ERRORS) {
      throw IllegalStateException("Unknown function format in line \"$line\". Error: ${functionCandidate.errorMessage}")
    }
  }

  private fun Expression.requireSyntaxCorrectness() {
    when {
      missingUserDefinedArguments.isNotEmpty() -> {
        throw IllegalStateException(
          "Missing user-defined arguments ${missingUserDefinedArguments.map { it }} in expression $this"
        )
      }
      missingUserDefinedFunctions.isNotEmpty() -> {
        throw IllegalStateException(
          "Missing user-defined functions ${missingUserDefinedFunctions.map { it }} in expression $this"
        )
      }
      checkSyntax() != Function.NO_SYNTAX_ERRORS || checkLexSyntax() != Function.NO_SYNTAX_ERRORS -> {
        throw IllegalStateException("Unknown expression format $expressionString. Error: $errorMessage")
      }
    }
  }

  private fun String.hasArgumentExpressionFor(argument: Argument) = Regex("(.*)(\\W)*\\b${argument.argumentName}\\b(\\W)*(.*)").matches(this)

  /**
   * contains sub-expression of f function call kind of ...f(...
   */
  private fun String.hasFunctionExpressionFor(function: Function) = Regex(".*(\\W)*\\b${function.functionName}\\b(\\s*\\().*").matches(this)

  private fun String.returnsComplex() = Regex("^return\\s*\\(.*,.*\\)").matches(this.trim())

  private fun Argument.debugInfo() = StringBuilder().apply {
    append("argument: $argumentName")
    append(", ")
    if (argumentExpressionString.isBlank()) {
      append("value: $argumentValue")
    } else {
      append("expression: $argumentExpressionString")
    }
    append(", ")
    append("correctness check: ${checkSyntax()}")
    appendln()
  }.toString().also { print(it) }

  private fun Function.debugInfo() = StringBuilder().apply {
    append("function: $functionName")
    append(", ")
    append("expression: $functionExpressionString")
    append(", ")
    append("correctness check: ${checkSyntax()}")
    appendln()
  }.toString().also { print(it) }

  private fun Expression.debugInfo() = StringBuilder().apply {
    missingUserDefinedArguments
    append("expression: $expressionString")
    append(", ")
    append("correctness check: ${checkSyntax()}  ${checkLexSyntax()}")
    appendln()
    append("result: ${calculate()}")
    appendln()
  }.toString().also { print(it) }
}
