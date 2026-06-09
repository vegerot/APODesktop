from PIL import Image, ImageOps, ImageDraw
import os

res_root = "/home/max/workspace/github.com/vegerot/apodesktop/android/app/src/main/res"
bg_path = "/home/max/.gemini/antigravity/brain/0702bf4b-5b2b-4ab6-83b2-5bfd2367e3cb/apod_icon_background_1780984723120.png"
fg_path = "/home/max/.gemini/antigravity/brain/0702bf4b-5b2b-4ab6-83b2-5bfd2367e3cb/apod_icon_foreground_1780984738991.png"

# Densities and sizes (canvas_size, safe_zone_size, legacy_size)
densities = {
    "mipmap-mdpi": (108, 72, 48),
    "mipmap-hdpi": (162, 108, 72),
    "mipmap-xhdpi": (216, 144, 96),
    "mipmap-xxhdpi": (324, 216, 144),
    "mipmap-xxxhdpi": (432, 288, 192)
}

# 1. Process Foreground (Key out black background to keep cyan glow transparent)
print("Processing foreground icon...")
fg_img = Image.open(fg_path).convert("RGBA")
datas = fg_img.getdata()

new_datas = []
for item in datas:
    r, g, b, a = item
    v = max(r, g, b)
    if v < 15:
        alpha = 0
    else:
        # Boost alpha slightly to keep lines solid, while preserving glow transparency
        alpha = min(255, int(v * 1.5))
    new_datas.append((r, g, b, alpha))

fg_img.putdata(new_datas)

# 2. Load Background
bg_img = Image.open(bg_path).convert("RGBA")

# Helper to create a circular mask
def create_circular_mask(size):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    return mask

# 3. Generate all assets
for folder, (canvas_sz, safe_sz, legacy_sz) in densities.items():
    dest_dir = os.path.join(res_root, folder)
    os.makedirs(dest_dir, exist_ok=True)
    
    # Resize background layer
    bg_resized = bg_img.resize((canvas_sz, canvas_sz), Image.Resampling.LANCZOS)
    bg_resized.save(os.path.join(dest_dir, "ic_launcher_background.webp"), "WEBP")
    
    # Resize and center foreground layer
    fg_resized = fg_img.resize((safe_sz, safe_sz), Image.Resampling.LANCZOS)
    fg_canvas = Image.new("RGBA", (canvas_sz, canvas_sz), (0, 0, 0, 0))
    offset = (canvas_sz - safe_sz) // 2
    fg_canvas.paste(fg_resized, (offset, offset), fg_resized)
    fg_canvas.save(os.path.join(dest_dir, "ic_launcher_foreground.webp"), "WEBP")
    
    # Create combined flat icon (legacy)
    flat_canvas = bg_resized.copy()
    flat_canvas.paste(fg_canvas, (0, 0), fg_canvas)
    
    # Save legacy square icon
    legacy_square = flat_canvas.resize((legacy_sz, legacy_sz), Image.Resampling.LANCZOS)
    legacy_square.save(os.path.join(dest_dir, "ic_launcher.webp"), "WEBP")
    
    # Save legacy round icon (using a circular mask)
    round_mask = create_circular_mask(legacy_sz)
    legacy_round = Image.new("RGBA", (legacy_sz, legacy_sz), (0, 0, 0, 0))
    legacy_round.paste(legacy_square, (0, 0), round_mask)
    legacy_round.save(os.path.join(dest_dir, "ic_launcher_round.webp"), "WEBP")
    
    print(f"Generated assets for {folder}")

print("All icons successfully generated!")
