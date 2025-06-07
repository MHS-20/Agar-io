package it.unibo.agar.view

import it.unibo.agar.model.World
import java.awt.Graphics2D
import scala.swing.*

class GlobalView extends MainFrame:
  title = "Agar.io - Global View"
  preferredSize = new Dimension(800, 800)

  private var currentWorld: Option[World] = None

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      currentWorld.foreach(world => AgarViewUtils.drawWorld(g, world))

  def update(world: World): Unit =
    println("Updating Global View with new world state")
    currentWorld = Some(world)
    repaint()