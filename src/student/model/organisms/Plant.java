/* ============================================================================
 * Path: src/student/model/organisms/Plant.java
 * Author: Zakary Gaillard-D.
 * Date: 2025-10-06
 * Description: Concrete plant organism handling growth, nutrition value, and simple reproduction.
 * ========================================================================== */
package student.model.organisms;

import student.model.behaviors.Edible;
import student.model.behaviors.Growable;
import student.model.behaviors.Reproducible;
import student.model.core.World;
import student.model.core.Position;
import student.model.core.Cell;
import java.util.List;

/**
 * Represents a simple plant with bounded energy and reproduction on saturation.
 * <p>Rules:
 * <ul>
 *   <li>Energy range: 1..3</li>
 *   <li>{@link #grow(World)} increases energy by 1 up to the maximum</li>
 *   <li>When energy == 3 and a free neighboring plant slot exists, spawns a new plant and resets
 *   parent energy to 1</li>
 * </ul>
 * Behavior strictly limited to local growth and reproduction logic.
 */
public class Plant extends Organism implements Growable, Edible, Reproducible {
    //=============================================================================
//                                   Constants
//=============================================================================
    private static final int MAX_ENERGY = 3;

//=============================================================================
//                               Construction
//=============================================================================

    public Plant() {
        // Default energy is 1.
        super(1);
    }

    /**
     * Construct a plant with a given initial energy clamped to [1, MAX_ENERGY].
     *
     * @param energy requested starting energy
     */
    public Plant(int energy) {
        super(Math.min(Math.max(1, energy), MAX_ENERGY));
    }

//=============================================================================
//                                   Growth
//=============================================================================

    /**
     * Increase energy by 1 up to {@code MAX_ENERGY} if alive.
     *
     * @param world world context (ignored, required by interface)
     */
    @Override
    public void grow(World world) {
        if (isAlive() && getEnergy() < MAX_ENERGY) {
            setEnergy(getEnergy() + 1);  // 确保能量增加
        }
    }

//=============================================================================
//                                  Edible
//=============================================================================

    /**
     * Return current energy as nutritional value.
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
     * Determine whether reproduction can occur (alive, saturated energy, free neighbor).
     *
     * @param world world providing neighborhood lookup
     * @return {@code true} if a spawn is possible
     */
    @Override
    public boolean canReproduce(World world) {
        if (energy < MAX_ENERGY) return false;
        if (!alive) return false;

        // On récupère les voisins adjacents (PAS DE DIAGONALES)
        List<Cell> neighbors = world.getNeighbors(position, false);

        for (Cell c : neighbors) {
            if (c.isEmptyPlant()) return true;
        }

        return false;
    }

    /**
     * Produce a new plant instance with base energy.
     *
     * @return child organism
     */
    @Override
    public Organism reproduce() {
        return new Plant();
    }

    /**
     * Attempt to spawn a child into a neighboring free plant slot.
     * <p>On success resets parent energy to 1.</p>
     *
     * @param world world context
     * @return {@code true} if a child was placed
     */
    @Override
    public boolean spawn(World world) {
        if (!canReproduce(world)) {
            return false;
        }

        // But only placed in 4 basic directions
        Position[] neighbors = position.getCardinalNeighbors();
        java.util.ArrayList<Position> emptyPositions = new java.util.ArrayList<>();

        // Check if it is empty
        for (Position neighbor : neighbors) {
            if (neighbor.isValid(world.getWidth(), world.getHeight())) {
                Cell cell = world.getCell(neighbor);
                if (cell != null && !cell.hasPlant()) {
                    emptyPositions.add(neighbor);
                }
            }
        }

        if (emptyPositions.isEmpty()) {
            return false;
        }

        int randomIndex = prof.utils.RandomGenerator.nextInt(emptyPositions.size());
        Position spawnPosition = emptyPositions.get(randomIndex);

        Plant child = new Plant(1);
        Cell targetCell = world.getCell(spawnPosition);
        if (targetCell != null) {
            targetCell.setPlant(child);
            child.setPosition(spawnPosition);
            this.setEnergy(1);
            return true;
        }

        return false;
    }

//=============================================================================
//                            Energy Management
//=============================================================================

    /**
     * Add energy clamped to {@code MAX_ENERGY}.
     *
     * @param amount increment value
     */
    @Override
    public void addEnergy(int amount) {
        if (isAlive()) {
            energy = Math.min(energy + amount, MAX_ENERGY);
        }
    }

    /**
     * Subtract energy and mark dead if depleted.
     *
     * @param amount decrement value
     */
    @Override
    public void subEnergy(int amount) {
        energy = Math.max(energy - amount, 0);
        if (energy <= 0) {
            // Use the setEnergy of the parent class to update the alive state.
            setEnergy(0);
        }
    }
}
