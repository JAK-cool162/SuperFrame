# GLShader

Chunk light cache, shadow cubemaps, cascaded shadows, heightmap sun occlusion, and cheap GI for Minecraft Fabric.

Designed to feed into **SuperFrame** for FSR upscaling + FrameGen output.

- **Target:** Minecraft 1.21.1, Java 21 (1.21.11 compatible – see gradle.properties)
- **Dependencies:** Fabric API, Cloth Config, ModMenu (all required)
- **Optional / Compatible:** Sodium, Iris, SuperFrame
  - If Iris shader pack is active, GLShader disables itself automatically
  - SuperFrame integration: static world layer → SuperFrame render target, scale change invalidates shadow cache

---

## Systems (implemented in order)

### 1. CHUNK LIGHT CACHE SYSTEM ✅
- Access `LevelChunk` data via Mixin (`LevelChunkMixin`)
- Pre-calculate light zones per chunk
- Store as lightweight lightmap per chunk (`ChunkLightCache`)
- Invalidate cache only on block change event (`setBlockState` mixin)
- API: `LightCacheManager.getOrCreate(chunkPos, chunk)`

Files:
- `light/LightCacheManager.java`
- `light/ChunkLightCache.java`
- `mixin/LevelChunkMixin.java`

### 2. PER LIGHT SOURCE SHADOW CUBEMAP (planned)
- Each light source owns a 360° shadow cubemap
- Cached, only recalculates when block changes within light range
- Not recalculated every frame

### 3. CASCADED SHADOW RINGS (planned)
- Ring 1: 0-16 blocks = full resolution
- Ring 2: 16-48 blocks = 50% res
- Ring 3: 48-96 blocks = 25% res
- Ring 4: 96+ blocks = chunk blob only
- Farther rings update less frequently

### 4. STATIC WORLD vs ENTITY LAYER (planned)
- Static world renders at 25% res → handed to SuperFrame for FSR upscaling
- Entity layer renders at 75% res → bounding box shadows only → temporal blending
- Composite both layers before SuperFrame output

### 5. HEIGHTMAP SUN OCCLUSION (planned)
- Use chunk heightmap to block sunlight below surface
- Prevents light leaking through terrain/caves
- Cave openings create natural light shafts via heightmap gaps

### 6. FAKE GLOBAL ILLUMINATION (planned)
- Color bleeding from neighbor block colors
- Ambient occlusion derived from heightmap
- Light bounce approximation between chunks

---

## SuperFrame integration
- `compat/SuperFrameCompat.java` – reflection-based, optional runtime dep
- Register scale change listener → invalidate light cache
- Static world layer is designed to render into SuperFrame's scaled target

Install both mods:
- `superframe-0.1.0.jar`
- `glshader-0.1.0.jar`

---

## Building
```
cd glshader
./gradlew build
```
Output: `build/libs/glshader-0.1.0.jar`

For first build, copy the gradle wrapper from the parent SuperFrame project:
```
cp ../gradlew ../gradlew.bat ../gradle -r .
```

---

## License
MIT
