import org.mariuszgromada.math.mxparser.*
import org.mariuszgromada.math.mxparser.parsertokens.Token


fun main() {
  val expr0 = """
  val x = 1
  val y = x^2
  fun f(r) = 5 * r + 1
  return f(y)
  """.trimIndent()

  val func = "f(a) = 5 * a + 1"
  val expressionString = "sin(x) + 5 * x + 1"
  val expr = Expression(expressionString)

  val tokens: List<Token> = expr.copyOfInitialTokens

  tokens.forEach { print(it.tokenStr + " " + it.looksLike) }
  println()
  tokens.forEach { token ->

//    if ("argument" == token.looksLike) {
//      println("Arg: ${token.tokenStr}")
//    }
  }

  listOf(1.0, 2.0, 3.0).forEach {
    if (expr.getArgumentIndex("x") == Argument.NOT_FOUND) {
      expr.defineArgument("x", it)
    }
    expr.setArgumentValue("x", it)
    println(expr.calculate())
  }


}

