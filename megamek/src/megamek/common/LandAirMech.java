/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.options.OptionsConstants;

public class LandAirMech extends BipedMech implements IAero {

    /**
     *
     */
    private static final long serialVersionUID = -8118673802295814548L;
    
    public static final int CONV_MODE_MECH    = 0;
    public static final int CONV_MODE_AIRMECH = 1;
    public static final int CONV_MODE_FIGHTER = 2;

    public static final int LAM_AVIONICS = 15;

    public static final int LAM_LANDING_GEAR = 16;

    public static final String systemNames[] =
        { "Life Support", "Sensors", "Cockpit", "Engine", "Gyro", null, null, "Shoulder", "Upper Arm", "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot", "Avionics", "Landing Gear" };
    
    public static final int LAM_UNKNOWN  = -1;
    public static final int LAM_STANDARD = 0;
    public static final int LAM_BIMODAL  = 1;
    
    public static final String[] LAM_STRING = { "Standard", "Bimodal" };

    private int lamType;
    
    /** Fighter mode **/
    // out of control
    private boolean outControl = false;
    private boolean outCtrlHeat = false;
    private boolean randomMove = false;

    // set up movement
    private int currentVelocity = 0;
    private int nextVelocity = currentVelocity;
    private boolean accLast = false;
    private boolean rolled = false;
    private boolean failedManeuver = false;
    private boolean accDecNow = false;
    private int straightMoves = 0;
    private int altLoss = 0;
    private int altLossThisRound = 0;

    private boolean critThresh = false;    

    private int maxBombPoints = 0;
    private int[] bombChoices = new int[BombType.B_NUM];

    private int fuel;
    private int whoFirst;

