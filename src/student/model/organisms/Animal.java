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
        // TODO - Implémenter la méthode perceive pour Animal
        List<Position> visiblePositions = new ArrayList<>();
        int range = visionRange();
        // Parcourir un carré de vision autour de la position
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                // Ignorer la position actuelle (0,0)
                if (dx == 0 && dy == 0) continue;
                Position newPos = new Position(pos.x() + dx, pos.y() + dy);
                // Vérifier si la position est dans les bornes du monde
                if (world.isValidPosition(newPos)) {
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
        // TODO - Implémenter la méthode chooseMove pour Animal
        // 获取四个方向的邻近格子
        Position[] neighbors = pos.getCardinalNeighbors();
        List<Cell> freeCells = new ArrayList<>();

        // 筛选出空的单元格
        for (Position neighborPos : neighbors) {
            if (neighborPos.isValid(world.getWidth(), world.getHeight())) {
                Cell cell = world.getCell(neighborPos);
                if (cell != null && cell.isEmptyAnimal()) {  // 修复：用 isEmptyAnimal
                    freeCells.add(cell);
                }
            }
        }

        // 如果没有可移动格子，则不移动
        if (freeCells.isEmpty()) {
            return null;
        }

        // 修复：正确的 RandomGenerator 用法
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
        // check energy,we need spllit 2
        boolean enoughEnergy = getEnergy() >= 2;

        // 检查是否有空闲空间
        boolean hasFreeSpace = false;
        Position currentPos = getPosition();
        if (currentPos != null) {
            Position[] neighbors = currentPos.getCardinalNeighbors();
            for (Position neighborPos : neighbors) {
                if (neighborPos.isValid(world.getWidth(), world.getHeight())) {
                    Cell cell = world.getCell(neighborPos);
                    if (cell != null && cell.isEmptyAnimal()) {
                        hasFreeSpace = true;
                        break;
                    }
                }
            }
        }

        return enoughEnergy && hasFreeSpace;
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

        Position currentPos = getPosition();  // 修复：直接用 getPosition
        if (currentPos == null) return false;

        // 获取四个基本方向的相邻位置
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

        setEnergy(getEnergy() / 2);
        return true;
    }
}

