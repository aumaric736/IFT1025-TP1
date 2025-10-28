/* ============================================================================
 * Path: src/student/model/organisms/Herbivore.java
 * Author: Zakary Gaillard-D.
 * Date: 2025-10-06
 * Description: Herbivore animal seeking high-energy plants and fleeing nearby carnivores.
 * ========================================================================== */
package student.model.organisms;

import student.model.behaviors.Edible;
import student.model.behaviors.Fleeing;
import student.model.core.Cell;
import student.model.core.Position;
import student.model.core.World;
import java.util.ArrayList;
import java.util.List;

/**
 * Herbivore organism that:
 * <ul>
 *   <li>Flees carnivores when detected</li>
 *   <li>Prefers moving toward the highest-energy plant in range</li>
 *   <li>Reproduces when energy threshold is met</li>
 * </ul>
 */
public class Herbivore extends Animal implements Fleeing, Edible {
//=============================================================================
//                                   Constants
//=============================================================================
private static final int MAX_ENERGY = 10;
private static final int REPRODUCTION_THRESHOLD = 7;
private static final int VISION_RANGE = 2; // 3x3
private static final int MOVEMENT_RANGE = 2; // 3x3

//=============================================================================
//                               Construction
//=============================================================================

/**
 * Construct a herbivore with base energy 3.
 */
public Herbivore() {
	super(3);
}

/**
 * Construct a herbivore with clamped initial energy.
 *
 * @param energy requested starting energy
 */
public Herbivore(int energy) {
	super(Math.min(Math.max(1, energy), MAX_ENERGY));
}

//=============================================================================
//                                Perception
//=============================================================================

/**
 * Return herbivore vision range.
 *
 * @return vision range radius
 */
@Override
public int visionRange() {
	return VISION_RANGE;
}

//=============================================================================
//                                   Fleeing
//=============================================================================
    /**
     * Choose a position maximizing distance from a nearby carnivore if one is perceived.
     *
     * @param world world context
     * @return flee destination or {@code null} if no threat detected
     */
    @Override
    public Position chooseFlee(World world) {
        Position currentPos = getPosition();
        if (currentPos == null) return null;

        // Look for carnivores in view
        List<Position> visiblePositions = perceive(world, currentPos);
        List<Position> carnivorePositions = new ArrayList<>();

        for (Position pos : visiblePositions) {
            Cell cell = world.getCell(pos);
            if (cell != null && cell.hasAnimal() && cell.getAnimal() instanceof Carnivore) {
                carnivorePositions.add(pos);
            }
        }

        if (carnivorePositions.isEmpty()) {
            return null;
        }

        // check all 8 directions (including diagonals)
        List<Position> escapePositions = new ArrayList<>();
        int x = currentPos.x();
        int y = currentPos.y();
        int width = world.getWidth();
        int height = world.getHeight();

        // 8个方向：上、下、左、右 + 4个对角线
        int[][] directions = {
                {0, -1},  // up
                {0, 1},   // down
                {-1, 0},  // left
                {1, 0},   // right
                {-1, -1}, // up left
                {1, -1},  // up right
                {-1, 1},  // down left
                {1, 1}    // down right
        };

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                Position escapePos = new Position(newX, newY);
                Cell cell = world.getCell(escapePos);
                if (cell != null && cell.isEmptyAnimal()) {
                    escapePositions.add(escapePos);
                }
            }
        }

        if (escapePositions.isEmpty()) {
            return null;
        }

        // Selection to the position with the greatest distance to the nearest predator
        Position safestPosition = null;
        int maxMinDistance = -1;

        for (Position escapePos : escapePositions) {
            int minDistance = Integer.MAX_VALUE;

            for (Position carnivorePos : carnivorePositions) {
                int distance = Math.abs(escapePos.x() - carnivorePos.x()) +
                        Math.abs(escapePos.y() - carnivorePos.y());
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }

            if (minDistance > maxMinDistance) {
                maxMinDistance = minDistance;
                safestPosition = escapePos;
            }
        }
        return safestPosition;
    }

