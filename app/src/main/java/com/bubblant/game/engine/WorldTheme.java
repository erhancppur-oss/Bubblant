package com.bubblant.game.engine;

/**
 * Real-life settings for each world. The physics stay the same, but every world dresses
 * the hazards as something you would really find there (a drain instead of a whirlpool,
 * rubber ducks instead of fish, a frog instead of an eel...).
 */
public final class WorldTheme {
    // floors
    public static final int FLOOR_MUD = 0, FLOOR_STEEL = 1, FLOOR_PORCELAIN = 2, FLOOR_GRAVEL = 3,
            FLOOR_POND = 4, FLOOR_POOL = 5, FLOOR_MARBLE = 6, FLOOR_FIELD = 7, FLOOR_ROCK = 8,
            FLOOR_LAKE = 9, FLOOR_CONCRETE = 10, FLOOR_BASEMENT = 11;
    // walls
    public static final int WALL_STONE = 0, WALL_TILE = 1, WALL_WOOD = 2, WALL_PLANT = 3, WALL_CONCRETE = 4,
            WALL_ROCK = 5, WALL_SPONGE = 6, WALL_LANE = 7;
    // spiky things
    public static final int SPIKE_THORN = 0, SPIKE_PIN = 1, SPIKE_URCHIN = 2, SPIKE_NAIL = 3, SPIKE_BARNACLE = 4;
    // patrolling things
    public static final int PATROL_TADPOLE = 0, PATROL_GOLDFISH = 1, PATROL_DUCK = 2, PATROL_KOI = 3,
            PATROL_BALL = 4, PATROL_PIRANHA = 5, PATROL_BEETLE = 6, PATROL_SHRIMP = 7, PATROL_CAN = 8;
    // chasers
    public static final int CHASE_FROG = 0, CHASE_ROBOT = 1, CHASE_CRAB = 2, CHASE_RAT = 3, CHASE_EEL = 4, CHASE_FISH = 5;
    // vortex
    public static final int VORTEX_DRAIN = 0, VORTEX_WHIRL = 1;
    // goals
    public static final int GOAL_LEAF = 0, GOAL_SPONGE = 1, GOAL_SOAP = 2, GOAL_SHELL = 3, GOAL_LILY = 4,
            GOAL_RING = 5, GOAL_COIN_ROCK = 6, GOAL_STRAW = 7, GOAL_ROCK = 8, GOAL_LOG = 9, GOAL_PIPE = 10, GOAL_STAIRS = 11;

    public final int waterTop, waterBottom, waterAlpha;
    public final int floor, wall, wallBase, wallLight, wallDark;
    public final int spike, patrol, chaser, vortex, goal;
    public final boolean outdoor;
    public final int music; // MusicStyle ordinal hint (see MusicStyle.forTheme)

    private WorldTheme(int waterTop, int waterBottom, int waterAlpha, int floor, int wall, int wallBase, int wallLight, int wallDark,
                       int spike, int patrol, int chaser, int vortex, int goal, boolean outdoor, int music) {
        this.waterTop = waterTop;
        this.waterBottom = waterBottom;
        this.waterAlpha = waterAlpha;
        this.floor = floor;
        this.wall = wall;
        this.wallBase = wallBase;
        this.wallLight = wallLight;
        this.wallDark = wallDark;
        this.spike = spike;
        this.patrol = patrol;
        this.chaser = chaser;
        this.vortex = vortex;
        this.goal = goal;
        this.outdoor = outdoor;
        this.music = music;
    }

