/* ============================================================================
 * Path: src/student/model/organisms/Organism.java
 * Author: Zakary Gaillard-D.
 * Date: 2025-10-06
 * Description: Abstract base for all organisms handling position, energy, and life state.
 * ========================================================================== */
package student.model.organisms;

import student.model.behaviors.Energetic;
import student.model.core.Position;

/**
 * Base abstract organism holding shared state: {@link Position}, energy, and alive flag.
 * <p>Specialized behavior (movement, feeding, reproduction) is defined in subtypes.</p>
 */
public abstract class Organism implements Energetic {
    //=============================================================================
    //                                   Fields
    //=============================================================================
    protected Position position;
    protected int energy;
    protected boolean alive = true;
    protected final int maxEnergy;

    //=============================================================================
    //                               Construction
    //=============================================================================

    /**
     * Construct an organism with initial energy (legacy constructor).
     * NOTE: This constructor should set a reasonable default maxEnergy.
     * Subclasses should override with proper maxEnergy values.
     *
     * @param energy starting energy value
     */
    public Organism(int energy) {
        this(energy, Integer.MAX_VALUE); // 使用较大的默认值
    }

    /**
     * Construct an organism with initial energy and max energy limit.
     *
     * @param energy starting energy value
     * @param maxEnergy maximum energy limit
     */
    public Organism(int energy, int maxEnergy) {
        this.energy = Math.min(energy, maxEnergy);
        this.maxEnergy = maxEnergy;
    }

    /**
     * Construct an organism with position and energy.
     *
     * @param position the position of the organism
     * @param energy starting energy value
     * @param maxEnergy maximum energy limit
     */
    public Organism(Position position, int energy, int maxEnergy) {
        this.position = position;
        this.energy = Math.min(energy, maxEnergy);
        this.maxEnergy = maxEnergy;
    }

    //=============================================================================
    //                               Accessors
    //=============================================================================

    /**
     * Return current position (may be {@code null} if not placed).
     *
     * @return position or {@code null}
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Set organism position reference.
     *
     * @param pos new position (may be {@code null})
     */
    public void setPosition(Position pos) {
        this.position = pos;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    //=============================================================================
    //                            Energetic Contract
    //=============================================================================

    /**
     * Return current energy.
     *
     * @return energy value
     */
    @Override
    public int getEnergy() {
        return energy;
    }

    /**
     * Set energy; marks organism dead if energy <= 0.
     *
     * @param value new energy value
     */
    @Override
    public void setEnergy(int value) {
        this.energy = Math.min(Math.max(value, 0), maxEnergy);
        this.alive = (this.energy > 0);
    }

    /**
     * Return whether organism is alive.
     *
     * @return {@code true} if alive
     */
    @Override
    public boolean isAlive() {
        return alive;
    }

    //=============================================================================
    //                               Utilities
    //=============================================================================

    @Override
    public String toString() {
        return String.format("%s[%s, energy=%d, alive=%s]", 
            getClass().getSimpleName(), position, energy, alive);
    }
}