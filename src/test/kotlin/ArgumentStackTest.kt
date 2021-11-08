import org.junit.Ignore
import org.junit.Test
import java.util.*

/**
 * extension-tester
 * 06 / 11 / 2021
 */
@Ignore
class ArgumentStackTest {

	fun takeArgs(args: Array<String>) {
		val stack = Stack<String>()
		args.reversed().forEach(stack::add)
		println(stack.pop())
	}

	@Test
	fun test() {
		takeArgs(arrayOf("-r", "/path/to/repo", "-e", "/path/to/ext"))
	}
}