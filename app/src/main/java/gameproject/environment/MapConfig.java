package gameproject.environment;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import gameproject.state.PlayingState.EventType;

public class MapConfig {
    public final MapType type;
    public final String name;
    public final String backgroundKey;
    public final String thumbnailKey;
    public final int maxBuildings;
    public final double obstacleDensity;
    public final List<EventType> possibleEvents;
    public final Color overlayColor;
    public final Color minimapWaterColor;
    public final String description;

    // Pools for monsters (keys for spawning)
    public final List<String> normalEnemies;
    public final List<String> eliteEnemies;
    public final int bossIndex; // 1 to 5
    public final int mapId;

    public MapConfig(MapType type, String name, String backgroundKey, String thumbnailKey, int maxBuildings,
            double obstacleDensity,
            List<EventType> possibleEvents, Color overlayColor, Color minimapWaterColor, String description,
            List<String> normalEnemies, List<String> eliteEnemies, int bossIndex, int mapId) {
        this.type = type;
        this.name = name;
        this.backgroundKey = backgroundKey;
        this.thumbnailKey = thumbnailKey;
        this.maxBuildings = maxBuildings;
        this.obstacleDensity = obstacleDensity;
        this.possibleEvents = possibleEvents;
        this.overlayColor = overlayColor;
        this.minimapWaterColor = minimapWaterColor;
        this.description = description;
        this.normalEnemies = normalEnemies;
        this.eliteEnemies = eliteEnemies;
        this.bossIndex = bossIndex;
        this.mapId = mapId;
    }

    public static MapConfig getConfig(MapType type) {
        Color defaultWater = new Color(64, 164, 223, 200);
        switch (type) {
            case FROZEN_TUNDRA:
                return new MapConfig(
                        MapType.FROZEN_TUNDRA,
                        "Frozen Tundra",
                        "background1",
                        "thumbnail",
                        2,
                        0.07,
                        Arrays.asList(EventType.DARKNESS, EventType.BLOOD_MOON),
                        new Color(180, 220, 255, 60),
                        defaultWater,
                        "A frozen wasteland where the cold is as deadly as the monsters.",
                        Arrays.asList("normal", "wizard"),
                        Arrays.asList("wizard"),
                        2, 3);
            case SWAMP:
                return new MapConfig(
                        MapType.SWAMP,
                        "Ancient Swamp",
                        "swamp_background",
                        "thumbnail1",
                        0, // maxBuildings
                        0.08, // obstacleDensity (Tăng từ 0.0 để sinh cây/đá)
                        Arrays.asList(EventType.DARKNESS, EventType.BLOOD_MOON, EventType.TOXIC_WATERS,
                                EventType.ALTAR_ASCENSION),
                        new Color(100, 120, 50, 70),
                        new Color(40, 60, 20, 220), // Xanh rêu đậm cho Swamp
                        "An ancient swamp filled with murky waters and hidden secrets. Seek out the mysterious Altar to trigger powerful world events.",
                        Arrays.asList("normal", "normal", "normal", "shooter", "wizard", "assassin", "spawner"),
                        Arrays.asList("shooter", "wizard", "assassin", "spawner"),
                        5, 1);
            case FACTORY:
                return new MapConfig(
                        MapType.FACTORY,
                        "Abandoned Factory",
                        "background1",
                        "thumbnail",
                        15,
                        0.04,
                        Arrays.asList(EventType.ACID_RAIN, EventType.MIMIC_MANIA, EventType.BLOOD_MOON),
                        new Color(80, 80, 90, 50),
                        defaultWater,
                        "An industrial complex full of steel and rust. Perfect for ambushes.",
                        Arrays.asList("normal", "shooter"),
                        Arrays.asList("shooter"),
                        1, 2);
            case CURSED_FOREST:
                return new MapConfig(
                        MapType.CURSED_FOREST,
                        "Cursed Forest",
                        "background1",
                        "thumbnail",
                        5,
                        0.15,
                        Arrays.asList(EventType.DARKNESS, EventType.BLOOD_MOON),
                        new Color(20, 80, 20, 60),
                        defaultWater,
                        "The trees themselves seem to watch you. High monster density.",
                        Arrays.asList("normal", "assassin", "wizard"),
                        Arrays.asList("assassin"),
                        4, 4);
            case OUTSKIRTS:
            default:
                return new MapConfig(
                        MapType.OUTSKIRTS,
                        "Quiet Outskirts",
                        "background1",
                        "thumbnail",
                        6,
                        0.05,
                        Arrays.asList(EventType.ACID_RAIN, EventType.DARKNESS, EventType.MIMIC_MANIA),
                        new Color(0, 0, 0, 0),
                        defaultWater,
                        "A suburban area with few buildings and plenty of open space.",
                        Arrays.asList("normal", "shooter", "assassin"),
                        Arrays.asList("normal", "shooter"),
                        5, 0);
        }
    }
}
