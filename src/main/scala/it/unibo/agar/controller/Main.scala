package it.unibo.agar.controller

import it.unibo.agar.model.{AIMovement, GameInitializer, MockGameStateManager, Player, World}
import it.unibo.agar.view.GlobalView
import it.unibo.agar.view.LocalView

import java.awt.Window
import java.util.Timer
import java.util.TimerTask
import scala.swing.*
import scala.swing.Swing.onEDT
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.util.Random

object Main extends SimpleSwingApplication:

  private val width = 1000
  private val height = 1000
  private val numPlayers = 4
  private val numFoods = 100
  private var players = GameInitializer.initialPlayers(numPlayers, width, height)
  private val foods = GameInitializer.initialFoods(numFoods, width, height)
  private val manager = new MockGameStateManager(World(width, height, players, foods))

  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    players = players :+ Player(s"player1", Random.nextInt(width), Random.nextInt(height), 120.0)

    val gameManager = context.spawn(GameManagerActor(players, foods), "gameManager")
    val foodGenerator = context.spawn(FoodGeneratorActor(), "foodGenerator")

    for (i <- 1 to numPlayers) {
      context.spawn(AIplayerActor(s"ai$i"), s"aiPlayer$i")
    }

    val player1 = context.spawn(PlayerActor("player1"), "player1")

    val globalView = new GlobalView
    context.spawn(GlobalViewActor(globalView), "globalViewActor")

    onEDT {
      globalView.open()
    }

    Behaviors.empty
  }

  override def top: Frame = new Frame {
    visible = false
  }

  override def main(args: Array[String]): Unit =
    ActorSystem(Main(), "AgarSystem")
    super.main(args)

//  private val timer = new Timer()
//  private val task: TimerTask = new TimerTask:
//    override def run(): Unit =
//      AIMovement.moveAI("p1", manager)
//      manager.tick()
//      onEDT(Window.getWindows.foreach(_.repaint()))
//  timer.scheduleAtFixedRate(task, 0, 30)

//  override def top: Frame =
//    // Open both views at startup
//    new GlobalView(manager).open()
//    new LocalView(manager, "p1").open()
//    new LocalView(manager, "p2").open()
//    // No launcher window, just return an empty frame (or null if allowed)
//    new Frame { visible = false }
