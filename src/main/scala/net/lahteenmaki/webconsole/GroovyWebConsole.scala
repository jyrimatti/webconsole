package net.lahteenmaki.webconsole

import scala.swing.Applet
import scala.swing.Applet

import scala.swing.Reactor
import scala.swing._
import scala.swing.Button
import scala.swing.event.ButtonClicked
import scala.swing.BoxPanel
import scala.swing._
import java.io._
import scala.swing.event._
import java.util.concurrent._
import java.awt.event.{ KeyEvent, KeyAdapter }
import java.security._
import java.net.URL
import java.io.InputStream
import java.net.URLDecoder.decode

import org.apache.commons.io.input.ReaderInputStream
import org.apache.commons.io.output.WriterOutputStream

import org.codehaus.groovy.tools.shell._

class GroovyWebConsole extends Applet {

	object ui extends UI with Reactor {
		def init() = {
			val pane = new TextArea

			val queue = new PipedWriter

			var handledLength = pane.text.length

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
			val out = new java.io.PrintWriter(new StringWriter {
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
					val shell = new Shell(new IO(new ReaderInputStream(in), new WriterOutputStream(out), System.err))
					val ref = getDocumentBase.getRef
					if (ref != null) {
						queue write decode(ref, "UTF-8") + '\n'
					}
					new InteractiveShellRunner(shell, new groovy.lang.Closure() {
						
					}).run()
				}
			}))
		}
	}

}