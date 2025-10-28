/* ============================================================================
 * Path: src/student/model/core/World.java
 * Author: Zakary Gaillard-D.
 * Date: 2025-10-06
 * Description: Simulation world grid composed of immutable dimensions and addressable cells.
 * ========================================================================== */
package student.model.core;

import java.util.ArrayList;
import java.util.List;
import student.model.organisms.Plant;
import student.model.organisms.Animal;
import student.model.organisms.Herbivore;
import student.model.organisms.Carnivore;

/**
 * Represents the simulation world as a rectangular grid of {@link Cell} instances.
 * <p>World is a simple container offering coordinate validation and neighbor lookup. Higher-level
 * behaviors (movement, spawning, cleanup) are orchestrated by controllers or organisms.</p>
 */
public class World {
//=============================================================================
//                                   Fields
//=============================================================================
private final int width;
private final int height;
private final Cell[][] grid;

//=============================================================================
//                               Construction
//=============================================================================

/**
 * Create a new rectangular world of the given dimensions.
 *
 * @param width  grid width (columns)
 * @param height grid height (rows)
 */
public World(int width, int height) {
	this.width = width;
	this.height = height;
	this.grid = new Cell[height][width];
	
	// Initialize every cell (row-major). Kept explicit for clarity over streams.
	for (int y = 0; y < height; y++) {
		for (int x = 0; x < width; x++) {
			grid[y][x] = new Cell(new Position(x, y));
		}
	}
}

//=============================================================================
//                               Accessors
//=============================================================================

/**
 * Return world width in cells.
 *
 * @return number of columns
 */
public int getWidth() {
	return width;
}

/**
 * Return world height in cells.
 *
 * @return number of rows
 */
public int getHeight() {
	return height;
}

//=============================================================================
//                              Cell Retrieval
//=============================================================================

/**
 * Return the cell at the given position or {@code null} if out of bounds.
 *
 * @param pos target position
 * @return cell reference or {@code null} if invalid
 */
public Cell getCell(Position pos) {
	if (!isValidPosition(pos)) return null;
	return grid[pos.y()][pos.x()];
}

//=============================================================================
//                               Validation
//=============================================================================

/**
 * Check whether a position lies within world bounds.
 *
 * @param pos position to validate
 * @return {@code true} if coordinates are inside the grid
 */
public boolean isValidPosition(Position pos) {
	return pos.x() >= 0 && pos.x() < width && pos.y() >= 0 && pos.y() < height;
}

//=============================================================================
//                               Transfers (Legacy)
//=============================================================================

/**
 * Transfer an organism from one cell to another (legacy alias).
 * <p>Deprecated in favor of explicit {@link #transferAnimal(Cell, Cell)} and
 * {@link #transferPlant(Cell, Cell)} for clarity. Only attempts moving the animal slot to
 * preserve historical behavior.</p>
 *
 * @param from source cell
 * @param to   destination cell
 * @deprecated Use {@link #transferAnimal(Cell, Cell)} / {@link #transferPlant(Cell, Cell)}.
 */
@Deprecated
public void transferOrganism(Cell from, Cell to) { // DONOTTOUCH[world] (MAINTAINER, 2025-10-06): Legacy semantics.
	if (from.hasAnimal()) {
		transferAnimal(from, to);
	}
}

/**
 * Transfer an animal between cells if the destination animal slot is empty.
 *
 * @param from source cell
 * @param to   destination cell
 */
public void transferAnimal(Cell from, Cell to) {
        // Allows movement to locations with prey
    boolean canTransfer = from.hasAnimal() &&
            (to.isEmptyAnimal() ||
                    (to.hasAnimal() && to.getAnimal() instanceof Herbivore));

    if (canTransfer) {
        var animal = from.getAnimal();
        Position oldPos = animal.getPosition();
        int nutrition = 0;
        // If there is prey at the target location, get energy first and remove the prey
        if (to.hasAnimal() && to.getAnimal() instanceof Herbivore herbivore) {
            nutrition = herbivore.nutrition();
            to.removeAnimal();
        }

        from.removeAnimal();
        to.setAnimal(animal);
        animal.setPosition(to.getPosition());

        // add energy
        if (nutrition > 0 && animal instanceof Carnivore carnivore) {
            int beforeEnergy = carnivore.getEnergy();
            carnivore.addEnergy(nutrition);
            int afterEnergy = carnivore.getEnergy();
        }

        Position newPos = animal.getPosition();
    }
}
/**
 * Transfer a plant between cells if the destination plant slot is empty.
 *
 * @param from source cell
 * @param to   destination cell
 */
public void transferPlant(Cell from, Cell to) {
	if (from.hasPlant() && to.isEmptyPlant()) {
		var plant = from.getPlant();
		from.removePlant();
		to.setPlant(plant);
		plant.setPosition(to.getPosition());
	}
}

//=============================================================================
//                               Neighborhood
//=============================================================================

/**
 * Return neighboring cells of a position (4-way or 8-way depending on {@code includeDiagonals}).
 *
 * @param pos              origin position
 * @param includeDiagonals include diagonals when {@code true}
 * @return list of neighboring cells (never {@code null})
 */
public List<Cell> getNeighbors(Position pos, boolean includeDiagonals) {
	List<Cell> neighbors = new ArrayList<>();
	
	int[][] deltas;
	if (includeDiagonals) {
		// 8 directions including diagonals.
		deltas = new int[][]{{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
	} else {
		// Cardinal directions only.
		deltas = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
	}
	
	for (int[] delta : deltas) {
		int nx = pos.x() + delta[0];
		int ny = pos.y() + delta[1];
		if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue; // Avoid invalid Position creation.
		Position neighborPos = new Position(nx, ny); // Safe now.
		neighbors.add(getCell(neighborPos));
	}
	
	return neighbors;
}
//=============================================================================
//                               Phase Execution
//=============================================================================
//
//
/**
     * Execute Phase 1: Plant Growth
     * All plants grow by 1 energy, until to their maximum
 */
public void executePhase1() {
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            Cell cell = grid[y][x];
             if (cell.hasPlant()) {
                    cell.getPlant().grow(this);
                }
            }
        }
    }
    /**
     * Execute Phase 2: Herbivore
     * Herbivores lose energy
     * *
     **/
    public void executePhase2() {
        List<Herbivore> herbivores = new ArrayList<>();

        // Collect all herbivores
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = grid[y][x];
                if (cell.hasAnimal() && cell.getAnimal() instanceof Herbivore herbivore) {
                    herbivores.add(herbivore);
                }
            }
        }

        // Each herbivore performs the behavior
        for (Herbivore herbivore : herbivores) {
            if (herbivore.isAlive()) {
                Position currentPos = herbivore.getPosition();
                Cell currentCell = getCell(currentPos);

                if (currentCell != null) {
                    // move and add border check
                    Cell targetCell = herbivore.chooseMove(this, currentPos);
                    if (targetCell != null && targetCell != currentCell && isValidPosition(targetCell.getPosition())) {
                        // move to new position
                        transferAnimal(currentCell, targetCell);
                        currentCell = targetCell;  // update new position
                    }
                    // Check if plants can be eaten (in the new location)
                    if (herbivore.canEat(currentCell)) {
                        herbivore.eat(currentCell, this);
                    }
                    // Energy -1 in each tour
                    int beforeEnergyHerbivore = herbivore.getEnergy();
                    herbivore.subEnergy(1);
                    int afterEnergyHerbivore = herbivore.getEnergy();
                }
            }
        }
    }

    /**
     * Execute Phase 3: Carnivore Phase
     * Carnivores hunt, move, eat, and lose energy
     */
    public void executePhase3() {
        List<Carnivore> carnivores = new ArrayList<>();

        // Collect all carnivores
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = grid[y][x];
                if (cell.hasAnimal() && cell.getAnimal() instanceof Carnivore carnivore) {
                    carnivores.add(carnivore);
                }
            }
        }

        // Each carnivore performs the behavior
        for (Carnivore carnivore : carnivores) {
            if (carnivore.isAlive()) {
                Position currentPos = carnivore.getPosition();
                Cell currentCell = getCell(currentPos);

                if (currentCell != null) {
                    int initialEnergy = carnivore.getEnergy();

                    // 1. sub 1 when move
                    carnivore.subEnergy(1);
                    int afterMoveCost = carnivore.getEnergy();

                    // 2. move
                    Cell targetCell = carnivore.chooseMove(this, currentPos);
                    boolean didMove = false;

                    if (targetCell != null && targetCell != currentCell && isValidPosition(targetCell.getPosition())) {

                        // Record target location
                        Position targetPosition = targetCell.getPosition();

                        // Execute the move
                        transferAnimal(currentCell, targetCell);

                        // update currentCell
                        currentCell = getCell(targetPosition);
                        didMove = true;
                    }


                    boolean canEat = carnivore.canEat(currentCell);
                    if (canEat) {
                        carnivore.eat(currentCell, this);
                    }
                    int finalEnergy = carnivore.getEnergy();
                }
            }
        }
    }
    /**
     * Execute Phase 4: Reproduction
     * Plants and animals attempt to reproduce
     */
    public void executePhase4() {
        // First, plants reproduce
        List<Plant> plantsToReproduce = new ArrayList<>();
        List<Animal> animalsToReproduce = new ArrayList<>();

        // Collect all plants that can reproduce
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = grid[y][x];

                // plant reproduce
                if (cell.hasPlant() && cell.getPlant().canReproduce(this)) {
                    plantsToReproduce.add(cell.getPlant());
                }

                // animal reproduce
                if (cell.hasAnimal() && cell.getAnimal().canReproduce(this)) {
                    animalsToReproduce.add(cell.getAnimal());
                }
            }
        }

        // Attempt reproduction for each plant
        for (Plant plant : plantsToReproduce) {
            plant.spawn(this);
        }

        // Attempt reproduction for each animal
        for (Animal animal : animalsToReproduce) {
            animal.spawn(this);
        }
    }
    /**
     * Execute Phase 5: Cleanup
     * Remove dead organisms from the world
     */
    public void executePhase5() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = grid[y][x];
                cell.cleanupDeadOrganisms();
            }
        }
    }
}
