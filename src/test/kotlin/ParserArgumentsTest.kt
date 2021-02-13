import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Test
import org.mariuszgromada.math.mxparser.*
import org.mariuszgromada.math.mxparser.Function

internal class ParserArgumentsTest {
  private val parser = Parser()

  @Test
  fun `single argument only`() {
    val expression = """
      val a = 1
      return a
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    assertThat(result, equalTo(1.0))
  }

  @Test
  fun `single argument only (against mxParser API)`() {
    val expression = """
      val a = 1
      return a
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val a = Argument("a = 1")
    val expr = Expression("a", a)
    val expected = expr.calculate()

    assertThat(result, equalTo(expected))
  }

  @Test
  fun `single dependent argument`() {
    val expression = """
      val a = 5
      val b = a^2
      return b
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    assertThat(result, equalTo(25.0))
  }

  @Test
  fun `single dependent only (against mxParser API)`() {
    val expression = """
      val a = 5
      val b = a^2
      return b
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val a = Argument("a = 5")
    val b = Argument("b = a^2", a)
    val expr = Expression("b", b)
    val expected = expr.calculate()

    assertThat(result, equalTo(expected))
  }

  @Test
  fun `dependent arguments chain`() {
    val expression = """
      val a = 1
      val b = a + 1
      val c = a^3 + b^2
      val d = a^4 + b^3 + c^2
      return d
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    assertThat(result, equalTo(34.0))
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
    val result = parser.parse(expression, x = 0.0).first

    val a = Argument("a = 1")
    val b = Argument("b = a + 1", a)
    val c = Argument("c = a^3 + b^2", a, b)
    val d = Argument("d = a^4 + b^3 + c^2", a, b, c)
    val expr = Expression("d", d)
    val expected = expr.calculate()

    assertThat(result, equalTo(expected))
  }

  @Test
  fun `single dependent argument in arithmetic expression`() {
    val expression = """
      val a = 5
      val b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2
      return b
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    assertThat(result, equalTo(-10.16))
  }

  @Test
  fun `single dependent argument in arithmetic expression (against mxParser API)`() {
    val expression = """
      val a = 5
      val b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2
      return b
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val a = Argument("a = 5")
    val b = Argument("b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2", a)
    val expr = Expression("b", b)
    val expected = expr.calculate()

    assertThat(result, equalTo(expected))
  }

  @Test
  fun `two dependent arguments in arithmetic expression`() {
    val expression = """
      val a = 5
      val b = 6 * a
      val c = b + 15 - 6 * 9  / a^2 - 4 * b * 2
      return c
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    assertThat(result, equalTo(-197.16))
  }

  @Test
  fun `two dependent arguments in arithmetic expression (against mxParser API)`() {
    val expression = """
      val a = 5
      val b = 6 * a
      val c = b + 15 - 6 * 9  / a^2 - 4 * b * 2
      return c
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val a = Argument("a = 5")
    val b = Argument("b = 6 * a", a)
    val c = Argument("c = b + 15 - 6 * 9  / a^2 - 4 * b * 2", a, b)
    val expr = Expression("c", c)
    val expected = expr.calculate()

    assertThat(result, equalTo(expected))
  }
}