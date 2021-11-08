import org.junit.Test
import kotlin.time.ExperimentalTime

/**
 * extension-tester
 * 08 / 11 / 2021
 */
class ExtensionTesterTest {

	@Test
	@ExperimentalTime
	fun testProgram() {
		main(arrayOf(
			"-r",
			"../shosetsuorg.extensions/",
			"../shosetsuorg.extensions/src/en/FastNovel.lua",
			"--version"
		))
	}
}