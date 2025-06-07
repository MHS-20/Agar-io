package it.unibo.agar.model

/** Object responsible for AI movement logic, separate from the game state management */
object AIMovement:

  /** Finds the nearest food for a given player in the world
    * @param player
    *   the ID of the player for whom to find the nearest food
    * @param world
    *   the current game world containing players and food
    * @return
    */
  def nearestFood(player: String, world: World): Option[Food] =
    world.foods
      .sortBy(food => world.playerById(player).map(p => p.distanceTo(food)).getOrElse(Double.MaxValue))
      .headOption
  
  def computeDirectionTowardNearestFood(playerId: String, world: World): Option[(Double, Double)] =
    for {
      player <- world.playerById(playerId)
      food <- nearestFood(playerId, world)
      dx = food.x - player.x
      dy = food.y - player.y
      distance = math.hypot(dx, dy)
      if distance > 0
    } yield (dx / distance, dy / distance)