//=============================================================================
//                                  Movement
//=============================================================================

    /**
     * Choose movement: first flee from carnivores, then seek plants, else random move.
     *
     * @param world world context
     * @param pos   current position
     * @return destination cell or {@code null}
     */
    @Override
    public Cell chooseMove(World world, Position pos) {
        // First check if the current position is valid
        if (pos == null || !world.isValidPosition(pos)) {
            return null;
        }

        // check if you need to escape
        Position fleePosition = chooseFlee(world);
        if (fleePosition != null && world.isValidPosition(fleePosition)) {
            Cell fleeCell = world.getCell(fleePosition);
            if (fleeCell != null && fleeCell.isEmptyAnimal()) {
                return fleeCell;
            }
        }

        // If there is no escape, look for the plant with the highest energy
        Plant bestPlant = findBestPlantInSight(world, pos);
        if (bestPlant != null) {
            Cell moveTowardPlant = moveTowardPlant(world, pos, bestPlant.getPosition());
            if (moveTowardPlant != null) {
                return moveTowardPlant;
            }
        }

        // 3. If there are no plants to pursue, move randomly
        return getRandomMoveCell(world, pos);
    }

    /**
     * Find the most energetic plant in view
     */
    private Plant findBestPlantInSight(World world, Position currentPos) {
        List<Position> visiblePositions = perceive(world, currentPos);
        Plant bestPlant = null;
        int bestEnergy = -1;

        for (Position visiblePos : visiblePositions) {
            if (world.isValidPosition(visiblePos)) {
                Cell cell = world.getCell(visiblePos);
                if (cell != null && cell.hasPlant()) {
                    Plant plant = cell.getPlant();
                    if (plant.isAlive() && plant.getEnergy() > bestEnergy) {
                        bestEnergy = plant.getEnergy();
                        bestPlant = plant;
                    }
                }
            }
        }
        return bestPlant;
    }

    /**
     * Move towards the plant
     */
    private Cell moveTowardPlant(World world, Position currentPos, Position plantPos) {
        // Get all valid movement directions
        List<Cell> possibleMoves = getValidMoveCells(world, currentPos);

        if (possibleMoves.isEmpty()) {
            return null;
        }

        // Find the direction closest to the plant
        Cell bestDirection = null;
        int minDistance = Integer.MAX_VALUE;

        for (Cell moveCell : possibleMoves) {
            int distance = moveCell.getPosition().distanceTo(plantPos);
            if (distance < minDistance) {
                minDistance = distance;
                bestDirection = moveCell;
            }
        }

        return bestDirection;
    }

    /**
     * Get all valid moving cells
     */
    private Cell getRandomMoveCell(World world, Position pos) {
        List<Cell> freeCells = getValidMoveCells(world, pos);

        if (freeCells.isEmpty()) {
            return null;
        }

        int randomIndex = prof.utils.RandomGenerator.nextInt(freeCells.size());
        return freeCells.get(randomIndex);
    }

    /**
     * Get all valid directions of movement (full border security)
     */
    private List<Cell> getValidMoveCells(World world, Position pos) {
        List<Cell> freeCells = new ArrayList<>();
        int x = pos.x();
        int y = pos.y();
        int width = world.getWidth();
        int height = world.getHeight();

        if (y > 0) {
            Position upPos = new Position(x, y - 1);
            Cell cell = world.getCell(upPos);
            if (cell != null && cell.isEmptyAnimal()) {
                freeCells.add(cell);
            }
        }

        if (y < height - 1) {
            Position downPos = new Position(x, y + 1);
            Cell cell = world.getCell(downPos);
            if (cell != null && cell.isEmptyAnimal()) {
                freeCells.add(cell);
            }
        }

        if (x > 0) {
            Position leftPos = new Position(x - 1, y);
            Cell cell = world.getCell(leftPos);
            if (cell != null && cell.isEmptyAnimal()) {
                freeCells.add(cell);
            }
        }

        if (x < width - 1) {
            Position rightPos = new Position(x + 1, y);
            Cell cell = world.getCell(rightPos);
            if (cell != null && cell.isEmptyAnimal()) {
                freeCells.add(cell);
            }
        }

        return freeCells;
    }
//=============================================================================
//                                   Feeding
//=============================================================================

/**
 * Return whether the herbivore can eat the plant in the cell.
 *
 * @param cell target cell
 * @return {@code true} if a plant is present
 */
@Override
public boolean canEat(Cell cell) {
    return cell != null && cell.hasPlant();
}

/**
 * Consume a plant if present.
 *
 * @param cell  target cell
 * @param world world context
 */
@Override
public void eat(Cell cell, World world) {
    if (canEat(cell)) {
        Plant plant = cell.getPlant();
        if (plant != null && plant.isAlive()) {
            // get the plant energy
            int nutrition = plant.nutrition();
            this.addEnergy(nutrition);
            // move plant when be eaten
            plant.subEnergy(plant.getEnergy());
            cell.removePlant();
        }
    }
}

//=============================================================================
//                                   Edible
//=============================================================================

/**
 * Return nutrition value (current energy).
 *
 * @return nutrition points
 */
@Override
public int nutrition() {
	return energy;
}

//=============================================================================
//                               Reproduction
//=============================================================================

/**
 * Return whether reproduction threshold is reached.
 *
 * @param world world context
 * @return {@code true} if energy permits reproduction
 */
@Override
public boolean canReproduce(World world) {
    return getEnergy() >= REPRODUCTION_THRESHOLD;
}

/**
 * Create a child herbivore with base energy.
 *
 * @return new herbivore
 */
@Override
public Organism reproduce() {
	return new Herbivore(3);
}

//=============================================================================
//                            Energy Management
//=============================================================================

/**
 * Add energy clamped to {@code MAX_ENERGY}.
 *
 * @param amount increment amount
 */
@Override
public void addEnergy(int amount) {
	setEnergy(Math.min(MAX_ENERGY, getEnergy() + amount));
}
}
