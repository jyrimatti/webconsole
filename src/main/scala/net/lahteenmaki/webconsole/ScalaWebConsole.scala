package net.lahteenmaki.webconsole

import scala.swing.Applet
import scala.swing.Reactor
import scala.swing.TextArea
import scala.swing.Button
import scala.swing.event.ButtonClicked
import scala.swing.BoxPanel
import scala.swing._
import scala.tools.nsc._
import scala.tools.nsc.settings._
import scala.tools.nsc.interpreter._
import java.io._
import scala.swing.event._
import java.util.concurrent.Semaphore
import java.awt.event.{ KeyEvent, KeyListener }

class ScalaWebConsole extends Applet {
	object ui extends UI with Reactor {
		def init() = {
			val pane = new TextArea

			var libPath = ""
			java.security.AccessController.doPrivileged(new java.security.PrivilegedAction[Any]() {
				def run() {

					val is = new java.net.URL(getCodeBase().toExternalForm() + "/scala-library-2.9.1.jar").openStream()
					val file = File.createTempFile("foo", ".jar")
					val os = new FileOutputStream(file)
					val b: Array[Byte] = Array.make(8192, 0)
					var read = is.read(b, 0, 8192)
					while (read > -1) {
						os.write(b, 0, read)
						read = is.read(b, 0, 8192)
					}
					is.close()
					libPath = "file:/" + file.getAbsolutePath()
					null
				}
			});

			pane.peer.addKeyListener(new KeyListener {
				def keyPressed(e: KeyEvent) = if (!e.isMetaDown && !e.isActionKey) pane.caret.position = pane.text.length
				def keyReleased(e: KeyEvent) {}
				def keyTyped(e: KeyEvent) {}
			})

			val lo = new Semaphore(0)
			var allowUpdate = true
			var length = pane.text.length
			val buf = new ByteArrayOutputStream
			pane.reactions += {
				case e: ValueChanged if allowUpdate && pane.text.length > length && pane.text.last == '\n' => {
					buf.write(pane.text.substring(length).getBytes); lo.release
				}
			}

			contents = new ScrollPane { contents = pane }
			val reader = new BufferedReader(new Reader {
				def close = Unit
				def read(b: Array[Char], start: Int, length: Int) = {
					while (buf.size == 0) lo.acquire; val data = new String(buf.toByteArray); val portion = data.zipWithIndex.take(length); (portion.foreach {
						case (v, i) => b(start + i) = v
					}); buf.reset(); buf.write(data.drop(portion.length).getBytes); portion.length
				}
			})
			val out = new JPrintWriter(new StringWriter() {
				override def flush = { validate(); allowUpdate = false; pane.text += getBuffer.toString; getBuffer.setLength(0); pane.caret.position = pane.text.length; length = pane.text.length; allowUpdate = true; super.flush; }
			})
			val loop = new ILoop(reader, out)

			val settings = new Settings
			settings.classpath.append(libPath)
			new Thread(new Runnable() {
				def run() {
					java.security.AccessController.doPrivileged(new java.security.PrivilegedAction[Any]() {
						def run() {
							loop.process(settings)
							null
						}
					});
				}
			}).start
		}
	}
}