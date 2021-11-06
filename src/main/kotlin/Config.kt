import app.shosetsu.lib.ExtensionType

/**
 * extension-tester
 * 06 / 11 / 2021
 */
object Config {
	var SEARCH_VALUE = "world"
	var PRINT_LISTINGS = false
	var PRINT_LIST_STATS = false
	var PRINT_NOVELS = false
	var PRINT_NOVEL_STATS = false
	var PRINT_PASSAGES = false
	var PRINT_REPO_INDEX = false
	var PRINT_METADATA = false
	var REPEAT = false


	/** Load only the [SPECIFIC_NOVEL_URL] to test */
	var SPECIFIC_NOVEL = false

	/** Novel to load via the extension, useful for novel cases */
	var SPECIFIC_NOVEL_URL = "/"
	var SPECIFIC_CHAPTER = 0

	/** Replace with the directory of the extensions you want to use*/
	var DIRECTORY = ""

	// Should be an array of the path of the script to the type of that script
	var SOURCES: Array<Pair<String, ExtensionType>> = arrayOf()
}