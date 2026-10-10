#!/usr/bin/env python3
"""Turn a saved Blockbench project into the game's model code and texture.

Usage (from the repo root; add --unrotated-ground for a model with rotated bones, see below):
    python3 tools/import_bbmodel.py topo
    python3 tools/import_bbmodel.py bunnay
    python3 tools/import_bbmodel.py <project.bbmodel> <model.java> <texture.png>

The first two use the known paths for that mob (see PRESETS). For anything else, pass the project, the model class to
rewrite (it needs the BEGIN/END GENERATED markers) and the texture to write.
It rewrites the generated block in the model class and the embedded texture. The texture has to be embedded in the
project, which Blockbench does when you save.
"""
import base64, io, json, math, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# (project, model class, texture), relative to the repo root.
PRESETS = {
    "topo": (
        "topo/art/topo.bbmodel",
        "topo/src/client/java/dev/jett/topomod/topo/client/render/TopoModel.java",
        "topo/src/main/resources/assets/topo/textures/entity/topo/topo.png",
    ),
    "bunnay": (
        "allay-variants/art/bunnay.bbmodel",
        "allay-variants/src/client/java/dev/jett/topomod/allayvariants/client/render/BunnayModel.java",
        "allay-variants/src/main/resources/assets/allay_variants/textures/entity/bunnay/bunnay.png",
    ),
}


# A model with rotated bones (a fox's tilted body, say) has cubes whose resting position is below the ground until the rotation is
# applied, so by default the lowest cube is not the ground. --unrotated-ground finds the ground from the cubes that are not under a rotated bone.
UNROTATED_GROUND = "--unrotated-ground" in sys.argv

def paths():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if len(args) == 1 and args[0] in PRESETS:
        return [os.path.join(ROOT, p) for p in PRESETS[args[0]]]
    if len(args) == 3:
        return args
    sys.exit(__doc__)


BBMODEL, MODEL_JAVA, TEXTURE = paths()

def jf(v):
    v = round(float(v), 4)
    return f"{0.0 if v == 0 else v}F"