    public LandAirMech(int inGyroType, int inCockpitType, int inLAMType) {
        super(inGyroType, inCockpitType);
        lamType = inLAMType;

        setTechLevel(TechConstants.T_IS_ADVANCED);
        setCritical(Mech.LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_LT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_RT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_LT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        setCritical(Mech.LOC_RT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        setCritical(Mech.LOC_CT, 10, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));

        previousMovementMode = movementMode;
        setFuel(80);
        
        setCrew(new LAMPilot(this));
    }
    
    public void setCrew(Crew newCrew) {
        if (newCrew instanceof LAMPilot) {
            super.setCrew(newCrew);
        } else {
            super.setCrew(LAMPilot.convertToLAMPilot(this, newCrew));
        }
    }

    @Override
    public String getSystemName(int index) {
        if (index == SYSTEM_GYRO) {
            return Mech.getGyroDisplayString(gyroType);
        }
        if (index == SYSTEM_COCKPIT) {
            return Mech.getCockpitDisplayString(cockpitType);
        }
        return systemNames[index];
    }

    @Override
    public String getRawSystemName(int index) {
        return systemNames[index];
    }
    
    public int getLAMType() {
        return lamType;
    }
    
    public void setLAMType(int lamType) {
        this.lamType = lamType;
    }
    
    public String getLAMTypeString(int lamType) {
        if (lamType < 0 || lamType >= LAM_STRING.length) {
            return LAM_STRING[LAM_UNKNOWN];
        }
        return LAM_STRING[lamType];
    }
    
    public String getLAMTypeString() {
        return getLAMTypeString(getLAMType());
    }

    public static int getLAMTypeForString(String inType) {
        if ((inType == null) || (inType.length() < 1)) {
            return LAM_UNKNOWN;
        }
        for (int x = 0; x < LAM_STRING.length; x++) {
            if (inType.equals(LAM_STRING[x])) {
                return x;
            }
        }
        return LAM_UNKNOWN;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return getConversionMode() != CONV_MODE_FIGHTER;
    }

    @Override
    public boolean doomedInSpace() {
        return getConversionMode() != CONV_MODE_FIGHTER;
    }

    /**
     * Current MP is calculated differently depending on the LAM's mode. AirMech mode returns
     * cruise/flank; walk/run is treated as a special case of WiGE ground movement.
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp;
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            mp = getFighterModeWalkMP(gravity, ignoremodulararmor);
        } else if (getConversionMode() == CONV_MODE_AIRMECH) {
            mp = getAirMechCruiseMP(gravity, ignoremodulararmor);
        } else {
            mp = super.getWalkMP(gravity, ignoreheat, ignoremodulararmor);
        }
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp;
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            mp = getFighterModeRunMP(gravity, ignoremodulararmor);
        } else if (getConversionMode() == CONV_MODE_AIRMECH) {
            mp = getAirMechFlankMP(gravity, ignoremodulararmor);
        } else {
            mp = super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
        }
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp;
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            mp = getFighterModeRunMP(gravity, ignoremodulararmor);
        } else if (getConversionMode() == CONV_MODE_AIRMECH) {
            mp = getAirMechFlankMP(gravity, ignoremodulararmor);
        } else {
            mp = super.getRunMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
        }
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    /**
     * This value should only be used for biped and airmech ground movement.
     */
    @Override
    public int getSprintMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            return getRunMP();
        } else if (getConversionMode() == CONV_MODE_AIRMECH) {
            if (hasHipCrit()) {
                return getAirMechRunMP(gravity, ignoreheat, ignoremodulararmor);
            }
            return (int)Math.ceil(getAirMechWalkMP(gravity, ignoreheat, ignoremodulararmor)
                    * (hasArmedMASC()? 2.5 : 2.0));
        }
        return super.getSprintMP(gravity, ignoreheat, ignoremodulararmor);
    }

    /**
     * This value should only be used for biped and airmech ground movement.
     */
    @Override
    public int getSprintMPwithoutMASC(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            return getRunMP();
        } else if (getConversionMode() == CONV_MODE_AIRMECH) {
            if (hasHipCrit()) {
                return getAirMechRunMP(gravity, ignoreheat, ignoremodulararmor);
            }
            return getAirMechWalkMP(gravity, ignoreheat, ignoremodulararmor)  * 2;
        }
        return super.getSprintMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
    }

    public int getOriginalSprintMPwithoutMASC() {
        if (getConversionMode() == CONV_MODE_MECH) {
            return (int) Math.ceil(getOriginalWalkMP() * 2.0);            
        } else {
            return getOriginalRunMP();
        }
    }

    public int getAirMechCruiseMP(boolean gravity, boolean ignoremodulararmor) {
        if (game != null && game.getBoard().inAtmosphere()
                && (isLocationBad(Mech.LOC_LT) || isLocationBad(Mech.LOC_RT))) {
            return 0;
        }
        return getJumpMP(gravity, ignoremodulararmor) * 3;
    }

    public int getAirMechFlankMP(boolean gravity, boolean ignoremodulararmor) {
        if (game != null && game.getBoard().inAtmosphere()
                && (isLocationBad(Mech.LOC_LT) || isLocationBad(Mech.LOC_RT))) {
            return 0;
        }
        return (int)Math.ceil(getAirMechCruiseMP(gravity, ignoremodulararmor) * 1.5);
    }
    
    public int getAirMechWalkMP() {
        return getAirMechWalkMP(true, false, false);
    }

    public int getAirMechWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp = (int)Math.ceil(super.getWalkMP(gravity, ignoreheat, ignoremodulararmor) * 0.33);
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    public int getAirMechRunMP() {
        return getAirMechRunMP(true, false, false);
    }

    public int getAirMechRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp = (int)Math.ceil(getAirMechWalkMP(gravity, ignoreheat, ignoremodulararmor) * 1.5);
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    public int getFighterModeWalkMP(boolean gravity, boolean ignoremodulararmor) {
        int thrust = getCurrentThrust();
        if (!isAirborne()) {
            thrust /= 2;
        }
        return thrust;
    }

    public int getFighterModeRunMP(boolean gravity, boolean ignoremodulararmor) {
        int walk = getFighterModeWalkMP(gravity, ignoremodulararmor);
        if (isAirborne()) {
            return (int)Math.ceil(walk * 1.5);
        } else {
            return walk; // Grounded asfs cannot use flanking movement
        }
    }

    @Override
    public int getCurrentThrust() {
        int j = getJumpMP();
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }
        }
        return j;
    }

    public int getAirMechCruiseMP() {
        return getAirMechCruiseMP(true, false);
    }

    public int getAirMechFlankMP() {
        return getAirMechFlankMP(true, false);
    }

    public int getFighterModeWalkMP() {
        return getFighterModeWalkMP(true, false);
    }

    public int getFighterModeRunMP() {
        return getFighterModeRunMP(true, false);
    }
    
    /**
     * LAMs cannot benefit from MASC in AirMech or fighter mode and cannot mount a supercharger.
     */
    @Override
    public boolean hasArmedMASC() {
        if (getConversionMode() == CONV_MODE_MECH) {
            return super.hasArmedMASC();
        } else {
            return false;
        }
    }
    
    @Override
    public boolean isImmobile() {
        if (getConversionMode() == CONV_MODE_FIGHTER
                && (isAirborne() || isSpaceborne())) {
            return false;
        }
        return super.isImmobile();
    }
    
    @Override
    public int getWalkHeat() {
        if (moved == EntityMovementType.MOVE_VTOL_WALK) {
            return getAirMechHeat();
        }
        return super.getWalkHeat();
    }
    
    @Override
    public int getRunHeat() {
        if (moved == EntityMovementType.MOVE_VTOL_RUN) {
            return getAirMechHeat();
        }
        return super.getRunHeat();
    }
    
    public int getAirMechHeat() {
        int mod = bDamagedCoolantSystem?1:0;
        return mod + (int)Math.round(getJumpHeat(mpUsed) / 3.0);
    }

    @Override
    public int getEngineCritHeat() {
        // Engine crit heat follows ASF rules in fighter mode. 
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            return 2 * getEngineHits();
        } else {
            return super.getEngineCritHeat();
        }
    }

    @Override
    public boolean usesTurnMode() {
        // Turn mode rule is not optional for LAMs in AirMech mode.
        return getConversionMode() == CONV_MODE_AIRMECH;
    }

    /**
     * When cycling through possible movement modes, we need to know if we've returned to the previous
     * mode, which means that no conversion is actually going to take place.
     * 
     * @return The movement mode on the previous turn.
     */
    public EntityMovementMode getPreviousMovementMode() {
        return previousMovementMode;
    }
    
    @Override
    public void setMovementMode(EntityMovementMode mode) {
        int prevMode = getConversionMode();
        if (mode == EntityMovementMode.AERODYNE
                || mode == EntityMovementMode.WHEELED) {
            setConversionMode(CONV_MODE_FIGHTER);
        } else if (mode == EntityMovementMode.WIGE) {
            setConversionMode(CONV_MODE_AIRMECH);
        } else {
            setConversionMode(CONV_MODE_MECH);
        }
        super.setMovementMode(mode);
        
        if (getConversionMode() != prevMode) {
            if (getConversionMode() == CONV_MODE_FIGHTER) {
                setRapidFire();
            } else if (prevMode == CONV_MODE_FIGHTER) {
                for (Mounted m : getTotalWeaponList()) {
                    WeaponType wtype = (WeaponType) m.getType();
                    if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                        m.setMode("");
                        m.setModeSwitchable(true);
                    } else if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA) {
                        m.setMode("");
                        m.setModeSwitchable(true);
                    } else if (wtype.hasIndirectFire()) {
                        m.setModeSwitchable(true);
                    }
                }
            }
        }
    }
    
    @Override
    public void setConversionMode(int mode) {
        if (mode == getConversionMode()) {
            return;
        }
        if (mode == CONV_MODE_MECH) {
            super.setMovementMode(EntityMovementMode.BIPED);
        } else if (mode == CONV_MODE_AIRMECH) {
            super.setMovementMode(EntityMovementMode.WIGE);
        } else if (mode == CONV_MODE_FIGHTER) {
            super.setMovementMode(EntityMovementMode.AERODYNE);
        } else {
            return;
        }
        super.setConversionMode(mode);
    }
    
    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        // Fighter mode has the same terrain restrictions as ASFs.
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            IHex hex = game.getBoard().getHex(c);
            if (isAirborne()) {
                if (hex.containsTerrain(Terrains.IMPASSABLE)) {
                    return true;
                }
                return false;
            }

            // Additional restrictions for hidden units
            if (isHidden()) {
                // Can't deploy in paved hexes
                if (hex.containsTerrain(Terrains.PAVEMENT)
                        || hex.containsTerrain(Terrains.ROAD)) {
                    return true;
                }
                // Can't deploy on a bridge
                if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation)
                        && hex.containsTerrain(Terrains.BRIDGE)) {
                    return true;
                }
                // Can't deploy on the surface of water
                if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                    return true;
                }
            }

            // grounded aeros have the same prohibitions as wheeled tanks
            return hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.ROUGH)
                    || ((hex.terrainLevel(Terrains.WATER) > 0) 
                            && !hex.containsTerrain(Terrains.ICE))
                    || hex.containsTerrain(Terrains.RUBBLE)
                    || hex.containsTerrain(Terrains.MAGMA)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.SNOW) > 1)
                    || (hex.terrainLevel(Terrains.GEYSER) == 2);
        } else if (getConversionMode() == CONV_MODE_AIRMECH && currElevation > 0) {
            // Cannot enter woods or a building hex in AirMech mode unless using ground movement
            // or flying over the terrain.
            IHex hex = game.getBoard().getHex(c);
            return (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || hex.containsTerrain(Terrains.BLDG_ELEV))
                    && hex.ceiling() > currElevation;
        } else {
            // Mech mode or AirMech mode using ground MP have the same restrictions as Biped Mech.
            return super.isLocationProhibited(c, currElevation);
        }
    }
    
    @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_WALK:
                if (getConversionMode() == CONV_MODE_FIGHTER) {
                    return "Cruised";
                } else {
                    return "Walked";
                }
            case MOVE_RUN:
                if (getConversionMode() == CONV_MODE_FIGHTER) {
                    return "Flanked";
                } else {
                    return "Ran";
                }
            case MOVE_VTOL_WALK:
                return "Cruised";
            case MOVE_VTOL_RUN:
                return "Flanked";
            case MOVE_SAFE_THRUST:
                return "Safe Thrust";
            case MOVE_OVER_THRUST:
                return "Over Thrust";
            default:
                return super.getMovementString(mtype);
        }
    }

    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        switch (mtype) {
        case MOVE_WALK:
            if (getConversionMode() == CONV_MODE_FIGHTER) {
                return "C";
            } else {
                return "W";
            }
        case MOVE_RUN:
            if (getConversionMode() == CONV_MODE_FIGHTER) {
                return "F";
            } else {
                return "R";
            }
        case MOVE_VTOL_WALK:
            return "C";
        case MOVE_VTOL_RUN:
            return "F";
            case MOVE_NONE:
                return "N";
            case MOVE_SAFE_THRUST:
                return "S";
            case MOVE_OVER_THRUST:
                return "O";
            default:
                return super.getMovementAbbr(mtype);
        }
    }
    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        if (getConversionMode() != CONV_MODE_FIGHTER
                && !isAirborneVTOLorWIGE()) {
            return super.addEntityBonuses(roll);
        }
        
        // In fighter mode a destroyed gyro gives +6 to the control roll.
        int gyroHits = getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                Mech.LOC_CT);
        if (gyroHits > 0) {
            if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
                if (gyroHits == 1) {
                    roll.addModifier(1, "HD Gyro damaged once");
                } else if (gyroHits == 2){
                    roll.addModifier(3, "HD Gyro damaged twice");
                } else {
                    roll.addModifier(6, "Gyro destroyed");
                }
            } else {
                if (gyroHits == 1) {
                    roll.addModifier(3, "Gyro damaged");
                } else {
                    roll.addModifier(6, "Gyro destroyed");
                }
            }
        }

        // EI bonus?
        if (hasActiveEiCockpit()) {
            roll.addModifier(-1, "Enhanced Imaging");
        }

        // VDNI bonus?
        if (getCrew().getOptions().booleanOption(OptionsConstants.MD_VDNI)
                && !getCrew().getOptions().booleanOption(OptionsConstants.MD_BVDNI)) {
            roll.addModifier(-1, "VDNI");
        }

        // Small/torso-mounted cockpit penalty?
        if ((getCockpitType() == Mech.COCKPIT_SMALL)
                && !getCrew().getOptions().booleanOption(OptionsConstants.MD_BVDNI)) {
            roll.addModifier(1, "Small Cockpit");
        }

        if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)) {
            roll.addModifier(1, "cramped cockpit");
        }
        
        int avionicsHits = getAvionicsHits();
        if (avionicsHits > 2) {
            roll.addModifier(5, "avionics destroyed");
        } else if (avionicsHits > 0) {
            roll.addModifier(avionicsHits, "avionics damage");
        }
        
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            if (getCrew().getHits(0) > 0) {
                roll.addModifier(getCrew().getHits(0), "pilot hits");
            }
            if (moved == EntityMovementType.MOVE_OVER_THRUST) {
                roll.addModifier(+1, "Used more than safe thrust");
            }
            int vel = getCurrentVelocity();
            int vmod = vel - (2 * getWalkMP());
            if (vmod > 0) {
                roll.addModifier(vmod, "Velocity greater than 2x safe thrust");
            }

            int atmoCond = game.getPlanetaryConditions().getAtmosphere();
            // add in atmospheric effects later
            if (!(game.getBoard().inSpace()
                    || (atmoCond == PlanetaryConditions.ATMO_VACUUM))
                    && isAirborne()) {
                roll.addModifier(+1, "Atmospheric operations");
            }

            if (hasQuirk(OptionsConstants.QUIRK_POS_ATMO_FLYER) && !game.getBoard().inSpace()) {
                roll.addModifier(-1, "atmospheric flyer");
            }
            if (hasQuirk(OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY) && !game.getBoard().inSpace()) {
                roll.addModifier(+1, "atmospheric flight instability");
            }
        }

        return roll;
    }

    /**
     * Landing in AirMech mode requires a control roll only if the gyro or any of the hip or leg actuators
     * are damaged.
     * 
     * @return The control roll that must be passed to land safely.
     */
    public PilotingRollData checkAirMechLanding() {
        // Base piloting skill
        PilotingRollData roll = new PilotingRollData(getId(), getCrew()
                .getPiloting(), "Base piloting skill");
        
        addEntityBonuses(roll);
        
        // Landing in AirMech mode only requires a roll if gyro or hip/leg actuators are damaged.
        int gyroHits = getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,
                Mech.LOC_CT);
        if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            gyroHits--;
        }

        boolean required = gyroHits > 0;

        for (int loc = 0; loc < locations(); loc++) {
            if (locationIsLeg(loc)) {
                if (isLocationBad(loc)) {
                    roll.addModifier(5, getLocationName(loc) + " destroyed");
                    required = true;
                } else {
                    // check for damaged hip actuators
                    if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) {
                        roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                        if (!game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                            continue;
                        }
                        required = true;
                    }
                    // upper leg actuators?
                    if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc) > 0) {
                        roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                        required = true;
                    }
                    // lower leg actuators?
                    if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc) > 0) {
                        roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                        required = true;
                    }
                    // foot actuators?
                    if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc) > 0) {
                        roll.addModifier(1, getLocationName(loc) + " Foot Actuator destroyed");
                        required = true;
                    }
                }
            }
        }
        if (required) {
            roll.addModifier(0, "landing with gyro or leg damage");
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check not required for landing");
        }
        return roll;
    }
    
    @Override
    public int getMaxElevationDown(int currElevation) {
        // Cannot spend AirMech MP above altitude 3 (level 30) so we use that as max descent.
        if ((currElevation > 0)
                && (getConversionMode() == CONV_MODE_AIRMECH)) {
            return 30;
        }
        return super.getMaxElevationDown(currElevation);
    }
    
    @Override
    public boolean canChangeSecondaryFacing() {
        if (getConversionMode() == CONV_MODE_MECH) {
            return super.canChangeSecondaryFacing();
        } else {
            return false;
        }
    }
    
    /**
     * Start a new round
     *
     * @param roundNumber the <code>int</code> number of the new round
     */
    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        previousMovementMode = movementMode;

        // reset threshold critted
        setCritThresh(false);

        // reset maneuver status
        setFailedManeuver(false);
        // reset acc/dec this turn
        setAccDecNow(false);

        updateBays();

        // update recovery turn if in recovery
        if (getRecoveryTurn() > 0) {
            setRecoveryTurn(getRecoveryTurn() - 1);
        }

        if (getConversionMode() == CONV_MODE_FIGHTER) {
            // if in atmosphere, then halve next turn's velocity
            if (!game.getBoard().inSpace() && isDeployed() && (roundNumber > 0)) {
                setNextVelocity((int) Math.floor(getNextVelocity() / 2.0));
            }

            // update velocity
            setCurrentVelocity(getNextVelocity());

            // if they are out of control due to heat, then apply this and reset
            if (isOutCtrlHeat()) {
                setOutControl(true);
                setOutCtrlHeat(false);
            }

            // get new random whofirst
            setWhoFirst();

            /* TODO: implement bomb bays
        // Remove all bomb attacks
        List<Mounted> bombAttacksToRemove = new ArrayList<>();
        EquipmentType spaceBomb = EquipmentType.get(SPACE_BOMB_ATTACK);
        EquipmentType altBomb = EquipmentType.get(ALT_BOMB_ATTACK);
        EquipmentType diveBomb = EquipmentType.get(DIVE_BOMB_ATTACK);
        for (Mounted eq : equipmentList) {
            if ((eq.getType() == spaceBomb) || (eq.getType() == altBomb)
                    || (eq.getType() == diveBomb)) {
                bombAttacksToRemove.add(eq);
            }
        }
        equipmentList.removeAll(bombAttacksToRemove);
        weaponList.removeAll(bombAttacksToRemove);
        totalWeaponList.removeAll(bombAttacksToRemove);
        weaponGroupList.removeAll(bombAttacksToRemove);
        weaponBayList.removeAll(bombAttacksToRemove);

        // Add the space bomb attack
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SPACE_BOMB)
                && game.getBoard().inSpace()
                && (getBombs(AmmoType.F_SPACE_BOMB).size() > 0)) {
            try {
                addEquipment(spaceBomb, LOC_NOSE, false);
            } catch (LocationFullException ex) {
            }
        }
        // Add ground bomb attacks
        int numGroundBombs = getBombs(AmmoType.F_GROUND_BOMB).size();
        if (!game.getBoard().inSpace() && (numGroundBombs > 0)) {
            try {
                addEquipment(diveBomb, LOC_NOSE, false);
            } catch (LocationFullException ex) {
            }
            for (int i = 0; i < Math.min(10, numGroundBombs); i++) {
                try {
                    addEquipment(altBomb, LOC_NOSE, false);
                } catch (LocationFullException ex) {
                }
            }
        }
             */
            resetAltLossThisRound();
        }
    }
    
    /**
     * Cannot make any physical attacks in fighter mode except ramming, which is
     * handled in the movement phase.
     */
    @Override
    public boolean isEligibleForPhysical() {
        return getConversionMode() != CONV_MODE_FIGHTER && super.isEligibleForPhysical();
    }
        
    /*
     * Cycling through conversion modes for LAMs in 'Mech or fighter mode is simple toggling between
     * two states. LAMs in AirMech mode have three possible states.
     */
    @Override
    public EntityMovementMode nextConversionMode(EntityMovementMode afterMode) {
        boolean inSpace = game != null && game.getBoard().inSpace();
        if (previousMovementMode == EntityMovementMode.WIGE) {
            if (afterMode == EntityMovementMode.WIGE) {
                return EntityMovementMode.AERODYNE;
            } else if (afterMode == EntityMovementMode.AERODYNE
                    || afterMode == EntityMovementMode.WHEELED) {
                return originalMovementMode;
            } else {
                return EntityMovementMode.WIGE;
            }
        } else if (afterMode == EntityMovementMode.WIGE) {
            return inSpace? EntityMovementMode.AERODYNE : previousMovementMode;
        } else if (afterMode == EntityMovementMode.AERODYNE || afterMode == EntityMovementMode.WHEELED) {
            return (inSpace || lamType == LAM_BIMODAL)? originalMovementMode : EntityMovementMode.WIGE;
        } else {
            return lamType == LAM_BIMODAL? EntityMovementMode.AERODYNE : EntityMovementMode.WIGE;
        }
    }
    
    public boolean canConvertTo(EntityMovementMode toMode) {
        return canConvertTo(getConversionMode(), getConversionModeFor(toMode));
    }
    
    public boolean canConvertTo(int fromMode, EntityMovementMode toMode) {
        return canConvertTo(fromMode, getConversionModeFor(toMode));
    }
    
    /**
     * Determines whether it is possible to assume a particular mode based on damage and type of map.
     * 
     * @param fromMode The mode to convert from (one of CONV_MODE_MECH, CONV_MODE_AIRMECH, or CONV_MODE_FIGHTER)
     * @param toMode   The mode to convert to (one of CONV_MODE_MECH, CONV_MODE_AIRMECH, or CONV_MODE_FIGHTER)
     * @return true if it is possible for the LAM to convert to the given mode.
     */
    public boolean canConvertTo(int fromMode, int toMode) {
        // Cannot convert with any gyro damage
        int gyroHits = getBadCriticals(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO, LOC_CT);
        if (getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            gyroHits--;
        }
        if (gyroHits > 0) {
            return false;
        }
        
        // Cannot convert to or from mech mode with damage shoulder or arm actuators
        if ((toMode == CONV_MODE_MECH || fromMode == CONV_MODE_MECH)
                && (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, LOC_RARM)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, LOC_RARM)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, LOC_RARM)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, LOC_LARM)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, LOC_LARM)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, LOC_LARM) > 0)) {
            return false;
        }
        
        // Cannot convert to or from fighter mode with damage hip or leg actuators
        if ((toMode == CONV_MODE_FIGHTER || fromMode == CONV_MODE_FIGHTER)
                && (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, LOC_RLEG)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, LOC_RLEG)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, LOC_RLEG)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, LOC_LLEG)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, LOC_LLEG)
                + getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, LOC_LLEG) > 0)) {
            return false;
        }
        
        if (toMode == CONV_MODE_AIRMECH) {
            if (getLAMType() == LAM_BIMODAL) {
                return false;
            }
        } else if (toMode == CONV_MODE_FIGHTER) {
            // Standard LAMs can convert from mech to fighter mode in a single round on a space map
            if (fromMode == CONV_MODE_MECH) {
                return getLAMType() == LAM_BIMODAL
                        || game.getBoard().inSpace();
            }
        } else if (toMode == CONV_MODE_MECH) {
            // Standard LAMs can convert from fighter to mech mode in a single round on a space map
            if (fromMode == CONV_MODE_FIGHTER) {
                return getLAMType() == LAM_BIMODAL
                        || game.getBoard().inSpace();
            }
        }
        return true;
    }
    
    public int getConversionModeFor(EntityMovementMode mmode) {
        if (mmode == EntityMovementMode.AERODYNE
                || mmode == EntityMovementMode.WHEELED) {
            return CONV_MODE_FIGHTER;
        } else if (mmode == EntityMovementMode.WIGE) {
            return CONV_MODE_AIRMECH;
        } else {
            return CONV_MODE_MECH;
        }
    }
    
    @Override
    public boolean canFall(boolean gyroLegDamage) {
        return getConversionMode() != CONV_MODE_FIGHTER
                && !isAirborneVTOLorWIGE();
    }
    
    @Override
    public int height() {
        if (getConversionMode() == CONV_MODE_MECH) {
            return super.height();
        }
        return 0;
    }
    
    /** Fighter Mode **/
    
    public void setWhoFirst() {
        whoFirst = Compute.randomInt(500);
    }

    public int getWhoFirst() {
        return whoFirst;
    }

    public int getMaxBombPoints() {
        return maxBombPoints;
    }

    public int[] getBombChoices() {
        return bombChoices.clone();
    }

    public void setBombChoices(int[] bc) {
        if (bc.length == bombChoices.length) {
            bombChoices = bc;
        }
    }

    @Override
    public int getCurrentVelocity() {
        // if using advanced movement then I just want to sum up
        // the different vectors
        if ((game != null) && game.useVectorMove()) {
            return getVelocity();
        }
        return currentVelocity;
    }

    @Override
    public void setCurrentVelocity(int velocity) {
        currentVelocity = velocity;
    }

    @Override
    public int getNextVelocity() {
        return nextVelocity;
    }

    @Override
    public void setNextVelocity(int velocity) {
        nextVelocity = velocity;
    }

    @Override
    public int getCurrentVelocityActual() {
        return currentVelocity;
    }

    @Override
    public boolean isRolled() {
        return rolled;
    }
    
    @Override
    public boolean isOutControlTotal() {
        return outControl || shutDown || getCrew().isUnconscious();
    }
    
    @Override
    public boolean isOutControl() {
        return outControl;
    }

    @Override
    public void setOutControl(boolean ocontrol) {
        outControl = ocontrol;
    }
    
    @Override
    public boolean isOutCtrlHeat() {
        return outCtrlHeat;
    }

    @Override
    public void setOutCtrlHeat(boolean octrlheat) {
        outCtrlHeat = octrlheat;
    }
    
    @Override
    public boolean isRandomMove() {
        return randomMove;
    }

    @Override
    public void setRandomMove(boolean randmove) {
        randomMove = randmove;
    }

    @Override
    public void setRolled(boolean roll) {
        rolled = roll;
    }
    
    @Override
    public boolean didAccLast() {
        return accLast;
    }

    @Override
    public void setAccLast(boolean b) {
        accLast = b;
    }
    
    @Override
    public void setSI(int si) {
        setInternal(si, LOC_CT);
    }

    @Override
    public int getSI() {
        return getInternal(LOC_CT);
    }

    @Override
    public int get0SI() {
        return getOInternal(LOC_CT);
    }
    
    @Override
    public boolean hasLifeSupport() {
        return getGoodCriticals(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT,
                LOC_HEAD) > 0;
    }
    
    /**
     * Used to determine modifier for landing.
     */
    @Override
    public int getNoseArmor() {
        return getArmor(LOC_CT);
    }

    @Override
    public int getAvionicsHits() {
        int hits = 0;
        for (int loc = 0; loc < locations(); loc++) {
            hits += getBadCriticals(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS, loc);
        }
        return hits;
    }
    
    @Override
    public int getSensorHits() {
        return getBadCriticals(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS, LOC_HEAD);
    }
    
    @Override
    public int getLeftThrustHits() {
        return 0;
    }
    
    @Override
    public int getRightThrustHits() {
        return 0;
    }
    
    @Override
    public void setGearHit(boolean hit) {
        if (hit) {
            List<CriticalSlot> gearSlots = new ArrayList<>();
            for (int loc = 0; loc < locations(); loc++) {
                for (int i = 0; i < crits[loc].length; i++) {
                    final CriticalSlot slot = crits[loc][i];
                    if (slot != null && slot.getType() == CriticalSlot.TYPE_SYSTEM
                            && slot.getIndex() == LAM_LANDING_GEAR
                            && !slot.isDestroyed()) {
                        gearSlots.add(slot);
                    }
                }
            }
            if (gearSlots.size() > 0) {
                int index = Compute.randomInt(gearSlots.size());
                gearSlots.get(index).setDestroyed(true);
            }
        }
    }
    
    /**
     * Modifier to landing or vertical takeoff roll for landing gear damage.
     * 
     * @param vTakeoff true if this is for a vertical takeoff, false if for a landing
     * @return the control roll modifier
     */
    public int getLandingGearMod(boolean vTakeoff) {
        int hits = 0;
        for (int loc = 0; loc < locations(); loc++) {
            hits += getBadCriticals(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR, loc);
        }
        if (vTakeoff) {
            return hits > 0? 1 : 0;
        } else {
            return hits > 3? 5 : hits;
        }
    }

    /**
     * In fighter mode the weapon arcs need to be translated to Aero arcs.
     */
    @Override
    public int getWeaponArc(int wn) {
        if (getConversionMode() != CONV_MODE_FIGHTER) {
            return super.getWeaponArc(wn);
        }
        final Mounted mounted = getEquipment(wn);
        if (mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB) || mounted.getType().hasFlag(WeaponType.F_DIVE_BOMB) || mounted.getType().hasFlag(WeaponType.F_ALT_BOMB)) {
            return Compute.ARC_360;
        }
        int arc = Compute.ARC_NOSE;
        switch (mounted.getLocation()) {
            case LOC_HEAD:
                arc = Compute.ARC_NOSE;
                break;
            case LOC_CT:
                if (mounted.isRearMounted()) {
                    arc = Compute.ARC_AFT;
                } else {
                    arc = Compute.ARC_NOSE;
                }
                break;
            case LOC_RT:
                if (mounted.isRearMounted()) {
                    arc = Compute.ARC_RWINGA;
                } else {
                    arc = Compute.ARC_RWING;
                }
                break;
            case LOC_LT:
                if (mounted.isRearMounted()) {
                    arc = Compute.ARC_LWINGA;
                } else {
                    arc = Compute.ARC_LWING;
                }
                break;
            case LOC_RARM:
                arc = Compute.ARC_RWING;
                break;
            case LOC_LARM:
                arc = Compute.ARC_LWING;
                break;
            case LOC_RLEG:
            case LOC_LLEG:
                arc = Compute.ARC_AFT;
                break;
            default:
                arc = Compute.ARC_360;
        }

        return rollArcs(arc);
    }

    /**
     * Hit location table for fighter mode
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        if (getConversionMode() != CONV_MODE_FIGHTER) {
            return super.rollHitLocation(table, side);
        }

        int roll = Compute.d6(2);

        // first check for above/below
        if ((table == ToHitData.HIT_ABOVE) || (table == ToHitData.HIT_BELOW)) {

            // have to decide which arm/leg
            int armloc = LOC_RARM;
            int legloc = LOC_RLEG;
            int wingroll = Compute.d6(1);
            if (wingroll > 3) {
                armloc = LOC_LARM;
                legloc = LOC_LLEG;
            }
            switch (roll) {
                case 2: case 6:
                    return new HitData(LOC_RT, false, HitData.EFFECT_NONE);
                case 3: case 4:
                case 10: case 11:
                    return new HitData(armloc, false, HitData.EFFECT_NONE);
                case 5: case 9:
                    return new HitData(legloc, false, HitData.EFFECT_NONE);
                case 7:
                    return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
                case 8: case 12:
                    return new HitData(LOC_LT, false, HitData.EFFECT_NONE);
            }
        }

        if (side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch (roll) {
                case 2: case 12:
                    return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
                case 3: case 6:
                    return new HitData(LOC_RT, false, HitData.EFFECT_NONE);
                case 4: case 5:
                    return new HitData(LOC_RARM, false, HitData.EFFECT_NONE);
                case 7:
                    return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
                case 8: case 11:
                    return new HitData(LOC_LT, false, HitData.EFFECT_NONE);
                case 9: case 10:
                    return new HitData(LOC_LARM, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_LEFT) {
            // normal left-side hits
            switch (roll) {
                case 2:
                    return new HitData(LOC_HEAD, false, HitData.EFFECT_NONE);
                case 3: case 7: case 11:
                    return new HitData(LOC_LARM, false, HitData.EFFECT_NONE);
                case 4: case 5:
                    return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
                case 6: case 8:
                    return new HitData(LOC_LT, false, HitData.EFFECT_NONE);
                case 9: case 10: case 12:
                    return new HitData(LOC_LLEG, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_RIGHT) {
            // normal right-side hits
            switch (roll) {
            case 2:
                return new HitData(LOC_HEAD, false, HitData.EFFECT_NONE);
            case 3: case 7: case 11:
                return new HitData(LOC_RARM, false, HitData.EFFECT_NONE);
            case 4: case 5:
                return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
            case 6: case 8:
                return new HitData(LOC_RT, false, HitData.EFFECT_NONE);
            case 9: case 10: case 12:
                return new HitData(LOC_RLEG, false, HitData.EFFECT_NONE);
        }
        } else if (side == ToHitData.SIDE_REAR) {
            // rear torso locations are only hit on a roll of 5-6 on d6
            boolean rear = Compute.d6() > 4;
            switch (roll) {
                case 2: case 12:
                    return new HitData(LOC_CT, rear, HitData.EFFECT_NONE);
                case 3: case 4:
                    return new HitData(LOC_RT, rear, HitData.EFFECT_NONE);
                case 5:
                    return new HitData(LOC_RARM, rear, HitData.EFFECT_NONE);
                case 6:
                    return new HitData(LOC_RLEG, rear, HitData.EFFECT_NONE);
                case 7:
                    if (Compute.d6() > 3) {
                        return new HitData(LOC_LLEG, rear, HitData.EFFECT_NONE);
                    } else {
                        return new HitData(LOC_RLEG, rear, HitData.EFFECT_NONE);
                    }
                case 8:
                    return new HitData(LOC_LLEG, rear, HitData.EFFECT_NONE);
                case 9:
                    return new HitData(LOC_LARM, rear, HitData.EFFECT_NONE);
                case 10: case 11:
                    return new HitData(LOC_LT, rear, HitData.EFFECT_NONE);
            }
        }
        return new HitData(LOC_CT, false, HitData.EFFECT_NONE);
    }
    
    @Override
    public void applyBombs() {
        //TODO: load the bays with the selected bombs
    }

    @Override
    public int getFuel() {
        return fuel;
    }

    /**
     * Sets the number of fuel points.
     * @param gas  Number of fuel points.
     */
    @Override
    public void setFuel(int gas) {
        fuel = gas;
    }

    @Override
    public double getFuelPointsPerTon(){
        return 80;
    }

    /**
     * Set number of fuel points based on fuel tonnage.
     *
     * @param fuelTons  The number of tons of fuel
     */
    @Override
    public void setFuelTonnage(double fuelTons){
        double pointsPerTon = getFuelPointsPerTon();
        fuel = (int)Math.ceil(pointsPerTon * fuelTons);
    }

    /**
     * Gets the fuel for this Aero in terms of tonnage.
     *
     * @return The number of tons of fuel on this Aero.
     */
    @Override
    public double getFuelTonnage(){
        return fuel / getFuelPointsPerTon();
    }
    
    /**
     * Exceeding damage threshold does not result in critical, but requires control roll.
     */
    @Override
    public int getThresh(int loc) {
        return getInternal(loc);
    }

    @Override
    public boolean wasCritThresh() {
        return critThresh;
    }

    @Override
    public void setCritThresh(boolean b) {
        critThresh = b;
    }
    
    @Override
    public boolean isSpheroid() {
        return false;
    }

    @Override
    public int getStraightMoves() {
        return straightMoves;
    }

    @Override
    public void setStraightMoves(int i) {
        straightMoves = i;
    }

    public int getAltLoss() {
        return altLoss;
    }

    public void setAltLoss(int i) {
        altLoss = i;
    }

    public void resetAltLoss() {
        altLoss = 0;
    }

    public int getAltLossThisRound() {
        return altLossThisRound;
    }

    public void setAltLossThisRound(int i) {
        altLossThisRound = i;
    }

    public void resetAltLossThisRound() {
        altLossThisRound = 0;
    }

    @Override
    public boolean isVSTOL() {
        return true;
    }

    @Override
    public boolean isSTOL() {
        return false;
    }

    @Override
    public int getElevation() {
        if ((game != null) && game.getBoard().inSpace()) {
            return 0;
        }
        // Altitude is not the same as elevation. If an aero is at 0 altitude,
        // then it is
        // grounded and uses elevation normally. Otherwise, just set elevation
        // to a very
        // large number so that a flying aero won't interact with the ground
        // maps in any way
        if (isAirborne()) {
            return 999;
        }
        return super.getElevation();
    }

    public int getFuelUsed(int thrust) {
        int overThrust = Math.max(thrust - getWalkMP(), 0);
        int safeThrust = thrust - overThrust;
        int used = safeThrust + (2 * overThrust);
        return used;
    }

    public boolean didFailManeuver() {
        return failedManeuver;
    }

    public void setFailedManeuver(boolean b) {
        failedManeuver = b;
    }

    public void setAccDecNow(boolean b) {
        accDecNow = b;
    }

    public boolean didAccDecNow() {
        return accDecNow;
    }

    @Override
    public void setBattleForceMovement(Map<String,Integer> movement) {
    	super.setBattleForceMovement(movement);
    	movement.put("g", getAirMechCruiseMP(true, false));
    	movement.put("a", getFighterModeWalkMP(true, false));
    }
    
    @Override
    public void setAlphaStrikeMovement(Map<String,Integer> movement) {
    	super.setBattleForceMovement(movement);
    	movement.put("g", getAirMechCruiseMP(true, false) * 2);
    	movement.put("a", getFighterModeWalkMP(true, false));
    }
    
    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        int bombs = (int)getEquipment().stream().filter(m -> m.getType().hasFlag(MiscType.F_BOMB_BAY))
                .count();
        if (bombs > 0) {
            specialAbilities.put(BattleForceSPA.BOMB, bombs / 5);
        }
        specialAbilities.put(BattleForceSPA.LAM, null);
    }

    @Override
    public String getTilesetModeString() {
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            return "_FIGHTER";
        } else if (getConversionMode() == CONV_MODE_AIRMECH) {
            return "_AIRMECH";
        } else {
            return "";
        }
    }
    
    @Override
    public boolean isAero() {
        return getConversionMode() == CONV_MODE_FIGHTER;
    }

    @Override
    public boolean canSpot() {
        if (getConversionMode() == CONV_MODE_FIGHTER) {
            return !isAirborne()
                    || hasWorkingMisc(MiscType.F_RECON_CAMERA)
                    || hasWorkingMisc(MiscType.F_INFRARED_IMAGER)
                    || hasWorkingMisc(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || (hasWorkingMisc(MiscType.F_HIRES_IMAGER)
                            && ((game.getPlanetaryConditions().getLight() == PlanetaryConditions.L_DAY)
                                    || (game.getPlanetaryConditions().getLight() == PlanetaryConditions.L_DUSK)));
        } else {
            return super.canSpot();
        }
    }


    public long getEntityType(){
        return Entity.ETYPE_MECH | Entity.ETYPE_BIPED_MECH | Entity.ETYPE_LAND_AIR_MECH;
    }
}
