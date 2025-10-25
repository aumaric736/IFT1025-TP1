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
            Position newPos = new Position(pos.getX() + dx, pos.getY() + dy);
            // Vérifier si la position est dans les bornes du monde
            if (world.isInside(newPos)) {
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
    List<Cell> neighbors = world.getCardinalNeighborCells(pos);  // 北、南、东、西
    List<Cell> freeCells = new ArrayList<>();

    // 筛选出空格
    for (Cell cell : neighbors) {
        if (cell.isEmptyForAnimal()) {  // 假设 Cell 提供这个方法
            freeCells.add(cell);
        }
    }

    // 如果没有可移动格子，则不移动
    if (freeCells.isEmpty()) {
        return null;
    }

    // 随机选一个目标格
    return prof.utils.RandomGenerator.getInstance().choose(freeCells);
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
	// TODO - Implémenter la méthode canReproduce pour Animal
    // 获取当前动物的位置
    Position pos = world.findPosition(this);
    if (pos == null) return false;

    // 检查是否有空的相邻格子
    boolean hasFreeSpace = false;
    List<Cell> neighbors = world.getCardinalNeighborCells(pos);
    for (Cell c : neighbors) {
        if (c.isEmptyForAnimal()) {
            hasFreeSpace = true;
            break;
        }
    }

    // 基础能量条件（子类可覆盖此逻辑）
    boolean enoughEnergy = getEnergy() >= 2;

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
    // 如果不能繁殖，直接返回 false
    if (!canReproduce(world)) return false;

    // 获取当前动物位置
    Position pos = world.findPosition(this);
    if (pos == null) return false;

    // 找到周围可用的格子（上下左右）
    List<Cell> neighbors = world.getCardinalNeighborCells(pos);
    List<Cell> freeCells = new ArrayList<>();
    for (Cell c : neighbors) {
        if (c.isEmptyForAnimal()) {
            freeCells.add(c);
        }
    }

    // 没有空位则返回 false
    if (freeCells.isEmpty()) return false;

    // 随机选择一个空格
    Cell target = prof.utils.RandomGenerator.getInstance().choose(freeCells);

    // 创建新动物（调用子类的 reproduce()）
    Organism baby = reproduce();

    // 将新动物放入选中的格子中
    target.setAnimal((Animal) baby);

    // 父代能量减半（取整）
    setEnergy(getEnergy() / 2);

    return true;
}