    // music hints: 0 happy, 1 breezy, 2 adventure, 3 mystery, 4 action, 5 horror
    public static final WorldTheme[] ALL = {
            // 1 Garden Puddle (after the rain)
            new WorldTheme(0xFF8FB9A8, 0xFF3E6B5A, 150, FLOOR_MUD, WALL_STONE, 0xFF7D7468, 0xFFB8AFA2, 0xFF3F3A33,
                    SPIKE_THORN, PATROL_TADPOLE, CHASE_FROG, VORTEX_WHIRL, GOAL_LEAF, true, 0),
            // 2 Kitchen Sink
            new WorldTheme(0xFFB9DDEB, 0xFF6E9DB3, 120, FLOOR_STEEL, WALL_SPONGE, 0xFFF2C94C, 0xFFFFE9A6, 0xFF8C6B1E,
                    SPIKE_PIN, PATROL_BEETLE, CHASE_FROG, VORTEX_DRAIN, GOAL_SPONGE, false, 0),
            // 3 Bathtub
            new WorldTheme(0xFFCFEFFC, 0xFF7FB8D6, 110, FLOOR_PORCELAIN, WALL_TILE, 0xFFE8F1F5, 0xFFFFFFFF, 0xFF8FA6B2,
                    SPIKE_PIN, PATROL_DUCK, CHASE_FROG, VORTEX_DRAIN, GOAL_SOAP, false, 1),
            // 4 Aquarium
            new WorldTheme(0xFF5FD0E8, 0xFF0F6B8C, 140, FLOOR_GRAVEL, WALL_PLANT, 0xFF2E7D32, 0xFF81C784, 0xFF103814,
                    SPIKE_URCHIN, PATROL_GOLDFISH, CHASE_FISH, VORTEX_WHIRL, GOAL_SHELL, false, 1),
            // 5 Garden Pond
            new WorldTheme(0xFF6FA88C, 0xFF1F4D3A, 160, FLOOR_POND, WALL_STONE, 0xFF6F6A5E, 0xFFA9A391, 0xFF2E2B25,
                    SPIKE_THORN, PATROL_KOI, CHASE_FROG, VORTEX_WHIRL, GOAL_LILY, true, 2),
            // 6 Swimming Pool
            new WorldTheme(0xFF7FE0F5, 0xFF1C8FC4, 110, FLOOR_POOL, WALL_LANE, 0xFFE53935, 0xFFFFFFFF, 0xFF7F1D1B,
                    SPIKE_PIN, PATROL_BALL, CHASE_ROBOT, VORTEX_DRAIN, GOAL_RING, true, 2),
            // 7 Park Fountain
            new WorldTheme(0xFF8ED1D6, 0xFF2A7480, 130, FLOOR_MARBLE, WALL_TILE, 0xFFD7CCC8, 0xFFF5F0EC, 0xFF6D5F5B,
                    SPIKE_NAIL, PATROL_KOI, CHASE_FROG, VORTEX_DRAIN, GOAL_COIN_ROCK, true, 2),
            // 8 Rice Paddy
            new WorldTheme(0xFF9DBB7A, 0xFF4B6B2E, 160, FLOOR_FIELD, WALL_WOOD, 0xFF8D6E4A, 0xFFC4A57A, 0xFF4A3622,
                    SPIKE_THORN, PATROL_SHRIMP, CHASE_FROG, VORTEX_WHIRL, GOAL_STRAW, true, 2),
            // 9 Tide Pool
            new WorldTheme(0xFF4FC1C8, 0xFF0D4F63, 150, FLOOR_ROCK, WALL_ROCK, 0xFF5D5753, 0xFF9C948C, 0xFF24201D,
                    SPIKE_URCHIN, PATROL_SHRIMP, CHASE_CRAB, VORTEX_WHIRL, GOAL_ROCK, true, 4),
            // 10 Thunderstorm Lake
            new WorldTheme(0xFF607D8B, 0xFF1A2A3A, 170, FLOOR_LAKE, WALL_WOOD, 0xFF5D4A3A, 0xFF8C7660, 0xFF261C14,
                    SPIKE_BARNACLE, PATROL_PIRANHA, CHASE_FISH, VORTEX_WHIRL, GOAL_LOG, true, 4),
            // 11 Storm Drain
            new WorldTheme(0xFF55635F, 0xFF151C1A, 180, FLOOR_CONCRETE, WALL_CONCRETE, 0xFF6B6F6C, 0xFF9EA39F, 0xFF2A2D2B,
                    SPIKE_NAIL, PATROL_CAN, CHASE_RAT, VORTEX_DRAIN, GOAL_PIPE, false, 3),
            // 12 Flooded Basement at night
            new WorldTheme(0xFF3A3550, 0xFF0A0812, 190, FLOOR_BASEMENT, WALL_CONCRETE, 0xFF4A4458, 0xFF77708A, 0xFF17141F,
                    SPIKE_NAIL, PATROL_CAN, CHASE_EEL, VORTEX_DRAIN, GOAL_STAIRS, false, 5),
    };

    public static WorldTheme of(int world) {
        return ALL[Math.max(0, Math.min(ALL.length - 1, world))];
    }
}
