package it.unibo.agar.view

import akka.actor.typed.ActorRef
import it.unibo.agar.controller.{GameManagerActor, GameManagerCommand}

import java.awt.Color
import java.awt.Graphics2D
import scala.swing.*

class GlobalView(manager: ActorRef[GameManagerCommand]) extends MainFrame:

  //  title = "Agar.io - Global View"
  //  preferredSize = new Dimension(800, 800)
  //
  //  contents = new Panel:
  //    override def paintComponent(g: Graphics2D): Unit =
  //      val world = manager.getWorld

  // AgarViewUtils.drawWorld(g, world)

  import akka.actor.typed.{ActorRef, Scheduler}
  import akka.actor.typed.scaladsl.AskPattern._
  import it.unibo.agar.controller.{GameManagerCommand, RequestWorld, WorldResponse}

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext
  import scala.util.{Failure, Success}
  import javax.swing.Timer

  class GlobalView(gameManager: ActorRef[GameManagerCommand]) extends MainFrame:

    title = "Agar.io - Global View"
    preferredSize = new Dimension(800, 800)

    // Stato locale per il mondo
    private var currentWorld: Option[it.unibo.agar.model.World] = None

    // Per usare il pattern ask, serve uno scheduler e ExecutionContext
    implicit val scheduler: Scheduler = ??? // lo devi passare o ottenere dal context
    implicit val ec: ExecutionContext = ExecutionContext.global

    // Timer Swing per aggiornare la UI e chiedere lo stato ogni 100ms
    private val updateTimer = new Timer(100, _ => requestWorldUpdate())

    contents = new Panel:
      override def paintComponent(g: Graphics2D): Unit =
        currentWorld match
          case Some(world) => AgarViewUtils.drawWorld(g, world)
          case None => () // niente da disegnare

    def requestWorldUpdate(): Unit =
      import akka.util.Timeout
      implicit val timeout: Timeout = Timeout(500.millis)

      val future = gameManager.ask[WorldResponse](replyTo => RequestWorld(replyTo))
      future.onComplete {
        case Success(WorldResponse(world)) =>
          currentWorld = Some(world)
          repaint()
        case Failure(ex) =>
          println(s"Errore nella richiesta world: $ex")
      }

    // Avvia il timer all'apertura della finestra
    listenTo(this)
    reactions += {
      case _: event.WindowOpened => updateTimer.start()
      case _: event.WindowClosing => updateTimer.stop()
    }

