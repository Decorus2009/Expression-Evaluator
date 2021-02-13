import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class SyntaxCheckParserTest {
  private val parser = Parser()

  @Test
  fun `empty expression`() {
    val expression = """
    """.trimIndent()

    val exception: Exception = assertThrows<IllegalStateException> {
      parser.parse(expression, x = 0.0)
    }
    assertThat(exception.message, equalTo("Empty expression"))
  }

  @Test
  fun `expression with comments only`() {
    val expression = """
      // some comment 1
      // some comment 2
    """.trimIndent()

    val exception: Exception = assertThrows<IllegalStateException> {
      parser.parse(expression, x = 0.0)
    }
    assertThat(exception.message, equalTo("Empty expression"))
  }

  @Test
  fun `argument should be prefixed with val`() {
    val expression = """
      a = 1
    """.trimIndent()

    val exception: Exception = assertThrows<IllegalStateException> {
      parser.parse(expression, x = 0.0)
    }
    assertThat(exception.message, equalTo("Unknown format in line \"a = 1\". Line should start with 'val', 'fun' or 'return'"))
  }

  @Test
  fun `function should be prefixed with fun`() {
    val expression = """
      f(a) = a + 1
    """.trimIndent()

    val exception: Exception = assertThrows<IllegalStateException> {
      parser.parse(expression, x = 0.0)
    }
    assertThat(exception.message, equalTo("Unknown format in line \"f(a) = a + 1\". Line should start with 'val', 'fun' or 'return'"))
  }

  @Test
  fun `return expression must be the last statement`() {
    val expression = """
      fun f(a) = a + 1
      return f(10)
      val b = 1
    """.trimIndent()

    val exception: Exception = assertThrows<IllegalStateException> {
      parser.parse(expression, x = 0.0)
    }
    assertThat(exception.message, equalTo("Return expression must be the last statement"))
  }

  @Test
  fun `return expression not found`() {
    val expression = """
      fun f(a) = a + 1
    """.trimIndent()

    val exception: Exception = assertThrows<IllegalStateException> {
      parser.parse(expression, x = 0.0)
    }
    assertThat(exception.message, equalTo("Return expression not found"))
  }

  @Test
  fun `argument with incorrect syntax`() {
    val expression = """
      val a = 10 - 2 +
    """.trimIndent()

    val exception: Exception = assertThrows<IllegalStateException> {
      parser.parse(expression, x = 0.0)
    }
    assertThat(exception.message, equalTo("Unknown argument format in line \"val a = 10 - 2 +\""))
  }

  @Test
  fun `function with incorrect syntax`() {
    val expression = """
      fun f(a) = 10 - 2 +
    """.trimIndent()

    val exception: Exception = assertThrows<IllegalStateException> {
      parser.parse(expression, x = 0.0)
    }
    assertThat(exception.message, equalTo("Unknown function format in line \"fun f(a) = 10 - 2 +\""))
  }
}