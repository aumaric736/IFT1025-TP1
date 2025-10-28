/* ============================================================================
 * Path: src/student/model/organisms/Animal.java
 * Author: Zakary Gaillard-D.
 * Date: 2025-10-06
 * Description: Abstract animal providing perception, movement defaults, and reproduction scaffold.
 * ========================================================================== */
package student.model.organisms;

import student.model.behaviors.Eater;
import student.model.behaviors.Movable;
import student.model.behaviors.Perceptive;
import student.model.behaviors.Reproducible;
import student.model.core.Cell;
import student.model.core.Position;
import student.model.core.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for all animal organisms.
 * <p>Provides reusable default implementations for:
 * <ul>
 *   <li>Perception area calculation</li>
 *   <li>Random movement within vision</li>
 *   <li>Simple reproduction placement</li>
 * </ul>
 * Feeding and reproduction energy thresholds are specialized in subclasses.</p>
 */
public abstract class Animal extends Organism implements Perceptive, Movable, Eater, Reproducible {
//=============================================================================
//                               Construction
//=============================================================================

    /**
     * Construct an animal with initial energy.
     *
     * @param energy starting energy
     */
    public Animal(int energy) {
        super(energy);
    }

//=============================================================================
//                                Perception
//=============================================================================

    /**
     * Return vision range radius (override to extend).
     *
     * @return vision range (>=1)
     */
    @Override
    public int visionRange() {
        return 1; // Base vision (cross neighborhood).
    }

    /**
     * Compute positions within vision range excluding the current cell.
     *
     * @param world world context
     * @param pos   origin position
     * @return list of perceived positions (never {@code null})
     */
    @Override
    public List<Position> perceive(World world, Position pos) {
        List<Position> visiblePositions = new ArrayList<>();
        int range = visionRange();

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                // Ignorer la position actuelle (0,0)
                if (dx == 0 && dy == 0) continue;

                int newX = pos.x() + dx;
                int newY = pos.y() + dy;

                // check the border before cree the position
                if (newX >= 0 && newY >= 0 && newX < world.getWidth() && newY < world.getHeight()) {
                    Position newPos = new Position(newX, newY);
                    visiblePositions.add(newPos);
                }
            }
        }

        return visiblePositions;
    }

//=============================================================================
//                                Movement
//=============================================================================

    /**
     * Choose a random empty neighboring cell inside vision range.
     *
     * @param world world context
     * @param pos   current position
     * @return chosen destination cell or {@code null}
     */
    @Override
    public Cell chooseMove(World world, Position pos) {
        // get 4 ways cardinal
        Position[] neighbors = pos.getCardinalNeighbors();
        List<Cell> freeCells = new ArrayList<>();

        // Filtering out empty cells
        for (Position neighborPos : neighbors) {
            if (neighborPos.isValid(world.getWidth(), world.getHeight())) {
                Cell cell = world.getCell(neighborPos);
                if (cell != null && cell.isEmptyAnimal()) {  // 修复：用 isEmptyAnimal
                    freeCells.add(cell);
                }
            }
        }

        // If there is no grid, no move
        if (freeCells.isEmpty()) {
            return null;
        }

        int randomIndex = prof.utils.RandomGenerator.nextInt(freeCells.size());
        return freeCells.get(randomIndex);
    }
//=============================================================================
//                                 Feeding
//=============================================================================

    /**
     * Determine if this animal can consume the given cell contents.
     *
     * @param cell target cell
     * @return {@code true} if edible
     */
    @Override
    public abstract boolean canEat(Cell cell);

    /**
     * Consume target cell contents if allowed.
     *
     * @param cell  target cell
     * @param world world context
     */
    @Override
    public abstract void eat(Cell cell, World world);

//=============================================================================
//                               Reproduction
//=============================================================================

    /**
     * Return whether reproduction conditions are satisfied (default energy check).
     *
     * @param world world context
     * @return {@code true} if reproduction is allowed
     */
    @Override
    public boolean canReproduce(World world) {
        return getEnergy() >= 2;
    }

    /**
     * Create a new organism instance of this species.
     *
     * @return new organism
     */
    @Override
    public abstract Organism reproduce();

    /**
     * Attempt to spawn an offspring in a neighboring empty animal slot.
     * <p>Energy cost: half of current energy subtracted from parent.</p>
     *
     * @param world world context
     * @return {@code true} if offspring placed
     */
    @Override
    public boolean spawn(World world) {
        if (!canReproduce(world)) return false;

        // get Position
        Position currentPos = getPosition();
        if (currentPos == null) return false;

        Position[] neighbors = currentPos.getCardinalNeighbors();
        List<Cell> freeCells = new ArrayList<>();

        for (Position neighborPos : neighbors) {
            if (neighborPos.isValid(world.getWidth(), world.getHeight())) {
                Cell cell = world.getCell(neighborPos);
                if (cell != null && cell.isEmptyAnimal()) {
                    freeCells.add(cell);
                }
            }
        }

        if (freeCells.isEmpty()) return false;

        int randomIndex = prof.utils.RandomGenerator.nextInt(freeCells.size());
        Cell target = freeCells.get(randomIndex);

        Organism baby = reproduce();
        target.setAnimal((Animal) baby);
        baby.setPosition(target.getPosition());

        int beforeEnergy = getEnergy();

        int newEnergy = getEnergy() / 2;
        if (getEnergy() % 2 == 1) {
            newEnergy++;
        }
        setEnergy(newEnergy);
        int afterEnergy = getEnergy();
        return true;
    }
}

