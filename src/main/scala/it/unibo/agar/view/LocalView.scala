package it.unibo.agar.view

import it.unibo.agar.model.MockGameStateManager
import it.unibo.agar.model.World

import java.awt.Graphics2D
import scala.swing.*

class LocalView(playerId: String) extends MainFrame:

  title = s"Agar.io - Local View ($playerId)"
  preferredSize = new Dimension(400, 400)
  private var currentWorld: Option[World] = None
  var onMouseMoved: java.awt.Point => Unit = _

  contents = new Panel:
    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      currentWorld match
        case Some(world) =>
          val playerOpt = world.players.find(_.id == playerId)
          val (offsetX, offsetY) = playerOpt
            .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
            .getOrElse((0.0, 0.0))
          AgarViewUtils.drawWorld(g, world, offsetX, offsetY)
        case None =>
          g.drawString("Loading...", size.width / 2, size.height / 2)

    reactions += { case e: event.MouseMoved =>
      //      val mousePos = e.point
      //      val playerOpt = manager.getWorld.players.find(_.id == playerId)
      //      playerOpt.foreach: player =>
      //        val dx = (mousePos.x - size.width / 2) * 0.01
      //        val dy = (mousePos.y - size.height / 2) * 0.01
      //        manager.movePlayerDirection(playerId, dx, dy)
      onMouseMoved(e.point)
      repaint()
    }

  def updateWorld(world: World): Unit =
    currentWorld = Some(world)
    repaint()
    
  def showGameOver(winner: String): Unit =
    Dialog.showMessage(
      contents.head,
      s"Game Over! Winner: $winner",
      title = "Game Over",
      Dialog.Message.Info
    )
    close()