def main():
    data = json.load(open(BBMODEL))
    elements = {e["uuid"]: e for e in data["elements"]}
    # Blockbench 5.x stores bone details (name, pivot, rotation) in "groups"; the outliner only
    # holds uuids. Older files put everything in the outliner. Support both.
    groups = {g["uuid"]: g for g in data.get("groups", [])}

    def resolve(node):
        if isinstance(node, dict) and "name" not in node and node.get("uuid") in groups:
            return {**groups[node["uuid"]], "children": node.get("children", [])}
        return node
    res = data.get("resolution", {})
    tex_w, tex_h = res.get("width", 64), res.get("height", 64)
    warnings = []
    lines = []
    infos = []

    def fixed_uv(e):
        u, v = e.get("uv_offset", [0, 0])
        if min(u, v) < 0:
            warnings.append(f"cube '{e['name']}' has a negative UV offset {[u, v]}; the texture wraps around the edge. Set its UV offset to 0 or more in Blockbench")
        return int(u), int(v)

    def region(e):
        f, t = e["from"], e["to"]
        w, h, d = math.ceil(t[0] - f[0]), math.ceil(t[1] - f[1]), math.ceil(t[2] - f[2])
        u, v = fixed_uv(e)
        return (u, v, u + 2 * d + 2 * w, v + d + h)

    # Snap the model to the ground: the lowest cube bottom (y up) becomes y = 0, like vanilla mobs.
    def unrotated_cubes():
        found = []
        def walk(node, rotated):
            node = resolve(node)
            if isinstance(node, str):
                if not rotated:
                    found.append(elements[node])
                return
            now = rotated or any(abs(r) > 1e-6 for r in node.get("rotation", [0, 0, 0]))
            for child in node.get("children", []):
                walk(child, now)
        for top in data["outliner"]:
            walk(top, False)
        return found
    ground_cubes = (unrotated_cubes() if UNROTATED_GROUND else []) or list(data["elements"])
    ground = min(e["from"][1] for e in ground_cubes)
    if abs(ground) > 1e-6:
        infos.append(f"model was {ground:g} px above the ground in Blockbench; lowered it to stand on the ground in game")

    seen_regions = {}
    for e in data["elements"]:
        r = region(e)
        for other_r, other in seen_regions.items():
            same_shape = other_r == r
            overlap = r[0] < other_r[2] and other_r[0] < r[2] and r[1] < other_r[3] and other_r[1] < r[3]
            if overlap and not same_shape:
                warnings.append(f"texture areas of '{e['name']}' and '{other}' overlap; painting one will change the other")
        seen_regions.setdefault(r, e["name"])

    def cube_call(e, origin, indent, warn_name):
        """addBox(...) text for a cube, with coordinates relative to `origin` (Blockbench space)."""
        if e.get("inflate"):
            warnings.append(f"cube '{e['name']}' uses inflate, which is ignored")
        if not e.get("box_uv", True):
            warnings.append(f"cube '{e['name']}' uses per-face UVs; switch it to box UV")
        u, v = fixed_uv(e)
        f, t = e["from"], e["to"]
        w, h, d = t[0] - f[0], t[1] - f[1], t[2] - f[2]
        ax, ay, az = f[0] - origin[0], origin[1] - t[1], f[2] - origin[2]
        mir = "true" if e.get("mirror_uv", warn_name) else "false"
        return f"\n{indent}\t\t.mirror({mir}).texOffs({int(u)}, {int(v)}).addBox({jf(ax)}, {jf(ay)}, {jf(az)}, {jf(w)}, {jf(h)}, {jf(d)})"

    def pose_text(rel, mc_rot):
        if any(abs(r) > 1e-6 for r in mc_rot):
            return f"PartPose.offsetAndRotation({jf(rel[0])}, {jf(rel[1])}, {jf(rel[2])}, {jf(mc_rot[0])}, {jf(mc_rot[1])}, {jf(mc_rot[2])})"
        return f"PartPose.offset({jf(rel[0])}, {jf(rel[1])}, {jf(rel[2])})"

    def to_mc_rot(rot):
        # Blockbench (y up) -> Minecraft model space (y down): x and z rotations negate
        return (math.radians(-rot[0]), math.radians(rot[1]), math.radians(-rot[2]))

    def emit(node, parent_var, parent_origin, indent="\t\t"):
        node = resolve(node)
        name = node["name"]
        origin = node["origin"]                           # Blockbench space: y up
        pivot = (origin[0], 24 - origin[1], origin[2])    # Minecraft model space: y down
        rel = [pivot[i] - parent_origin[i] for i in range(3)]
        if parent_var == "root":
            rel[1] += ground
        group_mirror = node.get("mirror_uv", False)

        plain, rotated, kids = [], [], []
        for child in node.get("children", []):
            if isinstance(child, str):
                e = elements[child]
                (rotated if any(e.get("rotation", [0, 0, 0])) else plain).append(e)
            else:
                kids.append(child)

        builder = "CubeListBuilder.create()"
        for e in plain:
            builder += cube_call(e, origin, indent, group_mirror)

        var = re.sub(r"\W", "_", name)
        pose = pose_text(rel, to_mc_rot(node.get("rotation", [0, 0, 0])))
        has_children = bool(kids or rotated)
        head = f"PartDefinition {var} = " if has_children else ""
        lines.append(f'{indent}{head}{parent_var}.addOrReplaceChild("{name}",\n{indent}\t{builder},\n{indent}\t{pose});')

        # A cube with its own rotation can't be rotated alone in model code, so it becomes a
        # small child bone pivoting at the cube's own origin (this is what Blockbench's exporter does).
        for i, e in enumerate(rotated):
            eo = e.get("origin", origin)
            erel = [eo[0] - origin[0], -(eo[1] - origin[1]), eo[2] - origin[2]]
            child_name = f"{re.sub(r'\W', '_', e['name'])}_r{i + 1}"
            body = "CubeListBuilder.create()" + cube_call(e, eo, indent, group_mirror)
            lines.append(f'{indent}{var}.addOrReplaceChild("{child_name}",\n{indent}\t{body},\n{indent}\t{pose_text(erel, to_mc_rot(e["rotation"]))});')

        for k in kids:
            emit(k, var, pivot, indent)

    for top in data["outliner"]:
        if isinstance(top, str):
            warnings.append("a cube is outside any bone group and was skipped")
            continue
        emit(top, "root", (0, 0, 0))

    java = open(MODEL_JAVA).read()
    new, n = re.subn(r"(// BEGIN GENERATED[^\n]*\n).*?(\t\t// END GENERATED)",
                     lambda m: m.group(1) + "\n".join(lines) + "\n" + m.group(2), java, flags=re.S)
    if n != 1:
        sys.exit("could not find the BEGIN/END GENERATED markers in " + MODEL_JAVA + "")
    new = re.sub(r"LayerDefinition\.create\(mesh, \d+, \d+\)", f"LayerDefinition.create(mesh, {tex_w}, {tex_h})", new)
    open(MODEL_JAVA, "w").write(new)

    tex = data.get("textures") or []
    src = tex[0].get("source", "") if tex else ""
    if src.startswith("data:image/png;base64,"):
        png = base64.b64decode(src.split(",", 1)[1])
        open(TEXTURE, "wb").write(png)
        print("texture updated")
    else:
        warnings.append("no embedded texture found in the project; texture NOT updated")

    names = [resolve(n)["name"] for n in data["outliner"] if not isinstance(n, str)]
    print("bones:", ", ".join(names))
    for i in infos:
        print("NOTE:", i)
    for w in warnings:
        print("WARNING:", w)

if __name__ == "__main__":
    main()
