import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test
import org.mariuszgromada.math.mxparser.*
import org.mariuszgromada.math.mxparser.Function

internal class ParserFunctionsTest {
  private val parser = Parser()

  @Test
  fun `single function returning const`() {
    val expression = """
      fun f(a) = 1
      return f(10)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    Assert.assertThat(result, CoreMatchers.equalTo(1.0))
  }

  @Test
  fun `single function returning const (against mxParser API)`() {
    val expression = """
      fun f(a) = 1
      return f(10)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val f = Function("f(x) = 1")
    val expr = Expression("f(10)", f)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `single linear function`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      return f(10, 5, 2)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    Assert.assertThat(result, CoreMatchers.equalTo(52.0))
  }

  @Test
  fun `single linear function (against mxParser API)`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      return f(10, 5, 2)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val f = Function("f(a, b, c) = a * b + c")
    val expr = Expression("f(10, 5, 2)", f)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `argument-dependent function`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      val a = 2
      val b = 5
      val c = b^2
      return f(a, b, c)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    Assert.assertThat(result, CoreMatchers.equalTo(35.0))
  }

  @Test
  fun `argument-dependent function (against mxParser API)`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      val a = 2
      val b = 5
      val c = b^2
      return f(a, b, c)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val a = Argument("a = 2")
    val b = Argument("b = 5")
    val c = Argument("c = b^2", b)
    val f = Function("f(a, b, c) = a * b + c")
    val expr = Expression("f(a, b, c)", f, a, b, c)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `function-dependence`() {
    val expression = """
      fun f(a) = a^2
      fun g(a) = a^2 + f(a)
      return g(10)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    Assert.assertThat(result, CoreMatchers.equalTo(200.0))
  }

  @Test
  fun `function-dependence (against mxParser API)`() {
    val expression = """
      fun f(a) = a^2
      fun g(a) = a^2 + f(a)
      return g(10)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val f = Function("f(a) = a^2")
    val g = Function("g(a) = a^2 + f(a)", f)
    val expr = Expression("g(10)", g)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `argument-dependent function-dependence`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      fun g(a, b, c) = f(a^2, b^2, c^2) * 10
      val a = 2
      val b = 5
      val c = b^2
      return g(a, b, c)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    Assert.assertThat(result, CoreMatchers.equalTo(7250.0))
  }

  @Test
  fun `argument-dependent function-dependence (against mxParser API)`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      fun g(a, b, c) = f(a^2, b^2, c^2) * 10
      val a = 2
      val b = 5
      val c = b^2
      return g(a, b, c)
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val a = Argument("a = 2")
    val b = Argument("b = 5")
    val c = Argument("c = b^2", b)
    val f = Function("f(a, b, c) = a * b + c")
    val g = Function("g(a, b, c) = f(a^2, b^2, c^2) * 10", f)
    val expr = Expression("g(a, b, c)", g, a, b, c)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `function composition`() {
    val expression = """
      fun f(a) = a * 10
      fun g(a) = a^2
      return g(f(5))
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    Assert.assertThat(result, CoreMatchers.equalTo(2500.0))
  }

  @Test
  fun `function composition (against mxParser API)`() {
    val expression = """
      fun f(a) = a * 10
      fun g(a) = a^2
      return g(f(5))
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val f = Function("f(a) = a * 10")
    val g = Function("g(a) = a^2", f)
    val expr = Expression("g(f(5))", g, f)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `argument-dependent function composition`() {
    val expression = """
      fun f(a) = a * 10
      fun g(a, b, c) = a * b + c
      val a = 2
      val b = 5
      val c = b^2
      return g(f(a), f(b), f(c))
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    Assert.assertThat(result, CoreMatchers.equalTo(1250.0))
  }

  @Test
  fun `argument-dependent function composition (against mxParser API)`() {
    val expression = """
      fun f(a) = a * 10
      fun g(a, b, c) = a * b + c
      val a = 2
      val b = 5
      val c = b^2
      return g(f(a), f(b), f(c))
    """.trimIndent()
    val result = parser.parse(expression, x = 0.0).first

    val a = Argument("a = 2")
    val b = Argument("b = 5")
    val c = Argument("c = b^2", b)
    val f = Function("f(a) = a * 10")
    val g = Function("g(a, b, c) = a * b + c")
    val expr = Expression("g(f(a), f(b), f(c))", g, f, a, b, c)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }
}