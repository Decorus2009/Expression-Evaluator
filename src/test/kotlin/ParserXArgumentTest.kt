import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.junit.Test
import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression

internal class ParserXArgumentTest {
  private val parser = Parser()

  @Test
  fun `single dependent argument`() {
    val expression = """
      val a = 5 * x
      return a
    """.trimIndent()
    val result = parser.parse(expression, x = 10.0).first

    Assert.assertThat(result, equalTo(50.0))
  }

  @Test
  fun `single dependent only (against mxParser API)`() {
    val expression = """
      val a = 5 * x
      return a
    """.trimIndent()
    val result = parser.parse(expression, x = 10.0).first

    val x = Argument("x = 10")
    val a = Argument("a = 5 * x", x)
    val expr = Expression("a", a)
    val expected = expr.calculate()

    Assert.assertThat(result, equalTo(expected))
  }

  @Test
  fun `dependent arguments chain`() {
    val expression = """
      val a = 1
      val b = a + 1
      val c = a^3 + b^2
      val d = a^4 + b^3 + c^2 * x
      return d
    """.trimIndent()
    val result = parser.parse(expression, x = 10.0).first

    Assert.assertThat(result, equalTo(259.0))
  }

  @Test
  fun `dependent arguments chain (against mxParser API)`() {
    val expression = """
      val a = 1
      val b = a + 1
      val c = a^3 + b^2
      val d = a^4 + b^3 + c^2
      return d
    """.trimIndent()
    val result = parser.parse(expression, x = 10.0).first

    val x = Argument("x = 10")
    val a = Argument("a = 1")
    val b = Argument("b = a + 1", a)
    val c = Argument("c = a^3 + b^2", a, b)
    val d = Argument("d = a^4 + b^3 + c^2", a, b, c)
    val expr = Expression("d", d)
    val expected = expr.calculate()

    Assert.assertThat(result, equalTo(expected))
  }

  @Test
  fun `single dependent argument in arithmetic expression`() {
    val expression = """
      val a = 5
      val b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2 * x
      return b
    """.trimIndent()
    val result = parser.parse(expression, x = 10.0).first

    Assert.assertThat(result, equalTo(-226.16))
  }

  @Test
  fun `single dependent argument in arithmetic expression (against mxParser API)`() {
    val expression = """
      val a = 5
      val b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2 * x
      return b
    """.trimIndent()
    val result = parser.parse(expression, x = 10.0).first

    val x = Argument("x = 10")
    val a = Argument("a = 5")
    val b = Argument("b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2 * x", a, x)
    val expr = Expression("b", b)
    val expected = expr.calculate()

    Assert.assertThat(result, equalTo(expected))
  }
}