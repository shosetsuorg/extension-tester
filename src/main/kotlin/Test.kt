/*
 * This file is part of shosetsu-services.
 * shosetsu-services is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * shosetsu-services is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with shosetsu-services.  If not, see https://www.gnu.org/licenses/.
 */

import Config.DIRECTORY
import Config.PRINT_LISTINGS
import Config.PRINT_LIST_STATS
import Config.PRINT_METADATA
import Config.PRINT_NOVELS
import Config.PRINT_NOVEL_STATS
import Config.PRINT_PASSAGES
import Config.PRINT_REPO_INDEX
import Config.REPEAT
import Config.SEARCH_VALUE
import Config.SOURCES
import Config.SPECIFIC_CHAPTER
import Config.SPECIFIC_NOVEL
import Config.SPECIFIC_NOVEL_URL
import app.shosetsu.lib.*
import app.shosetsu.lib.ExtensionType.LuaScript
import app.shosetsu.lib.ShosetsuSharedLib.httpClient
import app.shosetsu.lib.json.RepoIndex
import app.shosetsu.lib.lua.LuaExtension
import app.shosetsu.lib.lua.ShosetsuLuaLib
import app.shosetsu.lib.lua.shosetsuGlobals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import org.luaj.vm2.LuaValue
import java.io.File
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/*
 * shosetsu-services
 * 03 / June / 2019
 *
 * @author github.com/doomsdayrs; github.com/TechnoJo4
 */

private val json = Json { prettyPrint = true }

private val globals = shosetsuGlobals()

private fun loadScript(file: File, source_pre: String = "ext"): LuaValue {
	val l = try {
		globals.load(file.readText(), "$source_pre(${file.name})")!!
	} catch (e: Error) {
		throw e
	}
	return l.call()!!
}

@ExperimentalTime
private fun showNovel(ext: IExtension, novelURL: String) {
	val novel = outputTimedValue("ext.parseNovel") {
		ext.parseNovel(novelURL, true)
	}

	while (novel.chapters.isEmpty()) {
		println("$CRED Chapters are empty $CRESET")
		return
	}

	if (PRINT_NOVELS)
		println(novel)
	if (PRINT_NOVEL_STATS)
		println("${novel.title} - ${novel.chapters.size} chapters.")
	println()

	val passage = outputTimedValue("ext.getPassage") {
		ext.getPassage(novel.chapters[SPECIFIC_CHAPTER].link)
	}

	if (PRINT_PASSAGES)
		println("Passage:\t${passage.decodeToString()}")
	else
		println(with(passage.decodeToString()) {
			if (length < 25) "Result: $this"
			else "$length chars long result: " +
					"${take(10)} [...] ${takeLast(10)}"
		})
}

@ExperimentalTime
@Suppress("ConstantConditionIf")
private fun showListing(ext: IExtension, novels: Array<Novel.Listing>) {
	if (PRINT_LISTINGS) {
		println("$CPURPLE[")
		print(novels.joinToString(", ") { it.toString() })
		println("]$CRESET")
	}

	println("${novels.size} novels.")
	if (PRINT_LIST_STATS) {
		print("${novels.count { it.title.isEmpty() }} with no title, ")
		print("${novels.count { it.link.isEmpty() }} with no link, ")
		print("${novels.count { it.imageURL.isEmpty() }} with no image url.")
		println()
	}

	println()

	var selectedNovel = 0
	println(novels[selectedNovel].link)
	var novel = outputTimedValue("ext.parseNovel") {
		ext.parseNovel(novels[selectedNovel].link, true)
	}

	while (novel.chapters.isEmpty()) {
		println("$CRED Chapters are empty, trying next novel $CRESET")
		selectedNovel++
		novel = outputTimedValue("ext.parseNovel") {
			ext.parseNovel(novels[selectedNovel].link, true)
		}
	}

	if (PRINT_NOVELS)
		println(novel)

	if (PRINT_NOVEL_STATS)
		println("${novel.title} - ${novel.chapters.size} chapters.")

	println()


	val passage = outputTimedValue("ext.getPassage") {
		ext.getPassage(novel.chapters[0].link)
	}


	if (PRINT_PASSAGES)
		println("Passage:\t${passage.decodeToString()}")
	else
		println(with(passage.decodeToString()) {
			if (length < 25) "Result: $this"
			else "$length chars long result: " +
					"${take(10)} [...] ${takeLast(10)}"
		})
}

@Suppress("UNCHECKED_CAST")
fun Array<Filter<*>>.printOut(indent: Int = 0) {
	forEach { filter ->
		val id = filter.id
		val fName = filter.name

		val tabs = StringBuilder("\t").apply {
			for (i in 0 until indent)
				this.append("\t")
		}
		val name = filter.javaClass.simpleName.let {
			if (it.length > 7)
				it.substring(0, 6)
			else it
		}
		val fullName = filter.state?.javaClass?.simpleName

		println("$tabs>${name}\t[$id]\t${fName}\t={$fullName}")
		when (filter) {
			is Filter.List -> {
				filter.filters.printOut(indent + 1)
			}

			is Filter.Group<*> -> {
				(filter.filters as Array<Filter<*>>)
					.printOut(indent + 1)
			}

			else -> {
			}
		}
	}
}

