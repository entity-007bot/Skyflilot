package com.example.sim.world

import com.example.sim.math.Vector3
import com.example.sim.physics.BoundingBox
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MovingEntity(
    val id: String,
    val type: EntityType,
    var position: Vector3,
    var headingDeg: Float,
    val speedMps: Float,
    val waypoints: List<Vector3>,
    var currentWaypointIndex: Int = 0,
    val scale: Float = 1.0f
) {
    fun update(dt: Float) {
        if (waypoints.isEmpty()) return
        val target = waypoints[currentWaypointIndex]
        val dx = target.x - position.x
        val dz = target.z - position.z
        val dist = sqrt(dx * dx + dz * dz)

        if (dist < 3.0f) {
            currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size
        } else {
            val step = minOf(dist, speedMps * dt)
            val dirX = dx / dist
            val dirZ = dz / dist
            position.x += dirX * step
            position.z += dirZ * step
            headingDeg = Math.toDegrees(kotlin.math.atan2(dirX.toDouble(), dirZ.toDouble())).toFloat()
        }
    }
}

enum class EntityType {
    FISHING_BOAT,
    CARGO_SHIP,
    TUG_BOAT,
    CAR,
    TRUCK,
    PEDESTRIAN,
    ELEPHANT,
    ZEBRA,
    SEAGULL
}

class CoastalWorld {
    // 3km x 3km boundary
    val worldSizeMeters = 3000f

    // Moving life entities
    val movingEntities = mutableListOf<MovingEntity>()

    // Airfield training gates preserved for tutorial lessons
    val flightGates = listOf(
        FlightGate(1, "Gate 1 (Approach)", Vector3(0f, 2.5f, 15f), width = 4f, height = 3.5f),
        FlightGate(2, "Gate 2 (East Bank)", Vector3(25f, 3.2f, 35f), width = 4.2f, height = 3.8f),
        FlightGate(3, "Gate 3 (Apex Turn)", Vector3(0f, 3.0f, 60f), width = 4f, height = 3.5f),
        FlightGate(4, "Gate 4 (West Chute)", Vector3(-25f, 2.8f, 35f), width = 4.2f, height = 3.8f)
    )

    // Landing pads
    val landingPads = listOf(
        LandingPad("pad_airfield", "Airfield Launch Pad", Vector3(0f, 0.05f, 0f), radius = 3.5f, colorHex = 0xFF00E5FF),
        LandingPad("pad_village", "Fishing Jetty Pad", Vector3(-420f, 0.5f, -60f), radius = 2.5f, colorHex = 0xFFFF9100),
        LandingPad("pad_harbour", "Market Pier Pad", Vector3(0f, 1.0f, -70f), radius = 2.5f, colorHex = 0xFF00E676),
        LandingPad("pad_rig", "Oil Rig Helideck", Vector3(0f, 24.5f, -1250f), radius = 4.0f, colorHex = 0xFFFFD600),
        LandingPad("pad_teahouse", "Hillside Tea Pavilion Pad", Vector3(20f, 85.0f, 820f), radius = 2.5f, colorHex = 0xFF00E5FF),
        LandingPad("pad_lighthouse", "Lighthouse Bluff Pad", Vector3(-380f, 22.0f, 340f), radius = 2.5f, colorHex = 0xFFFF9100)
    )

    // Collision volumes
    val colliders = mutableListOf<BoundingBox>()

    // Lighthouse rotating beam
    var lighthouseBeamAngleDeg = 0f

    // Oil rig gas flare flame flicker
    var flareFlameScale = 1.0f

    // Animated water tide height
    var waterLevel = 0.0f
    private var tideTimer = 0f

    init {
        initColliders()
        initMovingEntities()
    }

    private fun initColliders() {
        // Airfield Hangar
        colliders.add(BoundingBox(-50f, -25f, 0f, 10f, -20f, 20f, "Airfield Hangar"))

        // Oil Rig Platform Structure (1.25 km out to sea, 24m high deck)
        colliders.add(BoundingBox(-35f, 35f, 0f, 24f, -1285f, -1215f, "Oil Rig Main Platform"))
        colliders.add(BoundingBox(-8f, 8f, 24f, 52f, -1258f, -1242f, "Oil Rig Derrick Tower"))

        // Commercial Port Warehouses & Container Stacks
        colliders.add(BoundingBox(220f, 340f, 0f, 12f, -180f, -80f, "Port Warehouse A"))
        colliders.add(BoundingBox(360f, 480f, 0f, 12f, -180f, -80f, "Port Warehouse B"))
        colliders.add(BoundingBox(240f, 420f, 0f, 9f, -70f, 10f, "Container Stack Terminal"))

        // Gantry Cranes
        colliders.add(BoundingBox(280f, 310f, 0f, 32f, -240f, -210f, "Port Gantry Crane 1"))
        colliders.add(BoundingBox(380f, 410f, 0f, 32f, -240f, -210f, "Port Gantry Crane 2"))

        // Lighthouse
        colliders.add(BoundingBox(-385f, -375f, 18f, 46f, 335f, 345f, "Headland Lighthouse"))

        // Resort Hotel
        colliders.add(BoundingBox(-340f, -260f, 5f, 28f, 220f, 290f, "Coastal Resort Hotel"))

        // Tea House
        colliders.add(BoundingBox(5f, 35f, 82f, 96f, 805f, 835f, "Hillside Tea House"))

        // Military Base Hangars & Radar
        colliders.add(BoundingBox(520f, 620f, 0f, 14f, 180f, 250f, "Military Hangar 1"))
        colliders.add(BoundingBox(520f, 620f, 0f, 14f, 270f, 340f, "Military Hangar 2"))
        colliders.add(BoundingBox(660f, 690f, 0f, 26f, 220f, 250f, "Military Radar Dome"))

        // Mine Processing Plant
        colliders.add(BoundingBox(-620f, -520f, 15f, 38f, 520f, 620f, "Mine Processing Plant"))
    }

