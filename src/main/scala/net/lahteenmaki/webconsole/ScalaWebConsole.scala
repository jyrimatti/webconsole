package net.lahteenmaki.webconsole

import scala.swing.Applet

import scala.swing.Reactor
import scala.swing._
import scala.swing.Button
import scala.swing.event.ButtonClicked
import scala.swing.BoxPanel
import scala.swing._
import scala.tools.nsc._
import scala.tools.nsc.settings._
import scala.tools.nsc.interpreter._
import java.io._
import scala.swing.event._
import java.util.concurrent._
import java.awt.event.{ KeyEvent, KeyAdapter }
import java.security._
import java.net.URL
import java.io.InputStream
import java.net.URLDecoder.decode

/**
 * The compiler seems to require the scala-lib from its own classpath, even though it's already
 * present in the applet classpath
 */
object ExternalFileProvider {
	def readToFile(is: InputStream) = {
		try {
			val file = File.createTempFile("scalalib", ".jar")
			val os = new FileOutputStream(file)
			val b: Array[Byte] = Array.make(8192, 0)
			var read = is.read(b, 0, 8192)
			while (read > -1) {
				os.write(b, 0, read)
				read = is.read(b, 0, 8192)
			}
			file.getAbsolutePath
		} finally {
			is.close
		}
	}
	def scalalib(codeBase: URL, scalaVersion: String) = {
		AccessController.doPrivileged(new PrivilegedAction[String]() {
			def run() = {
				val is = new URL(codeBase.toExternalForm + "static/scala-library-" + scalaVersion + ".jar").openStream
				readToFile(is)
			}
		});
	}
	def url(url: String) = readToFile(new URL(url).openStream)
}

class ScalaWebConsole extends Applet {
	def scalaVersion = getParameter("scalaVersion")

	object ui extends UI with Reactor {
		def init() = {
			val pane = new TextArea

			val queue = new PipedWriter

			var handledLength = pane.text.length

			object CustomSettings extends Settings {
				classpath append ExternalFileProvider.scalalib(getCodeBase, scalaVersion)
				val query = getDocumentBase.getQuery
				if (query != null) {
					decode(query, "UTF-8").split("\\s+").foreach(p => classpath.append(ExternalFileProvider.url(p)))
				}
				println(classpath)
			}

			object ResetCaretOnInputKeyListener extends KeyAdapter {
				override def keyPressed(e: KeyEvent) = if (e.getKeyCode != KeyEvent.VK_META && !e.isActionKey) {
					pane.caret.position = pane.text.length
				}
			}

			object IgnoreBackSpaceToHistoryKeyListener extends KeyAdapter {
				override def keyPressed(e: KeyEvent) = if (e.getKeyCode == KeyEvent.VK_BACK_SPACE && pane.text.length <= handledLength) {
					e.consume
				}
			}

			object SubmitContentKeyListener extends KeyAdapter {
				override def keyPressed(e: KeyEvent) = if (e.getKeyCode == KeyEvent.VK_ENTER) {
					queue write pane.text.substring(handledLength) + e.getKeyChar
					handledLength = pane.text.length
				}
			}

			contents = new ScrollPane { contents = pane }

			pane.peer.addKeyListener(ResetCaretOnInputKeyListener)
			pane.peer.addKeyListener(IgnoreBackSpaceToHistoryKeyListener)
			pane.peer.addKeyListener(SubmitContentKeyListener)

			val in = new BufferedReader(new PipedReader(queue))
			val out = new JPrintWriter(new StringWriter {
				override def flush = {
					pane.text += getBuffer.toString
					getBuffer.setLength(0)
					pane.caret.position = pane.text.length
					handledLength = pane.text.length
					super.flush
				}
			})

			Executors.newSingleThreadExecutor.submit(Executors.callable(new PrivilegedAction[Unit] {
				def run = {
					val loop = new ILoop(in, out)
					val ref = getDocumentBase.getRef
					if (ref != null) {
						queue write decode(ref, "UTF-8") + '\n'
					}
					loop.process(CustomSettings)
				}
			}))
		}
	}
}