@ExperimentalTime
private inline fun <T> outputTimedValue(jobName: String, block: () -> T): T {
	return measureTimedValue(block).also {
		printExecutionTime(jobName, it.duration)
	}.value
}

@ExperimentalTime
private fun printExecutionTime(job: String, time: Duration) {
	printExecutionTime(job, time.toDouble(DurationUnit.MILLISECONDS))
}

private fun printExecutionTime(job: String, timeMs: Double) {
	println("$CGREEN COMPLETED [$job] in $timeMs ms $CRED")
}

fun printErrorln(message: String) {
	println("$CRED$message$CRESET")
}

/**
 * Establish
 */
@ExperimentalTime
fun setupLibs() {
	ShosetsuLuaLib.libLoader = {
		outputTimedValue("loadScript") {
			loadScript(
				File("$DIRECTORY/lib/$it.lua"),
				"lib"
			)
		}
	}
	httpClient = OkHttpClient.Builder().addInterceptor {
		outputTimedValue("Time till response") {
			it.proceed(it.request().also { request ->
				println(request.url.toUrl().toString())
			})
		}
	}.build()
}

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalTime
fun main(args: Array<String>) {

	parseConfig(args)

	setupLibs()

	outputTimedValue("MAIN") {
		try {
			if (PRINT_REPO_INDEX)
				println(outputTimedValue("RepoIndexLoad") {
					RepoIndex.repositoryJsonParser.decodeFromStream<RepoIndex>(File("$DIRECTORY/index.json").inputStream())
						.prettyPrint()
				})

			run {
				for (extensionPath in SOURCES) {
					println("\n\n========== $extensionPath ==========")


					val extension = outputTimedValue("LuaExtension") {
						val file = File(extensionPath.first)
						when (extensionPath.second) {
							LuaScript -> LuaExtension(file)
						}
					}

					if (SPECIFIC_NOVEL) {
						showNovel(extension, SPECIFIC_NOVEL_URL)
						return@run
					}


					val settingsModel: Map<Int, *> =
						extension.settingsModel.also {
							println("Settings model:")
							it.printOut()
						}.mapify()
					val searchFiltersModel: Map<Int, *> =
						extension.searchFiltersModel.also {
							println("SearchFilters Model:")
							it.printOut()
						}.mapify()

					println(CCYAN)
					println("ID       : ${extension.formatterID}")
					println("Name     : ${extension.name}")
					println("BaseURL  : ${extension.baseURL}")
					println("Image    : ${extension.imageURL}")
					println("Settings : $settingsModel")
					println("Filters  : $searchFiltersModel")
					if (PRINT_METADATA)
						println(
							"MetaData : ${
								Json {
									prettyPrint = true
								}.encodeToString(extension.exMetaData)
							}"
						)
					println(CRESET)

					extension.listings.forEach { l ->
						with(l) {
							print("\n-------- Listing \"${name}\" ")
							print(if (isIncrementing) "(incrementing)" else "")
							println(" --------")

							var novels = getListing(
								HashMap(searchFiltersModel).apply {
									this[PAGE_INDEX] =
										if (isIncrementing) extension.startIndex else null

								}
							)

							if (isIncrementing)
								novels += getListing(HashMap(searchFiltersModel)
									.apply {
										this[PAGE_INDEX] = extension.startIndex + 1
									})

							if (REPEAT) {
								novels = getListing(
									HashMap(searchFiltersModel).apply {
										this[PAGE_INDEX] =
											if (isIncrementing) extension.startIndex else null

									}
								)

								if (isIncrementing)
									novels += getListing(HashMap(searchFiltersModel)
										.apply {
											this[PAGE_INDEX] = extension.startIndex + 1
										})
							}


							showListing(extension, novels)
							try {
								MILLISECONDS.sleep(500)
							} catch (e: InterruptedException) {
								e.printStackTrace()
							}
						}
					}

					if (extension.hasSearch) {
						println("\n-------- Search --------")
						showListing(
							extension,
							outputTimedValue("ext.search") {
								extension.search(
									HashMap(searchFiltersModel).apply {
										set(QUERY_INDEX, SEARCH_VALUE)
										set(PAGE_INDEX, extension.startIndex)
									}
								)
							}
						)
						if (extension.isSearchIncrementing) {
							showListing(
								extension,
								outputTimedValue("ext.search") {
									extension.search(
										HashMap(searchFiltersModel).apply {
											set(QUERY_INDEX, SEARCH_VALUE)
											set(PAGE_INDEX, extension.startIndex + 1)
										}
									)
								}
							)
						}
					}

					MILLISECONDS.sleep(500)
				}
			}


			println("\n\tTESTS COMPLETE")
			exitProcess(0)
		} catch (e: Exception) {
			e.printStackTrace()
			e.message?.let {
				print(CRED)
				print(it.substring(it.lastIndexOf("}") + 1))
				println(CRESET)
			}
			exitProcess(1)
		}
	}
}