    private fun initMovingEntities() {
        // 1. Moving Fishing Boats with wakes
        movingEntities.add(
            MovingEntity(
                id = "boat_fisher_1",
                type = EntityType.FISHING_BOAT,
                position = Vector3(-400f, 0f, -80f),
                headingDeg = 180f,
                speedMps = 4.2f,
                waypoints = listOf(
                    Vector3(-400f, 0f, -80f),
                    Vector3(-350f, 0f, -250f),
                    Vector3(-200f, 0f, -400f),
                    Vector3(-300f, 0f, -250f)
                ),
                scale = 1.2f
            )
        )
        movingEntities.add(
            MovingEntity(
                id = "boat_fisher_2",
                type = EntityType.FISHING_BOAT,
                position = Vector3(-280f, 0f, -120f),
                headingDeg = 210f,
                speedMps = 3.8f,
                waypoints = listOf(
                    Vector3(-280f, 0f, -120f),
                    Vector3(-180f, 0f, -320f),
                    Vector3(-100f, 0f, -200f)
                ),
                scale = 1.0f
            )
        )

        // 2. Large Cargo Ship in shipping lane
        movingEntities.add(
            MovingEntity(
                id = "ship_cargo",
                type = EntityType.CARGO_SHIP,
                position = Vector3(450f, 0f, -600f),
                headingDeg = 270f,
                speedMps = 5.5f,
                waypoints = listOf(
                    Vector3(650f, 0f, -600f),
                    Vector3(150f, 0f, -600f),
                    Vector3(-450f, 0f, -750f),
                    Vector3(650f, 0f, -600f)
                ),
                scale = 3.5f
            )
        )

        // 3. Tugboat near port
        movingEntities.add(
            MovingEntity(
                id = "boat_tug",
                type = EntityType.TUG_BOAT,
                position = Vector3(280f, 0f, -350f),
                headingDeg = 90f,
                speedMps = 4.0f,
                waypoints = listOf(
                    Vector3(280f, 0f, -350f),
                    Vector3(420f, 0f, -380f),
                    Vector3(350f, 0f, -280f)
                ),
                scale = 1.4f
            )
        )

        // 4. Highway traffic: Cars and Trucks
        movingEntities.add(
            MovingEntity(
                id = "traffic_bus_1",
                type = EntityType.TRUCK,
                position = Vector3(-450f, 2f, 120f),
                headingDeg = 90f,
                speedMps = 14f,
                waypoints = listOf(
                    Vector3(-600f, 2f, 120f),
                    Vector3(0f, 2f, 120f),
                    Vector3(500f, 2f, 120f),
                    Vector3(-600f, 2f, 120f)
                ),
                scale = 1.2f
            )
        )
        movingEntities.add(
            MovingEntity(
                id = "traffic_car_1",
                type = EntityType.CAR,
                position = Vector3(-200f, 2f, 125f),
                headingDeg = 90f,
                speedMps = 18f,
                waypoints = listOf(
                    Vector3(-500f, 2f, 125f),
                    Vector3(400f, 2f, 125f),
                    Vector3(-500f, 2f, 125f)
                ),
                scale = 1.0f
            )
        )

        // 5. Pedestrians at Harbour Market
        movingEntities.add(
            MovingEntity(
                id = "pedestrian_1",
                type = EntityType.PEDESTRIAN,
                position = Vector3(-20f, 1.0f, -40f),
                headingDeg = 0f,
                speedMps = 1.2f,
                waypoints = listOf(
                    Vector3(-20f, 1.0f, -40f),
                    Vector3(30f, 1.0f, -40f),
                    Vector3(30f, 1.0f, -10f),
                    Vector3(-20f, 1.0f, -10f)
                ),
                scale = 0.8f
            )
        )

        // 6. Wildlife in reserve: Elephants & Zebras
        movingEntities.add(
            MovingEntity(
                id = "wildlife_elephant",
                type = EntityType.ELEPHANT,
                position = Vector3(520f, 12f, 720f),
                headingDeg = 45f,
                speedMps = 1.5f,
                waypoints = listOf(
                    Vector3(520f, 12f, 720f),
                    Vector3(580f, 12f, 760f),
                    Vector3(540f, 12f, 800f),
                    Vector3(480f, 12f, 750f)
                ),
                scale = 1.8f
            )
        )
        movingEntities.add(
            MovingEntity(
                id = "wildlife_zebra",
                type = EntityType.ZEBRA,
                position = Vector3(440f, 10f, 680f),
                headingDeg = 120f,
                speedMps = 2.5f,
                waypoints = listOf(
                    Vector3(440f, 10f, 680f),
                    Vector3(490f, 10f, 640f),
                    Vector3(430f, 10f, 620f)
                ),
                scale = 1.1f
            )
        )
    }

    fun update(dt: Float) {
        // Update tide
        tideTimer += dt
        waterLevel = (sin(tideTimer * 0.05f) * 0.35f)

        // Update lighthouse sweep (30 RPM = 180 deg/s)
        lighthouseBeamAngleDeg = (lighthouseBeamAngleDeg + dt * 180f) % 360f

        // Flare stack flame flickering
        flareFlameScale = 0.85f + (sin(tideTimer * 14f) * 0.15f)

        // Update all moving traffic, ships, pedestrians, and animals
        for (entity in movingEntities) {
            entity.update(dt)
        }
    }
}
